package com.sowrobrana.usagetracker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process

/** One app's usage for a day. */
data class AppUsage(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalMillis: Long,
    val openCount: Int,
    val firstUsed: Long,
    val lastUsed: Long,
    /** Foreground millis per hour bucket 0..23 */
    val hourly: LongArray = LongArray(24)
) {
    val totalMinutes: Long get() = totalMillis / 60000
}

object UsageHelper {

    fun hasPermission(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getDailyUsage(ctx: Context, dayStart: Long, dayEnd: Long): List<AppUsage> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = ctx.packageManager

        // 1) Total foreground time per package.
        val totals = HashMap<String, Long>()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd)
        if (stats != null) {
            for (s in stats) {
                if (s.totalTimeInForeground > 0) {
                    totals[s.packageName] = (totals[s.packageName] ?: 0L) + s.totalTimeInForeground
                }
            }
        }

        // 2) Open counts + hourly buckets + first/last from events.
        val opens = HashMap<String, Int>()
        val hourly = HashMap<String, LongArray>()
        val first = HashMap<String, Long>()
        val last = HashMap<String, Long>()
        val resumeAt = HashMap<String, Long>()

        val events = usm.queryEvents(dayStart, dayEnd)
        val ev = UsageEvents.Event()
        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            when (ev.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    opens[pkg] = (opens[pkg] ?: 0) + 1
                    resumeAt[pkg] = ev.timeStamp
                    val t = ev.timeStamp
                    if (t in dayStart until dayEnd) {
                        if (!first.containsKey(pkg) || t < first[pkg]!!) first[pkg] = t
                        last[pkg] = maxOf(last[pkg] ?: 0L, t)
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = resumeAt.remove(pkg)
                    if (start != null && ev.timeStamp > start) {
                        distribute(pkg, start, ev.timeStamp, dayStart, hourly)
                        last[pkg] = maxOf(last[pkg] ?: 0L, ev.timeStamp)
                    }
                }
            }
        }
        // Sessions still open at query end.
        for ((pkg, start) in resumeAt) {
            distribute(pkg, start, dayEnd, dayStart, hourly)
        }

        val out = ArrayList<AppUsage>()
        val allPkgs = (totals.keys + opens.keys)
        for (pkg in allPkgs) {
            val total = totals[pkg] ?: 0L
            if (total <= 0 && (opens[pkg] ?: 0) == 0) continue
            var name = pkg
            var icon: Drawable? = null
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                name = pm.getApplicationLabel(ai).toString()
                icon = pm.getApplicationIcon(ai)
            } catch (_: PackageManager.NameNotFoundException) { }
            out.add(
                AppUsage(
                    packageName = pkg,
                    appName = name,
                    icon = icon,
                    totalMillis = total,
                    openCount = opens[pkg] ?: 0,
                    firstUsed = first[pkg] ?: 0L,
                    lastUsed = last[pkg] ?: 0L,
                    hourly = hourly[pkg] ?: LongArray(24)
                )
            )
        }
        out.sortByDescending { it.totalMillis }
        return out
    }

    /** Spread [start,end) foreground millis into hour buckets relative to dayStart. */
    private fun distribute(
        pkg: String, start: Long, end: Long, dayStart: Long,
        hourly: HashMap<String, LongArray>
    ) {
        val arr = hourly.getOrPut(pkg) { LongArray(24) }
        var s = maxOf(start, dayStart)
        val e = end
        while (s < e) {
            val hourIndex = ((s - dayStart) / 3600000L).toInt().coerceIn(0, 23)
            val hourEnd = dayStart + (hourIndex + 1) * 3600000L
            val chunk = minOf(e, hourEnd) - s
            arr[hourIndex] += chunk
            s += chunk
        }
    }
}
