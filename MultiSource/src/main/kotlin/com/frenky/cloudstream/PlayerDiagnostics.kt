package com.frenky.cloudstream

import java.net.URI
import java.util.Locale

/** Bounded diagnostics: never include queries, credentials or signed player paths. */
object PlayerDiagnostics {
    fun address(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return "[indirizzo non valido]"
        if (uri.scheme != "https" || uri.host != "vixsrc.to" || uri.userInfo != null ||
            uri.port !in setOf(-1, 443)) return "[indirizzo diverso da VixSrc]"
        val path = uri.path.orEmpty()
        val publicApi = Regex("""/api/movie/(?:tt\d{1,12}|\d{1,12})""").matches(path)
        return "https://vixsrc.to" + if (publicApi) path else "/[percorso omesso]"
    }

    fun contentType(value: String?): String {
        val mime = value?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        return when (mime) {
            "application/json", "text/html", "text/plain", "application/xhtml+xml",
            "application/vnd.apple.mpegurl", "application/x-mpegurl" -> mime
            null, "" -> "assente"
            else -> "altro"
        }
    }
}
