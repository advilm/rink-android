package net.adilm.rink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.json.JSONObject

internal class ManualPage(
    private val activity: Activity,
    private val onBack: () -> Unit
) {
    fun createView(): View {
        val page = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(color(com.google.android.material.R.attr.colorSurface))
        }
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(MaterialButton(
                activity,
                null,
                com.google.android.material.R.attr.materialIconButtonFilledTonalStyle
            ).apply {
                contentDescription = "Back to calculator"
                icon = activity.getDrawable(R.drawable.ic_arrow_back)
                iconSize = dp(24)
                setOnClickListener { onBack() }
            })
            addView(TextView(activity).apply {
                text = "Manual"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(com.google.android.material.R.attr.colorOnSurface))
                setPadding(dp(12), 0, 0, 0)
            })
        }
        page.addView(header)

        val manual = WebView(activity).apply {
            settings.javaScriptEnabled = true
            webViewClient = manualWebViewClient(page)
            loadUrl("file:///android_asset/manual.html")
        }
        page.addView(manual, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply { topMargin = dp(8) })

        val initialPadding = page.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(page) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialPadding + bars.left,
                initialPadding + bars.top,
                initialPadding + bars.right,
                initialPadding + bars.bottom
            )
            insets
        }
        return page
    }

    private fun manualWebViewClient(page: View) = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val surface = colorHex(page, com.google.android.material.R.attr.colorSurface)
            val onSurface = colorHex(page, com.google.android.material.R.attr.colorOnSurface)
            val primary = colorHex(page, com.google.android.material.R.attr.colorPrimary)
            val surfaceVariant = colorHex(page, com.google.android.material.R.attr.colorSurfaceVariant)
            val codeBackground = colorHex(page, com.google.android.material.R.attr.colorSurfaceContainerHighest)
            val css = """
                html, body { background: $surface !important; color: $onSurface !important; }
                body * { color: $onSurface !important; }
                body { box-sizing: border-box; max-width: 54rem; margin: 0 auto !important;
                    padding: 8px !important; font: 16px/1.55 sans-serif !important; }
                body.toc2 { padding-left: 8px !important; padding-right: 8px !important; }
                #header > h1:first-child { font-size: 1.8em; margin: .2em 0 .6em; }
                h1, h2, h3, h4, h5, h6 { color: $onSurface !important; line-height: 1.25; }
                a, #content a, #toc a { color: $primary !important; }
                #toc, table, .exampleblock > .content { background: $surfaceVariant !important; color: $onSurface !important; }
                #footer { background: $surfaceVariant !important; color: $onSurface !important; }
                #footer * { color: $onSurface !important; }
                .listingblock > .content, .literalblock > .content,
                .listingblock > .content pre, .literalblock > .content pre {
                    background: $codeBackground !important; border-radius: 8px;
                }
                pre, pre *, code { color: $onSurface !important; }
                code { background: $codeBackground !important; border-radius: 4px; padding: 0 .2em; }
                #content table *, #toc * { color: $onSurface !important; }
                pre { white-space: pre-wrap; overflow-wrap: anywhere; }
                img { max-width: 100%; height: auto; }
            """.trimIndent()
            view.evaluateJavascript(
                "(function(){var s=document.createElement('style');s.textContent=${JSONObject.quote(css)};document.head.appendChild(s);})()",
                null
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (uri.scheme == "file" && uri.lastPathSegment != "manual.html") {
                val websitePath = when (uri.lastPathSegment) {
                    "rink.1.html" -> "cli-manpage"
                    "rink.5.html" -> "configuration"
                    "rink-defs.5.html" -> "definitions-format"
                    "rink-dates.5.html" -> "datepatterns-format"
                    else -> return true
                }
                openUri(Uri.parse("https://rinkcalc.app/$websitePath"))
                return true
            }
            if (uri.scheme == "http" || uri.scheme == "https") {
                openUri(uri)
                return true
            }
            return false
        }
    }

    private fun openUri(uri: Uri) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun color(attribute: Int): Int =
        MaterialColors.getColor(activity.window.decorView, attribute)

    private fun colorHex(view: View, attribute: Int): String =
        String.format("#%06X", 0xFFFFFF and MaterialColors.getColor(view, attribute))

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
