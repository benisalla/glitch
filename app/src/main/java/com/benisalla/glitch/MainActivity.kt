package com.benisalla.glitch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.benisalla.glitch.apps.IntentAppLauncher
import com.benisalla.glitch.apps.PackageManagerAppCatalog
import com.benisalla.glitch.apps.appInfoIntent
import com.benisalla.glitch.apps.resolveDefaultDockApps
import com.benisalla.glitch.menu.GlitchTheme
import com.benisalla.glitch.menu.MenuScreen
import com.benisalla.glitch.menu.MenuViewModel
import com.benisalla.glitch.menu.SharedPreferencesMenuStore
import com.benisalla.glitch.widget.ShuffleWidget

/**
 * Glitch's shuffled app menu: a normal app opened from the user's own home screen. It is not a
 * launcher and never asks to be one. Wiring only: the rules live in the menu package.
 */
class MainActivity : ComponentActivity() {

    private val catalog by lazy { PackageManagerAppCatalog(applicationContext) }

    private val viewModel: MenuViewModel by viewModels {
        MenuViewModel.factory(
            catalog = catalog,
            launcher = IntentAppLauncher(applicationContext),
            store = SharedPreferencesMenuStore(applicationContext),
            defaultDockApps = { resolveDefaultDockApps(applicationContext) },
            onNewSession = { ShuffleWidget.requestReshuffle(applicationContext) },
        )
    }

    /** True from onStop until the next onResume: the menu was hidden and onStart began a new session. */
    private var stoppedSinceResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlitchTheme {
                MenuScreen(
                    viewModel = viewModel,
                    loadIcon = catalog::loadIcon,
                    onAppInfo = { startSafely(appInfoIntent(it)) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A new menu session starts each time the menu becomes visible again,
        // except after a configuration change (rotation), which keeps the order.
        viewModel.onMenuStarted()
    }

    override fun onResume() {
        super.onResume()
        stoppedSinceResume = false
    }

    override fun onStop() {
        stoppedSinceResume = true
        viewModel.onMenuStopped(changingConfigurations = isChangingConfigurations)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Launched again while another app covered Glitch, before onStop arrived (it can lag on a
        // busy device; singleTask delivers the launch here). If onStop did arrive, onStart already
        // began the new session.
        if (!hasWindowFocus() && !stoppedSinceResume) viewModel.onMenuReopenedBeforeStop()
    }

    /** Some OEM builds lack the App info screen; fall back to the main Settings app. */
    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity for ${intent.action}", e)
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                // Nothing more to try.
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
