package com.benisalla.shufflemenu.menu

import android.graphics.drawable.Drawable
import com.benisalla.shufflemenu.model.AppCatalog
import com.benisalla.shufflemenu.model.AppEntry
import com.benisalla.shufflemenu.model.AppLauncher
import com.benisalla.shufflemenu.model.LaunchResult
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        catalog = FakeCatalog(apps)
        launcher = RecordingLauncher()
        random = CountingRandom(seed = 42)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MenuViewModel(catalog, launcher, random)

    private fun MenuViewModel.readyApps(): List<AppEntry> =
        (state.value as? MenuUiState.Ready)?.apps ?: error("expected Ready, was ${state.value}")

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
        val vm = MenuViewModel(catalog, launcher, Random(2024))
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
        assertEquals(MenuUiState.Error("boom"), vm.state.value)

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
        assertEquals("App ${gone.packageName.removePrefix("pkg.")} is no longer available", vm.message.value)
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

        assertEquals("Couldn't open ${target.label}", vm.message.value)
        assertEquals(1, catalog.loadCount)
    }
}
