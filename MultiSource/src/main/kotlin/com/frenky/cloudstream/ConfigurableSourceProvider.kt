package com.frenky.cloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.net.URL

/**
 * Public catalogue and standard compatible player integration.
 * No DRM or access-control circumvention is implemented.
 */
open class ConfigurableSourceProvider(private val source: Source) : MainAPI() {
    override var mainUrl: String = source.fallbackUrl
    override var name: String = source.displayName
    override var lang: String = "it"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    // CloudStream hides providers without a main page from the home source selector.
    // This metadata-only provider exposes an empty home page so it can be selected
    // and searched without pretending to have a browsable video catalogue.
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return newHomePageResponse(request, emptyList(), false)
    }

    /**
     * A site can index "Sentinel", "The Sentinel" or "The Sentinel (1996)".
     * Match actual titles rather than arbitrary substrings, otherwise e.g.
     * "Preacher" can be mistaken for "Reacher".
     */
    private fun normalized(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("""[\u0300-\u036f]"""), "")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun withoutArticle(value: String): String {
        val text = normalized(value)
        val articles = listOf("the ", "an ", "a ", "il ", "lo ", "la ", "i ", "gli ", "le ", "un ", "uno ", "una ")
        return articles.firstOrNull { text.startsWith(it) }?.let { text.removePrefix(it) } ?: text
    }

    private fun simplified(value: String): String {
        val metadata = setOf("serie", "series", "tv", "film", "stagione", "season",
            "streaming", "hd", "ita", "subita")
        return withoutArticle(value).split(" ")
            .filter { it.isNotBlank() && it !in metadata &&
                !it.matches(Regex("""(?:19|20)\d{2}""")) }
            .joinToString(" ")
    }

    private fun titleScore(query: String, candidate: String): Int {
        val expected = withoutArticle(query)
        val actual = withoutArticle(candidate)
        if (expected.isBlank() || actual.isBlank()) return 0
        if (expected == actual) return 100
        val expectedSimple = simplified(query)
        val actualSimple = simplified(candidate)
        if (expectedSimple.length >= 3 && expectedSimple == actualSimple) return 95
        // Permit a nearly complete last word, not unrelated titles containing the query.
        val left = expectedSimple.split(" ")
        val right = actualSimple.split(" ")
        if (expectedSimple.split(" ").all { it in actualSimple.split(" ") }) return 70
        if (left.size != right.size || left.isEmpty()) return 0
        if (left.dropLast(1) != right.dropLast(1)) return 0
        val requestedLast = left.last()
        val foundLast = right.last()
        return if (requestedLast.length >= 4 && foundLast.startsWith(requestedLast) &&
            requestedLast.length * 10 >= foundLast.length * 7) 65 else 0
    }

    private fun queryVariants(query: String): List<String> {
        val original = query.trim().take(80)
        val noYear = original.replace(
            Regex("""\s*[\[(]?(?:19|20)\d{2}[\])]?\s*$"""), ""
        ).trim()
        return linkedSetOf(original, noYear, withoutArticle(noYear))
            .filter { it.length >= 3 }
            .take(3)
    }

    private val recentPosters = LinkedHashMap<String, String>()

    fun posterFor(url: String): String? = synchronized(recentPosters) { recentPosters[url] }

    private fun rememberPoster(url: String, poster: String?) {
        if (poster.isNullOrBlank()) return
        synchronized(recentPosters) {
            recentPosters[url] = poster
            while (recentPosters.size > 100) recentPosters.remove(recentPosters.keys.first())
        }
    }

    private data class CatalogueHit(
        val title: String,
        val url: String,
        val poster: String?,
        val score: Int
    )

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.trim().length < 2 || source.searchMode == SearchMode.NONE)
            return emptyList()
        mainUrl = DomainRegistry.resolve(source.id, source.fallbackUrl)
        val expectedHost = runCatching { URI(mainUrl).host?.lowercase(Locale.ROOT)
            ?.removePrefix("www.") }.getOrNull() ?: return emptyList()
        val hits = LinkedHashMap<String, CatalogueHit>()
        var responded = false

        for (variant in queryVariants(query)) {
            val encoded = URLEncoder.encode(variant, "UTF-8")
            val searchUrl = when (source.searchMode) {
                SearchMode.WORDPRESS -> mainUrl.trimEnd('/') + "/?s=" + encoded
                SearchMode.WORDPRESS_ROOT -> URI(mainUrl).let {
                    it.scheme + "://" + it.host + "/?s=" + encoded
                }
                SearchMode.DLE_GET -> mainUrl.trimEnd('/') +
                    "/?do=search&subaction=search&story=" + encoded
                SearchMode.NONE -> return emptyList()
            }
            val document = try {
                Jsoup.parse(getHtml(searchUrl), searchUrl).also { responded = true }
            } catch (error: Exception) {
                Log.w("FrenkyCloudStream", "Search failed for ${source.id}: ${error.javaClass.simpleName}")
                continue // Failed variant must not prevent fallback to another spelling.
            }
            for (anchor in document.select("a[href]")) {
                val url = anchor.absUrl("href")
                val uri = runCatching { URI(url) }.getOrNull() ?: continue
                if (!uri.scheme.equals("https", true) ||
                    uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") != expectedHost ||
                    uri.path.isNullOrBlank() || uri.path == "/") continue
                val img = anchor.selectFirst("img") ?: anchor.parent()?.selectFirst("img")
                val card = anchor.closest("article, .post, .posts, .item, .ml-item, .result, .search-result")
                val options = listOf(
                    anchor.attr("title"),
                    anchor.attr("aria-label"),
                    anchor.attr("oldtitle"),
                    card?.selectFirst("h1,h2,h3,h4,.entry-title,.post-title,.title,.mli-info h2")?.text().orEmpty(),
                    anchor.selectFirst("h1,h2,h3,h4")?.text().orEmpty(),
                    img?.attr("alt").orEmpty(),
                    anchor.text()
                ).map { it.trim() }.filter { it.length in 2..160 }
                val title = options.maxByOrNull { titleScore(query, it) } ?: continue
                val score = titleScore(query, title)
                if (score < 60) continue
                val poster = PosterFinder.fromSearchCard(anchor, card)
                rememberPoster(url, poster)
                val item = CatalogueHit(title, url, poster, score)
                if (hits[url] == null || hits[url]!!.score < score) hits[url] = item
            }
            // An exact title is already found: no unnecessary extra network requests.
            if (hits.values.any { it.score >= 95 }) break
        }
        check(responded) { "Fonte temporaneamente non disponibile" }
        return hits.values
            .sortedWith(compareByDescending<CatalogueHit> { it.score }
                .thenBy { it.title.length })
            .take(40)
            .map { hit ->
                val isSeries = URI(hit.url).path.contains("serie", ignoreCase = true)
                if (isSeries) {
                    newTvSeriesSearchResponse(hit.title, hit.url, TvType.TvSeries) {
                        posterUrl = hit.poster
                    }
                } else {
                    newMovieSearchResponse(hit.title, hit.url, TvType.Movie) {
                        posterUrl = hit.poster
                    }
                }
            }
    }

    override suspend fun load(url: String): LoadResponse {
        // CloudStream can load a previously found search result after a
        // provider instance has been recreated and mainUrl reset to fallback.
        mainUrl = DomainRegistry.resolve(source.id, source.fallbackUrl)
        val expected = URI(mainUrl).host?.lowercase(Locale.ROOT)?.removePrefix("www.")
        val itemUri = URI(url)
        val itemHost = itemUri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")
        val knownHosts = setOfNotNull(expected)
        if (!itemUri.scheme.equals("https", ignoreCase = true) ||
            itemUri.userInfo != null || itemUri.port !in setOf(-1, 443) ||
            itemHost == null || itemHost !in knownHosts)
            throw IllegalArgumentException("URL fuori dal provider")
        val doc: Document = Jsoup.parse(getHtml(url), url)
        val title = doc.selectFirst("h1, .mvi-title, #mv-info h3, .title-film")?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: "Scheda del catalogo"
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst("meta[name=description]")?.attr("content")
            ?: ""
        val plotNode = doc.selectFirst(".mvic-desc .desc, .fullstory, .full-text, .description, .text")?.clone()
        plotNode?.select(".simplesocialbuttons, script, style")?.remove()
        val plotText = plotNode?.text()
            ?.takeIf { it.isNotBlank() } ?: description
        val poster = PosterFinder.fromDocument(doc, title) ?: posterFor(url)
        val episodes = doc.select(".tvseason .les-content a[href], a[href*=/episodio/]")
            .distinctBy { it.absUrl("href") }.mapNotNull { anchor ->
                val episodeUrl = anchor.absUrl("href")
                val match = Regex("stagione-(\\d+)-episodio-(\\d+)").find(episodeUrl)
                    ?: return@mapNotNull null
                newEpisode(episodeUrl) {
                    name = anchor.text()
                    season = match.groupValues[1].toIntOrNull()
                    episode = match.groupValues[2].toIntOrNull()
                }
            }
        val isSeries = episodes.isNotEmpty() || URI(url).path.contains("serie", true) ||
            Regex("""var\s+SERIES\s*=\s*1""").containsMatchIn(doc.select("script").joinToString("\n") { it.data() })
        return if (isSeries) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plotText
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plotText
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        mainUrl = DomainRegistry.resolve(source.id, source.fallbackUrl)
        val uri = runCatching { URI(data) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.host != URI(mainUrl).host) return false
        val html = runCatching { getHtml(data) }.getOrNull() ?: return false
        val doc = Jsoup.parse(html, data)
        if (doc.select(".g-recaptcha, .h-captcha, input[type=password]").isNotEmpty()) return false
        val embeds = doc.select("#content-embed iframe[src], .movieplay iframe[src], .player iframe[src], iframe[data-src]")
            .map { it.absUrl(if (it.hasAttr("data-src")) "data-src" else "src") }
            .filter { runCatching { URI(it).scheme == "https" }.getOrDefault(false) }
            .distinct().toMutableList()
        // Only public, unencrypted player declarations; never evaluate arbitrary JavaScript.
        if (source.searchMode == SearchMode.DLE_GET) {
            PublicPlayerDeclarations.dleMovie(doc, html)?.let { embeds.add(it) }
        }
        var found = false
        var playerFailure: String? = null
        for (embed in embeds.distinct().take(10)) {
            try {
                val publicPlaylist = if (URI(embed).host == "vixsrc.to") PublicVixPlayer.resolve(embed) else null
                if (publicPlaylist != null) {
                    callback(newExtractorLink(name, "$name - VixSrc", publicPlaylist.url, ExtractorLinkType.M3U8) {
                        referer = publicPlaylist.referer
                        headers = mapOf("User-Agent" to "Mozilla/5.0", "Origin" to "https://vixsrc.to")
                    })
                    found = true
                    continue
                }
                loadExtractor(embed, data, subtitleCallback) { link ->
                    found = true
                    callback(link)
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val detail = if (error is PublicVixPlayer.PlayerFailure) error.message else error.javaClass.simpleName
                if (URI(embed).host == "vixsrc.to") playerFailure = detail
                Log.w("FrenkyExtra", "${source.id}: $detail")
            } catch (error: LinkageError) {
                playerFailure = "Incompatibilità con l'app: ${error.javaClass.simpleName}"
                Log.w("FrenkyExtra", "${source.id}: $playerFailure")
            }
        }
        if (!found) {
            val message = "$name: " + (playerFailure ?: "nessun lettore compatibile disponibile")
            Handler(Looper.getMainLooper()).post {
                com.lagradost.cloudstream3.CloudStreamApp.context?.let { context ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
        return found
    }

    private suspend fun getHtml(url: String): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            val status = connection.responseCode
            if (status != 200) {
                Log.w("FrenkyCloudStream", "Catalogue ${source.id} HTTP $status")
                throw IllegalStateException("Fonte non disponibile: HTTP $status")
            }
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val chunk = ByteArray(8192)
                var total = 0
                while (total < 600_000) {
                    val length = input.read(chunk, 0, minOf(chunk.size, 600_000 - total))
                    if (length <= 0) break
                    out.write(chunk, 0, length)
                    total += length
                }
            }
            val body = out.toString(Charsets.UTF_8.name())
            Log.i("FrenkyCloudStream", "Catalogue ${source.id}: received ${body.length} chars")
            body
        } finally {
            connection?.disconnect()
        }
    }
}
