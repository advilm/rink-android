package net.adilm.rink

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class HistoryEntry(val query: String, val result: String)

internal class HistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<HistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(HISTORY_KEY, "[]") ?: "[]")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            HistoryEntry(item.getString("query"), item.getString("result"))
        }
    }.getOrDefault(emptyList())

    fun save(entries: List<HistoryEntry>) {
        val json = JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject().put("query", entry.query).put("result", entry.result))
            }
        }
        preferences.edit().putString(HISTORY_KEY, json.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "rink"
        const val HISTORY_KEY = "history"
    }
}
