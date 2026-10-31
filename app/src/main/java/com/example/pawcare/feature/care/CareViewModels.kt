@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.pawcare.feature.care

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.pawcare.R
import com.example.pawcare.core.files.FileStore
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class CareEditorViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository, private val time: TimeSource) : SavedFormViewModel(savedState) {
    private var loaded = false
    private var originalEvent: CareEvent? = null
    private var eventId: String? = null
    private var series = false
    private var past = false
    val snapshot = repository.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CareSnapshot())
    fun load(petId: String?, id: String?, editSeries: Boolean, isPast: Boolean) = perform {
        if (loaded) return@perform
        val data = repository.snapshot.first()
        val event = id?.let { key -> data.events.find { it.occurrence.id == key } ?: error("Missing event") }
        originalEvent = event
        eventId = id; series = editSeries; past = isPast
        initialize(mapOf("id" to (event?.plan?.id ?: newId()), "pet" to (event?.pet?.id ?: petId ?: data.pets.firstOrNull()?.id.orEmpty()),
            "lockedPet" to (event != null).toString(), "title" to (event?.title ?: ""), "category" to (event?.category ?: Category.TREATMENT).name,
            "date" to (event?.occurrence?.date ?: time.today()).toString(), "time" to (event?.occurrence?.time?.toString() ?: ""),
            "medication" to (event?.medication ?: ""), "notes" to (event?.notes ?: ""), "mode" to (event?.plan?.mode ?: RepeatMode.ONCE).name,
            "interval" to (event?.plan?.interval ?: 1).toString(), "unit" to (event?.plan?.unit ?: IntervalUnit.DAYS).name,
            "remind" to (event?.plan?.remindOnDay ?: false).toString(), "advance" to (event?.plan?.advanceDays?.toString() ?: "")))
        loaded = true
    }
    fun save() = perform(exit = true) {
        require(value("title").isNotBlank() && value("pet").isNotBlank())
        val date = LocalDate.parse(value("date"))
        val eventTime = value("time").takeIf(String::isNotBlank)?.let(LocalTime::parse)
        val category = Category.valueOf(value("category"))
        if (eventId != null && !series) repository.editOccurrence(eventId!!, value("title"), category, value("notes"), value("medication"), date, eventTime)
        else {
            val plan = CarePlan(id = value("id"), petId = value("pet"), title = value("title").trim(), category = category,
                anchorDate = if (series && date == originalEvent?.occurrence?.date) originalEvent!!.plan.anchorDate else date, time = eventTime, notes = value("notes"), medication = value("medication"),
                mode = if (past) RepeatMode.ONCE else RepeatMode.valueOf(value("mode")), interval = value("interval").toInt(), unit = IntervalUnit.valueOf(value("unit")),
                remindOnDay = !past && value("remind").toBoolean(), advanceDays = if (past) null else value("advance").takeIf(String::isNotBlank)?.toInt())
            val at = date.atTime(eventTime ?: LocalTime.NOON).atZone(time.zone()).toInstant()
            if (past && at > time.clock.instant()) { reportError(R.string.error_future); throw com.example.pawcare.data.repository.CareOperationException(com.example.pawcare.data.repository.CareOperationException.Reason.FUTURE) }
            if (series) repository.editSeries(plan, originalEvent?.occurrence?.originalDate ?: date) else repository.savePlan(plan)
            if (past) {
                val occurrence = repository.snapshot.first().occurrences.first { it.planId == plan.id && it.originalDate == plan.anchorDate }
                repository.complete(occurrence.id, at, "")
            }
        }
    }
}
data class CareDetailUiState(val loading: Boolean = true, val event: CareEvent? = null, val completion: CompletionRecord? = null, val previous: CompletionRecord? = null, val files: List<Attachment> = emptyList(), val error: Int? = null)
@HiltViewModel
class CareDetailViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository, private val time: TimeSource, private val files: FileStore) : SavedFormViewModel(savedState) {
    private val id = MutableStateFlow("")
    val uiState = id.filter(String::isNotBlank).flatMapLatest { key -> repository.snapshot.flatMapLatest { snapshot ->
        val event = snapshot.events.find { it.occurrence.id == key }
        if (event == null) flowOf(CareDetailUiState(loading = false)) else repository.attachments(event.pet.id).map { attachments ->
            CareDetailUiState(false, event, snapshot.completions.find { it.occurrenceId == key }, snapshot.completions.filter { record -> record.occurrenceId != key && snapshot.occurrences.any { it.id == record.occurrenceId && it.planId == event.plan.id } }.maxByOrNull { it.completedAt }, attachments.filter { it.occurrenceId == key })
        }
    } }.catch { emit(CareDetailUiState(loading = false, error = R.string.error_generic)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CareDetailUiState())
    fun load(key: String) {
        id.value = key
        initialize(mapOf("date" to time.today().toString(), "time" to time.clock.instant().atZone(time.zone()).toLocalTime().withSecond(0).withNano(0).toString(), "comment" to ""))
    }
    fun complete() = perform {
        val at = LocalDate.parse(value("date")).atTime(LocalTime.parse(value("time"))).atZone(time.zone()).toInstant()
        repository.complete(id.value, at, value("comment"))
    }
    fun reschedule() = perform { repository.reschedule(id.value, LocalDate.parse(value("date")), value("time").takeIf(String::isNotBlank)?.let(LocalTime::parse)) }
    fun skip() = perform { repository.skip(id.value, value("date").takeIf(String::isNotBlank)?.let(LocalDate::parse)) }
    fun editCompletion() = perform {
        val record = uiState.value.completion ?: error("Missing record")
        val local = record.completedAt.atZone(time.zone())
        val edited = LocalDate.parse(value("date")).atTime(LocalTime.parse(value("time"))).atZone(time.zone()).toInstant()
        val sameMinute = local.toLocalDate().toString() == value("date") && local.toLocalTime().withSecond(0).withNano(0).toString() == value("time")
        repository.editCompletion(id.value, if (sameMinute) record.completedAt else edited, value("comment"))
    }
    fun undo() = perform { repository.undoCompletion(id.value) }
    fun delete() = perform(exit = true) { repository.deleteOccurrence(id.value) }
    fun archive() = perform(exit = true) { uiState.value.event?.let { repository.archivePlan(it.plan.id) } }
    fun importPhoto(uri: Uri) = perform {
        val event = uiState.value.event ?: error("Missing event")
        val imported = files.import(uri, photoOnly = true)
        try {
            repository.saveAttachment(Attachment(petId = event.pet.id, occurrenceId = event.occurrence.id, completionId = uiState.value.completion?.id,
                path = imported.path, name = imported.name, mime = imported.mime, size = imported.size, importedAt = time.clock.instant()))
        } catch (error: Exception) { kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { files.delete(imported.path) }; throw error }
    }
}
