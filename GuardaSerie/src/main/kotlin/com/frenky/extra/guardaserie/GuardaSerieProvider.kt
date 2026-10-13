package com.frenky.extra.guardaserie

import com.frenky.cloudstream.DomainRegistry
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import org.json.JSONObject
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.CancellationException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class GuardaSerieProvider : MainAPI() {
    override var name = "GuardaSerie"
    override var mainUrl = "https://guarda-serie.ovh"
    override var lang = "it"
    override val supportedTypes = setOf(TvType.TvSeries)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val mainPage = mainPageOf("home" to "I titoli del momento", "archive" to "Top IMDb")
    data class EpisodeData(val page: String, val season: Int, val episode: Int)
    private suspend fun base(): String = DomainRegistry.resolve("guardaserie", "https://guarda-serie.ovh").also { mainUrl = it }
    private suspend fun document(url: String, base: String): Document {
        check(GuardaSeriePages.secure(url)?.host == GuardaSeriePages.secure(base)?.host) { "Dominio non verificato" }
        val response = app.get(url, timeout = 20L, allowRedirects = false)
        check(response.code == 200) { "GuardaSerie: HTTP ${response.code}; redirect o accesso da verificare" }
        val body = response.text
        check(body.length <= 1_000_000) { "Risposta troppo grande" }
        check(!listOf("cf-chl-", "g-recaptcha", "h-captcha", "verify you are human").any { body.contains(it, true) }) { "Verifica di accesso richiesta" }
        return Jsoup.parse(body, url)
    }
    private fun results(doc: Document, base: String): List<SearchResponse> = GuardaSeriePages.cards(doc, base).map { card ->
        newTvSeriesSearchResponse(card.title, card.url) { posterUrl = card.poster }
    }
    override suspend fun search(query: String): List<SearchResponse> {
        val base = base()
        return results(document(GuardaSeriePages.search(base, query), base), base)
    }
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val base = base()
        val home = request.data == "home"
        if (home && page > 1) return newHomePageResponse(HomePageList(request.name, emptyList()), hasNext = false)
        val doc = document(if (home) "$base/" else GuardaSeriePages.archive(base, page), base)
        val next = !home && doc.select(".mlnew-pagination a").any { it.attr("href").contains("page=${page + 1}") }
        return newHomePageResponse(HomePageList(request.name, results(doc, base)), hasNext = next)
    }
    override suspend fun load(url: String): LoadResponse {
        val base = base()
        val page = GuardaSeriePages.detail(url, base) ?: error("Scheda non valida")
        val doc = document(page, base)
        val title = doc.select("h1.front-title, h1.gs-detail-title").text().trim()
        check(title.isNotBlank()) { "Scheda priva di titolo" }
        val poster = GuardaSeriePages.image(doc.select("#tv-info-poster img").attr("src"), base)
        val episodes = GuardaSeriePages.episodes(doc, page).map { item ->
            newEpisode(JSONObject().put("page", page).put("season", item.season).put("episode", item.episode).toString()) {
                name = item.title ?: "Episodio ${item.episode}"
                season = item.season
                episode = item.episode
                posterUrl = poster
            }
        }
        check(episodes.isNotEmpty()) { "Nessun episodio dichiarato dal sito" }
        val info = doc.select(".tv-info-right").text()
        val yearText = doc.select(".tv-info-list ul:contains(Anno) li:last-child").text()
        val state = doc.select(".tv-info-list ul:contains(Stato) li:last-child").text()
        return newTvSeriesLoadResponse(title, page, TvType.TvSeries, episodes) {
            posterUrl = poster
            plot = info.substringAfter("Trama", "").substringAfter(":", "").trim().takeIf { it.isNotBlank() }
            year = Regex("\\b(19|20)\\d{2}\\b").find(yearText)?.value?.toIntOrNull()
            tags = doc.select(".tv-info-list ul:contains(Categoria) a").map { it.text() }
            showStatus = when (state) { "Ended", "Canceled" -> ShowStatus.Completed; "Returning Series" -> ShowStatus.Ongoing; else -> null }
            addScore(doc.select(".entry-imdb").text().replace("★", "").trim())
        }
    }
    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val episode = runCatching {
            val json = JSONObject(data)
            EpisodeData(json.getString("page"), json.getInt("season"), json.getInt("episode"))
        }.getOrNull() ?: return false
        val base = base()
        val page = GuardaSeriePages.detail(episode.page, base) ?: return false
        val doc = document(page, base)
        // Recheck the actual episode and public declaration rather than trust saved URLs.
        val player = GuardaSeriePages.episodes(doc, page).singleOrNull { it.season == episode.season && it.episode == episode.episode }?.player ?: return false
        var found = false
        try {
            loadExtractor(player, page, subtitleCallback) { link -> found = true; callback(link) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
        }
        if (!found && !isCasting) GuardaSerieWebPlayer.offer(name, player)
        return found
    }
}
