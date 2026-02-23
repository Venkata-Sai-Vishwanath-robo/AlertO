package com.runanywhere.kotlin_starter_example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.runanywhere.kotlin_starter_example.data.WakeWordRepository

class AlertOViewModelFactory(
    private val context: Context,
    private val repository: WakeWordRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertOViewModel::class.java)) {
            return AlertOViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
