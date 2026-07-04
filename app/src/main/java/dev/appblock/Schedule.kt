package dev.appblock

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID

data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String = "🎯",
    val enabled: Boolean = true,
    val days: Set<Int> = (1..7).toSet(), // ISO days: 1 = Monday .. 7 = Sunday
    val allDay: Boolean = true,
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 17 * 60,
    val apps: Set<String> = emptySet(),
    val sites: Set<String> = emptySet(),
) {
    fun isActiveNow(): Boolean {
        if (!enabled) return false
        val now = LocalDateTime.now()
        val minute = now.hour * 60 + now.minute
        val today = now.dayOfWeek.value
        if (allDay) return today in days
        return if (startMinute <= endMinute) {
            today in days && minute >= startMinute && minute < endMinute
        } else {
            // Window spans midnight, e.g. 22:00 – 06:00.
            val yesterday = if (today == 1) 7 else today - 1
            (today in days && minute >= startMinute) || (yesterday in days && minute < endMinute)
        }
    }

    fun daysSummary(): String = when (days) {
        (1..7).toSet() -> "Every day"
        (1..5).toSet() -> "Weekdays"
        setOf(6, 7) -> "Weekends"
        else -> days.sorted().joinToString(", ") { DAY_NAMES[it - 1] }
    }

    fun timeSummary(): String =
        if (allDay) "All day" else "${formatMinute(startMinute)} – ${formatMinute(endMinute)}"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("emoji", emoji)
        put("enabled", enabled)
        put("days", JSONArray(days.toList()))
        put("allDay", allDay)
        put("start", startMinute)
        put("end", endMinute)
        put("apps", JSONArray(apps.toList()))
        put("sites", JSONArray(sites.toList()))
    }

    companion object {
        private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        fun formatMinute(m: Int) = "%02d:%02d".format(m / 60, m % 60)

        fun fromJson(o: JSONObject): Schedule = Schedule(
            id = o.getString("id"),
            name = o.getString("name"),
            emoji = o.optString("emoji", "🎯"),
            enabled = o.optBoolean("enabled", true),
            days = o.getJSONArray("days").let { a -> (0 until a.length()).map { a.getInt(it) } }.toSet(),
            allDay = o.optBoolean("allDay", true),
            startMinute = o.optInt("start", 9 * 60),
            endMinute = o.optInt("end", 17 * 60),
            apps = o.getJSONArray("apps").toStringSet(),
            sites = o.getJSONArray("sites").toStringSet(),
        )

        private fun JSONArray.toStringSet(): Set<String> =
            (0 until length()).map { getString(it) }.toSet()
    }
}
