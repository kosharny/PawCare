package com.example.pawcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import com.example.pawcare.app.PawCareApp
import com.example.pawcare.core.notifications.AndroidReminderScheduler
import com.example.pawcare.ui.theme.PawCareTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var scheduler: AndroidReminderScheduler
    private val notificationEvent = mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        lifecycleScope.launch(Dispatchers.IO) { scheduler.initialize() }
        if (savedInstanceState == null) readNotification(intent)
        setContent { PawCareTheme { PawCareApp(notificationEvent.value) { notificationEvent.value = null; intent.data = null } } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readNotification(intent) }
    private fun readNotification(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "pawcare" || uri.host != "event" || uri.pathSegments.size != 1) return
        notificationEvent.value = runCatching { UUID.fromString(uri.pathSegments.single()).toString() }.getOrNull()
    }
    override fun onResume() { super.onResume(); lifecycleScope.launch(Dispatchers.IO) { scheduler.requestSync() } }
}
