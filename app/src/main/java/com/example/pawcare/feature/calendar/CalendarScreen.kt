package com.example.pawcare.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat

@Composable fun CalendarRoute(onEvent: (String) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    CalendarScreen(state, operation, viewModel::change, viewModel::moveMonth, viewModel::goToday, onEvent, viewModel::dismissError)
}
@Composable fun CalendarScreen(state: CalendarUiState, operation: OperationState, onChange: (String, String) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit, onEvent: (String) -> Unit, onDismiss: () -> Unit) {
    Page(stringResource(R.string.calendar)) {
        ErrorState(operation.error ?: state.error, onDismiss)
        if (state.loading) CircularProgressIndicator()
        PetSwitcher(state.pets, state.petId, true) { onChange("pet", it ?: "") }
        Choice(stringResource(R.string.category), state.category?.name ?: "", listOf("" to stringResource(R.string.all_categories)) + Category.entries.map { it.name to stringResource(it.label()) }) { onChange("category", it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ onMonth(-1) }) { AppIcon(R.drawable.ic_back, stringResource(R.string.previous_month)) }
            Text(state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy")), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onToday) { Text(stringResource(R.string.today)) }
            IconButton({ onMonth(1) }) { AppIcon(R.drawable.ic_back, stringResource(R.string.next_month), Modifier.then(Modifier.rotate(180f))) }
        }
        val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ENGLISH
        val firstDay = WeekFields.of(locale).firstDayOfWeek
        val days = (0..6).map { DayOfWeek.of((firstDay.value - 1 + it) % 7 + 1) }
        Row(Modifier.fillMaxWidth()) { days.forEach { Text(it.getDisplayName(TextStyle.SHORT, locale), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium) } }
        val offset = (state.month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val marked = state.events.map { it.occurrence.date }.toSet()
        val cells = ((offset + state.month.lengthOfMonth() + 6) / 7) * 7
        (0 until cells step 7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { column ->
                    val day = row + column - offset + 1
                    if (day !in 1..state.month.lengthOfMonth()) Spacer(Modifier.weight(1f)) else {
                        val date = state.month.atDay(day)
                        val label = day.toString() + if (date in marked) " •" else ""
                        val colors = if (date == state.selected) ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                            else if (date == state.today) ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.textButtonColors()
                        TextButton({ onChange("day", date.toString()) }, Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp), colors = colors) { Text(label) }
                    }
                }
            }
        }
        SectionHeader(displayDate(state.selected))
        val events = state.events.filter { it.occurrence.date == state.selected }
        if (events.isEmpty()) Text(stringResource(R.string.selected_day_empty))
        events.forEach { CareEventCard(it) { onEvent(it.occurrence.id) } }
    }
}
