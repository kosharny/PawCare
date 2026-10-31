package com.example.pawcare.core.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.pawcare.R
import com.example.pawcare.domain.model.*
import com.example.pawcare.ui.theme.PawCareTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun Category.label(): Int = when (this) {
    Category.TREATMENT -> R.string.category_treatment; Category.VACCINATION -> R.string.category_vaccination
    Category.GROOMING -> R.string.category_grooming; Category.VET -> R.string.category_vet; Category.OTHER -> R.string.category_other
}
fun displayDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
@Composable fun AppIcon(@DrawableRes id: Int, description: String? = null, modifier: Modifier = Modifier) {
    Icon(painterResource(id), description, modifier.size(24.dp))
}
@Composable fun PetAvatar(pet: Pet?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (pet?.photoPath == null) AppIcon(R.drawable.ic_pet, stringResource(R.string.no_photo), Modifier.size(36.dp))
        else AsyncImage(File(context.filesDir, "media/${pet.photoPath}"), stringResource(R.string.no_photo), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
@Composable fun PetSwitcher(pets: List<Pet>, selected: String?, includeAll: Boolean, onSelect: (String?) -> Unit) {
    if (pets.isEmpty()) return
    Choice(stringResource(R.string.select_pet), selected ?: if (pets.size == 1 || !includeAll) pets.first().id else "", buildList {
        if (includeAll && pets.size > 1) add("" to stringResource(R.string.all_pets))
        addAll(pets.map { it.id to it.name })
    }) { onSelect(it.ifEmpty { null }) }
}
@Composable fun SectionHeader(title: String) { Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
@Composable fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled) { Text(text) }
}
@Composable fun EmptyState(title: String, body: String = "") {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(R.drawable.ic_pet)
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable fun ErrorState(message: Int?, onDismiss: () -> Unit) {
    if (message != null) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(stringResource(message)); TextButton(onDismiss) { Text(stringResource(R.string.dismiss)) } }
    }
}
@Composable fun CategoryChip(category: Category) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(stringResource(category.label()), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}
@Composable fun CareEventCard(event: CareEvent, actions: @Composable RowScope.() -> Unit = {}, onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(event.category)
            Text(event.title, style = MaterialTheme.typography.titleLarge)
            Text(event.pet.name + " · " + displayDate(event.occurrence.date) + (event.occurrence.time?.let { " · $it" } ?: ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Page(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        if (onBack != null) IconButton(onBack) { AppIcon(R.drawable.ic_back, stringResource(R.string.back)) }
    }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
@Composable fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, multiline: Boolean = false, error: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = !multiline, minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), isError = error,
        supportingText = if (error) ({ Text(stringResource(R.string.error_required)) }) else null, shape = MaterialTheme.shapes.small)
}
@Composable fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, onChange)
    }
}
@Composable fun Choice(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Column(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.labelSmall); Text(options.find { it.first == value }?.second ?: stringResource(R.string.select_pet)) }
        }
        DropdownMenu(expanded, { expanded = false }) { options.forEach { (key, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { onChange(key); expanded = false }) } }
    }
}
@Composable fun DateField(label: String, value: String, onChange: (String) -> Unit, optional: Boolean = false) {
    val context = LocalContext.current
    val date = value.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton({ DatePickerDialog(context, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).show() }, Modifier.weight(1f).heightIn(min = 52.dp)) {
            Text(label + if (value.isNotBlank()) ": ${displayDate(date)}" else "")
        }
        if (optional && value.isNotBlank()) TextButton({ onChange("") }) { Text(stringResource(R.string.cancel)) }
    }
}
@Composable fun TimeField(label: String, value: String, onChange: (String) -> Unit, optional: Boolean = false) {
    val context = LocalContext.current
    val time = value.takeIf { it.isNotBlank() }?.let(LocalTime::parse) ?: LocalTime.of(9, 0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton({ TimePickerDialog(context, { _, h, m -> onChange(LocalTime.of(h, m).toString()) }, time.hour, time.minute, android.text.format.DateFormat.is24HourFormat(context)).show() }, Modifier.weight(1f).heightIn(min = 52.dp)) {
            Text("$label: " + if (value.isBlank()) stringResource(R.string.no_time) else time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
        }
        if (optional && value.isNotBlank()) TextButton({ onChange("") }) { Text(stringResource(R.string.cancel)) }
    }
}
@Composable fun ConfirmDialog(body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.confirm_delete)) }, text = { Text(body) }, confirmButton = { TextButton(onConfirm) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}
@Composable fun SaveResult(operation: OperationState, acknowledge: () -> Unit, onSaved: () -> Unit) {
    LaunchedEffect(operation.saved) { if (operation.saved) { acknowledge(); onSaved() } }
}
@Preview(showBackground = true)
@Composable private fun EmptyStatePreview() { PawCareTheme { EmptyState("На сегодня всё", "Важные даты будут здесь") } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LazyPage(title: String, onBack: (() -> Unit)? = null, floatingActionButton: @Composable () -> Unit = {}, content: LazyListScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        if (onBack != null) IconButton(onBack) { AppIcon(R.drawable.ic_back, stringResource(R.string.back)) }
    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        floatingActionButton = floatingActionButton, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable private fun CareEventCardPreview() {
    val pet = Pet(id = "preview", name = "Бадди")
    val plan = CarePlan(id = "preview-plan", petId = pet.id, title = "Плановый уход", category = Category.GROOMING, anchorDate = LocalDate.of(2026, 10, 25))
    val occurrence = CareOccurrence(planId = plan.id, petId = pet.id, originalDate = plan.anchorDate, date = plan.anchorDate)
    PawCareTheme { CareEventCard(CareEvent(occurrence, plan, pet)) {} }
}
