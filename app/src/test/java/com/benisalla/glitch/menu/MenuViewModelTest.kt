package com.benisalla.glitch.menu

import android.graphics.drawable.Drawable
import com.benisalla.glitch.model.AppCatalog
import com.benisalla.glitch.model.AppEntry
import com.benisalla.glitch.model.AppLauncher
import com.benisalla.glitch.model.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private class FakeCatalog(var apps: List<AppEntry>) : AppCatalog {
        var loadCount = 0
        var failure: Exception? = null
        val changeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

        override suspend fun loadApps(): List<AppEntry> {
            loadCount++
            failure?.let { throw it }
            return apps
        }

        override suspend fun loadIcon(entry: AppEntry): Drawable? = null

        override fun changes(): Flow<Unit> = changeEvents
    }

    private class RecordingLauncher(var result: LaunchResult = LaunchResult.Launched) : AppLauncher {
        val launched = mutableListOf<AppEntry>()
        override fun launch(entry: AppEntry): LaunchResult {
            launched += entry
            return result
        }
    }

    private class FakeStore(var dock: List<String>? = null) : MenuStore {
        var homeCardDismissed = false
        val savedDocks = mutableListOf<List<String>>()
        override fun loadDock(): List<String>? = dock
        override fun saveDock(keys: List<String>) {
            dock = keys
            savedDocks += keys
        }
        override fun isHomeCardDismissed(): Boolean = homeCardDismissed
        override fun dismissHomeCard() {
            homeCardDismissed = true
        }
    }

    /**
     * Seeded Random that counts bounded draws. A Fisher-Yates shuffle of n items makes exactly
     * n - 1 of them, so the count tells how many shuffles happened.
     */
    private class CountingRandom(seed: Int) : Random() {
        private val delegate = Random(seed)
        var calls = 0
        override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)
        override fun nextInt(from: Int, until: Int): Int {
            calls++
            return delegate.nextInt(from, until)
        }
    }

    private val apps = (1..6).map { AppEntry("pkg.$it", "pkg.$it.Main", "App $it") }
    private lateinit var catalog: FakeCatalog
    private lateinit var launcher: RecordingLauncher
    private lateinit var random: CountingRandom
    private lateinit var store: FakeStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        catalog = FakeCatalog(apps)
        launcher = RecordingLauncher()
        random = CountingRandom(seed = 42)
        store = FakeStore(dock = emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(defaults: List<List<DefaultApp>> = emptyList()) =
        MenuViewModel(catalog, launcher, store, random, defaultDockApps = { defaults })

    private fun MenuViewModel.readyApps(): List<AppEntry> =
        ready().apps

    private fun MenuViewModel.ready(): MenuUiState.Ready =
        state.value as? MenuUiState.Ready ?: error("expected Ready, was ${state.value}")

    /** Grid plus dock: every launchable app currently shown. */
    private fun MenuViewModel.allShown(): List<AppEntry> = ready().dock + ready().apps

    /** One full shuffle of the catalog costs apps.size - 1 bounded draws. */
    private val drawsPerShuffle get() = apps.size - 1

    /** Glitch is the default home: setDefaultHome comes before onMenuStarted, as in MainActivity. */
    private fun MenuViewModel.startAsHome() {
        setDefaultHome(true)
        onMenuStarted()
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(MenuUiState.Loading, viewModel().state.value)
    }

    @Test
    fun firstStart_isReadyWithPermutationOfCatalog() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val shown = vm.readyApps()
        assertEquals(apps.size, shown.size)
        assertEquals(apps.toSet(), shown.toSet())
        assertEquals(1, catalog.loadCount)
        assertEquals(apps.size - 1, random.calls)
    }

    @Test
    fun repeatedStateReads_doNotChangeOrder() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val first = vm.readyApps()
        repeat(10) { assertEquals(first, vm.readyApps()) }
    }

    @Test
    fun configChangeRestart_keepsOrderWithoutReloadOrShuffle() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val before = vm.readyApps()
        val loads = catalog.loadCount
        val randomCalls = random.calls

        vm.onMenuStopped(changingConfigurations = true)
        vm.onMenuStarted()

        assertEquals(before, vm.readyApps())
        assertEquals(loads, catalog.loadCount)
        assertEquals(randomCalls, random.calls)
    }

    @Test
    fun realStopThenStart_startsNewSessionWithOneShuffle() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()

        vm.onMenuStopped(changingConfigurations = false)
        vm.onMenuStarted()

        assertEquals(2, catalog.loadCount)
        assertEquals(2 * (apps.size - 1), random.calls)
        assertEquals(apps.toSet(), vm.readyApps().toSet())
    }

    @Test
    fun manySessions_produceDistinctOrders() = runTest {
        val vm = MenuViewModel(catalog, launcher, store, Random(2024))
        val seen = HashSet<List<AppEntry>>()
        repeat(30) {
            vm.onMenuStarted()
            seen += vm.readyApps()
            vm.onMenuStopped(changingConfigurations = false)
        }
        // Deterministic for the seed; 6 apps have 720 orders, so 30 sessions repeat very few.
        assertTrue("only ${seen.size} distinct orders", seen.size > 20)
    }

    @Test
    fun catalogChangeWhileStarted_reconcilesWithoutReshuffle() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val before = vm.readyApps()
        val randomCalls = random.calls
        val removed = before[2]
        val added = AppEntry("pkg.new", "pkg.new.Main", "New")
        catalog.apps = apps - removed + added

        catalog.changeEvents.emit(Unit)

        assertEquals(before - removed + added, vm.readyApps())
        assertEquals(randomCalls, random.calls)
        assertEquals(2, catalog.loadCount)
    }

    @Test
    fun catalogChangeWhileStopped_isNotCollected() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val before = vm.readyApps()
        vm.onMenuStopped(changingConfigurations = true)
        catalog.apps = apps.drop(1)

        catalog.changeEvents.emit(Unit)

        assertEquals(0, catalog.changeEvents.subscriptionCount.value)
        assertEquals(1, catalog.loadCount)
        assertEquals(before, vm.readyApps())
    }

    @Test
    fun emptyCatalog_isEmpty() = runTest {
        catalog.apps = emptyList()
        val vm = viewModel()
        vm.onMenuStarted()
        assertEquals(MenuUiState.Empty, vm.state.value)
    }

    @Test
    fun throwingCatalog_isErrorAndRetryRecovers() = runTest {
        catalog.failure = IllegalStateException("boom")
        val vm = viewModel()
        vm.onMenuStarted()
        assertEquals(MenuUiState.Error, vm.state.value)

        catalog.failure = null
        vm.retry()
        assertEquals(apps.toSet(), vm.readyApps().toSet())
    }

    @Test
    fun launch_passesExactEntryToLauncher() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val target = vm.readyApps()[3]
        vm.launch(target)
        assertEquals(listOf(target), launcher.launched)
        assertNull(vm.message.value)
    }

    @Test
    fun launchUnavailable_refreshesAndShowsMessage() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val before = vm.readyApps()
        val gone = before[0]
        catalog.apps = apps - gone
        launcher.result = LaunchResult.Unavailable

        vm.launch(gone)

        assertEquals(before - gone, vm.readyApps())
        assertEquals(MenuMessage.Unavailable(gone.label), vm.message.value)
        vm.consumeMessage()
        assertNull(vm.message.value)
    }

    @Test
    fun launchFailed_showsMessageWithoutRefresh() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val target = vm.readyApps()[1]
        launcher.result = LaunchResult.Failed("ActivityNotFoundException")

        vm.launch(target)

        assertEquals(MenuMessage.LaunchFailed(target.label), vm.message.value)
        assertEquals(1, catalog.loadCount)
    }

    // --- Home screen: reshuffle per unlock (screen off), not per return from an app ---

    @Test
    fun home_returnFromApp_keepsOrderAndDoesNotShuffle() = runTest {
        val vm = viewModel()
        vm.startAsHome()
        val before = vm.readyApps()
        vm.launch(before[0])

        vm.onMenuStopped(changingConfigurations = false)
        vm.startAsHome()

        assertEquals(before, vm.readyApps())
        assertEquals(drawsPerShuffle, random.calls)
        assertEquals("start still reloads to pick up app changes", 2, catalog.loadCount)
    }

    @Test
    fun home_screenOff_shufflesOnceAndNextShowKeepsThatOrder() = runTest {
        val vm = viewModel()
        vm.startAsHome()
        val firstId = vm.ready().shuffleId

        vm.onMenuStopped(changingConfigurations = false)
        vm.onScreenOff()
        val atScreenOff = vm.readyApps()
        assertEquals(2 * drawsPerShuffle, random.calls)
        assertEquals(apps.toSet(), atScreenOff.toSet())
        assertTrue(vm.ready().shuffleId != firstId)

        // Unlock: the order drawn at screen off is the one shown; no further shuffle.
        vm.startAsHome()
        assertEquals(atScreenOff, vm.readyApps())
        assertEquals(2 * drawsPerShuffle, random.calls)
    }

    @Test
    fun home_screenOffWhileVisible_reshuffles() = runTest {
        val vm = viewModel()
        vm.startAsHome()
        vm.onScreenOff()
        assertEquals(2 * drawsPerShuffle, random.calls)
        assertEquals(apps.toSet(), vm.readyApps().toSet())
    }

    @Test
    fun home_screenOffBeforeFirstLoad_isSafeAndShufflesOnce() = runTest {
        val vm = viewModel()
        vm.setDefaultHome(true)
        vm.onScreenOff()
        assertEquals(0, random.calls)
        vm.onMenuStarted()
        assertEquals(drawsPerShuffle, random.calls)
    }

    @Test
    fun home_configChange_keepsOrder() = runTest {
        val vm = viewModel()
        vm.startAsHome()
        val before = vm.readyApps()
        vm.onMenuStopped(changingConfigurations = true)
        vm.startAsHome()
        assertEquals(before, vm.readyApps())
        assertEquals(drawsPerShuffle, random.calls)
        assertEquals(1, catalog.loadCount)
    }

    @Test
    fun home_coldStart_shuffles() = runTest {
        // A new ViewModel is a new process (or a recreated home activity): it draws an order.
        viewModel().startAsHome()
        assertEquals(drawsPerShuffle, random.calls)
    }

    @Test
    fun normalApp_screenOff_doesNotShuffleUntilNextOpen() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        vm.onMenuStopped(changingConfigurations = false)
        vm.onScreenOff()
        assertEquals(drawsPerShuffle, random.calls)
        vm.onMenuStarted()
        assertEquals(2 * drawsPerShuffle, random.calls)
    }

    @Test
    fun home_packageChangeWhileStopped_reconcilesOnReturn() = runTest {
        val vm = viewModel()
        vm.startAsHome()
        val before = vm.readyApps()
        vm.onMenuStopped(changingConfigurations = false)
        val removed = before[1]
        val added = AppEntry("pkg.new", "pkg.new.Main", "New")
        catalog.apps = apps - removed + added

        vm.startAsHome()

        assertEquals(before - removed + added, vm.readyApps())
        assertEquals(drawsPerShuffle, random.calls)
    }

    // --- Dock ---

    @Test
    fun dock_isExcludedFromGridAndStable() = runTest {
        store.dock = listOf(apps[4].key, apps[1].key)
        val vm = viewModel()
        vm.onMenuStarted()
        assertEquals(listOf(apps[4], apps[1]), vm.ready().dock)
        assertTrue(vm.readyApps().none { it == apps[4] || it == apps[1] })
        assertEquals(apps.toSet(), vm.allShown().toSet())
        assertEquals(apps.size, vm.allShown().size)

        // A new session reshuffles the grid only.
        vm.onMenuStopped(changingConfigurations = false)
        vm.onMenuStarted()
        assertEquals(listOf(apps[4], apps[1]), vm.ready().dock)
    }

    @Test
    fun dock_firstRunUsesDefaultsAndSavesThem() = runTest {
        store.dock = null
        val vm = viewModel(defaults = listOf(listOf(DefaultApp("pkg.3")), listOf(DefaultApp("pkg.missing")), listOf(DefaultApp("pkg.5", "pkg.5.Main"))))
        vm.onMenuStarted()
        assertEquals(listOf(apps[2], apps[4]), vm.ready().dock)
        assertEquals(listOf(apps[2].key, apps[4].key), store.dock)
    }

    @Test
    fun dock_savedEmptyDockIsNotReplacedByDefaults() = runTest {
        store.dock = emptyList()
        val vm = viewModel(defaults = listOf(listOf(DefaultApp("pkg.3"))))
        vm.onMenuStarted()
        assertEquals(emptyList<AppEntry>(), vm.ready().dock)
    }

    @Test
    fun dock_addAndRemove_persistAndDoNotReshuffle() = runTest {
        val vm = viewModel()
        vm.onMenuStarted()
        val grid = vm.readyApps()
        val target = grid[2]

        vm.addToDock(target)
        assertEquals(listOf(target), vm.ready().dock)
        assertEquals(grid - target, vm.readyApps())
        assertEquals(listOf(target.key), store.dock)

        vm.removeFromDock(target)
        assertEquals(emptyList<AppEntry>(), vm.ready().dock)
        assertEquals("back at its place in the current order", grid, vm.readyApps())
        assertEquals(emptyList<String>(), store.dock)
        assertEquals(drawsPerShuffle, random.calls)
    }

    @Test
    fun dock_full_saysSoAndKeepsDock() = runTest {
        store.dock = apps.take(4).map { it.key }
        val vm = viewModel()
        vm.onMenuStarted()
        vm.addToDock(apps[5])
        assertEquals(MenuMessage.DockFull, vm.message.value)
        assertEquals(apps.take(4), vm.ready().dock)
        assertEquals(apps.take(4).map { it.key }, store.dock)
    }

    @Test
    fun dock_uninstalledAppDisappearsAndFreesItsPlace() = runTest {
        store.dock = apps.take(4).map { it.key }
        val vm = viewModel()
        vm.onMenuStarted()
        catalog.apps = apps - apps[1]
        catalog.changeEvents.emit(Unit)
        assertEquals(listOf(apps[0], apps[2], apps[3]), vm.ready().dock)

        vm.addToDock(apps[5])
        assertEquals(listOf(apps[0], apps[2], apps[3], apps[5]), vm.ready().dock)
        assertNull(vm.message.value)
    }

    @Test
    fun homeCard_dismissIsPersisted() {
        val vm = viewModel()
        assertEquals(false, vm.homeCardDismissed.value)
        vm.dismissHomeCard()
        assertEquals(true, vm.homeCardDismissed.value)
        assertEquals(true, viewModel().homeCardDismissed.value)
    }
}
