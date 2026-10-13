package com.benisalla.glitch.widget

import android.app.Instrumentation
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.benisalla.glitch.apps.PackageManagerAppCatalog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val provider = ComponentName(context, ShuffleWidgetProvider::class.java)

    private fun clearPrefs() = context.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().clear().commit()

    @Before
    fun setUp() {
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    private fun providerInfo(): AppWidgetProviderInfo? =
        AppWidgetManager.getInstance(context).getInstalledProvidersForPackage(context.packageName, null)
            .firstOrNull { it.provider == provider }

    @Test
    fun appWidgetProviderIsDeclared() {
        val info = providerInfo()
        assertNotNull("ShuffleWidgetProvider is not an installed widget provider", info)
        info!!
        assertEquals(AppWidgetProviderInfo.RESIZE_BOTH, info.resizeMode and AppWidgetProviderInfo.RESIZE_BOTH)
        assertTrue(info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0)
        assertTrue("updatePeriodMillis >= 30 min", info.updatePeriodMillis >= 30 * 60 * 1000)
        assertTrue(info.minResizeWidth > 0 && info.minResizeHeight > 0)
        assertTrue(info.initialLayout != 0)
        assertTrue(info.previewImage != 0)
        assertNull("no configuration activity", info.configure)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(4, info.targetCellWidth)
            assertEquals(2, info.targetCellHeight)
            assertTrue(info.previewLayout != 0)
            assertNotNull(info.loadDescription(context))
        }
    }

    @Test
    fun gridServiceIsOnlyBindableByWidgetHosts() {
        @Suppress("DEPRECATION")
        val service = context.packageManager.resolveService(Intent(context, ShuffleWidgetService::class.java), 0)
        assertNotNull(service)
        assertEquals("android.permission.BIND_REMOTEVIEWS", service!!.serviceInfo.permission)
        assertFalse(service.serviceInfo.exported)
    }

    @Test
    fun trampolineResolvesAndIsNotExported() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.resolveActivity(Intent(context, LaunchTrampolineActivity::class.java), 0)
        assertNotNull(info)
        assertFalse(info!!.activityInfo.exported)
        assertEquals("", info.activityInfo.taskAffinity.orEmpty())
    }

    @Test
    fun trampolineLaunchesGivenComponentAndRequestsReshuffle() = runBlocking {
        val target = PackageManagerAppCatalog(context).loadApps().first()
        // Blocking monitor: the start is recorded but the target app does not actually open.
        val monitor = Instrumentation.ActivityMonitor(target.activityName, null, true)
        instrumentation.addMonitor(monitor)
        val store = SharedPreferencesWidgetOrderStore(context)
        val requestedBefore = store.requested()
        try {
            val intent = Intent(context, LaunchTrampolineActivity::class.java)
                .putExtras(LaunchTrampolineActivity.fillInIntent(target))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (monitor.hits == 0 && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
            assertEquals("${target.key} was not started", 1, monitor.hits)
            instrumentation.waitForIdleSync()
            assertEquals(requestedBefore + 1, store.requested())
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun trampolineIgnoresIntentWithoutApp() {
        assertNull(LaunchTrampolineActivity.entryFrom(Intent(context, LaunchTrampolineActivity::class.java)))
        val entry = LaunchTrampolineActivity.entryFrom(
            LaunchTrampolineActivity.fillInIntent(com.benisalla.glitch.model.AppEntry("a.b", "a.b.Main", "AB")),
        )
        assertEquals("a.b/a.b.Main", entry?.key)
    }

    @Test
    fun orderStoreRoundTripsOrderAndRequests() {
        val store = SharedPreferencesWidgetOrderStore(context)
        assertNull("never shown", store.load())
        store.requestReshuffle()
        store.save(listOf("b/b.Main", "a/a.Main"), handled = 1)
        val loaded = SharedPreferencesWidgetOrderStore(context).load()
        assertEquals(listOf("b/b.Main", "a/a.Main"), loaded?.keys)
        assertFalse(loaded!!.reshufflePending)
        store.requestReshuffle()
        assertTrue(store.load()!!.reshufflePending)
    }

    @Test
    fun noPermissionsRequested() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val dangerousOrNormal = info.requestedPermissions.orEmpty().filterNot {
            // AndroidX core's signature permission guarding its own not-exported receivers.
            it == "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        }
        assertEquals(emptyList<String>(), dangerousOrNormal)
    }
}
