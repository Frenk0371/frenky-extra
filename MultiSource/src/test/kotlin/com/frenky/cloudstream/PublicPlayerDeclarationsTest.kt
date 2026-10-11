package com.frenky.cloudstream

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class PublicPlayerDeclarationsTest {
    private val declaration = """
        var SERIES = 0;
        var imdb = 'tt0086874';
        iframe.src = 'https://vixsrc.to/movie/' + imdb + '?lang=it';
    """.trimIndent()
    private fun movie(html: String) = PublicPlayerDeclarations.dleMovie(Jsoup.parse(html), html)
    @Test fun discoversPlayerInValidFilmPage() {
        assertEquals("https://vixsrc.to/movie/tt0086874?lang=it",
            movie("<iframe id='vidxgo-player'></iframe><script>$declaration</script>"))
    }
    @Test fun recoversCasaCinemaUnclosedScriptBeforeIframe() {
        val html = "<script>var broken = ''; <iframe id='vidxgo-player' src=''></iframe></script><script>$declaration</script>"
        assertTrue(Jsoup.parse(html).select("#vidxgo-player").isEmpty())
        assertEquals("https://vixsrc.to/movie/tt0086874?lang=it", movie(html))
    }
    @Test fun doesNotCreateFilmLinkForSeriesOrUnrelatedPage() {
        assertNull(movie("<iframe id='vidxgo-player'></iframe><script>${declaration.replace("SERIES = 0", "SERIES = 1")}</script>"))
        assertNull(movie("<script>$declaration</script>"))
    }
}
