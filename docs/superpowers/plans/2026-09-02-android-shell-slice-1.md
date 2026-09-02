# TeleShield Android Shell — Sub-Milestone 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A runnable Android app that wires the existing pure-JVM `:engine` into Hilt, persists rules/config/logs via Room + DataStore, and exposes a Rules screen with list/add/delete end-to-end.

**Architecture:** New `:app` Android module depends on `:engine`. Room/DataStore adapters implement the existing ports; Hilt `@Binds` the ports to adapters and `@Provides` the engine + use cases (keeping `:engine` free of `javax.inject`). UI is Compose + ViewModel/StateFlow (MVVM/UDF). No `CallScreeningService` yet.

**Tech Stack:** AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01 (+ Compose Compiler plugin 2.1.0), Hilt 2.52 (KSP), Room 2.6.1 (KSP), DataStore 1.1.1, Lifecycle 2.8.7, Navigation-Compose 2.8.5, Robolectric 4.14.1, kotlinx-coroutines 1.9.0.

## Global Constraints

- `:engine` stays framework-free — **no `javax.inject`, Android, Room, or coroutines imports** in `com.teleshield` engine source. All DI lives in `:app`.
- Ports are **synchronous**; Room adapters bridge suspend DAOs with `runBlocking` internally (called off the main thread by the ViewModel's IO dispatcher). The screening hot path will use an in-memory cache in the later `CallScreeningService` milestone — never the Room adapter.
- TDD red→green, vertical slices, Conventional Commits, **≤3 files per commit** (exception: Task A1 is an interface change rippling through existing fakes — one atomic commit, noted).
- `minSdk 24`, `targetSdk 35`, `compileSdk 35`, JDK 17 bytecode throughout (engine lowered from 21 → 17 to match Android).
- Windows shell: use `.\gradlew.bat` in place of `./gradlew`.

---

## File Structure

```
settings.gradle.kts                          (modify: plugins + include :app)
build.gradle.kts                             (modify: plugins + google() repo)
engine/build.gradle.kts                      (modify: jvmTarget 21 -> 17)
engine/src/main/kotlin/com/teleshield/ports/ScreeningRuleRepository.kt   (add findAll)
engine/src/main/kotlin/com/teleshield/inmemory/InMemoryScreeningRuleRepository.kt (add findAll)
engine/src/main/kotlin/com/teleshield/application/QueryRulesUseCase.kt   (new)
engine/src/main/kotlin/com/teleshield/application/DeleteRuleUseCase.kt   (new)

app/build.gradle.kts                         (new)
app/src/main/AndroidManifest.xml             (new)
app/src/main/res/values/themes.xml           (new)
app/src/main/java/com/teleshield/app/TeleShieldApp.kt                 (new)
app/src/main/java/com/teleshield/app/MainActivity.kt                  (new)
app/src/main/java/com/teleshield/app/ui/theme/Theme.kt                (new)
app/src/main/java/com/teleshield/app/data/db/ScreeningRuleEntity.kt   (new)
app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordEntity.kt (new)
app/src/main/java/com/teleshield/app/data/db/ScreeningRuleDao.kt      (new)
app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordDao.kt  (new)
app/src/main/java/com/teleshield/app/data/db/TeleShieldDatabase.kt    (new)
app/src/main/java/com/teleshield/app/data/mapper/ScreeningRuleMapper.kt   (new)
app/src/main/java/com/teleshield/app/data/mapper/BlockedCallRecordMapper.kt (new)
app/src/main/java/com/teleshield/app/data/RoomScreeningRuleRepository.kt   (new)
app/src/main/java/com/teleshield/app/data/RoomBlockedCallRecordRepository.kt (new)
app/src/main/java/com/teleshield/app/data/ConfigurationDataSource.kt   (new)
app/src/main/java/com/teleshield/app/data/DataStoreSystemConfigurationRepository.kt (new)
app/src/main/java/com/teleshield/app/data/NoOpTelephonyInterceptionPort.kt (new)
app/src/main/java/com/teleshield/app/di/DatabaseModule.kt             (new)
app/src/main/java/com/teleshield/app/di/ConfigurationModule.kt        (new)
app/src/main/java/com/teleshield/app/di/RepositoryModule.kt           (new)
app/src/main/java/com/teleshield/app/di/EngineModule.kt               (new)
app/src/main/java/com/teleshield/app/ui/rules/RulesViewModel.kt       (new)
app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt          (new)
app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt        (new)
app/src/main/java/com/teleshield/app/ui/navigation/TeleShieldNavHost.kt (new)
```

Tests (all under `app/src/test/...` or `engine/src/test/...`) listed per task.

---

## Part A — engine prerequisites

### Task A1: Add `findAll()` to the rule repository

> Interface change — updates the port, the in-memory impl, its test, and the three existing fake repositories that implement the port. One atomic commit (6 files); cannot be split without a red build.

**Files:**
- Modify: `engine/src/main/kotlin/com/teleshield/ports/ScreeningRuleRepository.kt`
- Modify: `engine/src/main/kotlin/com/teleshield/inmemory/InMemoryScreeningRuleRepository.kt`
- Modify: `engine/src/test/kotlin/com/teleshield/inmemory/InMemoryScreeningRuleRepositoryTest.kt`
- Modify: `engine/src/test/kotlin/com/teleshield/application/ScreenIncomingCallUseCaseTest.kt`
- Modify: `engine/src/test/kotlin/com/teleshield/application/SimulateCallUseCaseTest.kt`
- Modify: `engine/src/test/kotlin/com/teleshield/application/AddRuleUseCaseTest.kt`

**Interfaces:**
- Produces: `ScreeningRuleRepository.findAll(): List<ScreeningRule>` — returns every stored rule regardless of enabled/whitelist.

- [ ] **Step 1: Write the failing test** — add to `InMemoryScreeningRuleRepositoryTest.kt`:

```kotlin
    @Test
    fun `findAll returns every rule regardless of state`() {
        repository.save(rule("on", enabled = true))
        repository.save(rule("off", enabled = false))
        repository.save(rule("wl", enabled = true, isWhitelist = true))

        val all = repository.findAll()

        assertEquals(setOf("on", "off", "wl"), all.map { it.id }.toSet())
    }
```

- [ ] **Step 2: Run to confirm it fails** — `.\gradlew.bat :engine:test --tests "*InMemoryScreeningRuleRepositoryTest*"`. Expected: compile error `Unresolved reference 'findAll'`.

- [ ] **Step 3: Implement** — add to the port interface:

```kotlin
    fun findAll(): List<ScreeningRule>
```

and to `InMemoryScreeningRuleRepository`:

```kotlin
    override fun findAll(): List<ScreeningRule> = store.values.toList()
```

- [ ] **Step 4: Fix the three broken fakes** — add this identical override to `FakeRuleRepository` in `ScreenIncomingCallUseCaseTest.kt`, `SimulateCallUseCaseTest.kt`, and `AddRuleUseCaseTest.kt`:

```kotlin
        override fun findAll(): List<ScreeningRule> = rules.toList()
```

- [ ] **Step 5: Run to verify green** — `.\gradlew.bat :engine:test`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add engine/src/main/kotlin/com/teleshield/ports/ScreeningRuleRepository.kt engine/src/main/kotlin/com/teleshield/inmemory/InMemoryScreeningRuleRepository.kt engine/src/test/kotlin/com/teleshield/inmemory/InMemoryScreeningRuleRepositoryTest.kt engine/src/test/kotlin/com/teleshield/application/ScreenIncomingCallUseCaseTest.kt engine/src/test/kotlin/com/teleshield/application/SimulateCallUseCaseTest.kt engine/src/test/kotlin/com/teleshield/application/AddRuleUseCaseTest.kt
git commit -m "feat: add findAll to rule repository"
```

### Task A2: Add `QueryRulesUseCase`

**Files:**
- Create: `engine/src/main/kotlin/com/teleshield/application/QueryRulesUseCase.kt`
- Create: `engine/src/test/kotlin/com/teleshield/application/QueryRulesUseCaseTest.kt`

**Interfaces:**
- Consumes: `ScreeningRuleRepository.findAll(): List<ScreeningRule>` (from A1).
- Produces: `QueryRulesUseCase.execute(): List<ScreeningRule>`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.teleshield.application

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryRulesUseCaseTest {

    @Test
    fun `returns all rules from the repository`() {
        val rules = listOf(rule("a", enabled = true), rule("b", enabled = false))
        val useCase = QueryRulesUseCase(FakeRepo(rules))

        assertEquals(rules, useCase.execute())
    }

    @Test
    fun `returns empty when repository is empty`() {
        assertEquals(emptyList(), QueryRulesUseCase(FakeRepo(emptyList())).execute())
    }

    private fun rule(id: String, enabled: Boolean) = ScreeningRule(
        id = id, pattern = PatternExpression("15551234567"), label = "l",
        ruleType = RuleType.EXACT, isWhitelist = false, isEnabled = enabled,
    )

    private class FakeRepo(private val rules: List<ScreeningRule>) : ScreeningRuleRepository {
        override fun findActiveRules() = rules.filter { it.isEnabled }
        override fun findWhitelistRules() = rules.filter { it.isWhitelist }
        override fun findAll() = rules
        override fun findById(id: String) = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String = rule.id
        override fun delete(id: String): Boolean = false
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
```

- [ ] **Step 2: Run to confirm red** — `.\gradlew.bat :engine:test --tests "*QueryRulesUseCaseTest*"`. Expected: `Unresolved reference 'QueryRulesUseCase'`.

- [ ] **Step 3: Implement**

```kotlin
package com.teleshield.application

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository

class QueryRulesUseCase(
    private val ruleRepository: ScreeningRuleRepository,
) {
    fun execute(): List<ScreeningRule> = ruleRepository.findAll()
}
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :engine:test`.

- [ ] **Step 5: Commit**

```bash
git add engine/src/main/kotlin/com/teleshield/application/QueryRulesUseCase.kt engine/src/test/kotlin/com/teleshield/application/QueryRulesUseCaseTest.kt
git commit -m "feat: add query rules use case"
```

### Task A3: Add `DeleteRuleUseCase`

**Files:**
- Create: `engine/src/main/kotlin/com/teleshield/application/DeleteRuleUseCase.kt`
- Create: `engine/src/test/kotlin/com/teleshield/application/DeleteRuleUseCaseTest.kt`

**Interfaces:**
- Consumes: `ScreeningRuleRepository.delete(id: String): Boolean`.
- Produces: `DeleteRuleUseCase.execute(id: String): Boolean`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.teleshield.application

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRuleUseCaseTest {

    @Test
    fun `delegates delete to the repository and returns its result`() {
        val repo = FakeRepo()
        val useCase = DeleteRuleUseCase(repo)

        assertEquals(true, useCase.execute("r1"))
        assertEquals("r1", repo.deletedId)
        assertEquals(false, useCase.execute("missing"))
    }

    private class FakeRepo : ScreeningRuleRepository {
        var deletedId: String? = null
        override fun findActiveRules(): List<ScreeningRule> = emptyList()
        override fun findWhitelistRules(): List<ScreeningRule> = emptyList()
        override fun findAll(): List<ScreeningRule> = emptyList()
        override fun findById(id: String): ScreeningRule? = null
        override fun save(rule: ScreeningRule): String = rule.id
        override fun delete(id: String): Boolean {
            deletedId = id
            return id == "r1"
        }
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
```

- [ ] **Step 2: Run to confirm red** — `.\gradlew.bat :engine:test --tests "*DeleteRuleUseCaseTest*"`. Expected: `Unresolved reference 'DeleteRuleUseCase'`.

- [ ] **Step 3: Implement**

```kotlin
package com.teleshield.application

import com.teleshield.ports.ScreeningRuleRepository

class DeleteRuleUseCase(
    private val ruleRepository: ScreeningRuleRepository,
) {
    fun execute(id: String): Boolean = ruleRepository.delete(id)
}
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :engine:test`.

- [ ] **Step 5: Commit**

```bash
git add engine/src/main/kotlin/com/teleshield/application/DeleteRuleUseCase.kt engine/src/test/kotlin/com/teleshield/application/DeleteRuleUseCaseTest.kt
git commit -m "feat: add delete rule use case"
```

---

## Part B — app module bootstrap

### Task B1: Wire the `:app` module and a minimal runnable activity

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `engine/build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/teleshield/app/TeleShieldApp.kt`
- Create: `app/src/main/java/com/teleshield/app/MainActivity.kt`
- Create: `app/src/main/java/com/teleshield/app/ui/theme/Theme.kt`

- [ ] **Step 1: `settings.gradle.kts`** — replace the whole file:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "teleshield"

include(":engine")
include(":app")
```

- [ ] **Step 2: root `build.gradle.kts`** — replace the whole file:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.android.application") version "8.7.3" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
}

allprojects {
    group = "com.teleshield"
    version = "0.1.0"

    repositories {
        google()
        mavenCentral()
    }
}
```

- [ ] **Step 3: `engine/build.gradle.kts`** — replace the `kotlin { jvmToolchain(21) }` block with:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

- [ ] **Step 4: `app/build.gradle.kts`** — create:

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.teleshield.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.teleshield.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":engine"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 5: `app/src/main/AndroidManifest.xml`** — create:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".TeleShieldApp"
        android:label="TeleShield"
        android:theme="@style/Theme.TeleShield"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.TeleShield">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: `app/src/main/res/values/themes.xml`** — create:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.TeleShield" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 7: `TeleShieldApp.kt`** — create:

```kotlin
package com.teleshield.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TeleShieldApp : Application()
```

- [ ] **Step 8: `ui/theme/Theme.kt`** — create:

```kotlin
package com.teleshield.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun TeleShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
```

- [ ] **Step 9: `MainActivity.kt`** — create (placeholder screen; replaced in Task E2):

```kotlin
package com.teleshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.teleshield.app.ui.theme.TeleShieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleShieldTheme {
                Text("TeleShield")
            }
        }
    }
}
```

- [ ] **Step 10: Verify the build compiles** — `.\gradlew.bat :app:assembleDebug`. Expected: BUILD SUCCESSFUL (first run downloads AGP/deps; may take several minutes).

- [ ] **Step 11: Commit** (split ≤3 files per commit):

```bash
git add settings.gradle.kts build.gradle.kts engine/build.gradle.kts
git commit -m "build: add android app module and plugin config"

git add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/themes.xml
git commit -m "build: add app build config and manifest"

git add app/src/main/java/com/teleshield/app/TeleShieldApp.kt app/src/main/java/com/teleshield/app/MainActivity.kt app/src/main/java/com/teleshield/app/ui/theme/Theme.kt
git commit -m "feat: add hilt app, activity and compose theme"
```

---

## Part C — persistence adapters

### Task C1: Room entities, DAOs, and database

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/db/ScreeningRuleEntity.kt`
- Create: `app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordEntity.kt`
- Create: `app/src/main/java/com/teleshield/app/data/db/ScreeningRuleDao.kt`
- Create: `app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordDao.kt`
- Create: `app/src/main/java/com/teleshield/app/data/db/TeleShieldDatabase.kt`

**Interfaces:**
- Produces: `ScreeningRuleEntity(id, expression, ruleType: String, label, isWhitelist, isEnabled, timesTriggered, createdAt, lastTriggeredAt)`, `BlockedCallRecordEntity(...)`, `ScreeningRuleDao`, `BlockedCallRecordDao`, `TeleShieldDatabase`.

- [ ] **Step 1: `ScreeningRuleEntity.kt`**

```kotlin
package com.teleshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screening_rules")
data class ScreeningRuleEntity(
    @PrimaryKey val id: String,
    val expression: String,
    val ruleType: String,
    val label: String,
    val isWhitelist: Boolean,
    val isEnabled: Boolean,
    val timesTriggered: Int,
    val createdAt: Long,
    val lastTriggeredAt: Long?,
)
```

- [ ] **Step 2: `BlockedCallRecordEntity.kt`**

```kotlin
package com.teleshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_call_records")
data class BlockedCallRecordEntity(
    @PrimaryKey val id: String,
    val callerIdentifier: String,
    val timestamp: Long,
    val matchedRuleId: String,
    val matchedPatternSnapshot: String,
    val matchedLabelSnapshot: String,
)
```

- [ ] **Step 3: `ScreeningRuleDao.kt`**

```kotlin
package com.teleshield.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScreeningRuleDao {

    @Query("SELECT * FROM screening_rules")
    suspend fun findAll(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE isEnabled = 1")
    suspend fun findActive(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE isWhitelist = 1")
    suspend fun findWhitelist(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE id = :id")
    suspend fun findById(id: String): ScreeningRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScreeningRuleEntity)

    @Query("DELETE FROM screening_rules WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE screening_rules SET timesTriggered = timesTriggered + 1, lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun incrementTriggerCount(id: String, timestamp: Long)
}
```

- [ ] **Step 4: `BlockedCallRecordDao.kt`**

```kotlin
package com.teleshield.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedCallRecordDao {

    @Query("SELECT * FROM blocked_call_records ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int, offset: Int): List<BlockedCallRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedCallRecordEntity)

    @Query("DELETE FROM blocked_call_records WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM blocked_call_records WHERE timestamp < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long): Int
}
```

- [ ] **Step 5: `TeleShieldDatabase.kt`**

```kotlin
package com.teleshield.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScreeningRuleEntity::class, BlockedCallRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TeleShieldDatabase : RoomDatabase() {
    abstract fun screeningRuleDao(): ScreeningRuleDao
    abstract fun blockedCallRecordDao(): BlockedCallRecordDao
}
```

- [ ] **Step 6: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL (KSP generates DAO/Database implementations).

- [ ] **Step 7: Commit** (3 commits, each compiles):

```bash
git add app/src/main/java/com/teleshield/app/data/db/ScreeningRuleEntity.kt app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordEntity.kt
git commit -m "feat: add room entities"

git add app/src/main/java/com/teleshield/app/data/db/ScreeningRuleDao.kt app/src/main/java/com/teleshield/app/data/db/BlockedCallRecordDao.kt
git commit -m "feat: add room daos"

git add app/src/main/java/com/teleshield/app/data/db/TeleShieldDatabase.kt
git commit -m "feat: add room database"
```

### Task C2: Mappers (pure JVM)

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/mapper/ScreeningRuleMapper.kt`
- Create: `app/src/main/java/com/teleshield/app/data/mapper/BlockedCallRecordMapper.kt`
- Create: `app/src/test/java/com/teleshield/app/data/mapper/ScreeningRuleMapperTest.kt`
- Create: `app/src/test/java/com/teleshield/app/data/mapper/BlockedCallRecordMapperTest.kt`

- [ ] **Step 1: `ScreeningRuleMapper.kt`**

```kotlin
package com.teleshield.app.data.mapper

import com.teleshield.app.data.db.ScreeningRuleEntity
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule

object ScreeningRuleMapper {

    fun toDomain(entity: ScreeningRuleEntity): ScreeningRule = ScreeningRule(
        id = entity.id,
        pattern = PatternExpression(entity.expression),
        label = entity.label,
        ruleType = RuleType.valueOf(entity.ruleType),
        isWhitelist = entity.isWhitelist,
        isEnabled = entity.isEnabled,
        timesTriggered = entity.timesTriggered,
        createdAt = entity.createdAt,
        lastTriggeredAt = entity.lastTriggeredAt,
    )

    fun toEntity(rule: ScreeningRule): ScreeningRuleEntity = ScreeningRuleEntity(
        id = rule.id,
        expression = rule.pattern.expression,
        ruleType = rule.ruleType.name,
        label = rule.label,
        isWhitelist = rule.isWhitelist,
        isEnabled = rule.isEnabled,
        timesTriggered = rule.timesTriggered,
        createdAt = rule.createdAt,
        lastTriggeredAt = rule.lastTriggeredAt,
    )
}
```

- [ ] **Step 2: `BlockedCallRecordMapper.kt`**

```kotlin
package com.teleshield.app.data.mapper

import com.teleshield.app.data.db.BlockedCallRecordEntity
import com.teleshield.domain.BlockedCallRecord

object BlockedCallRecordMapper {

    fun toDomain(entity: BlockedCallRecordEntity): BlockedCallRecord = BlockedCallRecord(
        id = entity.id,
        callerIdentifier = entity.callerIdentifier,
        timestamp = entity.timestamp,
        matchedRuleId = entity.matchedRuleId,
        matchedPatternSnapshot = entity.matchedPatternSnapshot,
        matchedLabelSnapshot = entity.matchedLabelSnapshot,
    )

    fun toEntity(record: BlockedCallRecord): BlockedCallRecordEntity = BlockedCallRecordEntity(
        id = record.id,
        callerIdentifier = record.callerIdentifier,
        timestamp = record.timestamp,
        matchedRuleId = record.matchedRuleId,
        matchedPatternSnapshot = record.matchedPatternSnapshot,
        matchedLabelSnapshot = record.matchedLabelSnapshot,
    )
}
```

- [ ] **Step 3: Write the failing mapper tests** — `ScreeningRuleMapperTest.kt`:

```kotlin
package com.teleshield.app.data.mapper

import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import org.junit.Test
import kotlin.test.assertEquals

class ScreeningRuleMapperTest {

    @Test
    fun `round-trips a screening rule`() {
        val rule = ScreeningRule(
            id = "r1",
            pattern = PatternExpression("1555*"),
            label = "Exchange block",
            ruleType = RuleType.WILDCARD,
            isWhitelist = false,
            isEnabled = true,
            timesTriggered = 3,
            createdAt = 1000L,
            lastTriggeredAt = 2000L,
        )

        val roundTripped = ScreeningRuleMapper.toDomain(ScreeningRuleMapper.toEntity(rule))

        assertEquals(rule.id, roundTripped.id)
        assertEquals(rule.pattern.expression, roundTripped.pattern.expression)
        assertEquals(rule.ruleType, roundTripped.ruleType)
        assertEquals(rule.label, roundTripped.label)
        assertEquals(rule.isWhitelist, roundTripped.isWhitelist)
        assertEquals(rule.isEnabled, roundTripped.isEnabled)
        assertEquals(rule.timesTriggered, roundTripped.timesTriggered)
        assertEquals(rule.createdAt, roundTripped.createdAt)
        assertEquals(rule.lastTriggeredAt, roundTripped.lastTriggeredAt)
    }
}
```

`BlockedCallRecordMapperTest.kt`:

```kotlin
package com.teleshield.app.data.mapper

import com.teleshield.domain.BlockedCallRecord
import org.junit.Test
import kotlin.test.assertEquals

class BlockedCallRecordMapperTest {

    @Test
    fun `round-trips a blocked call record`() {
        val record = BlockedCallRecord(
            id = "c1",
            callerIdentifier = "15551234567",
            timestamp = 42L,
            matchedRuleId = "r1",
            matchedPatternSnapshot = "1555*",
            matchedLabelSnapshot = "Exchange block",
        )

        val roundTripped = BlockedCallRecordMapper.toDomain(BlockedCallRecordMapper.toEntity(record))

        assertEquals(record, roundTripped)
    }
}
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.mapper.*"`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/mapper/ScreeningRuleMapper.kt app/src/main/java/com/teleshield/app/data/mapper/BlockedCallRecordMapper.kt
git commit -m "feat: add room domain mappers"

git add app/src/test/java/com/teleshield/app/data/mapper/ScreeningRuleMapperTest.kt app/src/test/java/com/teleshield/app/data/mapper/BlockedCallRecordMapperTest.kt
git commit -m "test: add mapper round-trip tests"
```

### Task C3: `RoomScreeningRuleRepository`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/RoomScreeningRuleRepository.kt`
- Create: `app/src/test/java/com/teleshield/app/data/RoomScreeningRuleRepositoryTest.kt`

**Interfaces:**
- Consumes: `ScreeningRuleDao`, `ScreeningRuleMapper`.
- Produces: `RoomScreeningRuleRepository(dao: ScreeningRuleDao) : ScreeningRuleRepository`.

- [ ] **Step 1: Implement** (`runBlocking` bridges suspend DAO → synchronous port; called off the main thread by the ViewModel):

```kotlin
package com.teleshield.app.data

import com.teleshield.app.data.db.ScreeningRuleDao
import com.teleshield.app.data.mapper.ScreeningRuleMapper
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RoomScreeningRuleRepository @Inject constructor(
    private val dao: ScreeningRuleDao,
) : ScreeningRuleRepository {

    override fun findActiveRules(): List<ScreeningRule> =
        runBlocking { dao.findActive().map(ScreeningRuleMapper::toDomain) }

    override fun findWhitelistRules(): List<ScreeningRule> =
        runBlocking { dao.findWhitelist().map(ScreeningRuleMapper::toDomain) }

    override fun findAll(): List<ScreeningRule> =
        runBlocking { dao.findAll().map(ScreeningRuleMapper::toDomain) }

    override fun findById(id: String): ScreeningRule? =
        runBlocking { dao.findById(id)?.let(ScreeningRuleMapper::toDomain) }

    override fun save(rule: ScreeningRule): String {
        runBlocking { dao.insert(ScreeningRuleMapper.toEntity(rule)) }
        return rule.id
    }

    override fun delete(id: String): Boolean = runBlocking { dao.deleteById(id) } > 0

    override fun incrementTriggerCount(id: String, timestamp: Long) {
        runBlocking { dao.incrementTriggerCount(id, timestamp) }
    }
}
```

- [ ] **Step 2: Write the failing Robolectric test**

```kotlin
package com.teleshield.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.teleshield.app.data.db.TeleShieldDatabase
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomScreeningRuleRepositoryTest {

    private lateinit var db: TeleShieldDatabase
    private lateinit var repository: RoomScreeningRuleRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TeleShieldDatabase::class.java).build()
        repository = RoomScreeningRuleRepository(db.screeningRuleDao())
    }

    @After fun teardown() {
        db.close()
    }

    @Test
    fun `save then findAll returns the rule`() {
        repository.save(rule("r1", enabled = true))

        val all = repository.findAll()
        assertEquals(listOf("r1"), all.map { it.id })
        assertEquals("15551234567", all.first().pattern.expression)
    }

    @Test
    fun `findActiveRules filters disabled rules`() {
        repository.save(rule("on", enabled = true))
        repository.save(rule("off", enabled = false))

        assertEquals(listOf("on"), repository.findActiveRules().map { it.id })
    }

    @Test
    fun `delete removes a rule`() {
        repository.save(rule("r1", enabled = true))

        assertEquals(true, repository.delete("r1"))
        assertEquals(emptyList(), repository.findAll())
        assertEquals(false, repository.delete("r1"))
    }

    @Test
    fun `incrementTriggerCount bumps the stored counter`() {
        repository.save(rule("r1", enabled = true))

        repository.incrementTriggerCount("r1", 99L)

        assertEquals(1, repository.findById("r1")!!.timesTriggered)
        assertEquals(99L, repository.findById("r1")!!.lastTriggeredAt)
    }

    private fun rule(id: String, enabled: Boolean) = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = enabled,
    )
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.RoomScreeningRuleRepositoryTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/RoomScreeningRuleRepository.kt app/src/test/java/com/teleshield/app/data/RoomScreeningRuleRepositoryTest.kt
git commit -m "feat: add room screening rule repository"
```

### Task C4: `RoomBlockedCallRecordRepository`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/RoomBlockedCallRecordRepository.kt`
- Create: `app/src/test/java/com/teleshield/app/data/RoomBlockedCallRecordRepositoryTest.kt`

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.data

import com.teleshield.app.data.db.BlockedCallRecordDao
import com.teleshield.app.data.mapper.BlockedCallRecordMapper
import com.teleshield.domain.BlockedCallRecord
import com.teleshield.ports.BlockedCallRecordRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RoomBlockedCallRecordRepository @Inject constructor(
    private val dao: BlockedCallRecordDao,
) : BlockedCallRecordRepository {

    override fun getAllRecords(limit: Int, offset: Int): List<BlockedCallRecord> =
        runBlocking { dao.getAll(limit, offset).map(BlockedCallRecordMapper::toDomain) }

    override fun save(record: BlockedCallRecord): String {
        runBlocking { dao.insert(BlockedCallRecordMapper.toEntity(record)) }
        return record.id
    }

    override fun delete(id: String): Boolean = runBlocking { dao.deleteById(id) } > 0

    override fun purgeOlderThan(cutoffTimestamp: Long): Int =
        runBlocking { dao.purgeOlderThan(cutoffTimestamp) }
}
```

- [ ] **Step 2: Write the failing Robolectric test**

```kotlin
package com.teleshield.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teleshield.app.data.db.TeleShieldDatabase
import com.teleshield.domain.BlockedCallRecord
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomBlockedCallRecordRepositoryTest {

    private lateinit var db: TeleShieldDatabase
    private lateinit var repository: RoomBlockedCallRecordRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TeleShieldDatabase::class.java).build()
        repository = RoomBlockedCallRecordRepository(db.blockedCallRecordDao())
    }

    @After fun teardown() {
        db.close()
    }

    @Test
    fun `save then getAllRecords returns the record`() {
        repository.save(record("1", 100L))

        assertEquals(listOf("1"), repository.getAllRecords(10, 0).map { it.id })
    }

    @Test
    fun `getAllRecords respects limit and offset`() {
        repository.save(record("1", 1L))
        repository.save(record("2", 2L))
        repository.save(record("3", 3L))

        assertEquals(listOf("2", "3"), repository.getAllRecords(2, 1).map { it.id })
    }

    @Test
    fun `purgeOlderThan removes only older records`() {
        repository.save(record("old", 100L))
        repository.save(record("new", 500L))

        assertEquals(1, repository.purgeOlderThan(200L))
        assertEquals(listOf("new"), repository.getAllRecords(10, 0).map { it.id })
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

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.RoomBlockedCallRecordRepositoryTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/RoomBlockedCallRecordRepository.kt app/src/test/java/com/teleshield/app/data/RoomBlockedCallRecordRepositoryTest.kt
git commit -m "feat: add room blocked call record repository"
```

### Task C5: DataStore configuration repository

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/ConfigurationDataSource.kt`
- Create: `app/src/main/java/com/teleshield/app/data/DataStoreSystemConfigurationRepository.kt`
- Create: `app/src/test/java/com/teleshield/app/data/DataStoreSystemConfigurationRepositoryTest.kt`

- [ ] **Step 1: `ConfigurationDataSource.kt`**

```kotlin
package com.teleshield.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.teleshield.domain.ScreeningConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigurationDataSource(
    private val dataStore: DataStore<Preferences>,
) {

    val configuration: Flow<ScreeningConfiguration> = dataStore.data.map { prefs ->
        ScreeningConfiguration(
            masterScreeningEnabled = prefs[KEY_MASTER] ?: DEFAULT_MASTER,
            blockUnknownEnabled = prefs[KEY_BLOCK_UNKNOWN] ?: DEFAULT_BLOCK_UNKNOWN,
            logRetentionDays = prefs[KEY_RETENTION] ?: DEFAULT_RETENTION,
        )
    }

    suspend fun save(config: ScreeningConfiguration) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER] = config.masterScreeningEnabled
            prefs[KEY_BLOCK_UNKNOWN] = config.blockUnknownEnabled
            prefs[KEY_RETENTION] = config.logRetentionDays
        }
    }

    companion object {
        private val KEY_MASTER = booleanPreferencesKey("master_screening_enabled")
        private val KEY_BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_enabled")
        private val KEY_RETENTION = intPreferencesKey("log_retention_days")
        private const val DEFAULT_MASTER = true
        private const val DEFAULT_BLOCK_UNKNOWN = false
        private const val DEFAULT_RETENTION = 30
    }
}
```

- [ ] **Step 2: `DataStoreSystemConfigurationRepository.kt`**

```kotlin
package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class DataStoreSystemConfigurationRepository @Inject constructor(
    private val dataSource: ConfigurationDataSource,
) : SystemConfigurationRepository {

    override fun load(): ScreeningConfiguration =
        runBlocking { dataSource.configuration.first() }

    override fun save(configuration: ScreeningConfiguration) {
        runBlocking { dataSource.save(configuration) }
    }
}
```

- [ ] **Step 3: Write the failing Robolectric test**

```kotlin
package com.teleshield.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.teleshield.domain.ScreeningConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DataStoreSystemConfigurationRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.filesDir, "test-${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After fun teardown() {
        scope.cancel()
    }

    @Test
    fun `load returns defaults when nothing saved`() {
        val repo = DataStoreSystemConfigurationRepository(ConfigurationDataSource(dataStore))

        assertEquals(ScreeningConfiguration(true, false, 30), repo.load())
    }

    @Test
    fun `save then load round-trips`() {
        val repo = DataStoreSystemConfigurationRepository(ConfigurationDataSource(dataStore))
        val config = ScreeningConfiguration(masterScreeningEnabled = false, blockUnknownEnabled = true, logRetentionDays = 90)

        repo.save(config)

        assertEquals(config, repo.load())
    }
}
```

- [ ] **Step 4: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.data.DataStoreSystemConfigurationRepositoryTest"`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/ConfigurationDataSource.kt app/src/main/java/com/teleshield/app/data/DataStoreSystemConfigurationRepository.kt
git commit -m "feat: add datastore configuration repository"

git add app/src/test/java/com/teleshield/app/data/DataStoreSystemConfigurationRepositoryTest.kt
git commit -m "test: add datastore configuration repository test"
```

### Task C6: `NoOpTelephonyInterceptionPort`

**Files:**
- Create: `app/src/main/java/com/teleshield/app/data/NoOpTelephonyInterceptionPort.kt`

- [ ] **Step 1: Implement** (real rejection lands with `CallScreeningService` later):

```kotlin
package com.teleshield.app.data

import com.teleshield.ports.TelephonyInterceptionPort
import javax.inject.Inject

class NoOpTelephonyInterceptionPort @Inject constructor() : TelephonyInterceptionPort {
    override fun reject() = Unit
    override fun suppressNotification() = Unit
    override fun suppressCallLog() = Unit
}
```

- [ ] **Step 2: Verify compile** — `.\gradlew.bat :app:compileDebugKotlin`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/teleshield/app/data/NoOpTelephonyInterceptionPort.kt
git commit -m "feat: add no-op telephony interception port"
```

---

## Part D — DI wiring

### Task D1: Hilt modules

**Files:**
- Create: `app/src/main/java/com/teleshield/app/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/teleshield/app/di/ConfigurationModule.kt`
- Create: `app/src/main/java/com/teleshield/app/di/RepositoryModule.kt`
- Create: `app/src/main/java/com/teleshield/app/di/EngineModule.kt`

- [ ] **Step 1: `DatabaseModule.kt`**

```kotlin
package com.teleshield.app.di

import android.content.Context
import androidx.room.Room
import com.teleshield.app.data.db.BlockedCallRecordDao
import com.teleshield.app.data.db.ScreeningRuleDao
import com.teleshield.app.data.db.TeleShieldDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TeleShieldDatabase =
        Room.databaseBuilder(context, TeleShieldDatabase::class.java, "teleshield.db").build()

    @Provides
    fun provideScreeningRuleDao(db: TeleShieldDatabase): ScreeningRuleDao = db.screeningRuleDao()

    @Provides
    fun provideBlockedCallRecordDao(db: TeleShieldDatabase): BlockedCallRecordDao = db.blockedCallRecordDao()
}
```

- [ ] **Step 2: `ConfigurationModule.kt`**

```kotlin
package com.teleshield.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.teleshield.app.data.ConfigurationDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("teleshield") },
        )

    @Provides
    @Singleton
    fun provideConfigurationDataSource(dataStore: DataStore<Preferences>): ConfigurationDataSource =
        ConfigurationDataSource(dataStore)
}
```

- [ ] **Step 3: `RepositoryModule.kt`** (the adapters already carry `@Inject constructor` from Tasks C3–C6):

```kotlin
package com.teleshield.app.di

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
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindScreeningRuleRepository(impl: RoomScreeningRuleRepository): ScreeningRuleRepository

    @Binds
    abstract fun bindBlockedCallRecordRepository(impl: RoomBlockedCallRecordRepository): BlockedCallRecordRepository

    @Binds
    abstract fun bindSystemConfigurationRepository(impl: DataStoreSystemConfigurationRepository): SystemConfigurationRepository

    @Binds
    abstract fun bindTelephonyInterceptionPort(impl: NoOpTelephonyInterceptionPort): TelephonyInterceptionPort
}
```

- [ ] **Step 4: `EngineModule.kt`** (provides engine + use cases without touching `:engine`):

```kotlin
package com.teleshield.app.di

import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.DeleteRuleUseCase
import com.teleshield.application.PurgeAuditLogsUseCase
import com.teleshield.application.QueryBlockedLogsUseCase
import com.teleshield.application.QueryRulesUseCase
import com.teleshield.application.ScreenIncomingCallUseCase
import com.teleshield.application.SimulateCallUseCase
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import com.teleshield.ports.TelephonyInterceptionPort
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideIdentifierNormalizer(): IdentifierNormalizer = IdentifierNormalizer()

    @Provides
    @Singleton
    fun provideScreeningEngine(normalizer: IdentifierNormalizer): ScreeningEngine = ScreeningEngine(normalizer)

    @Provides
    fun provideAddRuleUseCase(repo: ScreeningRuleRepository): AddRuleUseCase = AddRuleUseCase(repo)

    @Provides
    fun provideQueryRulesUseCase(repo: ScreeningRuleRepository): QueryRulesUseCase = QueryRulesUseCase(repo)

    @Provides
    fun provideDeleteRuleUseCase(repo: ScreeningRuleRepository): DeleteRuleUseCase = DeleteRuleUseCase(repo)

    @Provides
    fun provideScreenIncomingCallUseCase(
        engine: ScreeningEngine,
        normalizer: IdentifierNormalizer,
        rules: ScreeningRuleRepository,
        logs: BlockedCallRecordRepository,
        config: SystemConfigurationRepository,
        telephony: TelephonyInterceptionPort,
    ): ScreenIncomingCallUseCase =
        ScreenIncomingCallUseCase(engine, normalizer, rules, logs, config, telephony)

    @Provides
    fun provideSimulateCallUseCase(
        engine: ScreeningEngine,
        normalizer: IdentifierNormalizer,
        rules: ScreeningRuleRepository,
        config: SystemConfigurationRepository,
    ): SimulateCallUseCase = SimulateCallUseCase(engine, normalizer, rules, config)

    @Provides
    fun providePurgeAuditLogsUseCase(
        config: SystemConfigurationRepository,
        logs: BlockedCallRecordRepository,
    ): PurgeAuditLogsUseCase = PurgeAuditLogsUseCase(config, logs)

    @Provides
    fun provideQueryBlockedLogsUseCase(logs: BlockedCallRecordRepository): QueryBlockedLogsUseCase =
        QueryBlockedLogsUseCase(logs)
}
```

- [ ] **Step 5: Verify compile** — `.\gradlew.bat :app:assembleDebug`. Expected: BUILD SUCCESSFUL (Hilt codegen runs).

- [ ] **Step 6: Commit** (split ≤3 files):

```bash
git add app/src/main/java/com/teleshield/app/di/DatabaseModule.kt app/src/main/java/com/teleshield/app/di/ConfigurationModule.kt
git commit -m "feat: add hilt database and configuration modules"

git add app/src/main/java/com/teleshield/app/di/RepositoryModule.kt
git commit -m "feat: bind ports to adapters"

git add app/src/main/java/com/teleshield/app/di/EngineModule.kt
git commit -m "feat: add hilt engine and use case providers"
```

> Note: Hilt only validates a component when something injects into it. Nothing consumes these bindings until `RulesViewModel` (`@HiltViewModel`) lands in Task E1, so the intermediate D1 commits all compile green.

---

## Part E — Rules UI

### Task E1: `RulesViewModel` (list / add / delete)

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/rules/RulesViewModel.kt`
- Create: `app/src/test/java/com/teleshield/app/ui/rules/RulesViewModelTest.kt`

**Interfaces:**
- Consumes: `QueryRulesUseCase.execute()`, `AddRuleUseCase.execute(AddRuleRequest)`, `DeleteRuleUseCase.execute(id)`.
- Produces: `RulesViewModel.uiState: StateFlow<RulesUiState>`, `RulesUiState(rules: List<ScreeningRule>)`, `refresh()`, `addRule(request)`, `deleteRule(id)`.

- [ ] **Step 1: Implement**

```kotlin
package com.teleshield.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.DeleteRuleUseCase
import com.teleshield.application.QueryRulesUseCase
import com.teleshield.domain.ScreeningRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val queryRules: QueryRulesUseCase,
    private val addRule: AddRuleUseCase,
    private val deleteRule: DeleteRuleUseCase,
) : ViewModel() {

    data class RulesUiState(val rules: List<ScreeningRule> = emptyList())

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val rules = withContext(Dispatchers.IO) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }

    fun addRule(request: AddRuleUseCase.AddRuleRequest) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { addRule.execute(request) }
            val rules = withContext(Dispatchers.IO) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { deleteRule.execute(id) }
            val rules = withContext(Dispatchers.IO) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }
}
```

- [ ] **Step 2: Write the failing test** (uses the engine's `InMemoryScreeningRuleRepository` to back the real use cases — no mocking):

```kotlin
package com.teleshield.app.ui.rules

import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.DeleteRuleUseCase
import com.teleshield.application.QueryRulesUseCase
import com.teleshield.domain.RuleType
import com.teleshield.inmemory.InMemoryScreeningRuleRepository
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
class RulesViewModelTest {

    private val repo = InMemoryScreeningRuleRepository()
    private val query = QueryRulesUseCase(repo)
    private val add = AddRuleUseCase(repo, idGenerator = { "id-1" })
    private val delete = DeleteRuleUseCase(repo)

    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `starts with an empty list`() = runTest {
        val vm = RulesViewModel(query, add, delete)
        advanceUntilIdle()
        assertEquals(emptyList(), vm.uiState.value.rules)
    }

    @Test
    fun `addRule adds a rule and refreshes state`() = runTest {
        val vm = RulesViewModel(query, add, delete)
        advanceUntilIdle()

        vm.addRule(AddRuleUseCase.AddRuleRequest("15551234567", RuleType.EXACT, "block", false))
        advanceUntilIdle()

        assertEquals(listOf("id-1"), vm.uiState.value.rules.map { it.id })
    }

    @Test
    fun `deleteRule removes a rule and refreshes state`() = runTest {
        val vm = RulesViewModel(query, add, delete)
        advanceUntilIdle()

        vm.addRule(AddRuleUseCase.AddRuleRequest("15551234567", RuleType.EXACT, "block", false))
        advanceUntilIdle()
        vm.deleteRule("id-1")
        advanceUntilIdle()

        assertEquals(emptyList(), vm.uiState.value.rules)
    }
}
```

- [ ] **Step 3: Verify green** — `.\gradlew.bat :app:testDebugUnitTest --tests "com.teleshield.app.ui.rules.RulesViewModelTest"`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/teleshield/app/ui/rules/RulesViewModel.kt app/src/test/java/com/teleshield/app/ui/rules/RulesViewModelTest.kt
git commit -m "feat: add rules view model"
```

### Task E2: Compose UI + navigation

**Files:**
- Create: `app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt`
- Create: `app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt`
- Create: `app/src/main/java/com/teleshield/app/ui/navigation/TeleShieldNavHost.kt`
- Modify: `app/src/main/java/com/teleshield/app/MainActivity.kt`

- [ ] **Step 1: `AddRuleDialog.kt`**

```kotlin
package com.teleshield.app.ui.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teleshield.application.AddRuleUseCase
import com.teleshield.domain.RuleType

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (AddRuleUseCase.AddRuleRequest) -> Unit,
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.EXACT) }
    var isWhitelist by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add rule") },
        text = {
            Column {
                OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern") })
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                RuleTypeSelector(selected = type, onSelect = { type = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Whitelist")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = isWhitelist, onCheckedChange = { isWhitelist = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(AddRuleUseCase.AddRuleRequest(pattern, type, label, isWhitelist))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RuleTypeSelector(selected: RuleType, onSelect: (RuleType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(RuleType.EXACT, RuleType.PREFIX, RuleType.WILDCARD, RuleType.REGEX)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
```

- [ ] **Step 2: `RulesScreen.kt`**

```kotlin
package com.teleshield.app.ui.rules

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.ScreeningRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rules") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(uiState.rules, key = { it.id }) { rule ->
                RuleRow(rule = rule, onDelete = { viewModel.deleteRule(rule.id) })
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { request ->
                viewModel.addRule(request)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun RuleRow(rule: ScreeningRule, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(rule.label.ifBlank { rule.pattern.expression }) },
        supportingContent = { Text("${rule.ruleType.name} · ${rule.pattern.expression}") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
    )
}
```

- [ ] **Step 3: `TeleShieldNavHost.kt`**

```kotlin
package com.teleshield.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teleshield.app.ui.rules.RulesScreen

@Composable
fun TeleShieldNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "rules") {
        composable("rules") { RulesScreen() }
    }
}
```

- [ ] **Step 4: Update `MainActivity.kt`** — replace the `Text("TeleShield")` body:

```kotlin
package com.teleshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.teleshield.app.ui.navigation.TeleShieldNavHost
import com.teleshield.app.ui.theme.TeleShieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleShieldTheme {
                TeleShieldNavHost()
            }
        }
    }
}
```

- [ ] **Step 5: Verify** — `.\gradlew.bat :app:assembleDebug` then `.\gradlew.bat :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all unit tests green.

- [ ] **Step 6: Commit** (split ≤3 files):

```bash
git add app/src/main/java/com/teleshield/app/ui/rules/AddRuleDialog.kt app/src/main/java/com/teleshield/app/ui/rules/RulesScreen.kt
git commit -m "feat: add rules screen and add-rule dialog"

git add app/src/main/java/com/teleshield/app/ui/navigation/TeleShieldNavHost.kt app/src/main/java/com/teleshield/app/MainActivity.kt
git commit -m "feat: add navigation and wire rules screen"
```

- [ ] **Step 7: Manual smoke (device/emulator)** — `.\gradlew.bat :app:installDebug`, then verify: open app → Rules screen shows empty; tap FAB → add a rule → it appears; tap delete icon → it disappears; relaunch → rules persist.

---

## Verification (end of slice)

- `.\gradlew.bat :engine:test` — engine suite green.
- `.\gradlew.bat :app:testDebugUnitTest` — app unit/Robolectric suite green.
- `.\gradlew.bat :app:assembleDebug` — debug APK builds.
- Manual: list/add/delete rules persist across relaunch.

## Deferred (later sub-milestones)

Audit log screen · Simulator screen · Settings screen · rule edit · real `TelephonyInterceptionPort` (TelecomManager) · `CallScreeningService` + role request · Compose instrumented UI tests · in-memory rule cache for the ring-time hot path.
