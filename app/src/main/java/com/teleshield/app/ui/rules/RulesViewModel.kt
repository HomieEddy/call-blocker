package com.teleshield.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.DeleteRuleUseCase
import com.teleshield.application.QueryRulesUseCase
import com.teleshield.app.di.IoDispatcher
import com.teleshield.domain.ScreeningRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    data class RulesUiState(val rules: List<ScreeningRule> = emptyList())

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val rules = withContext(ioDispatcher) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }

    fun addRule(request: AddRuleUseCase.AddRuleRequest) {
        viewModelScope.launch {
            withContext(ioDispatcher) { addRule.execute(request) }
            val rules = withContext(ioDispatcher) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) { deleteRule.execute(id) }
            val rules = withContext(ioDispatcher) { queryRules.execute() }
            _uiState.value = RulesUiState(rules)
        }
    }
}
