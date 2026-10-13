package com.frenky.extra.guardaserie

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

/** Experimental native HLS discovery; the existing player web remains the fallback. */
internal object GuardaSerieNativeLink {
    suspend fun tryLoad(page: String, callback: (ExtractorLink) -> Unit): Boolean {
        if (GuardaSeriePages.player(page) == null) return false
        return try {
            val response = app.get(
                url = page,
                referer = "https://vixsrc.to/",
                interceptor = WebViewResolver(
                    interceptUrl = Regex("playlist.*token"),
                    useOkhttp = true,
                    timeout = 15000L
                )
            )
            val stream = GuardaSeriePages.secure(response.url) ?: return false
            if (!stream.path.contains("playlist") || !stream.toString().contains("token")) return false
            callback(
                newExtractorLink(
                    source = "GuardaSerie VixSrc",
                    name = "GuardaSerie HLS",
                    url = stream.toString(),
                    type = ExtractorLinkType.M3U8
                ) {
                    referer = "https://vixsrc.to/"
                    quality = Qualities.Unknown.value
                    headers = mapOf(
                        "Origin" to "https://vixsrc.to",
                        "Referer" to "https://vixsrc.to/"
                    )
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
