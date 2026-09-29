package com.burton.sonos.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burton.sonos.domain.SpotifyAuthTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.burtonStore: DataStore<Preferences> by preferencesDataStore("burton_sonos")

@Singleton
class LocalPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.burtonStore

    val selectedGroupId = store.data.map { it[SELECTED_GROUP] }

    suspend fun selectedGroup(): String? = selectedGroupId.first()

    suspend fun setSelectedGroupId(id: String) {
        store.edit { it[SELECTED_GROUP] = id }
    }

    suspend fun spotifyTokens(): SpotifyAuthTokens? {
        val data = store.data.first()
        val token = data[SPOTIFY_TOKEN] ?: return null
        val key = data[SPOTIFY_KEY] ?: return null
        return SpotifyAuthTokens(token, key)
    }

    suspend fun saveSpotifyTokens(tokens: SpotifyAuthTokens) {
        store.edit {
            it[SPOTIFY_TOKEN] = tokens.authToken
            it[SPOTIFY_KEY] = tokens.privateKey
        }
    }

    private companion object {
        val SELECTED_GROUP = stringPreferencesKey("selected_group")
        val SPOTIFY_TOKEN = stringPreferencesKey("spotify_token")
        val SPOTIFY_KEY = stringPreferencesKey("spotify_key")
    }
}
