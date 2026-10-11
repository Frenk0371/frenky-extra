package com.frenky.cloudstream

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class PosterFinderTest {
    @Test fun dleCssPosterIsAbsolute() {
        val doc = Jsoup.parse("""<div class="posts"><a href="/stream/test.html"><div style="background-image: url(/uploads/cover.jpg)"><div class="title">Test</div></div></a></div>""", "https://film-pertutti.fit/?do=search")
        val anchor = doc.selectFirst("a")!!
        assertEquals("https://film-pertutti.fit/uploads/cover.jpg", PosterFinder.fromSearchCard(anchor, anchor.parent()))
    }
    @Test fun savedDleDetailFindsCoverWithoutSearchCache() {
        val doc = Jsoup.parse("""<img src="/logo.png"><section id="content"><img class="wp-post-image" src="/uploads/cover.jpg"></section>""", "https://film-pertutti.fit/stream/test.html")
        assertEquals("https://film-pertutti.fit/uploads/cover.jpg", PosterFinder.fromDocument(doc))
    }
    @Test fun lazySeriePosterIsPreferredToSidebar() {
        val doc = Jsoup.parse("""<div id="mv-info"><img src="data:image/svg+xml;base64,abc" data-src="https://image.tmdb.org/t/p/w185/cover.jpg"></div><main><img src="/sidebar.jpg"></main>""", "https://guardoseries.sbs/serie/test/")
        assertEquals("https://image.tmdb.org/t/p/w185/cover.jpg", PosterFinder.fromDocument(doc))
    }
}
