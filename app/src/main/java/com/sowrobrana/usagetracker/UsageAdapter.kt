package com.sowrobrana.usagetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsageAdapter(
    private var items: List<AppUsage>,
    private val onClick: (AppUsage) -> Unit
) : RecyclerView.Adapter<UsageAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.appIcon)
        val name: TextView = v.findViewById(R.id.appName)
        val time: TextView = v.findViewById(R.id.appTime)
        val opens: TextView = v.findViewById(R.id.appOpens)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val a = items[position]
        h.icon.setImageDrawable(a.icon)
        h.name.text = a.appName
        h.time.text = Bangla.duration(a.totalMinutes)
        h.opens.text = if (a.openCount > 0) "${Bangla.num(a.openCount)} বার খোলা" else ""
        h.itemView.setOnClickListener { onClick(a) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<AppUsage>) {
        items = newItems
        notifyDataSetChanged()
    }
}
