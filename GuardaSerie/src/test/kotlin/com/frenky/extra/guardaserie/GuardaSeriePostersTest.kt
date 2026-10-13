package com.frenky.extra.guardaserie

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class GuardaSeriePostersTest {
    private val base = "https://guarda-serie.ovh"

    @Test fun archiveAndSearchImagesFillRankedCardsWithoutDetailRequests() = runBlocking {
        val ranked = listOf(GuardaSeriePages.Card("Dutton Ranch", "$base/detail/tv-299167-dutton-ranch", null))
        val resolver = GuardaSeriePosters()
        val observed = ranked.single().copy(poster = "$base/img/w200/poster.jpg")
        resolver.remember(listOf(observed))
        val result = resolver.resolve(ranked) { error("A cached catalogue poster must not fetch a detail page") }
        assertEquals(listOf(observed), result)
    }

    @Test fun rankedTitleGetsDetailPosterAndReusesCache() = runBlocking {
        val cards = GuardaSeriePages.cards(Jsoup.parse("""<a class="ranked-link" href="/detail/tv-299167-dutton-ranch"><span class="rank-name">Dutton Ranch</span></a>"""), base)
        assertNull(cards.single().poster)
        val resolver = GuardaSeriePosters()
        var requests = 0
        val load: suspend (String) -> String? = { url ->
            requests++
            assertEquals("$base/detail/tv-299167-dutton-ranch", url)
            GuardaSeriePages.metadata(Jsoup.parse("""<div id="tv-info-poster"><img src="/img/w200/oYaRozHp2xRYzXUA7Eltvuw5um4.jpg"></div>"""), base).poster
        }
        val first = resolver.resolve(cards, load)
        assertEquals("$base/img/w200/oYaRozHp2xRYzXUA7Eltvuw5um4.jpg", first.single().poster)
        assertEquals(first, resolver.resolve(cards, load))
        assertEquals(1, requests)
    }

    @Test fun failuresKeepTitlesOrderAndExistingPostersAndCanRetry() = runBlocking {
        val cards = listOf(
            GuardaSeriePages.Card("Existing", "$base/detail/tv-1", "$base/img/existing.jpg"),
            GuardaSeriePages.Card("Failed", "$base/detail/tv-2", null),
            GuardaSeriePages.Card("Recovered", "$base/detail/tv-3", null)
        )
        val resolver = GuardaSeriePosters()
        val result = resolver.resolve(cards) { url ->
            assertNotEquals(cards.first().url, url)
            if (url == cards[1].url) error("Image request failed")
            "$base/img/recovered.jpg"
        }
        assertEquals(cards.map { it.title }, result.map { it.title })
        assertEquals(cards.first(), result.first())
        assertNull(result[1].poster)
        assertNotNull(result[2].poster)
        assertNotNull(resolver.resolve(listOf(cards[1])) { "$base/img/retry.jpg" }.single().poster)
    }

    @Test fun requestsAreBoundedAndResultsStayInCatalogueOrder() = runBlocking {
        var active = 0
        var maximum = 0
        val cards = (1..12).map { GuardaSeriePages.Card("Title $it", "$base/detail/tv-$it", null) }
        val result = GuardaSeriePosters().resolve(cards) { url ->
            active++
            maximum = maxOf(maximum, active)
            try { delay(10); "$url.jpg" } finally { active-- }
        }
        assertEquals(4, maximum)
        assertEquals(cards.map { it.url }, result.map { it.url })
        assertTrue(result.all { it.poster != null })
    }

    @Test fun cancellationIsPropagated() = runBlocking {
        try {
            GuardaSeriePosters().resolve(listOf(GuardaSeriePages.Card("Title", "$base/detail/tv-1", null))) {
                throw CancellationException("Cancelled")
            }
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
    }
}
