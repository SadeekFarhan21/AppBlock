package dev.appblock

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.time.LocalDate

/**
 * Single source of truth for the blocking setup. Backed by SharedPreferences so
 * the accessibility service and the UI (separate lifecycles, same process) always
 * agree, and exposed as Compose snapshot state so the UI recomposes on change.
 */
object BlockRepository {

    private const val PREFS_NAME = "appblock"
    private const val KEY_SCHEDULES = "schedules_json"
    private const val KEY_QUICK_UNTIL = "quick_until"
    private const val KEY_STRICT_UNTIL = "strict_until"
    private const val KEY_ATTEMPTS_TOTAL = "attempts_total"
    private const val KEY_ATTEMPTS_DAY = "attempts_day"
    private const val KEY_ATTEMPTS_DAY_COUNT = "attempts_day_count"

    // v1 storage (flat app/site sets) — migrated into an "Always On" schedule.
    private const val KEY_LEGACY_APPS = "blocked_apps"
    private const val KEY_LEGACY_SITES = "blocked_sites"

    private lateinit var prefs: SharedPreferences

    var schedules by mutableStateOf<List<Schedule>>(emptyList())
        private set
    var quickBlockUntil by mutableLongStateOf(0L)
        private set
    var strictUntil by mutableLongStateOf(0L)
        private set
    var blockedToday by mutableIntStateOf(0)
        private set
    var blockedTotal by mutableIntStateOf(0)
        private set

    val isStrict: Boolean get() = System.currentTimeMillis() < strictUntil
    val isQuickBlocking: Boolean get() = System.currentTimeMillis() < quickBlockUntil

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyLists()
        schedules = loadSchedules()
        quickBlockUntil = prefs.getLong(KEY_QUICK_UNTIL, 0L)
        strictUntil = prefs.getLong(KEY_STRICT_UNTIL, 0L)
        blockedTotal = prefs.getInt(KEY_ATTEMPTS_TOTAL, 0)
        blockedToday = if (prefs.getLong(KEY_ATTEMPTS_DAY, 0L) == LocalDate.now().toEpochDay()) {
            prefs.getInt(KEY_ATTEMPTS_DAY_COUNT, 0)
        } else 0
    }

    private fun migrateLegacyLists() {
        if (prefs.contains(KEY_SCHEDULES)) return
        val apps = prefs.getStringSet(KEY_LEGACY_APPS, emptySet())!!
        val sites = prefs.getStringSet(KEY_LEGACY_SITES, emptySet())!!
        if (apps.isEmpty() && sites.isEmpty()) return
        val always = Schedule(
            name = "Always On",
            emoji = "🛡️",
            apps = apps.toSet(),
            sites = sites.toSet(),
        )
        prefs.edit()
            .putString(KEY_SCHEDULES, JSONArray().put(always.toJson()).toString())
            .remove(KEY_LEGACY_APPS)
            .remove(KEY_LEGACY_SITES)
            .apply()
    }

    private fun loadSchedules(): List<Schedule> {
        val raw = prefs.getString(KEY_SCHEDULES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Schedule.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistSchedules(list: List<Schedule>) {
        schedules = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SCHEDULES, arr.toString()).apply()
    }

    // ---- Mutations. Each returns false when Strict Mode forbids the change. ----

    fun upsertSchedule(s: Schedule): Boolean {
        val exists = schedules.any { it.id == s.id }
        if (exists && isStrict) return false
        persistSchedules(
            if (exists) schedules.map { if (it.id == s.id) s else it } else schedules + s
        )
        return true
    }

    fun deleteSchedule(id: String): Boolean {
        if (isStrict) return false
        persistSchedules(schedules.filterNot { it.id == id })
        return true
    }

    fun setScheduleEnabled(id: String, enabled: Boolean): Boolean {
        if (!enabled && isStrict) return false
        persistSchedules(schedules.map { if (it.id == id) it.copy(enabled = enabled) else it })
        return true
    }

    fun startQuickBlock(durationMs: Long) {
        quickBlockUntil = maxOf(quickBlockUntil, System.currentTimeMillis() + durationMs)
        prefs.edit().putLong(KEY_QUICK_UNTIL, quickBlockUntil).apply()
    }

    fun stopQuickBlock(): Boolean {
        if (isStrict) return false
        quickBlockUntil = 0L
        prefs.edit().putLong(KEY_QUICK_UNTIL, 0L).apply()
        return true
    }

    /**
     * Starts the lock, or extends it by [durationMs] when already active.
     * Time only ever gets added — the lock can never be shortened.
     */
    fun activateStrict(durationMs: Long) {
        val base = maxOf(strictUntil, System.currentTimeMillis())
        strictUntil = base + durationMs
        prefs.edit().putLong(KEY_STRICT_UNTIL, strictUntil).apply()
    }

    fun recordBlocked() {
        val today = LocalDate.now().toEpochDay()
        blockedToday = if (prefs.getLong(KEY_ATTEMPTS_DAY, 0L) == today) blockedToday + 1 else 1
        blockedTotal += 1
        prefs.edit()
            .putLong(KEY_ATTEMPTS_DAY, today)
            .putInt(KEY_ATTEMPTS_DAY_COUNT, blockedToday)
            .putInt(KEY_ATTEMPTS_TOTAL, blockedTotal)
            .apply()
    }

    // ---- Queries used by the accessibility service. ----

    /** Human-readable reason ("Quick Block" or the schedule name) or null if allowed. */
    fun blockReasonForApp(pkg: String): String? {
        if (isQuickBlocking && schedules.any { pkg in it.apps }) return "Quick Block"
        return schedules.firstOrNull { it.isActiveNow() && pkg in it.apps }
            ?.let { "${it.emoji} ${it.name}" }
    }

    fun blockReasonForSite(host: String): String? {
        val h = host.lowercase().removePrefix("www.")
        fun Set<String>.matches() = any { h == it || h.endsWith(".$it") }
        if (isQuickBlocking && schedules.any { it.sites.matches() }) return "Quick Block"
        return schedules.firstOrNull { it.isActiveNow() && it.sites.matches() }
            ?.let { "${it.emoji} ${it.name}" }
    }

    /** Turns user input like "https://www.YouTube.com/watch" into "youtube.com". */
    fun normalizeDomain(input: String): String? {
        var s = input.trim().lowercase()
        if (s.isEmpty()) return null
        s = s.substringAfter("://")
        s = s.substringBefore('/').substringBefore('?').substringBefore(':')
        s = s.removePrefix("www.")
        if (s.isEmpty() || !s.contains('.')) return null
        return s
    }
}
