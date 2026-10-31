package com.example.pawcare.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.pawcare.R
import com.example.pawcare.core.ui.AppIcon
import com.example.pawcare.feature.articles.ArticleDetailRoute
import com.example.pawcare.feature.articles.ArticlesRoute
import com.example.pawcare.feature.calendar.CalendarRoute
import com.example.pawcare.feature.care.CareDetailRoute
import com.example.pawcare.feature.care.CareEditorRoute
import com.example.pawcare.feature.pet.PetEditorRoute
import com.example.pawcare.feature.pet.PetRoute
import com.example.pawcare.feature.records.ContactsRoute
import com.example.pawcare.feature.records.DocumentsRoute
import com.example.pawcare.feature.records.HistoryRoute
import com.example.pawcare.feature.records.WeightRoute
import com.example.pawcare.feature.settings.SettingsRoute
import com.example.pawcare.feature.startup.StartupScreen
import com.example.pawcare.feature.startup.StartupState
import com.example.pawcare.feature.startup.StartupViewModel
import com.example.pawcare.feature.today.TodayRoute
import kotlinx.serialization.Serializable

@Serializable
data class ArticleDestination(val id: String) : NavKey

@Serializable
data object Home : NavKey

@Serializable
data class PetEditor(val id: String? = null) : NavKey

@Serializable
data class CareEditor(
    val petId: String? = null,
    val id: String? = null,
    val series: Boolean = false,
    val past: Boolean = false,
) : NavKey

@Serializable
data class EventDetail(val id: String, val action: String = "") : NavKey

@Serializable
data class Records(val section: String, val petId: String) : NavKey

@Serializable
data object SettingsDestination : NavKey

@Composable
fun PawCareApp(notificationEvent: String?, acknowledgeNotification: () -> Unit) {
    val startup: StartupViewModel = hiltViewModel()
    val startupState by startup.uiState.collectAsStateWithLifecycle()
    if (startupState.status != StartupState.READY) {
        StartupScreen(state = startupState, onRetry = startup::retry)
        return
    }

    val backStack = rememberNavBackStack(Home)
    val back: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
    LaunchedEffect(notificationEvent) {
        if (notificationEvent != null) {
            if (backStack.lastOrNull() != EventDetail(notificationEvent)) {
                backStack.add(EventDetail(notificationEvent))
            }
            acknowledgeNotification()
        }
    }
    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    onPetEditor = {
                        backStack.add(PetEditor(it))
                    },
                    onCareEditor = {
                        backStack.add(CareEditor(petId = it))
                    },
                    onArticle = {
                        backStack.add(ArticleDestination(it))
                    },
                    onEvent = {
                        backStack.add(EventDetail(it))
                    },
                    onQuickAction = { id, action ->
                        backStack.add(EventDetail(id, action))
                    },
                    onSection = { section, pet ->
                        backStack.add(Records(section, pet))
                    },
                    onSettings = {
                        backStack.add(SettingsDestination)
                    },
                )
            }
            entry<PetEditor> { key ->
                PetEditorRoute(key.id, back)
            }
            entry<CareEditor> { key ->
                CareEditorRoute(key.petId, key.id, key.series, key.past, back)
            }
            entry<EventDetail> { key ->
                CareDetailRoute(
                    id = key.id,
                    onBack = back,
                    initialAction = key.action,
                    onEdit = { id, series ->
                        backStack.add(CareEditor(id = id, series = series))
                    },
                )
            }
            entry<Records> { key ->
                when (key.section) {
                    "history" -> HistoryRoute(
                        key.petId,
                        back,
                        {
                            backStack.add(EventDetail(it))
                        },
                        {
                            backStack.add(CareEditor(petId = key.petId, past = true))
                        },
                    )
                    "weight" -> WeightRoute(key.petId, back)
                    "documents" -> DocumentsRoute(key.petId, back)
                    "contacts" -> ContactsRoute(key.petId, back)
                }
            }
            entry<ArticleDestination> { key ->
                ArticleDetailRoute(key.id, back)
            }
            entry<SettingsDestination> {
                SettingsRoute(back)
            }
        },
    )
}

private enum class HomeTab(val id: Int, val label: Int, val icon: Int) {
    TODAY(0, R.string.today, R.drawable.ic_today),
    CALENDAR(1, R.string.calendar, R.drawable.ic_calendar),
    ARTICLES(3, R.string.articles, R.drawable.ic_articles),
    PET(2, R.string.pet, R.drawable.ic_pet),
}

@Composable
private fun HomeScreen(
    onPetEditor: (String?) -> Unit,
    onCareEditor: (String?) -> Unit,
    onEvent: (String) -> Unit,
    onSection: (String, String) -> Unit,
    onSettings: () -> Unit,
    onQuickAction: (String, String) -> Unit,
    onArticle: (String) -> Unit,
) {
    var selected by rememberSaveable {
        mutableIntStateOf(0)
    }
    val holder = rememberSaveableStateHolder()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.id,
                        onClick = {
                            selected = tab.id
                        },
                        icon = {
                            AppIcon(tab.icon)
                        },
                        label = {
                            Text(stringResource(tab.label))
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            holder.SaveableStateProvider(selected) {
                when (selected) {
                    0 -> TodayRoute(
                        {
                            onPetEditor(null)
                        },
                        onCareEditor,
                        onEvent,
                        onQuickAction,
                        onArticle,
                        {
                            selected = 3
                        },
                    )
                    1 -> CalendarRoute(onEvent)
                    2 -> PetRoute(onPetEditor, onSection, onSettings)
                    3 -> ArticlesRoute(onArticle)
                }
            }
        }
    }
}
