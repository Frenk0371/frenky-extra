package com.frenky.extra.guardaserie
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class GuardaSeriePagesTest {
    private val base = "https://guarda-serie.ovh"
    private val page = "$base/detail/tv-1396-breaking-bad"
    // Reduced fixture using markup and the URL expression observed on the live page.
    private val fixture = """
        <div class="tt-season"><a href="#season-1">1</a><a href="#season-5">5</a></div>
        <div id="season-1"><a data-season="1" data-episode="1">1</a><a data-season="1" data-episode="7">7</a></div>
        <div id="season-5"><a data-season="5" data-episode="16">16</a></div>
        <script>var playerBaseURL = "https:\/\/vixsrc.to"; var tmdbID = 1396; var mediaType = "tv";
        var url = playerBaseURL + '/tv/' + tmdbID + '/' + season + '/' + episode + '?lang=it';</script>
    """.trimIndent()
    @Test fun optionalMetadataMissingDoesNotCrashOrInventValues() {
        val meta = GuardaSeriePages.metadata(Jsoup.parse("<h1 class='front-title'>Serie</h1>"), base)
        assertEquals("Serie", meta.title)
        assertNull(meta.poster); assertNull(meta.plot); assertNull(meta.year); assertNull(meta.state); assertNull(meta.rating)
        assertTrue(meta.genres.isEmpty())
    }
    @Test fun metadataReadsReturningSeriesAndLazyPoster() {
        val doc = Jsoup.parse("""<h1 class="front-title">Hanno ucciso l'Uomo Ragno</h1><div id="tv-info-poster"><img data-src="/img/poster.jpg"></div><div class="tv-info-right"><div class="tv-info-list"><ul><li>Anno:</li><li>2024 - Returning Series</li></ul><ul><li>Stato:</li><li>Returning Series</li></ul></div>Trama titolo: Una storia.</div>""")
        val meta = GuardaSeriePages.metadata(doc, base)
        assertEquals(2024, meta.year); assertEquals("Returning Series", meta.state)
        assertEquals("$base/img/poster.jpg", meta.poster); assertEquals("Una storia.", meta.plot)
    }
    @Test fun episodesRequireActualRowsAndDeclaration() {
        val items = GuardaSeriePages.episodes(Jsoup.parse(fixture), page)
        assertEquals(listOf(1 to 1, 1 to 7, 5 to 16), items.map { it.season to it.episode })
        assertEquals("https://vixsrc.to/tv/1396/5/16?lang=it", items.last().player)
    }
    @Test fun noInventedPlayerFromTmdbAlone() {
        val doc = Jsoup.parse(fixture); doc.select("script").remove()
        assertTrue(GuardaSeriePages.episodes(doc, page).all { it.player == null })
    }
    @Test fun changedOrForeignDeclarationCannotProducePlayer() {
        for (changed in listOf(fixture.replace("vixsrc.to", "evil.test"), fixture.replace("tmdbID = 1396", "tmdbID = 42"))) {
            assertTrue(GuardaSeriePages.episodes(Jsoup.parse(changed), page).all { it.player == null })
        }
    }
    @Test fun malformedSeasonAndDuplicateRowsAreRejected() {
        val doc = Jsoup.parse(fixture)
        doc.getElementById("season-1")!!.append("<a data-season='2' data-episode='99'>99</a><a data-season='1' data-episode='1'>1</a>")
        assertEquals(3, GuardaSeriePages.episodes(doc, page).size)
    }
    @Test fun catalogueKeepsPostersAndExcludesMoviesAndForeignLinks() {
        val doc = Jsoup.parse("""<div class="mlnew"><div class="mlnh-thumb"><a href="/detail/tv-1396-breaking-bad"><img src="/img/poster.jpg"></a></div><div class="mlnh-2"><h2><a href="/detail/tv-1396-breaking-bad">Breaking Bad</a></h2></div></div><a class="ranked-link" href="/detail/tv-1396-breaking-bad"><span class="rank-name">Breaking Bad</span></a><a class="ranked-link" href="https://evil.test/detail/tv-1"><span class="rank-name">Bad</span></a><a class="ranked-link" href="/detail/movie-42"><span class="rank-name">Movie</span></a>""")
        val result = GuardaSeriePages.cards(doc, base)
        assertEquals(1, result.size); assertEquals("$base/img/poster.jpg", result.single().poster)
    }
    @Test fun unsafePlayerUrlsCannotOpenWebview() {
        for (url in listOf("http://vixsrc.to/tv/1/1/1?lang=it", "https://vixsrc.to.evil.test/tv/1/1/1?lang=it", "https://user@vixsrc.to/tv/1/1/1?lang=it", "https://vixsrc.to:444/tv/1/1/1?lang=it", "https://vixsrc.to/movie/1?lang=it")) assertNull(GuardaSeriePages.player(url))
        assertNotNull(GuardaSeriePages.player("https://vixsrc.to/tv/1396/1/1?lang=it"))
    }
    @Test fun searchEncodesAllQueryCharactersAndArchiveUsesQuestionMark() {
        assertEquals("$base/search?q=L%27amica+%26+%C3%A8", GuardaSeriePages.search(base, "L'amica & è"))
        assertEquals("$base/archive?sort=vote&page=2", GuardaSeriePages.archive(base, 2))
    }
}
