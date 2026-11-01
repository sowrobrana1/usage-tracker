package com.sowrobrana.usagetracker

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val pkg = intent.getStringExtra("pkg") ?: return finish()
        val dayOffset = intent.getIntExtra("dayOffset", 0)

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis

        val app = UsageHelper.getDailyUsage(this, start, end).find { it.packageName == pkg }
        if (app == null) return finish()

        findViewById<ImageView>(R.id.dIcon).setImageDrawable(app.icon)
        findViewById<TextView>(R.id.dName).text = app.appName
        findViewById<TextView>(R.id.dTime).text = "মোট: ${Bangla.duration(app.totalMinutes)}"
        findViewById<TextView>(R.id.dOpens).text =
            if (app.openCount > 0) "${Bangla.num(app.openCount)} বার খোলা হয়েছে" else "খোলার হিসাব পাওয়া যায়নি"

        val tf = SimpleDateFormat("hh:mm a", Locale.US)
        val firstLast = findViewById<TextView>(R.id.dFirstLast)
        firstLast.text = if (app.firstUsed > 0) {
            "প্রথম: ${toBanglaTime(tf.format(app.firstUsed))}   •   শেষ: ${toBanglaTime(tf.format(app.lastUsed))}"
        } else ""

        // Peak hour
        val peak = app.hourly.indices.maxByOrNull { app.hourly[it] } ?: 0
        findViewById<TextView>(R.id.dPeak).text =
            if (app.hourly[peak] > 0) "সবচেয়ে বেশি: ${Bangla.hourLabel(peak)}" else ""

        findViewById<HourBarView>(R.id.hourChart).hours = app.hourly
    }

    private fun toBanglaTime(us: String): String {
        // "09:35 AM" -> "৯:৩৫ সকাল"
        val sb = StringBuilder()
        for (c in us) {
            sb.append(
                when (c) {
                    in '0'..'9' -> Bangla.num(c - '0')
                    'A', 'a' -> ""
                    'M', 'm' -> ""
                    'P' -> ""
                    else -> c.toString()
                }
            )
        }
        var s = sb.toString().trim()
        s += if (us.contains("AM", ignoreCase = true)) " সকাল" else " বিকাল"
        return s
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
