package dev.appblock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val label: String, val packageName: String, val icon: ImageBitmap)

/** Loads the launchable-app list once (icons pre-rasterized off the main thread). */
object AppsCache {

    @Volatile
    private var cached: List<InstalledApp>? = null

    suspend fun get(context: Context): List<InstalledApp> =
        cached ?: withContext(Dispatchers.IO) { load(context.applicationContext) }
            .also { cached = it }

    private fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map {
                InstalledApp(
                    label = pm.getApplicationLabel(it).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
