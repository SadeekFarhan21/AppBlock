package dev.appblock

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class DayUsage(val label: String, val ms: Long)
private data class AppUsage(val label: String, val icon: ImageBitmap?, val ms: Long)
private data class InsightsData(
    val todayMs: Long,
    val week: List<DayUsage>,
    val topApps: List<AppUsage>,
)

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private suspend fun loadInsights(context: Context): InsightsData = withContext(Dispatchers.IO) {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val zone = ZoneId.systemDefault()

    val week = (6 downTo 0).map { back ->
        val day = LocalDate.now().minusDays(back.toLong())
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            System.currentTimeMillis()
        )
        val total = usm.queryAndAggregateUsageStats(start, end)
            .values.sumOf { it.totalTimeInForeground }
        DayUsage(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), total)
    }

    val todayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    val pm = context.packageManager
    val topApps = usm.queryAndAggregateUsageStats(todayStart, System.currentTimeMillis())
        .values
        .filter { it.totalTimeInForeground >= 60_000 && it.packageName != context.packageName }
        .sortedByDescending { it.totalTimeInForeground }
        .take(6)
        .mapNotNull { stats ->
            try {
                val info = pm.getApplicationInfo(stats.packageName, 0)
                AppUsage(
                    label = pm.getApplicationLabel(info).toString(),
                    icon = info.loadIcon(pm).toBitmap(72, 72).asImageBitmap(),
                    ms = stats.totalTimeInForeground
                )
            } catch (e: Exception) {
                null
            }
        }

    InsightsData(todayMs = week.last().ms, week = week, topApps = topApps)
}

@Composable
fun InsightsScreen() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasUsageAccess(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasUsageAccess(context)
        })
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Insights",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        BlockedAttemptsCard()
        Spacer(Modifier.height(16.dp))

        if (!granted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("See your screen time", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Grant usage access to see daily screen time, weekly trends, and your most used apps. The data never leaves your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) {
                        Text("Grant usage access")
                    }
                }
            }
        } else {
            var data by remember { mutableStateOf<InsightsData?>(null) }
            LaunchedEffect(Unit) { data = loadInsights(context) }

            val loaded = data
            if (loaded == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                ScreenTimeCard(loaded)
                Spacer(Modifier.height(16.dp))
                TopAppsCard(loaded.topApps)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BlockedAttemptsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${BlockRepository.blockedToday}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "blocked today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${BlockRepository.blockedTotal}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "blocked all time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(data: InsightsData) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "SCREEN TIME TODAY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatDuration(data.todayMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            val maxMs = data.week.maxOf { it.ms }.coerceAtLeast(1L)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                data.week.forEachIndexed { index, day ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
                    ) {
                        Box(
                            Modifier
                                .width(20.dp)
                                .height(((104f * day.ms / maxMs).coerceAtLeast(4f)).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (index == data.week.lastIndex)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            day.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppsCard(topApps: List<AppUsage>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "TOP APPS TODAY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (topApps.isEmpty()) {
                Text(
                    "Nothing significant yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val maxMs = topApps.maxOfOrNull { it.ms }?.coerceAtLeast(1L) ?: 1L
            topApps.forEach { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    app.icon?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatDuration(app.ms),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { app.ms.toFloat() / maxMs },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
