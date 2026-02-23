package com.runanywhere.kotlin_starter_example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.kotlin_starter_example.data.WakeWord
import com.runanywhere.kotlin_starter_example.data.WakeWordRepository
import com.runanywhere.kotlin_starter_example.services.AlertOService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for AlertO screen
 */
class AlertOViewModel(
    private val context: Context,
    private val repository: WakeWordRepository
) : ViewModel() {
    
    val wakeWords: StateFlow<List<WakeWord>> = repository.wakeWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    var isServiceRunning by mutableStateOf(false)
        private set
    
    var newWakeWord by mutableStateOf("")
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var showBatteryOptimizationDialog by mutableStateOf(false)
        private set
    
    fun updateNewWakeWord(word: String) {
        newWakeWord = word
        errorMessage = null
    }
    
    fun addWakeWord() {
        if (newWakeWord.isBlank()) {
            errorMessage = "Please enter a wake word"
            return
        }
        
        viewModelScope.launch {
            repository.addWakeWord(newWakeWord).fold(
                onSuccess = {
                    newWakeWord = ""
                    errorMessage = null
                },
                onFailure = { error ->
                    errorMessage = error.message
                }
            )
        }
    }
    
    fun removeWakeWord(id: String) {
        viewModelScope.launch {
            repository.removeWakeWord(id)
        }
    }
    
    fun toggleWakeWord(id: String) {
        viewModelScope.launch {
            repository.toggleWakeWord(id)
        }
    }
    
    fun startListening() {
        // Check battery optimization
        if (!isBatteryOptimizationDisabled(context)) {
            showBatteryOptimizationDialog = true
            return
        }
        
        AlertOService.startService(context)
        isServiceRunning = true
    }
    
    fun stopListening() {
        AlertOService.stopService(context)
        isServiceRunning = false
    }
    
    fun dismissError() {
        errorMessage = null
    }
    
    fun dismissBatteryDialog() {
        showBatteryOptimizationDialog = false
    }
    
    fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general battery optimization settings
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
        showBatteryOptimizationDialog = false
    }
    
    private fun isBatteryOptimizationDisabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }
}
