package com.frenky.extra.guardaserie
import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
@CloudstreamPlugin
class GuardaSeriePlugin : Plugin() {
    override fun load(context: Context) { registerMainAPI(GuardaSerieProvider()) }
}
