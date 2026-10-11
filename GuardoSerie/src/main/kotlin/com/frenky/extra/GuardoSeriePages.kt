package com.frenky.extra.guardoserie

import java.net.URI

object GuardoSeriePages {
    private fun uri(url: String): URI? = runCatching { URI(url) }.getOrNull()?.takeIf {
        it.scheme == "https" && it.userInfo == null && it.port in setOf(-1, 443)
    }

    fun page(url: String): String? {
        val u = uri(url) ?: return null
        if (u.host != "guardoseries.sbs" || u.path.isNullOrBlank() || u.path == "/") return null
        return u.toString()
    }

    fun navigation(url: String): Boolean {
        val u = uri(url) ?: return false
        return u.host in setOf("guardoseries.sbs", "loadm.cam")
    }
}
