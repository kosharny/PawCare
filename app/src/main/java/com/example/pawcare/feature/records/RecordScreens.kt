package com.example.pawcare.feature.records

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import java.io.File
import java.text.NumberFormat

@Composable fun HistoryRoute(petId: String, onBack: () -> Unit, onEvent: (String) -> Unit, onAddPast: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    LaunchedEffect(petId) { viewModel.load(petId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, onBack, onEvent, onAddPast) { viewModel.change("category", it) }
}
@Composable fun HistoryScreen(state: HistoryUiState, onBack: () -> Unit, onEvent: (String) -> Unit, onAddPast: () -> Unit, onCategory: (String) -> Unit) {
    LazyPage(stringResource(R.string.history), onBack) {
        item("header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.loading) CircularProgressIndicator()
                state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                PrimaryButton(stringResource(R.string.add_past), onClick = onAddPast)
                Choice(stringResource(R.string.category), state.category, listOf("" to stringResource(R.string.all_categories)) + Category.entries.map { it.name to stringResource(it.label()) }, onCategory)
                if (!state.loading && state.error == null && state.events.isEmpty()) EmptyState(stringResource(R.string.no_entries), stringResource(R.string.empty_history))
            }
        }
        items(state.events, key = { it.occurrence.id }, contentType = { "history" }) { event ->
            val record = state.completions.find { it.occurrenceId == event.occurrence.id }
            OutlinedCard(onClick = { onEvent(event.occurrence.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(record?.category ?: event.category)
                    Text(record?.title ?: event.title, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (record == null) R.string.skipped else R.string.complete) + " · " + displayDate(record?.completedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate() ?: event.occurrence.date))
                    record?.let { if (it.comment.isNotBlank()) Text(it.comment); if (it.notes.isNotBlank()) Text(it.notes) }
                }
            }
        }
    }
}
@Composable fun WeightRoute(petId: String, onBack: () -> Unit, viewModel: WeightViewModel = hiltViewModel()) {
    LaunchedEffect(petId) { viewModel.load(petId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    WeightScreen(state, fields, operation, onBack, viewModel::edit, viewModel::change, viewModel::save, viewModel::delete, viewModel::dismissError, viewModel::acknowledgeSave)
}
@Composable fun WeightScreen(state: WeightUiState, fields: Map<String, String>, operation: OperationState, onBack: () -> Unit, onEdit: (WeightEntry?) -> Unit, onChange: (String, String) -> Unit, onSave: () -> Unit, onDelete: (String) -> Unit, onDismissError: () -> Unit, onAcknowledgeSave: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    SaveResult(operation, onAcknowledgeSave) { editing = false }
    Page(stringResource(R.string.weight), onBack) {
        ErrorState(operation.error ?: state.error, onDismissError)
        if (state.loading) CircularProgressIndicator()
        PrimaryButton(stringResource(R.string.add), enabled = !operation.busy) { onEdit(null); editing = true }
        WeightChart(state.entries)
        state.entries.forEach { entry ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.weight_value, NumberFormat.getNumberInstance().format(entry.kilograms)), style = MaterialTheme.typography.titleLarge)
                    Text(displayDate(entry.date)); if (entry.note.isNotBlank()) Text(entry.note)
                    Row { TextButton({ onEdit(entry); editing = true }, enabled = !operation.busy) { Text(stringResource(R.string.edit)) }; TextButton({ deleting = entry.id }, enabled = !operation.busy) { Text(stringResource(R.string.delete)) } }
                }
            }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { if (!operation.busy) editing = false }, title = { Text(stringResource(R.string.measurement)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorState(operation.error, onDismissError)
            Field(stringResource(R.string.kilograms), fields["weight"].orEmpty(), { onChange("weight", it) }, numeric = true)
            DateField(stringResource(R.string.date), fields["date"].orEmpty(), { onChange("date", it) })
            Field(stringResource(R.string.note), fields["note"].orEmpty(), { onChange("note", it) })
        }
    }, confirmButton = { TextButton(onSave, enabled = !operation.busy) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton({ editing = false }, enabled = !operation.busy) { Text(stringResource(R.string.cancel)) } })
    deleting?.let { id -> ConfirmDialog(stringResource(R.string.confirm_delete), { deleting = null; onDelete(id) }, { deleting = null }) }
}
@Composable fun WeightChart(entries: List<WeightEntry>) {
    if (entries.size < 2) { EmptyState(stringResource(R.string.weight_chart), stringResource(R.string.chart_hint)); return }
    Text(stringResource(R.string.weight_chart), style = MaterialTheme.typography.titleLarge)
    val color = MaterialTheme.colorScheme.primary
    val sorted = entries.sortedWith(compareBy({ it.date }, { it.id }))
    val min = sorted.minOf { it.kilograms }; val max = sorted.maxOf { it.kilograms }
    val start = sorted.first().date.toEpochDay(); val span = (sorted.last().date.toEpochDay() - start).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val padding = 16.dp.toPx()
        val points = sorted.map {
            Offset(padding + ((it.date.toEpochDay() - start).toFloat() / span) * (size.width - 2 * padding),
                size.height - padding - ((it.kilograms - min + 0.5) / (max - min + 1)).toFloat() * (size.height - 2 * padding))
        }
        points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, strokeWidth = 3.dp.toPx()) }
        points.forEach { drawCircle(color, 4.dp.toPx(), it) }
    }
    Text(displayDate(sorted.first().date) + " — " + displayDate(sorted.last().date), style = MaterialTheme.typography.bodySmall)
}
@Composable fun ContactsRoute(petId: String, onBack: () -> Unit, viewModel: ContactsViewModel = hiltViewModel()) {
    LaunchedEffect(petId) { viewModel.load(petId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    ContactsScreen(state, fields, operation, onBack, viewModel::edit, viewModel::change, viewModel::save, viewModel::delete, viewModel::dismissError, viewModel::acknowledgeSave, viewModel::reportError)
}
@Composable fun ContactsScreen(state: ContactsUiState, fields: Map<String, String>, operation: OperationState, onBack: () -> Unit, onEdit: (Contact?) -> Unit, onChange: (String, String) -> Unit, onSave: () -> Unit, onDelete: (String) -> Unit, onDismissError: () -> Unit, onAcknowledgeSave: () -> Unit, onError: (Int) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    SaveResult(operation, onAcknowledgeSave) { editing = false }
    androidx.activity.compose.BackHandler(editing) { if (!operation.busy) editing = false }
    if (editing) Page(stringResource(R.string.contact), { editing = false }) {
        ErrorState(operation.error, onDismissError)
        Field(stringResource(R.string.name), fields["name"].orEmpty(), { onChange("name", it) }, error = operation.error == R.string.error_validation && fields["name"].isNullOrBlank())
        Choice(stringResource(R.string.contact_type), fields["type"].orEmpty(), listOf(ContactType.VET.name to stringResource(R.string.contact_vet), ContactType.CLINIC.name to stringResource(R.string.contact_clinic), ContactType.GROOMER.name to stringResource(R.string.contact_groomer), ContactType.OTHER.name to stringResource(R.string.contact_other))) { onChange("type", it) }
        Field(stringResource(R.string.phone), fields["phone"].orEmpty(), { onChange("phone", it) })
        Field(stringResource(R.string.address), fields["address"].orEmpty(), { onChange("address", it) })
        Field(stringResource(R.string.note), fields["note"].orEmpty(), { onChange("note", it) }, multiline = true)
        PrimaryButton(stringResource(R.string.save), !operation.busy, onSave)
    } else Page(stringResource(R.string.contacts), onBack) {
        ErrorState(operation.error ?: state.error, onDismissError)
        if (state.loading) CircularProgressIndicator()
        PrimaryButton(stringResource(R.string.add), !operation.busy) { onEdit(null); editing = true }
        if (!state.loading && state.error == null && state.entries.isEmpty()) EmptyState(stringResource(R.string.no_entries))
        state.entries.forEach { contact ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contact.name, style = MaterialTheme.typography.titleLarge)
                    if (contact.phone.isNotBlank()) {
                        Text(contact.phone)
                        TextButton({ try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.phone, null))) } catch (_: android.content.ActivityNotFoundException) { onError(R.string.error_open) } }) { Text(stringResource(R.string.dial)) }
                    }
                    if (contact.address.isNotBlank()) {
                        Text(contact.address)
                        TextButton({ context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("", contact.address)) }) { Text(stringResource(R.string.copy_address)) }
                    }
                    if (contact.note.isNotBlank()) Text(contact.note)
                    Row { TextButton({ onEdit(contact); editing = true }) { Text(stringResource(R.string.edit)) }; TextButton({ deleting = contact.id }, enabled = !operation.busy) { Text(stringResource(R.string.delete)) } }
                }
            }
        }
    }
    deleting?.let { id -> ConfirmDialog(stringResource(R.string.confirm_delete), { deleting = null; onDelete(id) }, { deleting = null }) }
}
@Composable fun DocumentsRoute(petId: String, onBack: () -> Unit, viewModel: DocumentsViewModel = hiltViewModel()) {
    LaunchedEffect(petId) { viewModel.load(petId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    DocumentsScreen(state, fields, operation, onBack, viewModel::import, viewModel::change, viewModel::rename, viewModel::delete, viewModel::dismissError, viewModel::acknowledgeSave, viewModel::reportError, viewModel::uri)
}
@Composable fun DocumentsScreen(state: DocumentsUiState, fields: Map<String, String>, operation: OperationState, onBack: () -> Unit, onImport: (Uri) -> Unit, onChange: (String, String) -> Unit, onRename: (String, String) -> Unit, onDelete: (String) -> Unit, onDismissError: () -> Unit, onAcknowledgeSave: () -> Unit, onError: (Int) -> Unit, onUri: (String) -> Uri) {
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onImport) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(onImport) }
    SaveResult(operation, onAcknowledgeSave) { renaming = null }
    Page(stringResource(R.string.documents), onBack) {
        ErrorState(operation.error ?: state.error, onDismissError)
        if (state.loading) CircularProgressIndicator()
        PrimaryButton(stringResource(R.string.import_file), !operation.busy) { pdfPicker.launch(arrayOf("application/pdf", "image/*")) }
        OutlinedButton({ photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !operation.busy) { Text(stringResource(R.string.import_photo)) }
        if (!state.loading && state.error == null && state.entries.isEmpty()) EmptyState(stringResource(R.string.no_entries))
        state.entries.forEach { attachment ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(R.drawable.ic_document)
                    Text(attachment.name, style = MaterialTheme.typography.titleMedium)
                    Text(displayDate(attachment.importedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()))
                    Row {
                        TextButton({
                            if (attachment.mime.startsWith("image/")) preview = attachment.path
                            else try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(onUri(attachment.path), attachment.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (_: Exception) { onError(R.string.error_open) }
                        }) { Text(stringResource(R.string.open)) }
                        TextButton({
                            try {
                                val uri = onUri(attachment.path)
                                val intent = Intent(Intent.ACTION_SEND).setType(attachment.mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.clipData = ClipData.newRawUri("", uri)
                                context.startActivity(Intent.createChooser(intent, null))
                            } catch (_: Exception) { onError(R.string.error_open) }
                        }) { Text(stringResource(R.string.share)) }
                    }
                    Row {
                        TextButton({ onChange("name", attachment.name); renaming = attachment.id }) { Text(stringResource(R.string.rename)) }
                        TextButton({ deleting = attachment.id }, enabled = !operation.busy) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
    }
    deleting?.let { id -> ConfirmDialog(stringResource(R.string.confirm_delete), { deleting = null; onDelete(id) }, { deleting = null }) }
    renaming?.let { id -> AlertDialog(onDismissRequest = { renaming = null }, title = { Text(stringResource(R.string.rename)) }, text = { Column { ErrorState(operation.error, onDismissError); Field(stringResource(R.string.file_name), fields["name"].orEmpty(), { onChange("name", it) }) } }, confirmButton = { TextButton({ onRename(id, fields["name"].orEmpty()) }, enabled = !operation.busy) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton({ renaming = null }) { Text(stringResource(R.string.cancel)) } }) }
    preview?.let { path -> Dialog(onDismissRequest = { preview = null }) {
        Surface(shape = MaterialTheme.shapes.large) { Column(Modifier.padding(16.dp)) {
            AsyncImage(File(context.filesDir, "media/$path"), stringResource(R.string.documents), Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 480.dp))
            TextButton({ preview = null }) { Text(stringResource(R.string.dismiss)) }
        } }
    } }
}
