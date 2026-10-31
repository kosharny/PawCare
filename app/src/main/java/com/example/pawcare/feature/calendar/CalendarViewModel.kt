package com.example.pawcare.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.pawcare.R
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.core.ui.SavedFormViewModel
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(val loading: Boolean = true, val month: YearMonth = YearMonth.now(), val selected: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(), val pets: List<Pet> = emptyList(), val petId: String? = null, val category: Category? = null,
    val events: List<CareEvent> = emptyList(), val error: Int? = null)
@HiltViewModel
class CalendarViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository, private val time: TimeSource) : SavedFormViewModel(savedState) {
    init { initialize(mapOf("month" to YearMonth.from(time.today()).toString(), "day" to time.today().toString(), "pet" to "", "category" to "")) }
    val uiState = combine(repository.snapshot, fields) { snapshot, fields ->
        CalendarUiState(false, YearMonth.parse(fields.getValue("month")), LocalDate.parse(fields.getValue("day")), time.today(), snapshot.pets,
            fields["pet"]?.takeIf(String::isNotBlank), fields["category"]?.takeIf(String::isNotBlank)?.let(Category::valueOf),
            snapshot.events.filter { it.occurrence.status != OccurrenceStatus.CANCELLED && (fields["pet"].isNullOrEmpty() || it.pet.id == fields["pet"]) && (fields["category"].isNullOrEmpty() || it.category.name == fields["category"]) })
    }.catch { emit(CalendarUiState(loading = false, error = R.string.error_generic)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())
    fun moveMonth(delta: Long) {
        val month = YearMonth.parse(value("month")).plusMonths(delta)
        change("month", month.toString()); change("day", month.atDay(1).toString()); materialize(month)
    }
    fun goToday() { change("day", time.today().toString()); change("month", YearMonth.from(time.today()).toString()); materialize(YearMonth.from(time.today())) }
    private fun materialize(month: YearMonth) = perform { repository.materialize(month.atDay(1), month.atEndOfMonth()) }
}
