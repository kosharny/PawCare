package com.example.pawcare.feature.today

import androidx.lifecycle.viewModelScope
import com.example.pawcare.R
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.core.ui.OperationViewModel
import com.example.pawcare.data.preferences.PreferencesRepository
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.recurrence.RecurrenceCalculator
import com.example.pawcare.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.example.pawcare.domain.repository.ArticlesRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TodayUiState(val loading: Boolean = true, val pets: List<Pet> = emptyList(), val selected: String? = null,
    val overdue: List<CareEvent> = emptyList(), val today: List<CareEvent> = emptyList(), val upcoming: List<CareEvent> = emptyList(), val error: Int? = null, val recommended: Article? = null, val articleFailed: Boolean = false)
@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: CareRepository, private val preferences: PreferencesRepository, private val time: TimeSource, private val articles: ArticlesRepository) : OperationViewModel() {
    private val tick = flow { while (true) { emit(time.clock.instant()); delay(30_000) } }
    private val careState = combine(repository.snapshot, preferences.preferences, tick) { snapshot, settings, now ->
        val selected = settings.activePetId?.takeIf { id -> snapshot.pets.any { it.id == id } }
        val events = snapshot.events.filter { it.occurrence.status == OccurrenceStatus.PENDING && (selected == null || it.pet.id == selected) }
        val today = now.atZone(time.zone()).toLocalDate()
        TodayUiState(false, snapshot.pets, selected,
            events.filter { RecurrenceCalculator.overdue(it.occurrence, now, time.zone()) },
            events.filter { it.occurrence.date == today && !RecurrenceCalculator.overdue(it.occurrence, now, time.zone()) },
            events.filter { it.occurrence.date > today && it.occurrence.date <= today.plusDays(7) })
    }.catch { emit(TodayUiState(loading = false, error = R.string.error_generic)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())
    private val recommended = MutableStateFlow<Article?>(null)
    private val articleFailed = MutableStateFlow(false)
    private var articleLanguage: String? = null
    private var articleJob: Job? = null
    val uiState = combine(careState, recommended, articleFailed) { care, article, failed ->
        care.copy(recommended = article, articleFailed = failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun loadRecommended(language: String) {
        if (articleLanguage == language) return
        articleLanguage = language
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            recommended.value = null
            articleFailed.value = false
            try {
                val entries = articles.articles(language)
                recommended.value = entries.firstOrNull { it.featured } ?: entries.firstOrNull()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { articleFailed.value = true }
        }
    }
    fun select(id: String?) = perform { preferences.selectPet(id) }
    fun refresh() = perform { repository.materialize(time.today(), time.today().plusDays(90)) }
}
