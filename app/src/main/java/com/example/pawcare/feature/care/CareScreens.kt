package com.example.pawcare.feature.care

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable fun CareEditorRoute(petId: String?, id: String?, series: Boolean, past: Boolean, onBack: () -> Unit, viewModel: CareEditorViewModel = hiltViewModel()) {
    LaunchedEffect(petId, id) { viewModel.load(petId, id, series, past) }
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var permissionExplanation by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val change: (String, String) -> Unit = { key, value ->
        viewModel.change(key, value)
        if (((key == "remind" && value == "true") || (key == "advance" && value.isNotBlank())) && Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) permissionExplanation = true
    }
    SaveResult(operation, viewModel::acknowledgeSave, onBack)
    CareEditorScreen(fields, operation, snapshot, id != null && !series, past, change, viewModel::save, onBack, viewModel::dismissError)
    if (permissionExplanation) AlertDialog(onDismissRequest = { permissionExplanation = false }, text = { Text(stringResource(R.string.reminder_explanation)) }, confirmButton = {
        TextButton({ permissionExplanation = false; if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.allow)) }
    }, dismissButton = { TextButton({ permissionExplanation = false }) { Text(stringResource(R.string.cancel)) } })
}
@Composable fun CareEditorScreen(fields: Map<String, String>, operation: OperationState, snapshot: CareSnapshot, onlyEvent: Boolean, past: Boolean,
    onChange: (String, String) -> Unit, onSave: () -> Unit, onBack: () -> Unit, onDismissError: () -> Unit) {
    Page(stringResource(if (past) R.string.add_past else R.string.add_event), onBack) {
        ErrorState(operation.error, onDismissError)
        if (!onlyEvent && fields["lockedPet"] != "true") Choice(stringResource(R.string.pet), fields["pet"].orEmpty(), snapshot.pets.map { it.id to it.name }) { onChange("pet", it) }
        Field(stringResource(R.string.title), fields["title"].orEmpty(), { onChange("title", it) }, error = operation.error == R.string.error_validation && fields["title"].isNullOrBlank())
        Choice(stringResource(R.string.category), fields["category"].orEmpty(), Category.entries.map { it.name to stringResource(it.label()) }) { onChange("category", it) }
        snapshot.completions.filter { it.petId == fields["pet"] && it.category.name == fields["category"] }.maxByOrNull { it.completedAt }?.let {
            Text(stringResource(R.string.last_care, displayDate(it.completedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())), style = MaterialTheme.typography.bodyMedium)
        }
        DateField(stringResource(R.string.date), fields["date"].orEmpty(), { onChange("date", it) })
        TimeField(stringResource(R.string.time), fields["time"].orEmpty(), { onChange("time", it) }, optional = true)
        Field(stringResource(R.string.medication), fields["medication"].orEmpty(), { onChange("medication", it) })
        Field(stringResource(R.string.event_notes), fields["notes"].orEmpty(), { onChange("notes", it) }, multiline = true)
        if (!onlyEvent && !past) {
            Choice(stringResource(R.string.repeat), fields["mode"].orEmpty(), listOf(RepeatMode.ONCE.name to stringResource(R.string.once), RepeatMode.FIXED.name to stringResource(R.string.fixed), RepeatMode.AFTER_COMPLETION.name to stringResource(R.string.after_completion))) { onChange("mode", it) }
            if (fields["mode"] != RepeatMode.ONCE.name) {
                Field(stringResource(R.string.interval), fields["interval"].orEmpty(), { onChange("interval", it) }, numeric = true)
                Choice(stringResource(R.string.unit), fields["unit"].orEmpty(), listOf(IntervalUnit.DAYS.name to stringResource(R.string.days), IntervalUnit.WEEKS.name to stringResource(R.string.weeks), IntervalUnit.MONTHS.name to stringResource(R.string.months))) { onChange("unit", it) }
            }
            Toggle(stringResource(R.string.remind_on_day), fields["remind"].toBoolean()) { onChange("remind", it.toString()) }
            Field(stringResource(R.string.advance_days), fields["advance"].orEmpty(), { onChange("advance", it) }, numeric = true)
        }
        PrimaryButton(stringResource(R.string.save), enabled = !operation.busy && fields["id"] != null, onClick = onSave)
    }
}
@Composable fun CareDetailRoute(id: String, onBack: () -> Unit, onEdit: (String, Boolean) -> Unit, initialAction: String = "", viewModel: CareDetailViewModel = hiltViewModel()) {
    LaunchedEffect(id) { viewModel.load(id) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    CareDetailScreen(id, state, fields, operation, onBack, onEdit, initialAction, viewModel::change, viewModel::complete, viewModel::reschedule, viewModel::skip, viewModel::undo, viewModel::delete, viewModel::archive, viewModel::importPhoto, viewModel::dismissError, viewModel::acknowledgeSave, viewModel::editCompletion)
}
@Composable fun CareDetailScreen(id: String, state: CareDetailUiState, fields: Map<String, String>, operation: OperationState, onBack: () -> Unit, onEdit: (String, Boolean) -> Unit, initialAction: String, onChange: (String, String) -> Unit, onComplete: () -> Unit, onReschedule: () -> Unit, onSkip: () -> Unit, onUndo: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit, onPhoto: (android.net.Uri) -> Unit, onDismissError: () -> Unit, onAcknowledgeSave: () -> Unit, onEditCompletion: () -> Unit) {
    var action by rememberSaveable { mutableStateOf(initialAction) }
    LaunchedEffect(state.event?.occurrence?.id) {
        if (initialAction == "reschedule" && state.event != null) {
            onChange("date", state.event!!.occurrence.date.toString())
            onChange("time", state.event!!.occurrence.time?.toString() ?: "")
        }
    }
    var previewPath by rememberSaveable { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var editScope by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(onPhoto) }
    SaveResult(operation, onAcknowledgeSave, onBack)
    Page(stringResource(R.string.event_detail), onBack) {
        ErrorState(operation.error ?: state.error, onDismissError)
        if (state.loading) CircularProgressIndicator()
        val event = state.event
        if (!state.loading && event == null) Text(stringResource(R.string.not_found))
        if (event != null) {
            CategoryChip(event.category)
            Text(event.title, style = MaterialTheme.typography.headlineLarge)
            Text(event.pet.name + " · " + displayDate(event.occurrence.date) + (event.occurrence.time?.let { " · $it" } ?: ""))
            Text(stringResource(when (event.occurrence.status) { OccurrenceStatus.COMPLETED -> R.string.complete; OccurrenceStatus.SKIPPED -> R.string.skipped; OccurrenceStatus.CANCELLED -> R.string.cancel; else -> R.string.pending }))
            if (event.medication.isNotBlank()) Text(event.medication)
            if (event.notes.isNotBlank()) Text(event.notes)
            state.previous?.let { Text(stringResource(R.string.last_care, displayDate(it.completedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))) }
            state.completion?.let { completion ->
                Text(completion.completedAt.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)))
                Text(completion.title)
                if (completion.notes.isNotBlank()) Text(completion.notes)
                if (completion.medication.isNotBlank()) Text(completion.medication)
                if (completion.comment.isNotBlank()) Text(completion.comment)
                OutlinedButton({
                    val local = completion.completedAt.atZone(java.time.ZoneId.systemDefault())
                    onChange("date", local.toLocalDate().toString())
                    onChange("time", local.toLocalTime().withSecond(0).withNano(0).toString())
                    onChange("comment", completion.comment)
                    action = "editCompletion"
                }, enabled = !operation.busy) { Text(stringResource(R.string.edit)) }
                OutlinedButton(onUndo, enabled = !operation.busy) { Text(stringResource(R.string.undo_completion)) }
            }
            if (event.occurrence.status == OccurrenceStatus.PENDING) {
                PrimaryButton(stringResource(R.string.complete_event), !operation.busy) { action = "complete" }
                OutlinedButton({ onChange("date", event.occurrence.date.toString()); onChange("time", event.occurrence.time?.toString() ?: ""); action = "reschedule" }, enabled = !operation.busy) { Text(stringResource(R.string.reschedule)) }
                OutlinedButton({ onChange("date", event.occurrence.date.plusDays(1).toString()); action = "skip" }, enabled = !operation.busy) { Text(stringResource(R.string.skip)) }
                OutlinedButton({ if (event.plan.mode == RepeatMode.ONCE) onEdit(id, false) else editScope = true }) { Text(stringResource(R.string.edit)) }
                TextButton({ action = "delete" }, enabled = !operation.busy) { Text(stringResource(R.string.delete)) }
                if (event.plan.mode != RepeatMode.ONCE) TextButton({ action = "archive" }, enabled = !operation.busy) { Text(stringResource(R.string.archive)) }
            }
            SectionHeader(stringResource(R.string.linked_files))
            state.files.forEach { file -> TextButton({ previewPath = file.path }) { Text(file.name) } }
            OutlinedButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !operation.busy) { Text(stringResource(R.string.import_photo)) }
        }
    }
    if (editScope) AlertDialog(onDismissRequest = { editScope = false }, title = { Text(stringResource(R.string.edit_scope)) }, confirmButton = { TextButton({ editScope = false; onEdit(id, false) }) { Text(stringResource(R.string.only_event)) } }, dismissButton = { TextButton({ editScope = false; onEdit(id, true) }) { Text(stringResource(R.string.future_series)) } })
    if (action == "delete" || action == "archive") ConfirmDialog(if (action == "archive") stringResource(R.string.archive_body) else stringResource(R.string.confirm_delete), {
        if (action == "delete") onDelete() else onArchive(); action = ""
    }, { action = "" })
    if (action in listOf("complete", "editCompletion", "reschedule", "skip")) AlertDialog(onDismissRequest = { action = "" }, title = { Text(stringResource(when (action) { "complete", "editCompletion" -> R.string.complete_event; "skip" -> R.string.skip; else -> R.string.reschedule })) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (action == "skip") Text(stringResource(R.string.skip_explanation))
            DateField(stringResource(if (action in listOf("complete", "editCompletion")) R.string.actual_date else R.string.next_date), fields["date"].orEmpty(), { onChange("date", it) })
            if (action != "skip") TimeField(stringResource(if (action in listOf("complete", "editCompletion")) R.string.actual_time else R.string.time), fields["time"].orEmpty(), { onChange("time", it) }, optional = action == "reschedule")
            if (action in listOf("complete", "editCompletion")) Field(stringResource(R.string.comment), fields["comment"].orEmpty(), { onChange("comment", it) })
        }
    }, confirmButton = { TextButton({ when (action) { "complete" -> onComplete(); "editCompletion" -> onEditCompletion(); "skip" -> onSkip(); else -> onReschedule() }; action = "" }) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton({ action = "" }) { Text(stringResource(R.string.cancel)) } })
    previewPath?.let { path -> androidx.compose.ui.window.Dialog(onDismissRequest = { previewPath = null }) {
        Surface(shape = MaterialTheme.shapes.large) { Column(Modifier.padding(16.dp)) {
            coil3.compose.AsyncImage(java.io.File(context.filesDir, "media/$path"), stringResource(R.string.linked_files), Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 480.dp))
            TextButton({ previewPath = null }) { Text(stringResource(R.string.dismiss)) }
        } }
    } }

}
