package com.benisalla.glitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.benisalla.glitch.apps.IntentAppLauncher
import com.benisalla.glitch.apps.PackageManagerAppCatalog
import com.benisalla.glitch.menu.MenuScreen
import com.benisalla.glitch.menu.MenuViewModel
import com.benisalla.glitch.menu.GlitchTheme

/**
 * Glitch's first feature: a standalone shuffled app list. It is opened from the user's existing home
 * screen like any other app; it is not a launcher and never asks to be one.
 */
class MainActivity : ComponentActivity() {

    private val catalog by lazy { PackageManagerAppCatalog(applicationContext) }

    private val viewModel: MenuViewModel by viewModels {
        MenuViewModel.factory(catalog, IntentAppLauncher(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlitchTheme {
                MenuScreen(viewModel = viewModel, loadIcon = catalog::loadIcon)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A new menu session starts each time the menu becomes visible again,
        // except after a configuration change (rotation), which keeps the order.
        viewModel.onMenuStarted()
    }

    override fun onStop() {
        viewModel.onMenuStopped(changingConfigurations = isChangingConfigurations)
        super.onStop()
    }
}
