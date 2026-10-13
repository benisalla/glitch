package com.benisalla.glitch.menu

import android.content.Context
import androidx.core.content.edit

/** Small persistent menu settings: the dock. */
interface MenuStore {
    /** Saved dock keys (`AppEntry.key`) in dock order, or null if the dock was never set up. */
    fun loadDock(): List<String>?

    fun saveDock(keys: List<String>)
}

/** [MenuStore] in a private SharedPreferences file. It stays on the device (allowBackup is off). */
class SharedPreferencesMenuStore(context: Context) : MenuStore {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun loadDock(): List<String>? {
        if (!prefs.contains(KEY_DOCK)) return null
        return prefs.getString(KEY_DOCK, null).orEmpty().split(SEPARATOR).filter { it.isNotEmpty() }
    }

    // Keys are "package/activity" and never contain a newline. A joined string keeps the user's
    // order, which a string set would not.
    override fun saveDock(keys: List<String>) = prefs.edit { putString(KEY_DOCK, keys.joinToString(SEPARATOR)) }

    private companion object {
        const val FILE = "menu"
        const val KEY_DOCK = "dock"
        const val SEPARATOR = "\n"
    }
}
