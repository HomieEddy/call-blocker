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
