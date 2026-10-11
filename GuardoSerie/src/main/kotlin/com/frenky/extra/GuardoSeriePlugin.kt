package com.frenky.extra.guardoserie

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GuardoSeriePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(GuardoSerieProvider())
    }
}
