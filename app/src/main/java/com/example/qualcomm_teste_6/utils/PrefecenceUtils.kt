package com.example.qualcomm_teste_6.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PreferencesUtils {
    private const val PREFS_NAME = "LocationPrefs"
    private const val KEY_LAST_LOCATION = "lastLocationResult"
    private const val KEY_LAST_UPDATE = "lastUpdateTime"

    fun saveLocationResult(context: Context, result: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_LOCATION, result)
            .apply()
    }

    fun saveLastUpdateTime(context: Context) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_UPDATE, currentTime)
            .apply()
    }

    fun getLastLocationResult(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_LOCATION, "Nenhum resultado ainda.") ?: "Nenhum resultado ainda."
    }

    fun getLastUpdateTime(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_UPDATE, "Nenhuma busca realizada.") ?: "Nenhuma busca realizada."
    }
}