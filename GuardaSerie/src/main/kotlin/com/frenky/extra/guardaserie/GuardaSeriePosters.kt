package com.frenky.extra.guardaserie

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Ranked homepage links contain no image; retrieve it from the actual detail page. */
class GuardaSeriePosters {
    private val requests = Semaphore(4)
    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 256
    }

    fun remember(cards: List<GuardaSeriePages.Card>) {
        synchronized(cache) {
            cards.forEach { card -> card.poster?.let { cache[card.url] = it } }
        }
    }

    suspend fun resolve(
        cards: List<GuardaSeriePages.Card>,
        loadPoster: suspend (String) -> String?
    ): List<GuardaSeriePages.Card> = coroutineScope {
        remember(cards)
        cards.map { card ->
            async {
                if (card.poster != null) return@async card
                val poster = requests.withPermit {
                    synchronized(cache) { cache[card.url] } ?: try {
                        loadPoster(card.url)?.also { synchronized(cache) { cache[card.url] = it } }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        // A missing image must not remove the title or fail the entire row.
                        null
                    }
                }
                card.copy(poster = poster)
            }
        }.awaitAll()
    }
}
