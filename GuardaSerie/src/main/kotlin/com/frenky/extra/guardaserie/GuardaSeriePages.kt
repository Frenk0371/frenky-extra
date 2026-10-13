package com.frenky.extra.guardaserie

import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLEncoder

/** Parses only declared catalogue records and the observed public player contract. */
object GuardaSeriePages {
    data class Card(val title: String, val url: String, val poster: String?)
    data class Metadata(val title: String, val poster: String?, val plot: String?, val year: Int?, val genres: List<String>, val state: String?, val rating: String?)
    data class Item(val season: Int, val episode: Int, val title: String?, val player: String?)
    fun secure(url: String): URI? = runCatching { URI(url) }.getOrNull()?.takeIf {
        it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null && it.port in setOf(-1, 443)
    }
    fun detail(url: String, base: String): String? {
        val u = secure(url) ?: return null
        if (u.host != secure(base)?.host || u.query != null || !Regex("/detail/tv-[1-9][0-9]*(?:-[^/]*)?").matches(u.path)) return null
        return URI(u.scheme, u.authority, u.path, null, null).toString()
    }
    fun image(value: String, base: String): String? = runCatching {
        secure(URI(base).resolve(value).toString())?.toString()?.takeIf { value.isNotBlank() }
    }.getOrNull()
    fun search(base: String, query: String): String = base.trimEnd('/') + "/search?q=" + URLEncoder.encode(query, "UTF-8")
    fun archive(base: String, page: Int): String = base.trimEnd('/') + "/archive?sort=vote&page=" + page.coerceAtLeast(1)
    fun cards(doc: Document, base: String): List<Card> {
        val cards = linkedMapOf<String, Card>()
        // Read poster-bearing cards first so ranked duplicates never erase posters.
        for (element in doc.select(".mlnew, .slider-item, a.ranked-link")) {
            val anchor = if (element.tagName() == "a") element else element.selectFirst(".mlnh-2 h2 a, a[href]") ?: continue
            val url = runCatching { detail(URI(base).resolve(anchor.attr("href")).toString(), base) }.getOrNull() ?: continue
            val title = element.selectFirst(".mlnh-2 h2 a, .rank-name")?.text()
                ?.takeIf { it.isNotBlank() } ?: element.selectFirst("img")?.attr("alt")?.takeIf { it.isNotBlank() }
                ?: anchor.attr("title").ifBlank { anchor.text() }
            if (title.isBlank()) continue
            val poster = element.selectFirst("img")?.let { image(it.attr("data-src").ifBlank { it.attr("src") }, base) }
            val old = cards[url]
            cards[url] = Card(title, url, poster ?: old?.poster)
        }
        return cards.values.toList()
    }
    fun metadata(doc: Document, base: String): Metadata {
        val title = doc.select("h1.front-title, h1.gs-detail-title").text().trim()
        val imageNode = doc.selectFirst("#tv-info-poster img")
        val poster = imageNode?.let { image(it.attr("data-src").ifBlank { it.attr("src") }, base) }
        val info = doc.select(".tv-info-right").text()
        val yearText = doc.select(".tv-info-list ul:contains(Anno) li:last-child").text()
        val state = doc.select(".tv-info-list ul:contains(Stato) li:last-child").text().trim().takeIf { it.isNotBlank() }
        return Metadata(title, poster, info.substringAfter("Trama", "").substringAfter(":", "").trim().takeIf { it.isNotBlank() },
            Regex("\\b(19|20)\\d{2}\\b").find(yearText)?.value?.toIntOrNull(),
            doc.select(".tv-info-list ul:contains(Categoria) a").map { it.text() }, state,
            doc.select(".entry-imdb").text().replace("★", "").trim().takeIf { it.isNotBlank() })
    }
    fun player(url: String): String? {
        val u = secure(url) ?: return null
        return url.takeIf { u.host == "vixsrc.to" && u.fragment == null && u.rawQuery == "lang=it" &&
            Regex("/tv/[1-9][0-9]*/[0-9]+/[1-9][0-9]*").matches(u.path) }
    }
    fun navigation(url: String): Boolean = secure(url)?.host == "vixsrc.to"
    fun episodes(doc: Document, page: String): List<Item> {
        val id = Regex("/detail/tv-([1-9][0-9]*)").find(URI(page).path)?.groupValues?.get(1) ?: return emptyList()
        // Do not assume VixSrc from the TMDB identifier alone. Require the site's
        // actual script declaration and URL expression observed in the browser.
        val declared = doc.select("script").any { script ->
            val code = script.data().replace("\\/", "/")
            Regex("""var\s+playerBaseURL\s*=\s*["']https://vixsrc\.to["']\s*;""").containsMatchIn(code) &&
                Regex("""var\s+tmdbID\s*=\s*$id\s*;""").containsMatchIn(code) &&
                Regex("""var\s+mediaType\s*=\s*["']tv["']\s*;""").containsMatchIn(code) &&
                code.contains("playerBaseURL + '/tv/' + tmdbID + '/' + season + '/' + episode + '?lang=it'")
        }
        val items = mutableListOf<Item>()
        for (tab in doc.select(".tt-season a[href]")) {
            val season = tab.text().trim().toIntOrNull()?.takeIf { it >= 0 } ?: continue
            val pane = doc.getElementById(tab.attr("href").removePrefix("#")) ?: continue
            for (link in pane.select("a[data-season][data-episode]")) {
                if (link.attr("data-season").toIntOrNull() != season) continue
                val number = link.attr("data-episode").toIntOrNull()?.takeIf { it > 0 } ?: continue
                val player = if (declared) player("https://vixsrc.to/tv/$id/$season/$number?lang=it") else null
                items.add(Item(season, number, link.attr("data-title").takeIf { it.isNotBlank() }, player))
            }
        }
        return items.distinctBy { it.season to it.episode }.sortedWith(compareBy({ it.season }, { it.episode }))
    }
}
