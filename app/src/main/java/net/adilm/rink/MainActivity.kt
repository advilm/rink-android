package net.adilm.rink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import uniffi.rink_bridge.evaluateForKotlin
import uniffi.rink_bridge.previewForKotlin

class MainActivity : AppCompatActivity() {

    private lateinit var queryInput: TextInputEditText
    private lateinit var previewOutput: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var historyScroll: ScrollView
    private lateinit var calculatorPage: View
    private lateinit var historyBackCallback: androidx.activity.OnBackPressedCallback
    private val historyEntries = mutableListOf<HistoryEntry>()
    private val historyStore by lazy(LazyThreadSafetyMode.NONE) { HistoryStore(this) }

    private val handler = Handler(Looper.getMainLooper())

    private val previewRunnable = Runnable {
        updatePreview(queryInput.text.toString().trim())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        historyEntries.addAll(historyStore.load())

        title = "Rink"
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
        }

        // Keep content clear of system bars, including on Android 15+.
        val initialLeft = content.paddingLeft
        val initialTop = content.paddingTop
        val initialRight = content.paddingRight
        val initialBottom = content.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            view.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + maxOf(bars.bottom, ime.bottom)
            )

            insets
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "Rink"
                textSize = 30f
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(iconButton(R.drawable.ic_info, "Rink website", ::openRinkWebsite))
            addView(iconButton(R.drawable.ic_book, "Read the Rink manual", ::showManualPage),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(4)
                })
            addView(iconButton(R.drawable.ic_history, "View calculation history", ::showHistoryPage),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(4)
                })
            setPadding(0, 0, 0, dp(8))
        }

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        historyScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(historyContainer)
        }

        queryInput = TextInputEditText(this).apply {
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 18f
            setPadding(dp(16), dp(14), dp(12), dp(14))
        }

        val inputField = TextInputLayout(this).apply {
            hint = "Enter a calculation"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(
                dp(18).toFloat(),
                dp(18).toFloat(),
                dp(18).toFloat(),
                dp(18).toFloat()
            )
            addView(queryInput)
        }

        val evaluateButton = MaterialButton(this).apply {
            text = ""
            icon = getDrawable(R.drawable.ic_keyboard_return)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = 0
            insetTop = 0
            insetBottom = 0
            contentDescription = "Evaluate calculation"
            setIconTint(ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)))
            backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary))
            cornerRadius = dp(18)
            minWidth = dp(56)
            minimumWidth = dp(56)
            minHeight = dp(56)
            minimumHeight = dp(56)
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener {
                handler.removeCallbacks(previewRunnable)
                evaluate()
            }
        }

        previewOutput = TextView(this).apply {
            textSize = 16f
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            contentDescription = "Preview result. Tap to copy."
            setOnClickListener {
                previewOutput.text.toString().takeIf { it.isNotBlank() }?.let(::copyResult)
            }
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        inputRow.addView(
            inputField,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        inputRow.addView(evaluateButton, LinearLayout.LayoutParams(
            dp(56),
            dp(56)
        ).apply { marginStart = dp(10) })
        val inputPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(4))
            addView(MaterialCardView(this@MainActivity).apply {
                radius = dp(18).toFloat()
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
                addView(CappedScrollView(this@MainActivity, dp(200)).apply {
                    isVerticalScrollBarEnabled = true
                    addView(previewOutput)
                }, android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
            addView(inputRow)
        }
        content.addView(historyScroll, weightedContentParams())
        content.addView(inputPanel, spacedParams(top = 8))

        calculatorPage = content
        setContentView(calculatorPage)

        historyBackCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = showCalculatorPage()
        }
        onBackPressedDispatcher.addCallback(this, historyBackCallback)

        queryInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                handler.removeCallbacks(previewRunnable)

                if (s.isNullOrBlank()) {
                    updatePreview("")
                } else {
                    handler.postDelayed(previewRunnable, 250)
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        queryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handler.removeCallbacks(previewRunnable)
                evaluate()
                refocusInputAndKeepKeyboardOpen()
                true
            } else {
                false
            }
        }

        refocusInputAndKeepKeyboardOpen()

        CurrencyLoader.refresh { success, message ->
            if (!success) {
                android.util.Log.w("Rink", message)
            }
        }
    }

    private fun updatePreview(query: String) {
        previewOutput.text = if (query.isBlank()) {
            ""
        } else {
            try {
                previewForKotlin(query)
            } catch (e: Exception) {
                "Error: ${e.message ?: "Evaluation failed"}"
            }
        }
    }

    private fun evaluate() {
        val query = queryInput.text.toString().trim()

        if (query.isEmpty()) {
            return
        }

        val result = try {
            // This commits the result so the next query can use `ans`.
            evaluateForKotlin(query)
        } catch (e: Exception) {
            "Error: ${e.message ?: "Evaluation failed"}"
        }

        val entry = HistoryEntry(query, result)
        historyEntries.add(entry)
        historyStore.save(historyEntries)
        addHistoryCard(entry)

        queryInput.text?.clear()
        queryInput.requestFocus()
        historyScroll.post {
            historyScroll.fullScroll(View.FOCUS_DOWN)
            queryInput.requestFocus()
        }
    }

    private fun addHistoryCard(entry: HistoryEntry) {
        val entryContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(12), dp(12))
        }
        val restoreEntry = View.OnClickListener { restoreHistoryEntry(entry) }
        entryContent.addView(TextView(this).apply {
            text = entry.query
            textSize = 17f
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
            setOnClickListener(restoreEntry)
        })
        val resultRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val resultText = TextView(this).apply {
            text = entry.result
            textSize = 16f
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(4), 0, 0)
            setOnClickListener(restoreEntry)
        }
        resultRow.addView(CappedScrollView(this, dp(180)).apply {
            isVerticalScrollBarEnabled = true
            addView(resultText)
            setOnClickListener(restoreEntry)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        resultRow.addView(copyButton(entry.result), LinearLayout.LayoutParams(
            dp(48), dp(48)
        ).apply { marginStart = dp(4) })
        entryContent.addView(resultRow)
        historyContainer.addView(MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant))
            addView(entryContent)
            setOnClickListener { restoreHistoryEntry(entry) }
        }, spacedParams(top = 8))
    }

    private fun showHistoryPage() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(queryInput.windowToken, 0)
        historyBackCallback.isEnabled = true
        setContentView(HistoryPage(
            context = this,
            entries = historyEntries,
            onBack = ::showCalculatorPage,
            onClear = ::confirmClearHistory,
            onRestore = ::restoreHistoryEntry,
            onCopy = ::copyResult
        ).createView())
    }

    private fun showManualPage() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(queryInput.windowToken, 0)
        historyBackCallback.isEnabled = true
        setContentView(ManualPage(this, ::showCalculatorPage).createView())
    }

    private fun showCalculatorPage() {
        historyBackCallback.isEnabled = false
        setContentView(calculatorPage)
        refocusInputAndKeepKeyboardOpen()
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear history?")
            .setMessage("This will delete all saved calculations.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                historyEntries.clear()
                historyContainer.removeAllViews()
                historyStore.save(historyEntries)
                showHistoryPage()
            }
            .show()
    }

    private fun refocusInputAndKeepKeyboardOpen() {
        queryInput.post {
            queryInput.requestFocus()
            val inputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(queryInput, 0)
        }
    }

    private fun copyResult(result: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Rink result", result))
        Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show()
    }

    private fun copyButton(result: String) = MaterialButton(this).apply {
        text = ""
        icon = getDrawable(R.drawable.ic_copy)
        iconPadding = 0
        insetTop = 0
        insetBottom = 0
        contentDescription = "Copy result"
        setIconTint(ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)))
        backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant))
        cornerRadius = dp(18)
        setPadding(dp(12), 0, dp(12), 0)
        setOnClickListener { copyResult(result) }
    }

    private fun restoreHistoryEntry(entry: HistoryEntry) {
        queryInput.setText(entry.query)
        queryInput.setSelection(queryInput.text?.length ?: 0)
        showCalculatorPage()
    }

    private fun spacedParams(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun weightedContentParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun iconButton(iconRes: Int, description: String, action: () -> Unit): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonFilledTonalStyle).apply {
            contentDescription = description
            icon = getDrawable(iconRes)
            iconSize = dp(24)
            setOnClickListener { action() }
        }

    private fun openRinkWebsite() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://rinkcalc.app/")))
    }

    override fun onDestroy() {
        handler.removeCallbacks(previewRunnable)
        super.onDestroy()
    }
}

private class CappedScrollView(
    context: Context,
    private val maxHeightPx: Int
) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cappedHeightSpec = View.MeasureSpec.makeMeasureSpec(
            maxHeightPx,
            View.MeasureSpec.AT_MOST
        )
        super.onMeasure(widthMeasureSpec, cappedHeightSpec)
    }
}
