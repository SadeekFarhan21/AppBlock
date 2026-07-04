package dev.appblock

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EMOJIS = listOf("🎯", "📚", "🌙", "🧘", "💼", "🛡️", "⏳", "🏖️", "🍰", "🎮")
private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(initial: Schedule, onClose: () -> Unit) {
    val context = LocalContext.current
    val isNew = BlockRepository.schedules.none { it.id == initial.id }
    val locked = BlockRepository.isStrict && !isNew

    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var days by remember { mutableStateOf(initial.days) }
    var allDay by remember { mutableStateOf(initial.allDay) }
    var start by remember { mutableIntStateOf(initial.startMinute) }
    var end by remember { mutableIntStateOf(initial.endMinute) }
    var apps by remember { mutableStateOf(initial.apps) }
    var sites by remember { mutableStateOf(initial.sites) }
    var query by remember { mutableStateOf("") }
    var siteInput by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf<List<InstalledApp>?>(null) }

    LaunchedEffect(Unit) { installed = AppsCache.get(context) }
    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Schedule" else "Edit Schedule") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && !locked) {
                        IconButton(onClick = {
                            BlockRepository.deleteSchedule(initial.id)
                            onClose()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete schedule",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    BlockRepository.upsertSchedule(
                        Schedule(
                            id = initial.id,
                            name = name.trim(),
                            emoji = emoji,
                            enabled = initial.enabled,
                            days = days,
                            allDay = allDay,
                            startMinute = start,
                            endMinute = end,
                            apps = apps,
                            sites = sites,
                        )
                    )
                    onClose()
                },
                enabled = !locked && name.isNotBlank() && days.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Save")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            if (locked) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            "Strict Mode is on — this schedule is read-only until the lock ends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    EMOJIS.forEach { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { if (!locked) emoji = e },
                            label = { Text(e, fontSize = 18.sp) }
                        )
                    }
                }
            }

            item {
                Text("Days", style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = day in days,
                            onClick = {
                                if (!locked) days = if (day in days) days - day else days + day
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("All day", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = allDay,
                        onCheckedChange = { if (!locked) allDay = it },
                        enabled = !locked
                    )
                }
                if (!allDay) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!locked) TimePickerDialog(
                                    context,
                                    { _, h, m -> start = h * 60 + m },
                                    start / 60, start % 60, true
                                ).show()
                            }
                        ) { Text(Schedule.formatMinute(start)) }
                        Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = {
                                if (!locked) TimePickerDialog(
                                    context,
                                    { _, h, m -> end = h * 60 + m },
                                    end / 60, end % 60, true
                                ).show()
                            }
                        ) { Text(Schedule.formatMinute(end)) }
                        Text(
                            if (end <= start) "overnight" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    "Websites (${sites.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = siteInput,
                        onValueChange = { siteInput = it },
                        label = { Text("Domain, e.g. youtube.com") },
                        singleLine = true,
                        enabled = !locked,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            BlockRepository.normalizeDomain(siteInput)?.let { sites = sites + it }
                            siteInput = ""
                        },
                        enabled = !locked && BlockRepository.normalizeDomain(siteInput) != null
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add site")
                    }
                }
            }

            items(sites.sorted(), key = { "site-$it" }) { site ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(site, Modifier.weight(1f))
                    IconButton(onClick = { if (!locked) sites = sites - site }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove $site",
                            tint = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Text(
                    "Apps (${apps.size} selected)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            val appList = installed
            if (appList == null) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else {
                val visible =
                    if (query.isBlank()) appList
                    else appList.filter { it.label.contains(query, ignoreCase = true) }
                items(visible, key = { it.packageName }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = app.packageName in apps,
                            onCheckedChange = { checked ->
                                if (!locked) {
                                    apps = if (checked) apps + app.packageName
                                    else apps - app.packageName
                                }
                            },
                            enabled = !locked
                        )
                    }
                }
            }
        }
    }
}
