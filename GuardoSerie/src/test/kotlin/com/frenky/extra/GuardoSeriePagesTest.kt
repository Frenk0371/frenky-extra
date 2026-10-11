package com.frenky.extra.guardoserie

import org.junit.Assert.*
import org.junit.Test

class GuardoSeriePagesTest {
    @Test fun retainsRealEpisodePage() {
        val url = "https://guardoseries.sbs/episodio/the-night-agent-stagione-1-episodio-1"
        assertEquals(url, GuardoSeriePages.page(url))
        assertTrue(GuardoSeriePages.navigation("https://loadm.cam/#wfilk"))
        assertNull(GuardoSeriePages.page("https://loadm.cam/#wfilk"))
    }
    @Test fun rejectsUnsafeOrForeignAddresses() {
        listOf("http://guardoseries.sbs/episodio/x", "https://guardoseries.sbs.evil.test/episodio/x", "https://user@guardoseries.sbs/episodio/x", "https://guardoseries.sbs:444/episodio/x", "javascript:alert(1)", "file:///tmp/x", "intent://x", "https://evil.test/").forEach {
            assertNull(GuardoSeriePages.page(it))
            assertFalse(it, GuardoSeriePages.navigation(it))
        }
        assertNull(GuardoSeriePages.page("https://guardoseries.sbs/"))
    }
}
