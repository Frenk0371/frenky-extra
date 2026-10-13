package com.frenky.cloudstream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Only this repository's reviewed registry may nominate a replacement domain. */
object DomainRegistry {
    private const val CONFIG_URL = "https://raw.githubusercontent.com/Frenk0371/frenky-extra/main/config/domains.json"
    private val cache = mutableMapOf<String, Pair<Long, String>>()
    private fun valid(url: String): Boolean = runCatching {
        val u = URI(url)
        u.scheme == "https" && !u.host.isNullOrBlank() && u.userInfo == null &&
            u.port in setOf(-1, 443) && u.query == null && u.fragment == null
    }.getOrDefault(false)

    private fun read(url: String, limit: Int): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= limit) { "Oversized response" }
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
        } finally { connection.disconnect() }
    }

    suspend fun resolve(sourceId: String, fallback: String): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[sourceId] }?.let {
            if (now - it.first < 3_600_000) return@withContext it.second
        }
        val resolved = runCatching {
            val entries = JSONObject(read(CONFIG_URL, 150000)).getJSONArray("sources")
            val entry = (0 until entries.length()).map { entries.getJSONObject(it) }
                .single { it.getString("id") == sourceId }
            val candidate = entry.getString("current_url")
            check(valid(candidate))
            if (URI(candidate).host != URI(fallback).host) {
                val review = entry.getJSONObject("identity_review")
                check(review.getBoolean("approved"))
                check(review.getString("verified_domain") == URI(candidate).host)
                val evidence = review.getString("official_evidence_url")
                check(valid(evidence) && URI(evidence).host == URI(fallback).host)
                check(review.getString("reviewed_at").isNotBlank())
                val doc = Jsoup.parse(read(candidate, 600000), candidate)
                val actual = doc.title().lowercase().filter { it.isLetterOrDigit() }
                val expected = entry.getString("name").lowercase().filter { it.isLetterOrDigit() }
                check(actual.contains(expected))
                check(doc.select("form, a[href]").size > 10)
            }
            candidate.trimEnd('/')
        }.getOrElse { fallback.trimEnd('/') }
        synchronized(cache) { cache[sourceId] = now to resolved }
        resolved
    }
}
