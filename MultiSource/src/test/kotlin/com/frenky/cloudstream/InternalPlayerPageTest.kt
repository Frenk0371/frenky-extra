package com.frenky.cloudstream

import org.junit.Assert.*
import org.junit.Test

class InternalPlayerPageTest {
    @Test fun opensOnlyPublicMoviePages() {
        assertEquals("https://vixsrc.to/movie/tt0086874?lang=it", InternalPlayerPage.movie("https://vixsrc.to/movie/tt0086874?lang=en#x"))
        assertEquals("https://vixsrc.to/movie/123?lang=it", InternalPlayerPage.movie("https://vixsrc.to/movie/123/"))
        assertNull(InternalPlayerPage.movie("https://vixsrc.to/api/movie/123"))
        assertNull(InternalPlayerPage.movie("https://vixsrc.to/embed/signed-value"))
    }

    @Test fun rejectsForeignOriginsAndUnsafeNavigation() {
        listOf("http://vixsrc.to/movie/123", "https://vixsrc.to.evil.test/movie/123", "https://vixsrc.to@evil.test/movie/123", "https://user@vixsrc.to/movie/123", "https://vixsrc.to:444/movie/123", "javascript:alert(1)", "file:///tmp/x", "intent://x", "https://evil.test/").forEach {
            assertFalse(it, InternalPlayerPage.navigation(it))
            assertNull(InternalPlayerPage.movie(it))
        }
        assertTrue(InternalPlayerPage.navigation("https://vixsrc.to/embed/public"))
    }
}
