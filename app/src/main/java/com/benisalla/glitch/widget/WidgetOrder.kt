package com.benisalla.glitch.widget

import android.content.Context
import androidx.core.content.edit
import com.benisalla.glitch.menu.MenuOrder
import com.benisalla.glitch.model.AppEntry
import kotlin.random.Random

/**
 * The widget's persisted order. [requested] counts reshuffle requests and [handled] is the value
 * of [requested] when [keys] were last saved, so a request that arrives while the widget is
 * reloading is not lost: it stays pending until the next reload.
 */
data class SavedWidgetOrder(
    val keys: List<String>,
    val requested: Int,
    val handled: Int,
) {
    val reshufflePending: Boolean get() = requested != handled
}

/** Where the widget keeps its order between process deaths. */
interface WidgetOrderStore {
    /** The saved order, or null if the widget never showed apps. */
    fun load(): SavedWidgetOrder?

    /** Saves [keys] as the current order and marks requests up to [handled] as done. */
    fun save(keys: List<String>, handled: Int)

    /** Asks for a new shuffle on the next reload. */
    fun requestReshuffle()

    /** The current request counter (also when nothing was saved yet). */
    fun requested(): Int
}

/** Pure ordering rules for the widget, unit tested on the JVM. */
object WidgetOrder {

    /**
     * A new shuffle of [latest] when nothing is saved or a reshuffle is pending. Otherwise the saved
     * order reconciled against [latest], with the same rule as the menu: survivors keep their
     * relative order, removed apps drop out, new apps are appended.
     */
    fun resolve(saved: SavedWidgetOrder?, latest: List<AppEntry>, random: Random): List<AppEntry> {
        if (saved == null || saved.reshufflePending) return MenuOrder.shuffled(latest, random)
        val byKey = HashMap<String, AppEntry>(latest.size)
        latest.forEach { byKey.putIfAbsent(it.key, it) }
        return MenuOrder.reconcile(saved.keys.mapNotNull { byKey[it] }, latest)
    }
}

/**
 * Loads, resolves and saves the widget order in one step. Several widget instances may reload at
 * once; the lock makes the first one consume a pending reshuffle and the others reuse its order.
 */
class WidgetOrderState(
    private val store: WidgetOrderStore,
    private val random: Random = Random.Default,
) {
    fun refresh(latest: List<AppEntry>): List<AppEntry> = synchronized(LOCK) {
        val saved = store.load()
        val order = WidgetOrder.resolve(saved, latest, random)
        store.save(order.map { it.key }, handled = saved?.requested ?: store.requested())
        order
    }

    private companion object {
        val LOCK = Any()
    }
}

/** [WidgetOrderStore] in a private SharedPreferences file. It stays on the device (allowBackup is off). */
class SharedPreferencesWidgetOrderStore(context: Context) : WidgetOrderStore {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load(): SavedWidgetOrder? {
        if (!prefs.contains(KEY_ORDER)) return null
        return SavedWidgetOrder(
            keys = prefs.getString(KEY_ORDER, null).orEmpty().split(SEPARATOR).filter { it.isNotEmpty() },
            requested = prefs.getInt(KEY_REQUESTED, 0),
            handled = prefs.getInt(KEY_HANDLED, 0),
        )
    }

    // Keys are "package/activity" and never contain a newline.
    override fun save(keys: List<String>, handled: Int) = prefs.edit {
        putString(KEY_ORDER, keys.joinToString(SEPARATOR))
        putInt(KEY_HANDLED, handled)
    }

    override fun requestReshuffle() = synchronized(LOCK) {
        prefs.edit { putInt(KEY_REQUESTED, prefs.getInt(KEY_REQUESTED, 0) + 1) }
    }

    override fun requested(): Int = prefs.getInt(KEY_REQUESTED, 0)

    private companion object {
        const val FILE = "widget"
        const val KEY_ORDER = "order"
        const val KEY_REQUESTED = "requested"
        const val KEY_HANDLED = "handled"
        const val SEPARATOR = "\n"
        val LOCK = Any()
    }
}
