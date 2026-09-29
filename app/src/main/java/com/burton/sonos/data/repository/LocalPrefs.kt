package com.burton.sonos.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    private companion object {
        val SELECTED_GROUP = stringPreferencesKey("selected_group")
    }
}
