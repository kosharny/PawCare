package com.example.pawcare.feature.pet

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import com.example.pawcare.ui.theme.PawCareTheme
import java.time.LocalDate
import java.time.Period

@Composable fun PetRoute(onEdit: (String?) -> Unit, onSection: (String, String) -> Unit, onSettings: () -> Unit, viewModel: PetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    PetScreen(state, operation, viewModel::select, onEdit, onSection, onSettings, viewModel::delete, viewModel::dismissError)
}
@Composable fun PetScreen(state: PetUiState, operation: OperationState, onSelect: (String?) -> Unit, onEdit: (String?) -> Unit,
    onSection: (String, String) -> Unit, onSettings: () -> Unit, onDelete: (String) -> Unit, onDismissError: () -> Unit) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    Page(stringResource(R.string.pet), actions = { IconButton(onSettings) { AppIcon(R.drawable.ic_settings, stringResource(R.string.settings)) } }) {
        ErrorState(operation.error ?: state.error, onDismissError)
        if (state.loading) CircularProgressIndicator()
        val pet = state.selected
        if (pet == null && !state.loading) {
            EmptyState(stringResource(R.string.welcome), stringResource(R.string.welcome_body))
            PrimaryButton(stringResource(R.string.add_dog), onClick = { onEdit(null) })
        } else if (pet != null) {
            PetSwitcher(state.pets, pet.id, false, onSelect)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PetAvatar(pet)
                Column { Text(pet.name, style = MaterialTheme.typography.headlineLarge); if (pet.breed.isNotBlank()) Text(pet.breed)
                    pet.birthDate?.let {
                        val age = Period.between(it, LocalDate.now())
                        val text = pluralStringResource(R.plurals.age_years, age.years, age.years) + ", " + pluralStringResource(R.plurals.age_months, age.months, age.months)
                        Text(if (pet.approximateBirth) stringResource(R.string.approximate_age, text) else text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.latestWeight?.let { Text(stringResource(R.string.weight_value, java.text.NumberFormat.getNumberInstance().format(it.kilograms)) + " · " + displayDate(it.date)) }
            if (pet.notes.isNotBlank()) Text(pet.notes)
            OutlinedButton({ onEdit(pet.id) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit)) }
            listOf(Triple("history", R.string.history, R.drawable.ic_history), Triple("weight", R.string.weight, R.drawable.ic_weight),
                Triple("documents", R.string.documents, R.drawable.ic_document), Triple("contacts", R.string.contacts, R.drawable.ic_contacts)).forEach { (key, label, icon) ->
                OutlinedCard(onClick = { onSection(key, pet.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { AppIcon(icon); Text(stringResource(label), style = MaterialTheme.typography.titleMedium) }
                }
            }
            TextButton({ onEdit(null) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_dog)) }
            TextButton({ deleting = true }, enabled = !operation.busy) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            if (deleting) ConfirmDialog(stringResource(R.string.delete_pet_body), { deleting = false; onDelete(pet.id) }, { deleting = false })
        }
    }
}
@Composable fun PetEditorRoute(id: String?, onBack: () -> Unit, viewModel: PetEditorViewModel = hiltViewModel()) {
    LaunchedEffect(id) { viewModel.load(id) }
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::importPhoto) }
    BackHandler { if (!operation.busy) viewModel.discard() }
    SaveResult(operation, viewModel::acknowledgeSave, onBack)
    PetEditorScreen(fields, operation, viewModel::change, viewModel::save, viewModel::discard,
        { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, viewModel::dismissError)
}
@Composable fun PetEditorScreen(fields: Map<String, String>, operation: OperationState, onChange: (String, String) -> Unit, onSave: () -> Unit, onBack: () -> Unit, onPhoto: () -> Unit, onDismissError: () -> Unit) {
    Page(stringResource(R.string.pet_profile), onBack) {
        ErrorState(operation.error, onDismissError)
        PetAvatar(Pet(photoPath = fields["photo"]?.takeIf { it.isNotBlank() }))
        OutlinedButton(onPhoto, enabled = !operation.busy) { Text(stringResource(R.string.photo)) }
        if (!fields["photo"].isNullOrBlank()) TextButton({ onChange("photo", "") }) { Text(stringResource(R.string.remove_photo)) }
        Field(stringResource(R.string.name), fields["name"].orEmpty(), { onChange("name", it) }, error = operation.error == R.string.error_validation && fields["name"].isNullOrBlank())
        Field(stringResource(R.string.breed), fields["breed"].orEmpty(), { onChange("breed", it) })
        Choice(stringResource(R.string.sex), fields["sex"].orEmpty(), listOf(Sex.UNSPECIFIED.name to stringResource(R.string.sex_unspecified), Sex.MALE.name to stringResource(R.string.sex_male), Sex.FEMALE.name to stringResource(R.string.sex_female))) { onChange("sex", it) }
        DateField(stringResource(R.string.birth), fields["birth"].orEmpty(), { onChange("birth", it) }, optional = true)
        Toggle(stringResource(R.string.approximate), fields["approximate"].toBoolean()) { onChange("approximate", it.toString()) }
        Field(stringResource(R.string.chip), fields["chip"].orEmpty(), { onChange("chip", it) })
        Field(stringResource(R.string.notes), fields["notes"].orEmpty(), { onChange("notes", it) }, multiline = true)
        PrimaryButton(stringResource(R.string.save), enabled = !operation.busy && fields["id"] != null, onClick = onSave)
    }
}
@Preview(showBackground = true) @Composable private fun WelcomePreview() {
    PawCareTheme { PetScreen(PetUiState(loading = false), OperationState(), {}, {}, { _, _ -> }, {}, {}, {}) }
}
