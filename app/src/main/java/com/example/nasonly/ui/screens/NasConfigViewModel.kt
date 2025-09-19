package nasonly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nasonly.data.repository.NasConfig
import nasonly.data.repository.NasRepository
import javax.inject.Inject

@HiltViewModel
class NasConfigViewModel @Inject constructor(
    private val repository: NasRepository
) : ViewModel() {
    // UI状态
    private val _uiState = MutableStateFlow(NasConfigUiState())
    val uiState: StateFlow<NasConfigUiState> = _uiState.asStateFlow()

    // 保存的配置
    private val _savedConfig = MutableStateFlow<NasConfig?>(null)
    val savedConfig = _savedConfig.asStateFlow()

    init {
        // 加载保存的配置
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
        if (ip.isEmpty() || username.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "IP地址和用户名不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                repository.saveNasConfig(
                    NasConfig(ip, port, username, password),
                    saveCredentials
                )
                _savedConfig.value = repository.getSavedNasConfig()
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "配置保存成功"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "保存失败: ${e.message}"
                ) }
            }
        }
    }

    // 测试连接
    fun testConnection(
        ip: String,
        port: Int,
        username: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        if (ip.isEmpty() || username.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "IP地址和用户名不能为空") }
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }
            try {
                val config = NasConfig(ip, port, username, password)
                val success = repository.connectToNas(config)
                if (success) {
                    // 连接成功，保存配置
                    repository.saveNasConfig(config, true)
                    _savedConfig.value = config
                    _uiState.update { it.copy(isLoading = false) }
                    onResult(true)
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "连接失败，请检查配置"
                    ) }
                    onResult(false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "连接错误: ${e.message}"
                ) }
                onResult(false)
            }
        }
    }
}

// UI状态类
data class NasConfigUiState(
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)