@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.pawcare.feature.records

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
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(savedState: SavedStateHandle, repository: CareRepository) : SavedFormViewModel(savedState) {
    private val petId = MutableStateFlow("")
    val uiState = combine(repository.snapshot, petId, fields) { snapshot, pet, fields ->
        val category = fields["category"].orEmpty()
        val events = snapshot.events.filter { it.pet.id == pet && (it.occurrence.status == OccurrenceStatus.COMPLETED || it.occurrence.status == OccurrenceStatus.SKIPPED) }
            .filter { event -> category.isBlank() || (snapshot.completions.find { it.occurrenceId == event.occurrence.id }?.category ?: event.category).name == category }
        HistoryUiState(false, events.sortedByDescending { event -> snapshot.completions.find { it.occurrenceId == event.occurrence.id }?.completedAt?.toEpochMilli() ?: event.occurrence.date.toEpochDay() * 86400000 }, snapshot.completions, category)
    }.catch { emit(HistoryUiState(loading = false, error = R.string.error_generic)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
    fun load(id: String) { petId.value = id }
}
data class HistoryUiState(val loading: Boolean = true, val events: List<CareEvent> = emptyList(), val completions: List<CompletionRecord> = emptyList(), val category: String = "", val error: Int? = null)

data class WeightUiState(val loading: Boolean = true, val entries: List<WeightEntry> = emptyList(), val error: Int? = null)
@HiltViewModel
class WeightViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository, private val time: TimeSource) : SavedFormViewModel(savedState) {
    private val petId = MutableStateFlow("")
    val uiState = petId.filter(String::isNotBlank).flatMapLatest { id -> repository.weights(id).map { WeightUiState(false, it) } }
        .catch { emit(WeightUiState(loading = false, error = R.string.error_generic)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUiState())
    fun load(id: String) { petId.value = id; initialize(mapOf("id" to "", "date" to time.today().toString(), "weight" to "", "note" to "")) }
    fun edit(entry: WeightEntry?) {
        change("id", entry?.id ?: ""); change("date", (entry?.date ?: time.today()).toString()); change("weight", entry?.kilograms?.toString() ?: ""); change("note", entry?.note ?: "")
    }
    fun save() = perform(exit = true) {
        val weight = value("weight").trim().replace(',', '.').toDouble()
        repository.saveWeight(WeightEntry(id = value("id").ifBlank(::newId), petId = petId.value, date = LocalDate.parse(value("date")), kilograms = weight, note = value("note")))
    }
    fun delete(id: String) = perform { repository.deleteWeight(id) }
}

data class ContactsUiState(val loading: Boolean = true, val entries: List<Contact> = emptyList(), val error: Int? = null)
@HiltViewModel
class ContactsViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository) : SavedFormViewModel(savedState) {
    private val petId = MutableStateFlow("")
    val uiState = petId.filter(String::isNotBlank).flatMapLatest { id -> repository.contacts(id).map { ContactsUiState(false, it) } }
        .catch { emit(ContactsUiState(loading = false, error = R.string.error_generic)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContactsUiState())
    fun load(id: String) { petId.value = id }
    fun edit(contact: Contact?) {
        change("id", contact?.id ?: ""); change("name", contact?.name ?: ""); change("type", (contact?.type ?: ContactType.VET).name)
        change("phone", contact?.phone ?: ""); change("address", contact?.address ?: ""); change("note", contact?.note ?: "")
    }
    fun save() = perform(exit = true) {
        repository.saveContact(Contact(value("id").ifBlank(::newId), petId.value, ContactType.valueOf(value("type")), value("name"), value("phone"), value("address"), value("note")))
    }
    fun delete(id: String) = perform { repository.deleteContact(id) }
}

data class DocumentsUiState(val loading: Boolean = true, val entries: List<Attachment> = emptyList(), val error: Int? = null)
@HiltViewModel
class DocumentsViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: CareRepository, private val files: FileStore, private val time: TimeSource) : SavedFormViewModel(savedState) {
    private val petId = MutableStateFlow("")
    val uiState = petId.filter(String::isNotBlank).flatMapLatest { id -> repository.attachments(id).map { DocumentsUiState(false, it) } }
        .catch { emit(DocumentsUiState(loading = false, error = R.string.error_generic)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocumentsUiState())
    fun load(id: String) { petId.value = id }
    fun import(uri: Uri) = perform {
        val imported = files.import(uri)
        try { repository.saveAttachment(Attachment(petId = petId.value, path = imported.path, name = imported.name, mime = imported.mime, size = imported.size, importedAt = time.clock.instant())) }
        catch (error: Exception) { kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { files.delete(imported.path) }; throw error }
    }
    fun rename(id: String, name: String) = perform(exit = true) { repository.renameAttachment(id, name) }
    fun delete(id: String) = perform { repository.deleteAttachment(id) }
    fun uri(path: String): Uri = files.uri(path)
}
