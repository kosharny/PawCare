@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.pawcare.feature.pet

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.pawcare.core.files.FileStore
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.core.ui.*
import com.example.pawcare.data.preferences.PreferencesRepository
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class PetUiState(val loading: Boolean = true, val pets: List<Pet> = emptyList(), val selected: Pet? = null, val latestWeight: WeightEntry? = null, val error: Int? = null)
@HiltViewModel
class PetViewModel @Inject constructor(private val repository: CareRepository, private val preferences: PreferencesRepository) : OperationViewModel() {
    private val selection = combine(repository.snapshot, preferences.preferences) { snapshot, settings ->
        snapshot.pets to (snapshot.pets.find { it.id == settings.activePetId } ?: snapshot.pets.firstOrNull())
    }
    val uiState = selection.flatMapLatest { (pets, selected) ->
        if (selected == null) flowOf(PetUiState(loading = false, pets = pets))
        else repository.weights(selected.id).map { PetUiState(false, pets, selected, it.firstOrNull()) }
    }.catch { emit(PetUiState(loading = false, error = com.example.pawcare.R.string.error_generic)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetUiState())
    fun select(id: String?) = perform { preferences.selectPet(id) }
    fun delete(id: String) = perform { repository.deletePet(id) }
}
@HiltViewModel
class PetEditorViewModel @Inject constructor(
    savedState: SavedStateHandle, private val repository: CareRepository, private val files: FileStore, private val time: TimeSource,
) : SavedFormViewModel(savedState) {
    private var original: Pet? = null
    private var loaded = false
    fun load(id: String?) = perform {
        if (loaded) return@perform
        val pet = id?.let { key -> repository.snapshot.first().pets.find { it.id == key } ?: error("Missing pet") } ?: Pet(createdAt = time.clock.instant())
        original = pet
        initialize(mapOf("id" to pet.id, "name" to pet.name, "breed" to pet.breed, "sex" to pet.sex.name,
            "birth" to (pet.birthDate?.toString() ?: ""), "approximate" to pet.approximateBirth.toString(), "chip" to pet.chip, "notes" to pet.notes, "photo" to (pet.photoPath ?: "")))
        loaded = true
    }
    fun importPhoto(uri: Uri) = perform {
        val imported = files.import(uri, photoOnly = true)
        val previous = value("photo")
        change("photo", imported.path)
        if (previous.isNotBlank() && previous != original?.photoPath) files.delete(previous)
    }
    fun save() = perform(exit = true) {
        val pet = original ?: error("Not loaded")
        repository.savePet(pet.copy(id = value("id"), name = value("name"), breed = value("breed"), sex = Sex.valueOf(value("sex")),
            birthDate = value("birth").takeIf(String::isNotBlank)?.let(LocalDate::parse), approximateBirth = value("approximate").toBoolean(),
            chip = value("chip"), notes = value("notes"), photoPath = value("photo").takeIf(String::isNotBlank)))
    }
    fun discard() = perform(exit = true) {
        value("photo").takeIf { it.isNotBlank() && it != original?.photoPath }?.let { files.delete(it) }
    }
}
