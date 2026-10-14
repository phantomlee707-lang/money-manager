package com.rainweaver.moneymgr.data

import android.content.Context

/** Persisted app settings (fully offline, via SharedPreferences). */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("mm_settings", Context.MODE_PRIVATE)

    var currency: String
        get() = prefs.getString("currency", "$") ?: "$"
        set(v) = prefs.edit().putString("currency", v).apply()

    var decimals: Int
        get() = prefs.getInt("decimals", 2)
        set(v) = prefs.edit().putInt("decimals", v).apply()

    /** One of: comma, dot, space, none (thousands separator) */
    var separator: String
        get() = prefs.getString("separator", "comma") ?: "comma"
        set(v) = prefs.edit().putString("separator", v).apply()

    /** Date display format key: ymd, dmy, mdy */
    var dateFormat: String
        get() = prefs.getString("dateFormat", "ymd") ?: "ymd"
        set(v) = prefs.edit().putString("dateFormat", v).apply()

    /** Default dashboard period: month, last30, all */
    var defaultPeriod: String
        get() = prefs.getString("defaultPeriod", "month") ?: "month"
        set(v) = prefs.edit().putString("defaultPeriod", v).apply()

    companion object {
        val CURRENCIES = listOf("$", "€", "£", "¥", "Rp", "₹", "₩", "R$", "RM", "฿")
        val SEPARATORS = listOf("comma" to "1,234.56", "dot" to "1.234,56", "space" to "1 234.56", "none" to "1234.56")
        val DATE_FORMATS = listOf("ymd" to "2026-10-13", "dmy" to "13/10/2026", "mdy" to "10/13/2026")
    }
}
