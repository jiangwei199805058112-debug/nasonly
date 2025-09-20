package com.example.nasonly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.nasonly.data.repository.NasConfig
import com.example.nasonly.data.repository.NasRepository
import javax.inject.Inject

@HiltViewModel
class NasConfigViewModel @Inject constructor(
    private val repository: NasRepository
) : ViewModel() {
    // UI 状态
    private val _uiState = MutableStateFlow(NasConfigUiState())
    val uiState: StateFlow<NasConfigUiState> = _uiState.asStateFlow()

    // 保存的配置
    private val _savedConfig = MutableStateFlow<NasConfig?>(null)
    val savedConfig: StateFlow<NasConfig?> = _savedConfig.asStateFlow()

    init {
        // 初始化时加载保存的配置
        viewModelScope.launch {
            _savedConfig.value = repository.getSavedNasConfig()
        }
    }

    // 保存配置
    fun saveConfig(
        ip: String,
        port: Int,
        username: String,
        password: String,
        saveCredentials: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val config = NasConfig(ip, port, username, password, saveCredentials)
                repository.saveNasConfig(config)
                _savedConfig.value = config
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }
}

data class NasConfigUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
