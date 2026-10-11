package com.frenky.cloudstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerDiagnosticsTest {
    @Test fun publicApiNeverIncludesQueryOrFragment() {
        assertEquals("https://vixsrc.to/api/movie/tt0086874",
            PlayerDiagnostics.address("https://vixsrc.to/api/movie/tt0086874?token=secret#private"))
    }

    @Test fun signedPathsAndForeignAddressesAreRedacted() {
        for (url in listOf("https://vixsrc.to/embed/secret?token=secret",
            "https://vixsrc.to/playlist/secret", "https://secret@vixsrc.to/api/movie/123",
            "https://secret.example/api/movie/123", "https://vixsrc.to:8443/api/movie/123",
            "https://vixsrc.to/api/movie/123/secret", "invalid secret")) {
            assertFalse(PlayerDiagnostics.address(url).contains("secret"))
        }
    }

    @Test fun contentTypeDoesNotReflectArbitraryServerValues() {
        assertEquals("text/html", PlayerDiagnostics.contentType("Text/HTML; charset=UTF-8"))
        assertEquals("altro", PlayerDiagnostics.contentType("secret"))
        assertEquals("assente", PlayerDiagnostics.contentType(null))
    }
}
