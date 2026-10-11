package com.frenky.extra.guardoserie

import com.frenky.cloudstream.ConfigurableSourceProvider
import com.frenky.cloudstream.SourceList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink

class GuardoSerieProvider : ConfigurableSourceProvider(SourceList.all.single { it.id == "guardoserie_new" }) {
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val found = super.loadLinks(data, isCasting, subtitleCallback, callback)
        if (!found && !isCasting && GuardoSeriePages.page(data) != null) {
            GuardoSerieWebPlayer.offer(name, data)
        }
        return found
    }
}
