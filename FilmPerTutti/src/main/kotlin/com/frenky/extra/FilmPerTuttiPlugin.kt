package com.frenky.extra.filmpertutti

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.frenky.cloudstream.ConfigurableSourceProvider
import com.frenky.cloudstream.SourceList

@CloudstreamPlugin
class FilmPerTuttiPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ConfigurableSourceProvider(SourceList.all.single { it.id == "filmpertutti" }))
    }
}
