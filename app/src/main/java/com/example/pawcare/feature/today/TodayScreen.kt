package com.example.pawcare.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.ui.theme.PawCareTheme
import com.example.pawcare.feature.articles.ArticleCard
import com.example.pawcare.feature.articles.articleLanguage

@Composable fun TodayRoute(onAddPet: () -> Unit, onAddEvent: (String?) -> Unit, onEvent: (String) -> Unit, onQuickAction: (String, String) -> Unit, onArticle: (String) -> Unit, onAllArticles: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = articleLanguage()
    LaunchedEffect(language) { viewModel.loadRecommended(language) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    TodayScreen(state, viewModel::select, onAddPet, onAddEvent, onEvent, onQuickAction, onArticle, onAllArticles)
}
@Composable fun TodayScreen(state: TodayUiState, onSelect: (String?) -> Unit, onAddPet: () -> Unit, onAddEvent: (String?) -> Unit, onEvent: (String) -> Unit, onQuickAction: (String, String) -> Unit, onArticle: (String) -> Unit, onAllArticles: () -> Unit) {
    LazyPage(stringResource(R.string.today), floatingActionButton = {
        if (state.pets.isNotEmpty()) ExtendedFloatingActionButton(onClick = { onAddEvent(state.selected) }, icon = { AppIcon(R.drawable.ic_add) }, text = { Text(stringResource(R.string.add_event)) })
    }) {
        item("header") {
            Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(12f))) {
                if (state.loading) CircularProgressIndicator()
                if (state.error != null) Text(stringResource(state.error), color = MaterialTheme.colorScheme.error)
                if (!state.loading && state.error == null && state.pets.isEmpty()) {
                    EmptyState(stringResource(R.string.welcome), stringResource(R.string.welcome_body))
                    PrimaryButton(stringResource(R.string.add_dog), onClick = onAddPet)
                } else if (state.pets.isNotEmpty()) {
                    PetSwitcher(state.pets, state.selected, true, onSelect)
                    state.pets.find { it.id == state.selected }?.let { pet -> PetAvatar(pet); Text(pet.name, style = MaterialTheme.typography.headlineLarge) }
                    if (state.overdue.isEmpty() && state.today.isEmpty()) EmptyState(stringResource(R.string.all_done), stringResource(R.string.empty_care))
                }
            }
        }
        listOf(R.string.attention to state.overdue, R.string.today to state.today, R.string.next_week to state.upcoming).forEach { (title, events) ->
            if (events.isNotEmpty()) {
                item("section:$title") { SectionHeader(stringResource(title)) }
                items(events, key = { it.occurrence.id }, contentType = { "care" }) { event ->
                    CareEventCard(event, actions = {
                        TextButton({ onQuickAction(event.occurrence.id, "complete") }) { AppIcon(R.drawable.ic_done); Text(stringResource(R.string.complete)) }
                        TextButton({ onQuickAction(event.occurrence.id, "reschedule") }) { Text(stringResource(R.string.reschedule)) }
                    }) { onEvent(event.occurrence.id) }
                }
            }
        }
        item("recommended-article") {
            Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(12f))) {
                state.recommended?.let { article -> ArticleCard(article, recommended = true) { onArticle(article.id) } }
                if (state.articleFailed) Text(stringResource(R.string.articles_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onAllArticles) { Text(stringResource(R.string.all_articles)) }
            }
        }

    }
}
@Preview(showBackground = true) @Composable private fun TodayEmptyPreview() {
    PawCareTheme { TodayScreen(TodayUiState(loading = false), {}, {}, {}, {}, { _, _ -> }, {}, {}) }
}
