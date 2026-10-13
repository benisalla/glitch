package com.benisalla.glitch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.benisalla.glitch.R

/**
 * The "Shuffled apps" home-screen widget: a grid of the launchable apps in a shuffled order, shown
 * by the phone's own launcher (e.g. One UI Home). Glitch stays a normal app; this is not a home
 * screen. Built with classic RemoteViews and a [ShuffleWidgetService]-backed GridView.
 */
object ShuffleWidget {

    /** Shuffle again on the next reload, then reload every placed widget. */
    fun requestReshuffle(context: Context) {
        SharedPreferencesWidgetOrderStore(context).requestReshuffle()
        reload(context)
    }

    /** Reload every placed widget without reshuffling (reconcile installed and removed apps). */
    fun reload(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, ShuffleWidgetProvider::class.java))
        if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
    }

    /** The widget frame: header, grid (filled by [ShuffleWidgetService]) and the empty text. */
    fun views(context: Context, appWidgetId: Int): RemoteViews {
        val adapter = Intent(context, ShuffleWidgetService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // A distinct data URI per widget, so each instance gets its own adapter.
        adapter.data = adapter.toUri(Intent.URI_INTENT_SCHEME).toUri()

        // Mutable so the grid's fill-in intent can add the tapped app. The intent is explicit
        // (our own trampoline), which Android 12+ requires for mutable PendingIntents.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val template = PendingIntent.getActivity(
            context,
            0,
            Intent(context, LaunchTrampolineActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )

        return RemoteViews(context.packageName, R.layout.widget_shuffle).apply {
            // RemoteCollectionItems (API 31+) would send every icon bitmap in one binder
            // transaction, which overflows with a full app list. The service loads them lazily.
            @Suppress("DEPRECATION")
            setRemoteAdapter(R.id.widget_grid, adapter)
            setEmptyView(R.id.widget_grid, R.id.widget_empty)
            setPendingIntentTemplate(R.id.widget_grid, template)
        }
    }
}

/**
 * Receives the system widget updates. [onUpdate] runs when a widget is added and on every periodic
 * update (`updatePeriodMillis`, 30 minutes at most often), and it reshuffles. A fully removed app
 * reloads the widget without reshuffling.
 */
class ShuffleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        SharedPreferencesWidgetOrderStore(context).requestReshuffle()
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, ShuffleWidget.views(context, it)) }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // PACKAGE_FULLY_REMOVED is one of the few package broadcasts still delivered to manifest
        // receivers. Installs are not; new apps show up on the next reload (see AGENTS.md).
        if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            ShuffleWidget.reload(context)
            return
        }
        super.onReceive(context, intent)
    }
}
