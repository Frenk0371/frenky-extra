package com.frenky.extra.guardaserie

import java.net.HttpURLConnection
import java.net.URL
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real catalogue requests, opt-in. No stream request, access bypass or Android playback claim. */
class GuardaSerieLiveTest {
    private val base = "https://guarda-serie.ovh"
    private fun read(url: String): org.jsoup.nodes.Document {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "FrenkyCatalogueTest/1.0")
            assertEquals("Live catalogue access failed: $url", 200, connection.responseCode)
            val bytes = connection.inputStream.use { it.readNBytes(1_000_001) }
            assertTrue(bytes.size <= 1_000_000)
            val text = String(bytes, Charsets.UTF_8)
            assertFalse(text.contains("cf-chl-"))
            return Jsoup.parse(text, url)
        } finally { connection.disconnect() }
    }
    @Test fun rankedHomepageTitlesRecoverDetailPosters() = kotlinx.coroutines.runBlocking {
        assumeTrue(System.getenv("GUARDASERIE_LIVE_TEST") == "1")
        val cards = GuardaSeriePages.cards(read("$base/"), base)
        val ranked = cards.filter { it.poster == null }.take(3)
        assumeTrue("No image-free ranked links on today's homepage", ranked.isNotEmpty())
        val resolved = GuardaSeriePosters().resolve(ranked) { url ->
            GuardaSeriePages.metadata(read(url), base).poster
        }
        assertEquals(ranked.map { it.url }, resolved.map { it.url })
        assertTrue("Ranked titles still have no detail posters", resolved.all { it.poster != null })
    }
    @Test fun real883MetadataAndDeclaredEpisodes() {
        assumeTrue(System.getenv("GUARDASERIE_LIVE_TEST") == "1")
        val page = "$base/detail/tv-273419-hanno-ucciso-l-uomo-ragno-la-leggendaria-storia-degli-883"
        val doc = read(page)
        val meta = GuardaSeriePages.metadata(doc, base)
        assertTrue(meta.title.contains("883"))
        assertEquals(2024, meta.year)
        assertEquals("Returning Series", meta.state)
        assertNotNull(meta.poster)
        assertNotNull(meta.plot)
        val episodes = GuardaSeriePages.episodes(doc, page)
        assertEquals(8, episodes.size)
        assertTrue(episodes.all { it.season == 1 })
        assertEquals("https://vixsrc.to/tv/273419/1/8?lang=it", episodes.last().player)
    }
    @Test fun realSearchPosterDetailSeasonsAndDeclaredPlayer() {
        assumeTrue(System.getenv("GUARDASERIE_LIVE_TEST") == "1")
        val home = GuardaSeriePages.cards(read("$base/"), base)
        assertTrue("Homepage has no parsed series", home.isNotEmpty())
        val results = GuardaSeriePages.cards(read(GuardaSeriePages.search(base, "Breaking Bad")), base)
        val card = results.single { it.url == "$base/detail/tv-1396-breaking-bad" }
        assertEquals("Breaking Bad", card.title)
        assertNotNull("Search poster missing", card.poster)
        val doc = read(card.url)
        assertEquals("Breaking Bad", doc.select("h1.front-title").text())
        assertTrue(doc.select("#tv-info-poster img").isNotEmpty())
        val items = GuardaSeriePages.episodes(doc, card.url)
        assertEquals(listOf(7, 13, 13, 13, 16), (1..5).map { season -> items.count { it.season == season } })
        assertEquals(62, items.size)
        assertEquals("https://vixsrc.to/tv/1396/5/16?lang=it", items.single { it.season == 5 && it.episode == 16 }.player)
        println("LIVE VERIFIED: homepage, Breaking Bad search/poster/detail, 5 seasons, 62 rows, declared S5E16 player. Playback not tested.")
    }
}
