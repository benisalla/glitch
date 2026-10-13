package com.benisalla.glitch.apps

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.content.pm.PackageManager
import com.benisalla.glitch.MainActivity
import com.benisalla.glitch.menu.Dock
import com.benisalla.glitch.menu.SharedPreferencesMenuStore
import com.benisalla.glitch.model.AppEntry
import com.benisalla.glitch.model.LaunchResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalog = PackageManagerAppCatalog(context)
    private val bogus = AppEntry("com.example.does.not.exist", "com.example.does.not.exist.Main", "Bogus")

    @Test
    fun loadAppsReturnsUniqueEntriesExcludingSelf() = runBlocking {
        val apps = catalog.loadApps()
        assertTrue("expected launchable apps on device", apps.isNotEmpty())
        assertEquals(apps.size, apps.map { it.key }.toSet().size)
        assertTrue(apps.none { it.packageName == context.packageName })
        assertTrue(apps.all { it.label.isNotBlank() })
    }

    @Test
    fun loadAppsIncludesEveryExportedLauncherActivity() = runBlocking {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val expected = context.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo }
            .filter { it.exported && it.packageName != context.packageName }
            .map { "${it.packageName}/${it.name}" }
            .toSet()
        assertEquals(expected, catalog.loadApps().map { it.key }.toSet())
    }

    @Test
    fun everyEntryResolves() = runBlocking {
        val pm = context.packageManager
        catalog.loadApps().forEach { entry ->
            @Suppress("DEPRECATION")
            val resolved = pm.resolveActivity(launchIntentFor(entry), 0)
            assertNotNull("${entry.key} does not resolve", resolved)
        }
    }

    @Test
    fun loadIconForKnownAndBogusEntries() = runBlocking {
        val entry = catalog.loadApps().first()
        assertNotNull(catalog.loadIcon(entry))
        assertNull(catalog.loadIcon(bogus))
    }

    @Test
    fun launchIntentTargetsExactComponent() = runBlocking {
        val entry = catalog.loadApps().first()
        val component = launchIntentFor(entry).component
        assertEquals(entry.packageName, component?.packageName)
        assertEquals(entry.activityName, component?.className)
    }

    @Test
    fun launchingBogusEntryIsUnavailable() {
        assertEquals(LaunchResult.Unavailable, IntentAppLauncher(context).launch(bogus))
    }

    @Test
    fun manifestOffersMainActivityAsOptionalHome() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val handlers = context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
        assertEquals(listOf(MainActivity::class.java.name), handlers.map { it.activityInfo.name })
        assertTrue(isHomeIntent(home))
        assertTrue(!isHomeIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)))
    }

    @Test
    fun defaultDockAppsMapToInstalledLauncherEntries() = runBlocking {
        val apps = catalog.loadApps()
        val slots = resolveDefaultDockApps(context)
        assertEquals("dialer, messaging, camera, browser", 4, slots.size)
        val dock = Dock.defaults(slots, apps)
        val keys = apps.map { it.key }.toSet()
        assertTrue(dock.size <= Dock.MAX_APPS)
        assertEquals(dock.size, dock.toSet().size)
        assertTrue("$dock not all launchable", keys.containsAll(dock))
    }

    @Test
    fun systemScreenIntentsResolve() = runBlocking {
        val pm = context.packageManager
        val entry = catalog.loadApps().first()
        @Suppress("DEPRECATION")
        assertNotNull(pm.resolveActivity(appInfoIntent(entry), 0))
        @Suppress("DEPRECATION")
        assertNotNull(pm.resolveActivity(homeSettingsIntent(), 0))
    }

    @Test
    fun menuStoreRoundTripsDockInOrder() {
        context.getSharedPreferences("menu", Context.MODE_PRIVATE).edit().clear().commit()
        val store = SharedPreferencesMenuStore(context)
        assertNull("never set up", store.loadDock())
        store.saveDock(listOf("b/b.Main", "a/a.Main"))
        assertEquals(listOf("b/b.Main", "a/a.Main"), SharedPreferencesMenuStore(context).loadDock())
        store.saveDock(emptyList())
        assertEquals(emptyList<String>(), store.loadDock())
        assertTrue(!store.isHomeCardDismissed())
        store.dismissHomeCard()
        assertTrue(SharedPreferencesMenuStore(context).isHomeCardDismissed())
        context.getSharedPreferences("menu", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
