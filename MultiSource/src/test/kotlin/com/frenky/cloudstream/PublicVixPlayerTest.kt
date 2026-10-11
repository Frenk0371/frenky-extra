package com.frenky.cloudstream

import org.junit.Assert.*
import org.junit.Test

class PublicVixPlayerTest {
    private val page = """
        window.streams = [{"name":"Server1","active":true,"url":"https:\/\/vixsrc.to\/playlist\/123?ub=1"}];
        window.masterPlaylist = {
            params: { 'token': 'issued-token', 'expires': '2000000000', 'asn': '' },
            url: 'https://vixsrc.to/playlist/123',
        }
        window.canPlayFHD = true
    """.trimIndent()

    @Test fun followsActiveServerAndPlayerParameters() {
        val url = PublicVixPlayer.parsePlaylist(page, true)!!
        assertTrue(url.startsWith("https://vixsrc.to/playlist/123?ub=1&"))
        assertTrue(url.contains("token=issued-token"))
        assertTrue(url.contains("expires=2000000000"))
        assertTrue(url.endsWith("h=1"))
        assertFalse(url.contains("asn="))
    }
    @Test fun doesNotRequestFullHdWithoutServerCapability() {
        assertFalse(PublicVixPlayer.parsePlaylist(page, false)!!.contains("h=1"))
    }
    @Test fun rejectsUntrustedPlayerHosts() {
        assertNull(PublicVixPlayer.parsePlaylist(page.replace("vixsrc.to", "untrusted.invalid")))
    }
    @Test fun stopsAtCaptcha() {
        assertNull(PublicVixPlayer.parsePlaylist(page + "<div class='h-captcha'></div>"))
    }
    @Test fun supportsStandardIdentityHlsButRejectsDrm() {
        assertTrue(PublicVixPlayer.supportedHls("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"https://cdn.example/key\""))
        assertFalse(PublicVixPlayer.supportedHls("#EXTM3U\n#EXT-X-KEY:METHOD=SAMPLE-AES,KEYFORMAT=\"com.apple.streamingkeydelivery\""))
        assertFalse(PublicVixPlayer.supportedHls("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,KEYFORMAT=\"unrecognized-drm\""))
        assertFalse(PublicVixPlayer.supportedHls("<html>Forbidden</html>"))
    }
}
