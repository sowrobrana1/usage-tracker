package com.sowrobrana.usagetracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: UsageAdapter
    private var dayOffset = 0 // 0 = today, 1 = yesterday, ...

    private lateinit var dateLabel: TextView
    private lateinit var totalLabel: TextView
    private lateinit var list: RecyclerView
    private lateinit var emptyLabel: TextView
    private lateinit var permBox: View
    private lateinit var mainBox: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dateLabel = findViewById(R.id.dateLabel)
        totalLabel = findViewById(R.id.totalLabel)
        list = findViewById(R.id.usageList)
        emptyLabel = findViewById(R.id.emptyLabel)
        permBox = findViewById(R.id.permBox)
        mainBox = findViewById(R.id.mainBox)

        adapter = UsageAdapter(emptyList()) { app ->
            val i = Intent(this, DetailActivity::class.java)
            i.putExtra("pkg", app.packageName)
            i.putExtra("dayOffset", dayOffset)
            startActivity(i)
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<ImageButton>(R.id.prevDay).setOnClickListener {
            if (dayOffset < 6) { dayOffset++; load() }
        }
        findViewById<ImageButton>(R.id.nextDay).setOnClickListener {
            if (dayOffset > 0) { dayOffset--; load() }
        }
        findViewById<Button>(R.id.grantBtn).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val ok = UsageHelper.hasPermission(this)
        permBox.visibility = if (ok) View.GONE else View.VISIBLE
        mainBox.visibility = if (ok) View.VISIBLE else View.GONE
        if (ok) load()
    }

    private fun dayRange(offset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun load() {
        val (start, end) = dayRange(dayOffset)
        val items = UsageHelper.getDailyUsage(this, start, end)

        val cal = Calendar.getInstance().apply { timeInMillis = start }
        dateLabel.text = when (dayOffset) {
            0 -> "আজ"
            1 -> "গতকাল"
            else -> "${Bangla.num(cal.get(Calendar.DAY_OF_MONTH))}/${Bangla.num(cal.get(Calendar.MONTH) + 1)}"
        }

        val totalMins = items.sumOf { it.totalMinutes }
        totalLabel.text = "মোট স্ক্রিন টাইম: ${Bangla.duration(totalMins)}"

        adapter.update(items)
        emptyLabel.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        findViewById<ImageButton>(R.id.nextDay).isEnabled = dayOffset > 0
    }
}
