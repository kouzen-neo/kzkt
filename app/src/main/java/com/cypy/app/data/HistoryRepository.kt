package com.cypy.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "cypy_history")

/** A finished translation record shown in the Riwayat tab. */
data class HistoryEntry(
    val timestamp: Long,
    val fileName: String,
    val outputPath: String,
    val pageCount: Int,
    val provider: String,
    val targetLanguage: String,
)

/**
 * History persistence via DataStore Preferences.
 * Stores a JSON-encoded list of [HistoryEntry] in a single string key —
 * no Room, no schema migration, safe to add alongside [SettingsRepository].
 */
class HistoryRepository(private val context: Context) {

    private companion object {
        val KEY_ENTRIES = stringPreferencesKey("entries")
        val KEY_REV = stringPreferencesKey("rev")
    }

    private val gson = Gson()

    val entriesFlow: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_ENTRIES]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val arr = gson.fromJson(json, Array<HistoryEntry>::class.java)
                arr.toList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** Newest first. Also keeps a monotonically increasing revision to bust any caching. */
    suspend fun record(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_ENTRIES]
            val existing = if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    gson.fromJson(json, Array<HistoryEntry>::class.java).toList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val updated = (listOf(entry) + existing).sortedByDescending { it.timestamp }
            prefs[KEY_ENTRIES] = gson.toJson(updated)
            prefs[KEY_REV] = (System.currentTimeMillis()).toString()
        }
    }

    suspend fun delete(timestamp: Long) {
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_ENTRIES]
            if (json.isNullOrBlank()) return@edit
            try {
                val updated = gson.fromJson(json, Array<HistoryEntry>::class.java)
                    .filterNot { it.timestamp == timestamp }
                prefs[KEY_ENTRIES] = gson.toJson(updated)
                prefs[KEY_REV] = (System.currentTimeMillis()).toString()
            } catch (_: Exception) {
                // corrupt entry list — leave as is
            }
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { prefs ->
            prefs[KEY_ENTRIES] = "[]"
            prefs[KEY_REV] = (System.currentTimeMillis()).toString()
        }
    }
}
