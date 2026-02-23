package com.runanywhere.kotlin_starter_example.data

/**
 * Represents a wake word that the app should listen for
 */
data class WakeWord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
