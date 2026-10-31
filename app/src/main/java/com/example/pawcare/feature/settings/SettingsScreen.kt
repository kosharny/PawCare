package com.example.pawcare.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import com.example.pawcare.R
import com.example.pawcare.core.notifications.ReminderScheduler
import com.example.pawcare.core.ui.*
import com.example.pawcare.data.preferences.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: PreferencesRepository, private val scheduler: ReminderScheduler) : OperationViewModel() {
    val uiState = preferences.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())
    fun notifications(value: Boolean) = perform { preferences.setNotifications(value); scheduler.requestSync() }
    fun setTime(value: String) = perform { preferences.setTime(LocalTime.parse(value)); scheduler.requestSync() }
    fun sync() { scheduler.requestSync() }
}
@Composable fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    SettingsScreen(state, operation, onBack, viewModel::notifications, viewModel::setTime, viewModel::sync, viewModel::dismissError)
}
@Composable fun SettingsScreen(state: UserPreferences, operation: OperationState, onBack: () -> Unit, onNotifications: (Boolean) -> Unit, onTime: (String) -> Unit, onSync: () -> Unit, onDismissError: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var channelEnabled by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        channelEnabled = context.getSystemService(android.app.NotificationManager::class.java).getNotificationChannel(com.example.pawcare.core.notifications.REMINDER_CHANNEL)?.importance != android.app.NotificationManager.IMPORTANCE_NONE
        onSync()
        onPauseOrDispose { }
    }
    var explanation by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed -> enabled = allowed; onSync() }
    Page(stringResource(R.string.settings), onBack) {
        ErrorState(operation.error, onDismissError)
        Toggle(stringResource(R.string.notifications), state.notifications) {
            onNotifications(it)
            if (it && !enabled && android.os.Build.VERSION.SDK_INT >= 33) explanation = true
        }
        Text(stringResource(if (enabled && channelEnabled) R.string.notifications_enabled else R.string.notifications_disabled))
        OutlinedButton({ context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(stringResource(R.string.system_settings)) }
        TimeField(stringResource(R.string.default_time), state.defaultTime.toString(), onTime)
        Text(stringResource(R.string.notification_timing), style = MaterialTheme.typography.bodyMedium)
        Choice(stringResource(R.string.language), AppCompatDelegate.getApplicationLocales().toLanguageTags(), listOf("" to stringResource(R.string.system_language), "ru" to stringResource(R.string.language_ru), "uk" to stringResource(R.string.language_uk), "pl" to stringResource(R.string.language_pl), "en" to stringResource(R.string.language_en))) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
        }
        SectionHeader(stringResource(R.string.about))
        Text(stringResource(R.string.local_data))
        Text(stringResource(R.string.app_name) + " 1.0", style = MaterialTheme.typography.labelMedium)
    }
    if (explanation) AlertDialog(onDismissRequest = { explanation = false }, text = { Text(stringResource(R.string.reminder_explanation)) }, confirmButton = {
        TextButton({ explanation = false; if (android.os.Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.allow)) }
    }, dismissButton = { TextButton({ explanation = false }) { Text(stringResource(R.string.cancel)) } })
}
