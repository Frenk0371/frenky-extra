package com.frenky.cloudstream

import org.jsoup.nodes.Document

/** Read only literal, public player declarations. Never execute site scripts. */
object PublicPlayerDeclarations {
    fun dleMovie(doc: Document, html: String): String? {
        // Some DLE pages have an unclosed script before the iframe. Jsoup treats
        // the iframe markup as script data, while the browser recovers the player.
        val iframeDeclared = doc.select("iframe#vidxgo-player").isNotEmpty() ||
            Regex("""<iframe\b[^>]*\bid\s*=\s*['"]vidxgo-player['"][^>]*>""", RegexOption.IGNORE_CASE).containsMatchIn(html)
        if (!iframeDeclared) return null
        val scripts = doc.select("script").joinToString("\n") { it.data() }
        if (!scripts.contains("https://vixsrc.to/movie/")) return null
        if (Regex("""var\s+SERIES\s*=\s*1\b""").containsMatchIn(scripts)) return null
        val imdb = Regex("""var\s+imdb\s*=\s*['"](tt\d+)['"]""")
            .find(scripts)?.groupValues?.get(1) ?: return null
        return "https://vixsrc.to/movie/$imdb?lang=it"
    }
}
