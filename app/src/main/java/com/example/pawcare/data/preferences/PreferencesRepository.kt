package com.example.pawcare.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings")
data class UserPreferences(val activePetId: String? = null, val notifications: Boolean = true, val defaultTime: LocalTime = LocalTime.of(9, 0), val onboarding: Boolean = false)
@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.settingsStore
    private val petKey = stringPreferencesKey("active_pet")
    private val notificationsKey = booleanPreferencesKey("notifications")
    private val timeKey = stringPreferencesKey("default_time")
    private val onboardingKey = booleanPreferencesKey("onboarding")
    val preferences = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        UserPreferences(it[petKey], it[notificationsKey] ?: true, it[timeKey]?.let(LocalTime::parse) ?: LocalTime.of(9, 0), it[onboardingKey] ?: false)
    }
    suspend fun selectPet(id: String?) { store.edit { if (id == null) it.remove(petKey) else it[petKey] = id } }
    suspend fun setNotifications(value: Boolean) { store.edit { it[notificationsKey] = value } }
    suspend fun setTime(value: LocalTime) { store.edit { it[timeKey] = value.toString() } }
    suspend fun finishOnboarding() { store.edit { it[onboardingKey] = true } }
}
