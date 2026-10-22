package net.adilm.rink

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uniffi.rink_bridge.evaluateForKotlin
import uniffi.rink_bridge.previewForKotlin

class MainActivity : Activity() {

    private lateinit var queryInput: EditText
    private lateinit var output: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val previewRunnable = Runnable {
        val query = queryInput.text.toString().trim()

        if (query.isEmpty()) {
            output.text = "Your result will appear here."
        } else {
            try {
                output.text = previewForKotlin(query)
            } catch (e: Exception) {
                output.text = "Error: ${e.message ?: "Evaluation failed"}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Rink"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
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

            view.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + bars.bottom
            )

            insets
        }

        val heading = TextView(this).apply {
            text = "Rink Calculatoar"
            textSize = 26f
            gravity = Gravity.CENTER_VERTICAL
        }

        queryInput = EditText(this).apply {
            hint = "Enter a calculation"
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 18f
        }

        val evaluateButton = Button(this).apply {
            text = "Evaluate"
            setOnClickListener {
                handler.removeCallbacks(previewRunnable)
                evaluate()
            }
        }

        val resultHeading = TextView(this).apply {
            text = "Result"
            textSize = 18f
        }

        output = TextView(this).apply {
            text = "Your result will appear here."
            textSize = 20f
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(8))
        }

        val outputScroll = ScrollView(this).apply {
            addView(output)
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            queryInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        )

        content.addView(
            evaluateButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        content.addView(
            resultHeading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        )

        content.addView(
            outputScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(content)

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
                    output.text = "Your result will appear here."
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
                true
            } else {
                false
            }
        }

        CurrencyLoader.refresh { success, message ->
            runOnUiThread {
                if (success) {
                    // Live currency rates are now loaded.
                    // Optionally refresh the current preview here.
                } else {
                    // Keep the calculator usable; report the problem if appropriate.
                    android.util.Log.w("Rink", message)
                }
            }
        }
    }

    private fun evaluate() {
        val query = queryInput.text.toString().trim()

        if (query.isEmpty()) {
            output.text = "Enter a calculation first."
            return
        }

        try {
            // This commits the result so the next query can use `ans`.
            output.text = evaluateForKotlin(query)
        } catch (e: Exception) {
            output.text = "Error: ${e.message ?: "Evaluation failed"}"
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(previewRunnable)
        super.onDestroy()
    }
}