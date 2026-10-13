package com.benisalla.glitch

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.benisalla.glitch.apps.IntentAppLauncher
import com.benisalla.glitch.apps.PackageManagerAppCatalog
import com.benisalla.glitch.apps.appInfoIntent
import com.benisalla.glitch.apps.homeSettingsIntent
import com.benisalla.glitch.apps.isDefaultHome
import com.benisalla.glitch.apps.isHomeIntent
import com.benisalla.glitch.apps.requestHomeRoleIntent
import com.benisalla.glitch.apps.resolveDefaultDockApps
import com.benisalla.glitch.menu.GlitchTheme
import com.benisalla.glitch.menu.MenuScreen
import com.benisalla.glitch.menu.MenuViewModel
import com.benisalla.glitch.menu.SharedPreferencesMenuStore

/**
 * Glitch's shuffled app menu. It works as a normal app opened from its icon, and the user may also
 * make it the default home screen (see the HOME intent filter in the manifest). Wiring only: the
 * rules live in the menu package.
 */
class MainActivity : ComponentActivity() {

    private val catalog by lazy { PackageManagerAppCatalog(applicationContext) }

    private val viewModel: MenuViewModel by viewModels {
        MenuViewModel.factory(
            catalog = catalog,
            launcher = IntentAppLauncher(applicationContext),
            store = SharedPreferencesMenuStore(applicationContext),
            defaultDockApps = { resolveDefaultDockApps(applicationContext) },
        )
    }

    /** Bumped when Home is pressed while already on Glitch's home screen; the grid scrolls to the top. */
    private var scrollToTopSignal by mutableIntStateOf(0)

    /** As the home screen there is nothing to go back to, so Back does nothing. */
    private val swallowBackOnHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    // The result itself does not matter: onResume re-checks the default home either way.
    private val requestHomeRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateDefaultHome() }

    /**
     * Screen off is when the next home-screen order is drawn. ACTION_SCREEN_OFF is only delivered to
     * receivers registered at runtime; it is a protected system broadcast and needs no permission.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) viewModel.onScreenOff()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, swallowBackOnHome)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setContent {
            GlitchTheme {
                MenuScreen(
                    viewModel = viewModel,
                    loadIcon = catalog::loadIcon,
                    scrollToTopSignal = scrollToTopSignal,
                    onAppInfo = { startSafely(appInfoIntent(it)) },
                    onMakeHome = ::requestToBeHome,
                    onChangeHome = { startSafely(homeSettingsIntent()) },
                )
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        // Before onMenuStarted: as the home screen, returning from an app must not reshuffle.
        updateDefaultHome()
        viewModel.onMenuStarted()
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed the default home in a dialog or in Settings.
        updateDefaultHome()
    }

    override fun onStop() {
        viewModel.onMenuStopped(changingConfigurations = isChangingConfigurations)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Same check as AOSP Launcher3: focused and not brought to front means Home was pressed
        // while the user was already looking at the home screen (not returning from an app).
        val alreadyOnHome = hasWindowFocus() &&
            (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0
        if (isHomeIntent(intent) && alreadyOnHome) scrollToTopSignal++
    }

    private fun updateDefaultHome() {
        val isHome = isDefaultHome(this)
        swallowBackOnHome.isEnabled = isHome
        viewModel.setDefaultHome(isHome)
    }

    private fun requestToBeHome() {
        val roleIntent = requestHomeRoleIntent(this)
        if (roleIntent != null) {
            try {
                requestHomeRole.launch(roleIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Home role request unavailable", e)
            }
        }
        startSafely(homeSettingsIntent())
    }

    /** Some OEM builds lack a settings screen; fall back to the main Settings app. */
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
