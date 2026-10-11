package com.frenky.cloudstream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.lagradost.cloudstream3.app
import android.util.Log
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/** Follows only the public player/API declarations used by VixSrc's browser player. */
object PublicVixPlayer {
    private const val ORIGIN = "https://vixsrc.to"
    data class Playlist(val url: String, val referer: String)

    fun parsePlaylist(html: String, fullHd: Boolean = false): String? {
        if (html.contains("g-recaptcha") || html.contains("h-captcha")) return null
        val start = html.indexOf("window.masterPlaylist")
        if (start < 0) return null
        val block = html.substring(start).substringBefore("window.canPlayFHD").take(12000)
        val url = Regex("""url\s*:\s*['"]([^'"]+)['"]""").find(block)?.groupValues?.get(1) ?: return null
        val active = Regex(""""active"\s*:\s*true\s*,\s*"url"\s*:\s*"([^"]+)"""")
            .find(html)?.groupValues?.get(1)?.replace("\\/", "/") ?: url
        val uri = runCatching { URI(active) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.host != "vixsrc.to" || !uri.path.startsWith("/playlist/")) return null
        val params = Regex("""['"](token|expires|asn)['"]\s*:\s*['"]([^'"]*)['"]""")
            .findAll(block).filter { it.groupValues[2].isNotEmpty() }
            .associate { it.groupValues[1] to it.groupValues[2] }.toMutableMap()
        if (params["token"].isNullOrBlank() || params["expires"]?.toLongOrNull() == null) return null
        if (fullHd) params["h"] = "1"
        return active + (if (uri.query == null) "?" else "&") + params.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
    }

    /** Native HLS AES-128 with identity keys is handled by the app's standard player.
     * DRM key formats, license URLs and SAMPLE-AES are never handled here. */
    fun supportedHls(manifest: String): Boolean {
        if (!manifest.trimStart().startsWith("#EXTM3U")) return false
        val keys = manifest.lineSequence().filter {
            it.startsWith("#EXT-X-KEY:") || it.startsWith("#EXT-X-SESSION-KEY:")
        }.toList()
        return keys.all { line ->
            val method = Regex("""METHOD=([^,]+)""").find(line)?.groupValues?.get(1)
            val format = Regex("""KEYFORMAT="([^"]+)"""").find(line)?.groupValues?.get(1)
            (method == "NONE" || method == "AES-128") && (format == null || format == "identity")
        }
    }

    class PlayerFailure(val stage: String, val detail: String) : IOException("$stage: $detail")

    private suspend fun read(url: String, referer: String, stage: String): String {
        val requested = PlayerDiagnostics.address(url)
        Log.i("FrenkyExtra", "$stage: GET $requested")
        val response = try {
            // NiceHttp timeouts are in seconds. Preserve the app's configured
            // headers rather than overriding its User-Agent with a partial value.
            app.get(url, referer = referer, timeout = 20L)
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            // Do not log signed URLs, tokens or response bodies.
            throw PlayerFailure(stage, error.javaClass.simpleName)
        }
        val mime = PlayerDiagnostics.contentType(response.headers["Content-Type"])
        val destination = PlayerDiagnostics.address(response.url)
        val cloudflare = response.headers["CF-Ray"] != null ||
            response.headers["Server"].equals("cloudflare", ignoreCase = true)
        Log.i("FrenkyExtra", "$stage: HTTP ${response.code}; tipo=$mime; " +
            "richiesta=$requested; destinazione=$destination; cloudflare=$cloudflare")
        if (response.code != 200) {
            // Report only a category, never server bodies, cookies or signed URLs.
            val body = response.text.take(12000).lowercase()
            val category = when {
                listOf("verify you are human", "checking your browser", "cf-chl-", "g-recaptcha", "h-captcha")
                    .any { body.contains(it) } -> "verifica di accesso richiesta"
                body.trimStart().startsWith("{") -> "risposta JSON"
                body.contains("<html") || body.contains("<!doctype html") -> "pagina HTML"
                else -> "risposta del server"
            }
            throw PlayerFailure(stage, "HTTP ${response.code} ($category)")
        }
        val text = response.text
        if (text.length > 600000) throw PlayerFailure(stage, "risposta troppo grande")
        if (text.contains("g-recaptcha") || text.contains("h-captcha")) {
            throw PlayerFailure(stage, "verifica di accesso richiesta")
        }
        return text
    }

    suspend fun resolve(page: String): Playlist? = withContext(Dispatchers.IO) {
        val uri = runCatching { URI(page) }.getOrNull() ?: return@withContext null
        if (uri.scheme != "https" || uri.host != "vixsrc.to") return@withContext null
        if (!Regex("""/movie/(?:tt\d+|\d+)""").matches(uri.path)) return@withContext null
        val apiUrl = ORIGIN + "/api" + uri.rawPath + "?lang=it"
        val src = JSONObject(read(apiUrl, page, "API VixSrc")).optString("src")
        val embed = URI(ORIGIN).resolve(src)
        if (src.isBlank() || embed.scheme != "https" || embed.host != "vixsrc.to" || !embed.path.startsWith("/embed/")) throw PlayerFailure("API VixSrc", "indirizzo del lettore non disponibile")
        val playlist = parsePlaylist(read(embed.toString(), page, "Pagina VixSrc"), embed.rawQuery?.contains("canPlayFHD=1") == true) ?: throw PlayerFailure("Pagina VixSrc", "playlist non dichiarata")
        val manifest = read(playlist, embed.toString(), "Playlist VixSrc")
        if (!supportedHls(manifest)) throw PlayerFailure("Playlist VixSrc", "formato non supportato")
        val variant = manifest.lineSequence().map { it.trim() }
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        if (variant != null && manifest.contains("#EXT-X-STREAM-INF")) {
            val mediaUrl = URI(playlist).resolve(variant).toString()
            if (URI(mediaUrl).scheme != "https") return@withContext null
            val media = read(mediaUrl, embed.toString(), "Video VixSrc")
            if (!supportedHls(media)) throw PlayerFailure("Video VixSrc", "formato non supportato")
        }
        Playlist(playlist, embed.toString())
    }
}
