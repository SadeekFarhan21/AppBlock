package dev.appblock

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class Template(
    val name: String,
    val emoji: String,
    val days: Set<Int>,
    val allDay: Boolean,
    val start: Int,
    val end: Int,
    val tagline: String,
)

private val TEMPLATES = listOf(
    Template("Focus", "🎯", (1..5).toSet(), false, 9 * 60, 17 * 60, "Boost your work sessions."),
    Template("Study Time", "📚", (1..5).toSet(), false, 16 * 60, 21 * 60, "Navigate your studies with ease."),
    Template("Wind Down", "🌙", (1..7).toSet(), false, 20 * 60, 23 * 60 + 59, "Make sweet dreams come easier."),
    Template("Digital Detox", "🧘", setOf(7), true, 0, 0, "Treat yourself to a phone-free day."),
)

@Composable
fun BlockingScreen(onEditSchedule: (Schedule) -> Unit) {
    val context = LocalContext.current
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) serviceEnabled = isServiceEnabled(context)
        })
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "AppBlock",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (BlockRepository.blockedToday > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${BlockRepository.blockedToday} today",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!serviceEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Blocking is OFF",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Enable the AppBlock accessibility service so blocking can work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }) {
                        Text("Open Accessibility Settings")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        QuickBlockCard()
        Spacer(Modifier.height(16.dp))
        ContentBlocksCard()
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEditSchedule(Schedule(name = "")) }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (BlockRepository.schedules.isEmpty()) {
            Text(
                "No schedules yet. Add one, or start from a template below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        BlockRepository.schedules.forEach { schedule ->
            ScheduleCard(schedule, onClick = { onEditSchedule(schedule) })
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Templates",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Need ideas? Tap one to customize it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(TEMPLATES) { t ->
                Card(
                    onClick = {
                        onEditSchedule(
                            Schedule(
                                name = t.name,
                                emoji = t.emoji,
                                days = t.days,
                                allDay = t.allDay,
                                startMinute = t.start,
                                endMinute = t.end,
                            )
                        )
                    },
                    modifier = Modifier.width(170.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(t.emoji, fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (t.allDay) "All day" else
                                "${Schedule.formatMinute(t.start)} – ${Schedule.formatMinute(t.end)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickBlockCard() {
    val now = rememberNow()
    val active = BlockRepository.quickBlockUntil > now
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var note by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Quick Block",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (active) {
                Text(
                    formatDuration(BlockRepository.quickBlockUntil - now) + " left",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    "Blocking every app and site from all your schedules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    note = if (BlockRepository.stopQuickBlock()) null
                    else "Strict Mode is on — Quick Block can't be stopped early."
                }) {
                    Text("Stop")
                }
            } else {
                Text(
                    "One tap to block everything on your schedule lists, right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { min ->
                        FilterChip(
                            selected = selectedMinutes == min,
                            onClick = { selectedMinutes = min },
                            label = { Text(if (min < 60) "${min}m" else "${min / 60}h") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        BlockRepository.startQuickBlock(selectedMinutes * 60_000L)
                        note = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    enabled = BlockRepository.schedules.any { it.apps.isNotEmpty() || it.sites.isNotEmpty() }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start blocking", style = MaterialTheme.typography.titleMedium)
                }
                if (BlockRepository.schedules.none { it.apps.isNotEmpty() || it.sites.isNotEmpty() }) {
                    Text(
                        "Add apps or sites to a schedule first — Quick Block uses those lists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentBlocksCard() {
    var note by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Content Blocks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No-go zones inside apps you otherwise allow. Always on, day and night.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            ContentBlockRow(
                title = "YouTube Shorts",
                subtitle = "The Shorts player in the app, and youtube.com/shorts on the web",
                checked = BlockRepository.blockYtShorts,
                onChange = { checked ->
                    note = if (BlockRepository.setBlockYtShorts(checked)) null
                    else "Strict Mode is on — content blocks can't be turned off."
                }
            )
            ContentBlockRow(
                title = "YouTube Music",
                subtitle = "The YT Music app, and music.youtube.com on the web",
                checked = BlockRepository.blockYtMusic,
                onChange = { checked ->
                    note = if (BlockRepository.setBlockYtMusic(checked)) null
                    else "Strict Mode is on — content blocks can't be turned off."
                }
            )
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentBlockRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule, onClick: () -> Unit) {
    var note by remember { mutableStateOf<String?>(null) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActiveNow())
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(schedule.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                        if (schedule.isActiveNow()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${schedule.daysSummary()}, ${schedule.timeSummary()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${schedule.apps.size} apps · ${schedule.sites.size} sites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { checked ->
                        note = if (BlockRepository.setScheduleEnabled(schedule.id, checked)) null
                        else "Strict Mode is on — schedules can't be disabled."
                    }
                )
            }
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
