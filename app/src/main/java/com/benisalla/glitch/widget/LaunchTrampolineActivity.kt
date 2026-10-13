package com.benisalla.glitch.widget

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.benisalla.glitch.R
import com.benisalla.glitch.apps.IntentAppLauncher
import com.benisalla.glitch.model.AppEntry
import com.benisalla.glitch.model.LaunchResult

/**
 * Invisible step between a widget tap and the app: it opens the tapped app, asks the widget to
 * reshuffle, and finishes before it is ever drawn (Theme.NoDisplay, no animation).
 *
 * Android 12+ allows this: the widget tap starts this activity through a PendingIntent sent by the
 * launcher the user is touching, and an activity that is being started in the foreground may start
 * another one. (The Android 12 trampoline ban covers notifications, and services or receivers.)
 * Not exported; the only way in is the widget's PendingIntent.
 */
class LaunchTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = entryFrom(intent)
        if (entry != null) {
            when (IntentAppLauncher(this).launch(entry)) {
                LaunchResult.Launched -> ShuffleWidget.requestReshuffle(this)
                LaunchResult.Unavailable -> {
                    toast(getString(R.string.menu_app_unavailable, entry.label))
                    ShuffleWidget.reload(this)
                }
                is LaunchResult.Failed -> toast(getString(R.string.menu_app_launch_failed, entry.label))
            }
        }
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun toast(text: String) = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()

    companion object {
        private const val EXTRA_PACKAGE = "com.benisalla.glitch.extra.PACKAGE"
        private const val EXTRA_ACTIVITY = "com.benisalla.glitch.extra.ACTIVITY"
        private const val EXTRA_LABEL = "com.benisalla.glitch.extra.LABEL"

        /** The per-item part of the widget click; merged into the grid's PendingIntent template. */
        fun fillInIntent(entry: AppEntry): Intent = Intent()
            .putExtra(EXTRA_PACKAGE, entry.packageName)
            .putExtra(EXTRA_ACTIVITY, entry.activityName)
            .putExtra(EXTRA_LABEL, entry.label)

        /**
         * The app to open, or null. [IntentAppLauncher] only starts exported MAIN/LAUNCHER
         * activities, so a crafted intent cannot reach anything else.
         */
        fun entryFrom(intent: Intent?): AppEntry? {
            val pkg = intent?.getStringExtra(EXTRA_PACKAGE)?.takeIf { it.isNotBlank() } ?: return null
            val activity = intent.getStringExtra(EXTRA_ACTIVITY)?.takeIf { it.isNotBlank() } ?: return null
            return AppEntry(pkg, activity, intent.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() } ?: pkg)
        }
    }
}
