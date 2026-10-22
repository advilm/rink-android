package net.adilm.rink

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import uniffi.rink_bridge.loadCurrencyDataForKotlin

object CurrencyLoader {
    private const val TAG = "CurrencyLoader"
    private const val CURRENCY_URL =
        "https://rinkcalc.app/data/currency.json"

    private val executor = Executors.newSingleThreadExecutor()

    fun refresh(onComplete: (Boolean, String) -> Unit) {
        executor.execute {
            var connection: HttpURLConnection? = null

            val result = runCatching {
                connection = URL(CURRENCY_URL)
                    .openConnection() as HttpURLConnection

                connection!!.requestMethod = "GET"
                connection!!.connectTimeout = 10_000
                connection!!.readTimeout = 10_000

                check(connection!!.responseCode == 200) {
                    "HTTP ${connection!!.responseCode}"
                }

                val liveData = connection!!.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val loadResult = loadCurrencyDataForKotlin(liveData)

                check(loadResult == "OK") {
                    loadResult
                }
            }

            connection?.disconnect()

            result.onFailure {
                Log.w(TAG, "Currency refresh failed", it)
            }

            onComplete(
                result.isSuccess,
                result.exceptionOrNull()?.message ?: "Currency rates updated"
            )
        }
    }
}