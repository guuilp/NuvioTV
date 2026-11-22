package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authSessionCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_session_cache_store",
    corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler { androidx.datastore.preferences.core.emptyPreferences() }
)

/**
 * The last account that was confirmed to be signed in on this device.
 *
 * Launch chain: this is a purely local record. It lets the app publish an
 * [com.nuvio.tv.domain.model.AuthState.FullAccount] from disk before the
 * network validation of the Supabase session resolves, so the app surface is
 * never gated on an HTTP round-trip.
 */
data class CachedAuthSession(
    val userId: String,
    val email: String
)

@Singleton
class AuthSessionCacheDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.authSessionCacheDataStore
    private val userIdKey = stringPreferencesKey("cached_user_id")
    private val emailKey = stringPreferencesKey("cached_email")

    /** Local-only read. Never performs a network call. */
    suspend fun read(): CachedAuthSession? = withContext(Dispatchers.IO) {
        val preferences = dataStore.data.first()
        val userId = preferences[userIdKey]?.takeIf { it.isNotBlank() } ?: return@withContext null
        val email = preferences[emailKey]?.takeIf { it.isNotBlank() } ?: return@withContext null
        CachedAuthSession(userId = userId, email = email)
    }

    suspend fun write(userId: String, email: String) {
        if (userId.isBlank() || email.isBlank()) return
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[userIdKey] = userId
                preferences[emailKey] = email
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences.remove(userIdKey)
                preferences.remove(emailKey)
            }
        }
    }
}
