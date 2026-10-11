package com.frenky.cloudstream

import java.net.URI

/** Only the public movie page can be opened by the optional interactive player. */
object InternalPlayerPage {
    fun movie(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!navigation(url) || !Regex("""/movie/(?:tt\d+|\d+)/?""").matches(uri.path)) return null
        return "https://vixsrc.to" + uri.path.trimEnd('/') + "?lang=it"
    }

    fun navigation(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host == "vixsrc.to" &&
            uri.userInfo == null && uri.port in setOf(-1, 443)
    }
}
