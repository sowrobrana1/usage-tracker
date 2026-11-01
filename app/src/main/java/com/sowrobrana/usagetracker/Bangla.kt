package com.sowrobrana.usagetracker

/** Bangla digit + duration formatting helpers. */
object Bangla {
    private val digits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun num(n: Long): String {
        val s = n.toString()
        val b = StringBuilder(s.length)
        for (c in s) b.append(if (c in '0'..'9') digits[c - '0'] else c)
        return b.toString()
    }

    fun num(n: Int): String = num(n.toLong())

    /** 95 -> "১ ঘণ্টা ৩৫ মিনিট", 45 -> "৪৫ মিনিট", 0 -> "১ মিনিটের কম" */
    fun duration(mins: Long): String {
        if (mins < 1) return "১ মিনিটের কম"
        val h = mins / 60
        val m = mins % 60
        return when {
            h == 0L -> "${num(m)} মিনিট"
            m == 0L -> "${num(h)} ঘণ্টা"
            else -> "${num(h)} ঘণ্টা ${num(m)} মিনিট"
        }
    }

    fun durationShort(mins: Long): String {
        if (mins < 1) return "<১ মি."
        val h = mins / 60
        val m = mins % 60
        return if (h == 0L) "${num(m)} মি." else "${num(h)}ঘ ${num(m)}মি."
    }

    /** "কখন" line: 1698765432000 -> "সকাল ৯:৩৫" style via hour */
    fun hourLabel(hour24: Int): String {
        val h12 = when {
            hour24 == 0 -> 12
            hour24 <= 12 -> hour24
            else -> hour24 - 12
        }
        val period = when (hour24) {
            in 4..11 -> "সকাল"
            in 12..16 -> "দুপুর"
            in 17..19 -> "বিকাল"
            in 20..23 -> "রাত"
            else -> "রাত"
        }
        return "$period ${num(h12)}টা"
    }
}
