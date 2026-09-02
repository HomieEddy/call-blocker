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
