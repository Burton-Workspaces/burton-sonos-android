package com.burton.sonos.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burton.sonos.domain.Household
import com.burton.sonos.domain.NamedGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
    val namedGroups: Flow<List<NamedGroup>> = store.data.map { prefs ->
        NamedGroupCache.decode(prefs[NAMED_GROUPS].orEmpty())
    }

    suspend fun selectedGroup(): String? = selectedGroupId.first()

    suspend fun setSelectedGroupId(id: String) {
        store.edit { it[SELECTED_GROUP] = id }
    }

    suspend fun lastSpeakerIp(): String? = store.data.map { it[LAST_SPEAKER_IP] }.first()

    suspend fun setLastSpeakerIp(ip: String) {
        store.edit { it[LAST_SPEAKER_IP] = ip }
    }

    suspend fun cachedHousehold(): Household? =
        HouseholdCache.decode(store.data.map { it[CACHED_HOUSEHOLD].orEmpty() }.first())

    suspend fun setCachedHousehold(household: Household) {
        store.edit { it[CACHED_HOUSEHOLD] = HouseholdCache.encode(household) }
    }

    suspend fun saveNamedGroups(groups: List<NamedGroup>) {
        store.edit { it[NAMED_GROUPS] = NamedGroupCache.encode(groups) }
    }

    suspend fun updateNamedGroups(transform: (List<NamedGroup>) -> List<NamedGroup>) {
        store.edit { prefs ->
            val current = NamedGroupCache.decode(prefs[NAMED_GROUPS].orEmpty())
            prefs[NAMED_GROUPS] = NamedGroupCache.encode(transform(current))
        }
    }

    private companion object {
        val SELECTED_GROUP = stringPreferencesKey("selected_group")
        val LAST_SPEAKER_IP = stringPreferencesKey("last_speaker_ip")
        val CACHED_HOUSEHOLD = stringPreferencesKey("cached_household")
        val NAMED_GROUPS = stringPreferencesKey("named_groups")
    }
}
