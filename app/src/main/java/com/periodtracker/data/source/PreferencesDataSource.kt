package com.periodtracker.data.source

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.periodtracker.data.backup.JournalJson
import com.periodtracker.domain.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.journalPreferences by preferencesDataStore("journal_preferences")
class PreferencesDataSource(context: Context) {
    private val store = context.applicationContext.journalPreferences
    private val key = stringPreferencesKey("user_preferences_v1")
    val preferences = store.data.map { it[key]?.let(JournalJson::decodePreferences) ?: UserPreferences() }
    suspend fun read(): UserPreferences = preferences.first()
    suspend fun write(value: UserPreferences) { store.edit { it[key] = JournalJson.encodePreferences(value) } }
}
