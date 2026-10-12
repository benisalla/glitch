package com.benisalla.shufflemenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.benisalla.shufflemenu.apps.IntentAppLauncher
import com.benisalla.shufflemenu.apps.PackageManagerAppCatalog
import com.benisalla.shufflemenu.menu.MenuScreen
import com.benisalla.shufflemenu.menu.MenuViewModel
import com.benisalla.shufflemenu.menu.ShuffleMenuTheme

/**
 * Standalone shuffled app menu. It is opened from the user's existing home
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
            ShuffleMenuTheme {
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
