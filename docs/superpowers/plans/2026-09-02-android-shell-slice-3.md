# TeleShield Android Shell — Sub-Milestone 3 (Real Call Screening) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement task-by-task. Steps use `- [ ]` for tracking.

**Goal:** Make TeleShield actually intercept calls: an in-memory rule/config snapshot for the ring-time hot path, a `CallScreeningService` that screens each incoming call and rejects blockers, and the screening-role request flow in Settings.

**Architecture:** Two caching repositories wrap the Room/DataStore adapters and serve the hot path from memory (never disk at ring time). A `CallScreeningService` (`onScreenCall` → extract number → normalize → `ScreeningEngine` against the cached snapshot → `CallResponse`) is declared in the manifest. Settings gains an "Enable call screening" row that drives the `RoleManager` role request. Decision logic (verdict → action, number extraction) is pure/fake-testable; the Android glue (`CallResponse`, `RoleManager`) is thin and device-verified.

**Tech Stack:** unchanged (Compose, Hilt 2.52, `@IoDispatcher`). Adds `android.telecom.CallScreeningService`, `android.app.role.RoleManager`, `android.Manifest.permission.BIND_SCREENING_SERVICE`. `minSdk 24`; `CallScreeningService` works on 24+, the *role* requires 29+ (guarded).

## Global Constraints

- `:engine` stays framework-free. No engine changes in this slice.
- The ring-time path reads from the in-memory caches; the Room/DataStore adapters are only touched by the management (screen) paths, offloaded to `@IoDispatcher`.
- `CallScreeningService.onScreenCall()` must produce a `CallResponse` fast; it must never block on disk.
- `minSdk 24`, `targetSdk 35`, `compileSdk 35`, JDK 17. TDD red→green, Conventional Commits, ≤3 files per commit. Windows: `.\gradlew.bat`.
- **Device verification deferred:** the role/permission flow and end-to-end call blocking can only be verified on an Android 10+ device.

---

## File Structure

```
app/src/main/java/com/teleshield/app/data/CachingScreeningRuleRepository.kt        (new)
app/src/main/java/com/teleshield/app/data/CachingSystemConfigurationRepository.kt   (new)
app/src/main/java/com/teleshield/app/di/RepositoryModule.kt                        (bind caches)
app/src/main/java/com/teleshield/app/screening/ScreeningAction.kt                  (enum)
app/src/main/java/com/teleshield/app/screening/ScreeningActionMapper.kt            (verdict->action)
app/src/main/java/com/teleshield/app/screening/CallNumberExtractor.kt              (tel: -> number)
app/src/main/java/com/teleshield/app/screening/TeleShieldCallScreeningService.kt   (CallScreeningService)
app/src/main/java/com/teleshield/app/screening/ScreeningRoleController.kt          (interface + impl)
app/src/main/AndroidManifest.xml                                                  (declare service)
app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt                 (enable row)
app/src/main/java/com/teleshield/app/di/EngineModule.kt                            (bind role controller)
```
Tests per task.

---

## Part A — in-memory caches

### Task A1: `CachingScreeningRuleRepository`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/CachingScreeningRuleRepository.kt`
- Create: `app/src/test/java/com/teleshield/app/data/CachingScreeningRuleRepositoryTest.kt`

**Interfaces:**
- Consumes: `ScreeningRuleRepository` (delegate); `@Inject constructor(delegate)`.
- Produces: implements `ScreeningRuleRepository` (caching reads from memory, forwarding writes to delegate and refreshing the snapshot), plus `snapshot(): List<ScreeningRule>` for the ring-time path.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.data

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingScreeningRuleRepository @Inject constructor(
    private val delegate: ScreeningRuleRepository,
) : ScreeningRuleRepository {

    @Volatile
    private var cached: List<ScreeningRule>? = null

    private fun snapshotList(): List<ScreeningRule> =
        cached ?: delegate.findAll().also { cached = it }

    override fun findAll(): List<ScreeningRule> = snapshotList()
    override fun findActiveRules(): List<ScreeningRule> = snapshotList().filter { it.isEnabled }
    override fun findWhitelistRules(): List<ScreeningRule> = snapshotList().filter { it.isWhitelist }
    override fun findById(id: String): ScreeningRule? = snapshotList().firstOrNull { it.id == id }

    override fun save(rule: ScreeningRule): String {
        val id = delegate.save(rule)
        cached = delegate.findAll()
        return id
    }

    override fun delete(id: String): Boolean {
        val deleted = delegate.delete(id)
        cached = delegate.findAll()
        return deleted
    }

    override fun incrementTriggerCount(id: String, timestamp: Long) {
        delegate.incrementTriggerCount(id, timestamp)
    }

    fun snapshot(): List<ScreeningRule> = snapshotList()
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.teleshield.app.data

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import org.junit.Test
import kotlin.test.assertEquals

class CachingScreeningRuleRepositoryTest {

    @Test
    fun `serves reads from an in-memory snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)

        assertEquals(listOf("r1"), cache.findAll().map { it.id })
        assertEquals(0, delegate.findAllCalls) // served from memory after first load
        assertEquals(listOf("r1"), cache.snapshot().map { it.id })
    }

    @Test
    fun `save forwards then refreshes the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)
        cache.findAll()

        cache.save(rule("r2"))

        assertEquals(setOf("r1", "r2"), cache.snapshot().map { it.id }.toSet())
        assertEquals(true, delegate.contains("r2"))
    }

    @Test
    fun `delete forwards then refreshes the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)
        cache.findAll()

        cache.delete("r1")

        assertEquals(emptyList(), cache.snapshot())
    }

    @Test
    fun `findActiveRules filters the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("on", enabled = true), rule("off", enabled = false)))
        val cache = CachingScreeningRuleRepository(delegate)

        assertEquals(listOf("on"), cache.findActiveRules().map { it.id })
    }

    private fun rule(id: String, enabled: Boolean = true) = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = enabled,
    )

    private class FakeRepo(private val rules: MutableList<ScreeningRule>) : ScreeningRuleRepository {
        var findAllCalls = 0
        fun contains(id: String) = rules.any { it.id == id }

        override fun findActiveRules() = rules.filter { it.isEnabled }
        override fun findWhitelistRules() = rules.filter { it.isWhitelist }
        override fun findAll(): List<ScreeningRule> {
            findAllCalls++
            return rules.toList()
        }
        override fun findById(id: String) = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String {
            rules.removeIf { it.id == rule.id }
            rules.add(rule)
            return rule.id
        }
        override fun delete(id: String): Boolean = rules.removeIf { it.id == id }
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.CachingScreeningRuleRepositoryTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/CachingScreeningRuleRepository.kt app/src/test/java/com/teleshield/app/data/CachingScreeningRuleRepositoryTest.kt
git commit -m "feat: add caching screening rule repository"
```

### Task A2: `CachingSystemConfigurationRepository`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/CachingSystemConfigurationRepository.kt`
- Create: `app/src/test/java/com/teleshield/app/data/CachingSystemConfigurationRepositoryTest.kt`

**Interfaces:**
- Consumes: `SystemConfigurationRepository` (delegate); `@Inject constructor(delegate)`.
- Produces: implements `SystemConfigurationRepository` caching `load()`; `save()` forwards and updates the cache. `snapshot(): ScreeningConfiguration` for the ring path.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingSystemConfigurationRepository @Inject constructor(
    private val delegate: SystemConfigurationRepository,
) : SystemConfigurationRepository {

    @Volatile
    private var cached: ScreeningConfiguration? = null

    override fun load(): ScreeningConfiguration =
        cached ?: delegate.load().also { cached = it }

    override fun save(configuration: ScreeningConfiguration) {
        delegate.save(configuration)
        cached = configuration
    }

    fun snapshot(): ScreeningConfiguration = load()
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import org.junit.Test
import kotlin.test.assertEquals

class CachingSystemConfigurationRepositoryTest {

    @Test
    fun `load caches the delegate value`() {
        val delegate = FakeConfig(ScreeningConfiguration(true, false, 30))
        val cache = CachingSystemConfigurationRepository(delegate)

        assertEquals(ScreeningConfiguration(true, false, 30), cache.load())
        cache.load()
        assertEquals(1, delegate.loadCalls)
    }

    @Test
    fun `save forwards and updates the snapshot`() {
        val delegate = FakeConfig(ScreeningConfiguration(true, false, 30))
        val cache = CachingSystemConfigurationRepository(delegate)
        val updated = ScreeningConfiguration(false, true, 90)

        cache.save(updated)

        assertEquals(updated, cache.snapshot())
        assertEquals(updated, delegate.saved)
    }

    private class FakeConfig(var config: ScreeningConfiguration) : SystemConfigurationRepository {
        var loadCalls = 0
        var saved: ScreeningConfiguration? = null
        override fun load(): ScreeningConfiguration {
            loadCalls++
            return config
        }
        override fun save(configuration: ScreeningConfiguration) {
            config = configuration
            saved = configuration
        }
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.CachingSystemConfigurationRepositoryTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/CachingSystemConfigurationRepository.kt app/src/test/java/com/teleshield/app/data/CachingSystemConfigurationRepositoryTest.kt
git commit -m "feat: add caching system configuration repository"
```

### Task A3: Bind the caches as the ports

**Files:**
- Modify: `app/src/main/java/com/teleshield/app/di/RepositoryModule.kt`

**Interfaces:**
- Produces: `ScreeningRuleRepository` bound to `CachingScreeningRuleRepository` (wrapping the Room repo); `SystemConfigurationRepository` bound to `CachingSystemConfigurationRepository` (wrapping the DataStore repo). `BlockedCallRecordRepository` stays bound to the Room repo.

- [ ] **Step 1: Replace `RepositoryModule.kt`** with:

```kotlin
package com.teleshield.app.di

import com.teleshield.app.data.CachingScreeningRuleRepository
import com.teleshield.app.data.CachingSystemConfigurationRepository
import com.teleshield.app.data.DataStoreSystemConfigurationRepository
import com.teleshield.app.data.NoOpTelephonyInterceptionPort
import com.teleshield.app.data.RoomBlockedCallRecordRepository
import com.teleshield.app.data.RoomScreeningRuleRepository
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import com.teleshield.ports.TelephonyInterceptionPort
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindBlockedCallRecordRepository(impl: RoomBlockedCallRecordRepository): BlockedCallRecordRepository

    @Binds
    abstract fun bindTelephonyInterceptionPort(impl: NoOpTelephonyInterceptionPort): TelephonyInterceptionPort

    @Provides
    fun provideCachingScreeningRuleRepository(room: RoomScreeningRuleRepository): ScreeningRuleRepository =
        CachingScreeningRuleRepository(room)

    @Provides
    fun provideCachingSystemConfigurationRepository(dataStore: DataStoreSystemConfigurationRepository): SystemConfigurationRepository =
        CachingSystemConfigurationRepository(dataStore)
}
```

- [ ] **Step 2: Verify build + tests** — `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL (the existing screens/use cases now read through the caches).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/di/RepositoryModule.kt
git commit -m "feat: serve management path through caching repositories"
```

---

## Part B — screening service (logic + glue)

### Task B1: `ScreeningAction` + mapper

**Files:**
- Create: `app/src/main/java/com/teleshield/app/screening/ScreeningAction.kt`
- Create: `app/src/main/java/com/teleshield/app/screening/ScreeningActionMapper.kt`
- Create: `app/src/test/java/com/teleshield/app/screening/ScreeningActionMapperTest.kt`

**Interfaces:**
- Produces: `enum class ScreeningAction { ALLOW, REJECT }`; `ScreeningActionMapper.toAction(verdict: ScreeningVerdict): ScreeningAction`.

- [ ] **Step 1: `ScreeningAction.kt`**

```kotlin
package com.teleshield.app.screening

enum class ScreeningAction {
    ALLOW,
    REJECT,
}
```

- [ ] **Step 2: `ScreeningActionMapper.kt`**

```kotlin
package com.teleshield.app.screening

import com.teleshield.domain.ScreeningVerdict

object ScreeningActionMapper {

    fun toAction(verdict: ScreeningVerdict): ScreeningAction = when (verdict) {
        is ScreeningVerdict.Blocked -> ScreeningAction.REJECT
        is ScreeningVerdict.Whitelisted -> ScreeningAction.ALLOW
        is ScreeningVerdict.Allowed -> ScreeningAction.ALLOW
    }
}
```

- [ ] **Step 3: Write the failing test**

```kotlin
package com.teleshield.app.screening

import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import org.junit.Test
import kotlin.test.assertEquals

class ScreeningActionMapperTest {

    @Test
    fun `blocked maps to REJECT`() {
        assertEquals(ScreeningAction.REJECT, ScreeningActionMapper.toAction(ScreeningVerdict.Blocked(rule(), 0)))
    }

    @Test
    fun `whitelisted maps to ALLOW`() {
        assertEquals(ScreeningAction.ALLOW, ScreeningActionMapper.toAction(ScreeningVerdict.Whitelisted(rule())))
    }

    @Test
    fun `allowed maps to ALLOW`() {
        assertEquals(ScreeningAction.ALLOW, ScreeningActionMapper.toAction(ScreeningVerdict.Allowed("No matching rules")))
    }

    private fun rule() = ScreeningRule(
        id = "r1",
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )
}
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.screening.ScreeningActionMapperTest"`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/teleshield/app/screening/ScreeningAction.kt app/src/main/java/com/teleshield/app/screening/ScreeningActionMapper.kt app/src/test/java/com/teleshield/app/screening/ScreeningActionMapperTest.kt
git commit -m "feat: add screening action mapper"
```

### Task B2: Call-number extraction

**Files:**
- Create: `app/src/main/java/com/teleshield/app/screening/CallNumberExtractor.kt`
- Create: `app/src/test/java/com/teleshield/app/screening/CallNumberExtractorTest.kt`

- [ ] **Step 1: Implement** (extract the scheme-specific part of a `tel:` Uri; empty otherwise):

```kotlin
package com.teleshield.app.screening

import android.net.Uri

object CallNumberExtractor {

    fun extract(handle: Uri?): String = when {
        handle == null -> ""
        handle.scheme == "tel" -> handle.schemeSpecificPart ?: ""
        else -> ""
    }
}
```

- [ ] **Step 2: Write the failing test** (Robolectric-free; `android.net.Uri` is available in JVM tests via the Android SDK stub):

```kotlin
package com.teleshield.app.screening

import android.net.Uri
import org.junit.Test
import kotlin.test.assertEquals

class CallNumberExtractorTest {

    @Test
    fun `extracts a tel number`() {
        assertEquals("+15551234567", CallNumberExtractor.extract(Uri.parse("tel:+15551234567")))
    }

    @Test
    fun `returns empty for a null handle`() {
        assertEquals("", CallNumberExtractor.extract(null))
    }

    @Test
    fun `returns empty for a non-tel uri`() {
        assertEquals("", CallNumberExtractor.extract(Uri.parse("urn:anonymous")))
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.screening.CallNumberExtractorTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/screening/CallNumberExtractor.kt app/src/test/java/com/teleshield/app/screening/CallNumberExtractorTest.kt
git commit -m "feat: add call number extractor"
```

### Task B3: `CallScreeningService` + manifest declaration

**Files:**
- Create: `app/src/main/java/com/teleshield/app/screening/TeleShieldCallScreeningService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `ScreeningEngine`, `IdentifierNormalizer`, `CachingScreeningRuleRepository`, `CachingSystemConfigurationRepository` (all Hilt-injected), `CallNumberExtractor`, `ScreeningActionMapper`.
- Produces: `TeleShieldCallScreeningService : CallScreeningService` — overrides `onScreenCall(callDetails)`.

- [ ] **Step 1: `TeleShieldCallScreeningService.kt`**

```kotlin
package com.teleshield.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.app.data.CachingScreeningRuleRepository
import com.teleshield.app.data.CachingSystemConfigurationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TeleShieldCallScreeningService : CallScreeningService() {

    @Inject lateinit var engine: ScreeningEngine
    @Inject lateinit var normalizer: IdentifierNormalizer
    @Inject lateinit var ruleRepository: CachingScreeningRuleRepository
    @Inject lateinit var configurationRepository: CachingSystemConfigurationRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val number = CallNumberExtractor.extract(callDetails.handle)
        val config = configurationRepository.snapshot()
        val caller = CallerIdentifier.from(number, normalizer)
        val verdict = engine.screen(
            caller = caller,
            rules = ruleRepository.snapshot(),
            masterScreeningEnabled = config.masterScreeningEnabled,
            blockUnknownEnabled = config.blockUnknownEnabled,
        )
        val action = ScreeningActionMapper.toAction(verdict)
        respondToCall(callDetails, callResponseFor(action))
    }

    private fun callResponseFor(action: ScreeningAction): Call.CallResponse = when (action) {
        ScreeningAction.ALLOW -> Call.CallResponse.Builder().setDisallowCall(false).build()
        ScreeningAction.REJECT -> Call.CallResponse.Builder().setRejectCall(true).build()
    }
}
```

- [ ] **Step 2: `AndroidManifest.xml`** — add this inside `<application>`:

```xml
        <service
            android:name=".screening.TeleShieldCallScreeningService"
            android:permission="android.permission.BIND_SCREENING_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.telecom.CallScreeningService" />
            </intent-filter>
        </service>
```

- [ ] **Step 3: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL. (Runtime behavior + actual call blocking requires a device — deferred.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/screening/TeleShieldCallScreeningService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add call screening service"
```

---

## Part C — screening role / permission flow

### Task C1: `ScreeningRoleController` (interface + Android impl)

**Files:**
- Create: `app/src/main/java/com/teleshield/app/screening/ScreeningRoleController.kt`
- Create: `app/src/test/java/com/teleshield/app/screening/ScreeningRoleControllerTest.kt`

**Interfaces:**
- Produces: `ScreeningRoleController { fun isRoleHeld(): Boolean; fun requestRoleIntent(): Intent }`; `AndroidScreeningRoleController @Inject constructor(@ApplicationContext context)`.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.screening

import android.content.Context
import android.app.role.RoleManager
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface ScreeningRoleController {
    fun isRoleHeld(): Boolean
    fun requestRoleIntent(): Intent
}

class AndroidScreeningRoleController @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScreeningRoleController {

    private val roleManager: RoleManager?
        get() = context.getSystemService(RoleManager::class.java)

    override fun isRoleHeld(): Boolean =
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false

    override fun requestRoleIntent(): Intent =
        roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            ?: createFallbackIntent()

    private fun createFallbackIntent(): Intent {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
```

- [ ] **Step 2: Write the failing test** (mock RoleManager via a fake controller):

```kotlin
package com.teleshield.app.screening

import android.content.Intent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreeningRoleControllerTest {

    private class FakeRoleController(
        var held: Boolean = false,
        val intent: Intent = Intent(),
    ) : ScreeningRoleController {
        override fun isRoleHeld(): Boolean = held
        override fun requestRoleIntent(): Intent = intent
    }

    @Test
    fun `isRoleHeld reflects the platform state`() {
        assertFalse(FakeRoleController(held = false).isRoleHeld())
        assertTrue(FakeRoleController(held = true).isRoleHeld())
    }

    @Test
    fun `requestRoleIntent returns a launchable intent`() {
        val intent = Intent("android.intent.action.VIEW")
        assertEquals(intent, FakeRoleController(intent = intent).requestRoleIntent())
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.screening.ScreeningRoleControllerTest"`. (The `AndroidScreeningRoleController` itself is device-verified; the fake-interface test covers the controller contract.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/screening/ScreeningRoleController.kt app/src/test/java/com/teleshield/app/screening/ScreeningRoleControllerTest.kt
git commit -m "feat: add screening role controller"
```

### Task C2: Settings "Enable call screening" row

**Files:**
- Modify: `app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `ScreeningRoleController` (via `LocalContext` in Compose), Activity result launcher.
- Produces: a row in `SettingsScreen` showing screening-role status and a button that launches the role request; on result, re-check.

- [ ] **Step 1: Add to `SettingsScreen.kt`** this composable and reference it (add imports: `androidx.activity.compose.rememberLauncherForActivityResult`, `androidx.activity.result.contract.ActivityResultContracts`, `androidx.compose.runtime.*`, `androidx.compose.material3.Button`, `androidx.compose.platform.LocalContext`, `com.teleshield.app.screening.ScreeningRoleController`):

```kotlin
@Composable
private fun ScreeningRoleRow(controller: ScreeningRoleController) {
    var roleHeld by remember { mutableStateOf(controller.isRoleHeld()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = controller.isRoleHeld()
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val status = if (roleHeld) "Enabled" else "Not enabled"
        Text("Call screening  ·  $status", Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Button(onClick = { launcher.launch(controller.requestRoleIntent()) }) {
            Text(if (roleHeld) "Configure" else "Enable")
        }
    }
}
```

and add `ScreeningRoleRow(remember { AndroidScreeningRoleController(LocalContext.current) })` as the last child of the Settings `Column`.

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`. (Runtime role flow requires a device — deferred.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/settings/SettingsScreen.kt
git commit -m "feat: add enable-call-screening row to settings"
```

---

## Sequencing note

Walk order is A1 → A2 → A3 → B1 → B2 → B3 → C1 → C2. The cache bindings (A3) switch the management path to the caches earlier than the UI screens change — apply A3 only after A1/A2, and re-run the full unit suite to confirm no regression before continuing.

## Verification (end of slice)

- `.\gradlew.bat :engine:test` — green.
- `.\gradlew.bat :app:testDebugUnitTest` — green.
- `.\gradlew.bat :app:assembleDebug` — green.
- Manual on device (deferred to user): grant the call-screening role in Settings → place an incoming call from a blocked number → it is rejected/silenced; a whitelisted number gets through; master switch off allows all.

## Deferred (later)

Formatted audit timestamps · audit "purge expired" vs clear-all clarification · preserve `isEnabled` on rule edit (needs an enable toggle) · Compose instrumented UI tests · repeated-call/do-not-disturb edge policies.
