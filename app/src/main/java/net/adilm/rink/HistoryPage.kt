package net.adilm.rink

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

internal class HistoryPage(
    private val context: Context,
    private val entries: List<HistoryEntry>,
    private val onBack: () -> Unit,
    private val onClear: () -> Unit,
    private val onRestore: (HistoryEntry) -> Unit,
    private val onCopy: (String) -> Unit
) {
    fun createView(): View {
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(color(this, MaterialR.attr.colorSurface))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(iconButton(R.drawable.ic_arrow_back, "Back to calculator", onBack))
            addView(TextView(context).apply {
                text = "History"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(color(this, MaterialR.attr.colorOnSurface))
                setPadding(dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(MaterialButton(context).apply {
                text = "Clear"
                setOnClickListener { onClear() }
            })
        }
        page.addView(header)

        val historyList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        if (entries.isEmpty()) {
            historyList.addView(TextView(context).apply {
                text = "No calculations yet"
                textSize = 16f
                setTextColor(color(this, MaterialR.attr.colorOnSurface))
                setPadding(dp(16), dp(12), dp(16), dp(12))
            })
        } else {
            entries.forEach { entry -> historyList.addView(historyCard(entry), spacedParams(8)) }
        }

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(historyList)
        }
        page.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply { topMargin = dp(8) })

        val originalTopPadding = page.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(page) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                dp(16) + bars.left,
                originalTopPadding + bars.top,
                dp(16) + bars.right,
                dp(16) + bars.bottom
            )
            insets
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return page
    }

    private fun historyCard(entry: HistoryEntry): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12))
            addView(TextView(context).apply {
                text = entry.query
                textSize = 17f
                setTextColor(color(this, MaterialR.attr.colorOnSurface))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = entry.result
                    textSize = 16f
                    setTextColor(color(this, MaterialR.attr.colorOnSurfaceVariant))
                    setPadding(0, dp(4), 0, 0)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(copyButton(entry.result), LinearLayout.LayoutParams(dp(48), dp(48)))
            })
        }
        return MaterialCardView(context).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = color(this, MaterialR.attr.colorOutline)
            setCardBackgroundColor(color(this, MaterialR.attr.colorSurfaceVariant))
            addView(content)
            setOnClickListener { onRestore(entry) }
        }
    }

    private fun copyButton(result: String) = MaterialButton(context).apply {
        text = ""
        icon = context.getDrawable(R.drawable.ic_copy)
        iconPadding = 0
        insetTop = 0
        insetBottom = 0
        contentDescription = "Copy result"
        setIconTint(android.content.res.ColorStateList.valueOf(color(this, MaterialR.attr.colorOnSurfaceVariant)))
        backgroundTintList = android.content.res.ColorStateList.valueOf(color(this, MaterialR.attr.colorSurfaceVariant))
        cornerRadius = dp(18)
        setPadding(dp(12), 0, dp(12), 0)
        setOnClickListener { onCopy(result) }
    }

    private fun iconButton(iconRes: Int, description: String, action: () -> Unit) =
        MaterialButton(context, null, MaterialR.attr.materialIconButtonFilledTonalStyle).apply {
            contentDescription = description
            icon = context.getDrawable(iconRes)
            iconSize = dp(24)
            setOnClickListener { action() }
        }

    private fun color(view: View, attribute: Int) = MaterialColors.getColor(view, attribute)

    private fun spacedParams(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
