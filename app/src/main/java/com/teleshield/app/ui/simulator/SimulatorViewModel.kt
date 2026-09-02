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
