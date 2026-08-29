package com.basemax.smsforwarder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Settings(private val context: Context) {

    val baseUrl: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: "" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API_KEY] ?: "" }
    val deviceId: Flow<String> = context.dataStore.data.map { it[KEY_DEVICE_ID] ?: "" }
    val uploadedTotal: Flow<Int> = context.dataStore.data.map { it[KEY_UPLOADED] ?: 0 }

    /**
     * How far the sweep has read through the SMS provider, in the provider's
     * own raw `DATE` units. Opaque: it is a position in a table, not an
     * instant, and is never formatted or compared against a clock.
     *
     * Installs from before the two were separated stored that same raw value
     * under the old "last_sync_ms" key, so it is read as the fallback and an
     * upgrading phone resumes where it left off instead of re-uploading its
     * whole inbox.
     */
    val syncCursor: Flow<Long> =
        context.dataStore.data.map { it[KEY_SYNC_CURSOR] ?: it[KEY_LEGACY_LAST_SYNC] ?: 0L }

    /** When the last sync actually finished: UTC epoch ms, this phone's clock. */
    val lastSyncMs: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SYNC_AT] ?: 0L }

    /**
     * Server clock minus phone clock, in milliseconds, as of the last upload.
     * Positive means this phone is running behind. A large value is the one
     * hint a user gets that their date is wrong, which would otherwise show up
     * only as messages filed under the wrong day.
     */
    val clockSkewMs: Flow<Long> = context.dataStore.data.map { it[KEY_CLOCK_SKEW] ?: 0L }

    suspend fun setConfig(baseUrl: String, apiKey: String, deviceId: String) {
        context.dataStore.edit {
            it[KEY_BASE_URL] = baseUrl.trim()
            it[KEY_API_KEY] = apiKey.trim()
            it[KEY_DEVICE_ID] = deviceId.trim()
        }
    }

    suspend fun setSyncCursor(value: Long) {
        context.dataStore.edit { it[KEY_SYNC_CURSOR] = value }
    }

    suspend fun setLastSyncAt(epochMs: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC_AT] = epochMs }
    }

    suspend fun setClockSkewMs(value: Long) {
        context.dataStore.edit { it[KEY_CLOCK_SKEW] = value }
    }

    suspend fun addUploaded(delta: Int) {
        if (delta <= 0) return
        context.dataStore.edit { it[KEY_UPLOADED] = (it[KEY_UPLOADED] ?: 0) + delta }
    }

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_SYNC_CURSOR = longPreferencesKey("sync_cursor")
        private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at_ms")
        private val KEY_CLOCK_SKEW = longPreferencesKey("clock_skew_ms")
        private val KEY_UPLOADED = intPreferencesKey("uploaded_total")

        // Written by versions that used one value for both the provider
        // cursor and the "last sync" shown on screen. Read, never written.
        private val KEY_LEGACY_LAST_SYNC = longPreferencesKey("last_sync_ms")
    }
}
