# TeleShield Android Shell — Sub-Milestone 2 (Complete the UI) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the app's UI: a bottom-nav shell exposing Rules, Simulator, Audit Log, and Settings screens, plus rule editing. All driven by the existing `:engine` use cases; no new persistence.

**Architecture:** A top-level `Scaffold` in `TeleShieldNavHost` adds a Material3 `NavigationBar` (bottom) with 4 routes; each screen keeps its own inner `Scaffold` (top bar + FAB) inside the NavHost's padded content. New screens (`SimulatorScreen`, `AuditLogScreen`, `SettingsScreen`) each get a `@HiltViewModel` that injects the existing use cases + the `@IoDispatcher` coroutine dispatcher (established pattern), verified with JVM tests backed by the engine's in-memory adapters.

**Tech Stack:** unchanged — Compose (BOM 2024.12.01) + Material3, Hilt 2.52, `@IoDispatcher` bind, `kotlinx-coroutines-test`, Robolectric (unused here — all new tests are plain JVM via the engine's `InMemory*` adapters).

## Global Constraints

- `:engine` stays framework-free. The only engine change is a new optional `id` field on `AddRuleUseCase.AddRuleRequest`.
- Every new `ViewModels`-injected coroutine dispatcher uses `@IoDispatcher` `CoroutineDispatcher` (the E1 pattern); offload the synchronous (runBlocking-backed) use cases to it.
- ViewModel tests use the engine's in-memory adapters (`InMemoryScreeningRuleRepository`, `InMemoryBlockedCallRecordRepository`, `InMemorySystemConfigurationRepository`) to back the REAL use cases — no mocking. Share one `StandardTestDispatcher` across `setMain`, `runTest`, and the ViewModel's `ioDispatcher`.
- `minSdk 24`, `targetSdk 35`, `compileSdk 35`, JDK 17. TDD red→green, Conventional Commits, ≤3 files per commit.
- Windows: use `.\gradlew.bat`.
- Manual device smoke (no emulator attached) is deferred to the user in each screen task.

---

## File Structure

```
engine/src/main/kotlin/com/teleshield/application/AddRuleUseCase.kt       (add optional id)
engine/src/test/kotlin/com/teleshield/application/AddRuleUseCaseTest.kt   (id-preserving test)

app/src/main/java/com/teleshield/app/ui/navigation/TeleShieldNavHost.kt   (bottom nav + 4 routes)
app/src/main/java/com/teleshield/app/ui/simulator/SimulatorViewModel.kt   (new)
app/src/main/java/com/teleshield/app/ui/simulator/SimulatorScreen.kt      (new)
app/src/main/java/com/teleshield/app/ui/audit/AuditLogViewModel.kt        (new)
app/src/main/java/com/teleshield/app/ui/audit/AuditLogScreen.kt           (new)
app/src/main/java/com/teleshield/app/ui/settings/SettingsViewModel.kt     (new)
app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt        (new)
app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt            (edit mode)
app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt              (row edit)
```
Tests per task.

---

## Part A — engine

### Task A1: Add optional `id` to `AddRuleUseCase.AddRuleRequest`

**Files:**
- Modify: `engine/src/main/kotlin/com/teleshield/application/AddRuleUseCase.kt`
- Modify: `engine/src/test/kotlin/com/teleshield/application/AddRuleUseCaseTest.kt`

**Interfaces:**
- Produces: `AddRuleRequest(..., id: String? = null)`; `execute()` uses `request.id ?: idGenerator()` (so a provided id = upsert-edit via the repository's REPLACE save).

- [ ] **Step 1: Write the failing test** — add to `AddRuleUseCaseTest.kt`:

```kotlin
    @Test
    fun `uses a provided id instead of generating one`() {
        val repository = FakeRuleRepository()
        val useCase = AddRuleUseCase(ruleRepository = repository, idGenerator = { "gen-id" })

        val saved = useCase.execute(
            AddRuleUseCase.AddRuleRequest(
                patternExpression = "1555*",
                ruleType = RuleType.WILDCARD,
                label = "Exchange block",
                isWhitelist = false,
                id = "existing-id",
            ),
        )

        assertEquals("existing-id", saved.id)
    }
```

- [ ] **Step 2: Run to confirm red** — `.\gradlew.bat :engine:test --tests "*AddRuleUseCaseTest*"`. Expected: compile error `no value passed for parameter 'id'` (the request has no `id` param yet).

- [ ] **Step 3: Implement** — `AddRuleUseCase.kt`, change the `AddRuleRequest` data class:

```kotlin
    data class AddRuleRequest(
        val patternExpression: String,
        val ruleType: RuleType,
        val label: String,
        val isWhitelist: Boolean,
        val isEnabled: Boolean = true,
        val id: String? = null,
    )
```

and the `execute` body:

```kotlin
        val rule = ScreeningRule(
            id = request.id ?: idGenerator(),
            pattern = PatternExpression(request.patternExpression),
            label = request.label,
            ruleType = request.ruleType,
            isWhitelist = request.isWhitelist,
            isEnabled = request.isEnabled,
        )
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :engine:test`.

- [ ] **Step 5: Commit**

```bash
git add engine/src/main/kotlin/com/teleshield/application/AddRuleUseCase.kt engine/src/test/kotlin/com/teleshield/application/AddRuleUseCaseTest.kt
git commit -m "feat: support rule edit via optional id"
```

---

## Part B — navigation shell

### Task B1: Bottom navigation with 4 destinations

**Files:**
- Modify: `app/src/main/java/com/teleshield/app/ui/navigation/TeleShieldNavHost.kt`

**Interfaces:**
- Produces: `TeleShieldNavHost()` — a `Scaffold` with a `NavigationBar` (Rules, Simulator, Audit Log, Settings) wrapping a `NavHost` (padded) with routes `"rules"`, `"simulator"`, `"audit"`, `"settings"`. Screens keep their own inner `Scaffold`.
- Requires (from later tasks): `SimulatorScreen()`, `AuditLogScreen()`, `SettingsScreen()`.

- [ ] **Step 1: Replace `TeleShieldNavHost.kt`** with:

```kotlin
package com.teleshield.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.teleshield.app.ui.audit.AuditLogScreen
import com.teleshield.app.ui.rules.RulesScreen
import com.teleshield.app.ui.settings.SettingsScreen
import com.teleshield.app.ui.simulator.SimulatorScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("rules", "Rules", Icons.AutoMirrored.Filled.List),
    Destination("simulator", "Simulator", Icons.Default.Phone),
    Destination("audit", "Audit Log", Icons.Default.Info),
    Destination("settings", "Settings", Icons.Default.Settings),
)

@Composable
fun TeleShieldNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "rules",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("rules") { RulesScreen() }
            composable("simulator") { SimulatorScreen() }
            composable("audit") { AuditLogScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 2: Verify compile** (the new screens don't exist yet, so this is a placeholder gate) — compile will fail until Tasks C2/D2/E2 create `SimulatorScreen`/`AuditLogScreen`/`SettingsScreen`. Do NOT commit yet; proceed to create the screens first, OR create empty stubs and finish this task after the screens land. The controller will sequence: create the three screens (Tasks C2/D2/E2) first, then commit B1 together with them. Mark B1 `DONE` and commit after C2/D2/E2 exist.

---

## Part C — Simulator

### Task C1: `SimulatorViewModel` + test

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/simulator/SimulatorViewModel.kt`
- Create: `app/src/test/java/com/teleshield/app/ui/simulator/SimulatorViewModelTest.kt`

**Interfaces:**
- Consumes: `SimulateCallUseCase.simulate(callerId): SimulationResult`.
- Produces: `SimulatorViewModel.uiState: StateFlow<SimulatorUiState>`, `SimulatorUiState(result: SimulationResult? = null)`, `simulate(callerId: String)`.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleshield.application.SimulateCallUseCase
import com.teleshield.application.SimulationResult
import com.teleshield.app.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SimulatorViewModel @Inject constructor(
    private val simulateCall: SimulateCallUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    data class SimulatorUiState(val result: SimulationResult? = null)

    private val _uiState = MutableStateFlow(SimulatorUiState())
    val uiState: StateFlow<SimulatorUiState> = _uiState.asStateFlow()

    fun simulate(callerId: String) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { simulateCall.simulate(callerId) }
            _uiState.value = SimulatorUiState(result)
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.teleshield.app.ui.simulator

import com.teleshield.application.SimulateCallUseCase
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.inmemory.InMemoryScreeningRuleRepository
import com.teleshield.inmemory.InMemorySystemConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorViewModelTest {

    private val normalizer = IdentifierNormalizer()
    private val engine = ScreeningEngine(normalizer)
    private val rulesRepo = InMemoryScreeningRuleRepository()
    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val simulate = SimulateCallUseCase(engine, normalizer, rulesRepo, configRepo)
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `simulating a blocked number returns a Blocked verdict`() = runTest(testDispatcher) {
        rulesRepo.save(rule("r1", "15551234567"))
        val vm = SimulatorViewModel(simulate, ioDispatcher = testDispatcher)

        vm.simulate("15551234567")
        advanceUntilIdle()

        val result = vm.uiState.value.result!!
        assertTrue(result.verdict is ScreeningVerdict.Blocked)
        assertTrue(result.executionDurationMs >= 0)
    }

    @Test
    fun `simulating an allowed number returns Allowed`() = runTest(testDispatcher) {
        rulesRepo.save(rule("r1", "15551234567"))
        val vm = SimulatorViewModel(simulate, ioDispatcher = testDispatcher)

        vm.simulate("9999999999")
        advanceUntilIdle()

        assertEquals(ScreeningVerdict.Allowed("No matching rules"), vm.uiState.value.result!!.verdict)
    }

    private fun rule(id: String, expression: String) = ScreeningRule(
        id = id,
        pattern = PatternExpression(expression),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.ui.simulator.SimulatorViewModelTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/simulator/SimulatorViewModel.kt app/src/test/java/com/teleshield/app/ui/simulator/SimulatorViewModelTest.kt
git commit -m "feat: add simulator view model"
```

### Task C2: `SimulatorScreen`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/simulator/SimulatorScreen.kt`

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.simulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.ScreeningVerdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var callerId by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Simulator") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = callerId,
                    onValueChange = { callerId = it },
                    label = { Text("Caller ID") },
                    enabled = !isPrivate,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("Private")
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }

            Button(
                onClick = { viewModel.simulate(if (isPrivate) "Private" else callerId) },
                enabled = isPrivate || callerId.isNotBlank(),
            ) { Text("Simulate") }

            val result = uiState.result
            if (result != null) {
                Text(verdictText(result.verdict), style = MaterialTheme.typography.bodyLarge)
                Text("Duration: ${result.executionDurationMs} ms")
            }
        }
    }
}

private fun verdictText(verdict: ScreeningVerdict): String = when (verdict) {
    is ScreeningVerdict.Blocked ->
        "BLOCKED: ${verdict.matchedRule.label.ifBlank { verdict.matchedRule.pattern.expression }}"
    is ScreeningVerdict.Whitelisted -> "ALLOWED (whitelisted)"
    is ScreeningVerdict.Allowed -> "ALLOWED: ${verdict.reason}"
}
```

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`. (This also lets Task B1's NavHost route resolve.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/simulator/SimulatorScreen.kt
git commit -m "feat: add simulator screen"
```

---

## Part D — Audit Log

### Task D1: `AuditLogViewModel` + test

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/audit/AuditLogViewModel.kt`
- Create: `app/src/test/java/com/teleshield/app/ui/audit/AuditLogViewModelTest.kt`

**Interfaces:**
- Consumes: `QueryBlockedLogsUseCase.execute(limit, offset): List<BlockedCallRecord>`, `PurgeAuditLogsUseCase.purge(): Int`.
- Produces: `AuditLogViewModel.uiState: StateFlow<AuditLogUiState>`, `AuditLogUiState(records)`, `refresh()`, `purge()`.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleshield.application.PurgeAuditLogsUseCase
import com.teleshield.application.QueryBlockedLogsUseCase
import com.teleshield.app.di.IoDispatcher
import com.teleshield.domain.BlockedCallRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val queryLogs: QueryBlockedLogsUseCase,
    private val purgeLogs: PurgeAuditLogsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    data class AuditLogUiState(val records: List<BlockedCallRecord> = emptyList())

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val records = withContext(ioDispatcher) { queryLogs.execute(limit = 100, offset = 0) }
            _uiState.value = AuditLogUiState(records)
        }
    }

    fun purge() {
        viewModelScope.launch {
            withContext(ioDispatcher) { purgeLogs.purge() }
            val records = withContext(ioDispatcher) { queryLogs.execute(limit = 100, offset = 0) }
            _uiState.value = AuditLogUiState(records)
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.teleshield.app.ui.audit

import com.teleshield.application.PurgeAuditLogsUseCase
import com.teleshield.application.QueryBlockedLogsUseCase
import com.teleshield.domain.BlockedCallRecord
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.inmemory.InMemoryBlockedCallRecordRepository
import com.teleshield.inmemory.InMemorySystemConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogViewModelTest {

    private val logRepo = InMemoryBlockedCallRecordRepository()
    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val query = QueryBlockedLogsUseCase(logRepo)
    private val purge = PurgeAuditLogsUseCase(configRepo, logRepo)
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `loads existing records on init`() = runTest(testDispatcher) {
        logRepo.save(record("1", 100L))
        val vm = AuditLogViewModel(query, purge, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals(listOf("1"), vm.uiState.value.records.map { it.id })
    }

    @Test
    fun `purge clears the records and refreshes`() = runTest(testDispatcher) {
        logRepo.save(record("1", 100L))
        val vm = AuditLogViewModel(query, purge, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.purge()
        advanceUntilIdle()

        assertEquals(emptyList(), vm.uiState.value.records)
    }

    private fun record(id: String, timestamp: Long) = BlockedCallRecord(
        id = id,
        callerIdentifier = "15551234567",
        timestamp = timestamp,
        matchedRuleId = "r1",
        matchedPatternSnapshot = "15551234567",
        matchedLabelSnapshot = "label",
    )
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.ui.audit.AuditLogViewModelTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/audit/AuditLogViewModel.kt app/src/test/java/com/teleshield/app/ui/audit/AuditLogViewModelTest.kt
git commit -m "feat: add audit log view model"
```

### Task D2: `AuditLogScreen`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/audit/AuditLogScreen.kt`

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.audit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.BlockedCallRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(viewModel: AuditLogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Log") },
                actions = {
                    IconButton(onClick = { viewModel.purge() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Purge")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(uiState.records, key = { it.id }) { record ->
                LogRow(record)
            }
        }
    }
}

@Composable
private fun LogRow(record: BlockedCallRecord) {
    ListItem(
        headlineContent = { Text(record.callerIdentifier) },
        supportingContent = { Text("${record.matchedLabelSnapshot} · ${record.timestamp}") },
    )
}
```

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/audit/AuditLogScreen.kt
git commit -m "feat: add audit log screen"
```

---

## Part E — Settings

### Task E1: `SettingsViewModel` + test

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/settings/SettingsViewModel.kt`
- Create: `app/src/test/java/com/teleshield/app/ui/settings/SettingsViewModelTest.kt`

**Interfaces:**
- Consumes: `SystemConfigurationRepository.load(): ScreeningConfiguration`, `save(ScreeningConfiguration)`.
- Produces: `SettingsViewModel.uiState: StateFlow<SettingsUiState>`, `SettingsUiState(config: ScreeningConfiguration? = null)`, `setMasterEnabled(Boolean)`, `setBlockUnknown(Boolean)`, `setRetention(Int)`.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleshield.app.di.IoDispatcher
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configurationRepository: SystemConfigurationRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    data class SettingsUiState(val config: ScreeningConfiguration? = null)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val config = withContext(ioDispatcher) { configurationRepository.load() }
            _uiState.value = SettingsUiState(config)
        }
    }

    fun setMasterEnabled(enabled: Boolean) = update { it.copy(masterScreeningEnabled = enabled) }

    fun setBlockUnknown(enabled: Boolean) = update { it.copy(blockUnknownEnabled = enabled) }

    fun setRetention(days: Int) = update { it.copy(logRetentionDays = days) }

    private fun update(transform: (ScreeningConfiguration) -> ScreeningConfiguration) {
        val current = _uiState.value.config ?: return
        viewModelScope.launch {
            val updated = withContext(ioDispatcher) { transform(current).also { configurationRepository.save(it) } }
            _uiState.value = SettingsUiState(updated)
        }
    }
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.teleshield.app.ui.settings

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.inmemory.InMemorySystemConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `loads the configuration on init`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals(ScreeningConfiguration(true, false, 30), vm.uiState.value.config)
    }

    @Test
    fun `setMasterEnabled persists and updates state`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.setMasterEnabled(false)
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.config!!.masterScreeningEnabled)
        assertEquals(false, configRepo.load().masterScreeningEnabled)
    }

    @Test
    fun `setRetention persists`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.setRetention(90)
        advanceUntilIdle()

        assertEquals(90, vm.uiState.value.config!!.logRetentionDays)
        assertEquals(90, configRepo.load().logRetentionDays)
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.ui.settings.SettingsViewModelTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/settings/SettingsViewModel.kt app/src/test/java/com/teleshield/app/ui/settings/SettingsViewModelTest.kt
git commit -m "feat: add settings view model"
```

### Task E2: `SettingsScreen`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val retentionOptions = listOf(
    7 to "7 days",
    14 to "14 days",
    30 to "30 days",
    90 to "90 days",
    0 to "Never",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config ?: return

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToggleSetting("Master screening", config.masterScreeningEnabled, viewModel::setMasterEnabled)
            ToggleSetting("Block unknown callers", config.blockUnknownEnabled, viewModel::setBlockUnknown)
            RetentionSetting(config.logRetentionDays, viewModel::setRetention)
        }
    }
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetentionSetting(selectedDays: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = retentionOptions.firstOrNull { it.first == selectedDays }?.second
        ?: retentionOptions.first { it.first == 30 }.second

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Log retention") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            retentionOptions.forEach { (days, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(days); expanded = false },
                )
            }
        }
    }
}
```

Note: this file needs `import androidx.compose.runtime.getValue`, `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.remember`, `androidx.compose.runtime.setValue` (for the `expanded` var). Add them to the import block.

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt
git commit -m "feat: add settings screen"
```

---

## Part F — rule edit

### Task F1: Add-rule dialog edit mode + Rules screen row edit

**Files:**
- Modify: `app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt`
- Modify: `app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt`

- [ ] **Step 1: `AddRuleDialog.kt`** — add an `initialRule: ScreeningRule? = null` param and prefill from it. Change the signature to:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (AddRuleUseCase.AddRuleRequest) -> Unit,
    initialRule: ScreeningRule? = null,
) {
    var pattern by remember { mutableStateOf(initialRule?.pattern?.expression ?: "") }
    var label by remember { mutableStateOf(initialRule?.label ?: "") }
    var type by remember { mutableStateOf(initialRule?.ruleType ?: RuleType.EXACT) }
    var isWhitelist by remember { mutableStateOf(initialRule?.isWhitelist ?: false) }
    val isInputValid = pattern.isNotBlank() && PatternExpression(pattern).isValid(type)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "Add rule" else "Edit rule") },
        text = {
            // unchanged Column
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(AddRuleUseCase.AddRuleRequest(pattern, type, label, isWhitelist, id = initialRule?.id))
                },
                enabled = isInputValid,
            ) { Text(if (initialRule == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
```

Add import `com.teleshield.domain.ScreeningRule`.

- [ ] **Step 2: `RulesScreen.kt`** — add row tap-to-edit. Replace `var showAddDialog by remember { mutableStateOf(false) }` with editing state and wire the row:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingRule by remember { mutableStateOf<ScreeningRule?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rules") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(uiState.rules, key = { it.id }) { rule ->
                RuleRow(
                    rule = rule,
                    onClick = { editingRule = rule; showDialog = true },
                    onDelete = { viewModel.deleteRule(rule.id) },
                )
            }
        }
    }

    if (showDialog) {
        AddRuleDialog(
            onDismiss = { showDialog = false },
            onAdd = { request ->
                viewModel.addRule(request)
                showDialog = false
            },
            initialRule = editingRule,
        )
    }
}

@Composable
private fun RuleRow(rule: ScreeningRule, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(rule.label.ifBlank { rule.pattern.expression }) },
        supportingContent = { Text("${rule.ruleType.name} · ${rule.pattern.expression}") },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
    )
}
```

Add `import androidx.compose.foundation.clickable`.

- [ ] **Step 3: Verify** — `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all unit tests green.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt
git commit -m "feat: add rule editing"
```

---

## Sequencing note for Task B1

Task B1 (NavHost with 4 routes) references `SimulatorScreen`/`AuditLogScreen`/`SettingsScreen`. In execution, do the screen tasks (C2, D2, E2) BEFORE finalizing B1's commit, OR commit B1 last. Recommended order: A1 → C1 → C2 → D1 → D2 → E1 → E2 → B1 → F1. B1's commit depends on C2/D2/E2 existing.

## Verification (end of slice)

- `.\gradlew.bat :engine:test` — green.
- `.\gradlew.bat :app:testDebugUnitTest` — green.
- `.\gradlew.bat :app:assembleDebug` — green.
- Manual on device (deferred): navigate all 4 tabs; add/edit/delete a rule; simulate a call; view + purge audit log; toggle settings and confirm persistence.

## Deferred (later)

Real `TelephonyInterceptionPort` (TelecomManager) · `CallScreeningService` + role request · in-memory rules cache for the ring-time path · formatted audit-log timestamps · Compose instrumented UI tests.
