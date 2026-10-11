package com.frenky.extra.guardoserie

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.lagradost.cloudstream3.CommonActivity

/** Visible, user-operated web player. No extraction, injected scripts or session export. */
object GuardoSerieWebPlayer {
    fun offer(name: String, url: String) {
        val page = GuardoSeriePages.page(url) ?: return
        Handler(Looper.getMainLooper()).post {
            val activity = CommonActivity.activity ?: return@post
            if (activity.isFinishing || activity.isDestroyed) return@post
            AlertDialog.Builder(activity)
                .setTitle("$name — player interno")
                .setMessage("Nessun link disponibile per il lettore diretto. Puoi provare il player web dentro l’app e scegliere il player e premere Play sul sito. L’avanzamento non viene salvato in CloudStream.")
                .setPositiveButton("Apri player interno") { _, _ -> open(page) }
                .setNegativeButton("Chiudi", null)
                .show()
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun open(page: String) {
        val activity = CommonActivity.activity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val toolbar = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        val label = TextView(activity).apply {
            text = "Player web interno · GuardoSerie"
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
        }
        val close = Button(activity).apply {
            text = "Chiudi"
            setOnClickListener { dialog.dismiss() }
        }
        toolbar.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
        toolbar.addView(close, LinearLayout.LayoutParams(-2, -2))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        val content = FrameLayout(activity)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        val web = WebView(activity)
        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Keep Android WebView's own user agent and normal TLS validation.
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                request == null || (request.isForMainFrame && !GuardoSeriePages.navigation(request.url.toString()))

            @Deprecated("Needed for Android 5 and 6")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                url == null || !GuardoSeriePages.navigation(url)
        }
        var fullscreen: View? = null
        var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
        fun exitFullscreen() {
            fullscreen?.let { content.removeView(it) }
            fullscreen = null
            web.visibility = View.VISIBLE
            fullscreenCallback?.onCustomViewHidden()
            fullscreenCallback = null
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null || fullscreen != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                fullscreen = view
                fullscreenCallback = callback
                web.visibility = View.GONE
                content.addView(view, FrameLayout.LayoutParams(-1, -1))
            }
            override fun onHideCustomView() = exitFullscreen()
        }
        dialog.setContentView(root)
        dialog.setOnDismissListener {
            exitFullscreen()
            web.stopLoading()
            web.onPause()
            content.removeView(web)
            web.removeAllViews()
            web.destroy()
        }
        dialog.show()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        web.loadUrl(page)
    }
}
