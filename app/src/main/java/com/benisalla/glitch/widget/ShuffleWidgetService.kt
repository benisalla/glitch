package com.benisalla.glitch.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import com.benisalla.glitch.R
import com.benisalla.glitch.apps.PackageManagerAppCatalog
import com.benisalla.glitch.model.AppEntry
import kotlinx.coroutines.runBlocking

/** Feeds the widget grid. The launcher binds it (BIND_REMOTEVIEWS); it is not exported to apps. */
class ShuffleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = AppGridFactory(applicationContext)
}

private class AppGridFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val catalog = PackageManagerAppCatalog(context)
    private val state = WidgetOrderState(SharedPreferencesWidgetOrderStore(context))
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.widget_icon_size)
    private var apps: List<AppEntry> = emptyList()

    override fun onCreate() = Unit

    // Runs on a binder thread, so blocking here is allowed and expected.
    override fun onDataSetChanged() {
        apps = try {
            state.refresh(runBlocking { catalog.loadApps() })
        } catch (e: Exception) {
            // Keep showing the last order rather than an empty widget.
            Log.w(TAG, "Couldn't load apps for the widget", e)
            apps
        }
    }

    override fun onDestroy() {
        apps = emptyList()
    }

    override fun getCount(): Int = apps.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_app_item)
        val entry = apps.getOrNull(position) ?: return views
        views.setTextViewText(R.id.widget_label, entry.label)
        views.setContentDescription(R.id.widget_item, entry.label)
        val icon = runBlocking { catalog.loadIcon(entry) }
        if (icon != null) {
            try {
                views.setImageViewBitmap(R.id.widget_icon, icon.toBitmap(iconSize, iconSize))
            } catch (e: Exception) {
                Log.w(TAG, "Icon render failed for ${entry.key}", e)
            }
        }
        views.setOnClickFillInIntent(R.id.widget_item, LaunchTrampolineActivity.fillInIntent(entry))
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = apps.getOrNull(position)?.key?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private companion object {
        const val TAG = "ShuffleWidget"
    }
}
