package com.frenky.cloudstream

enum class SearchMode { NONE, WORDPRESS, WORDPRESS_ROOT, DLE_GET }

data class Source(
    val id: String,
    val displayName: String,
    val fallbackUrl: String,
    val searchMode: SearchMode = SearchMode.NONE,
    val isSeries: Boolean = false
)

object SourceList {
    val all = listOf(
        Source("filmpertutti", "FilmPerTutti", "https://film-pertutti.fit/", SearchMode.DLE_GET),
        Source("casacinema", "CasaCinema", "https://casa-cinema.boats/", SearchMode.DLE_GET)
    )
}
