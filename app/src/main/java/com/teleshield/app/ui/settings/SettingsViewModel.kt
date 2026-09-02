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
        viewModelScope.launch {
            val updated = withContext(ioDispatcher) {
                val latest = _uiState.value.config ?: return@withContext null
                transform(latest).also { configurationRepository.save(it) }
            } ?: return@launch
            _uiState.value = SettingsUiState(updated)
        }
    }
}
