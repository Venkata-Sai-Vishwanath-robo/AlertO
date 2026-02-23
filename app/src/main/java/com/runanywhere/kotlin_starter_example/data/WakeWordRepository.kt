package com.runanywhere.kotlin_starter_example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alerto_prefs")

@Serializable
data class SerializableWakeWord(
    val id: String,
    val word: String,
    val isEnabled: Boolean,
    val createdAt: Long
)

/**
 * Repository for managing wake words using DataStore
 */
class WakeWordRepository(private val context: Context) {
    
    companion object {
        private val WAKE_WORDS_KEY = stringPreferencesKey("wake_words")
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Get all wake words as a Flow
     */
    val wakeWords: Flow<List<WakeWord>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[WAKE_WORDS_KEY] ?: "[]"
            try {
                val serializableWords = json.decodeFromString<List<SerializableWakeWord>>(jsonString)
                serializableWords.map { it.toWakeWord() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    
    /**
     * Add a new wake word
     */
    suspend fun addWakeWord(word: String): Result<WakeWord> {
        if (word.isBlank()) {
            return Result.failure(IllegalArgumentException("Wake word cannot be empty"))
        }
        
        val newWord = WakeWord(word = word.trim().lowercase())
        
        // Check for duplicates
        val currentWords = wakeWords.first()
        if (currentWords.any { it.word.equals(newWord.word, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Wake word already exists"))
        }
        
        context.dataStore.edit { preferences ->
            val currentJson = preferences[WAKE_WORDS_KEY] ?: "[]"
            val existingWords = try {
                json.decodeFromString<List<SerializableWakeWord>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updatedWords = existingWords + newWord.toSerializable()
            preferences[WAKE_WORDS_KEY] = json.encodeToString(updatedWords)
        }
        
        return Result.success(newWord)
    }
    
    /**
     * Remove a wake word by ID
     */
    suspend fun removeWakeWord(id: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[WAKE_WORDS_KEY] ?: "[]"
            val currentWords = try {
                json.decodeFromString<List<SerializableWakeWord>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updatedWords = currentWords.filter { it.id != id }
            preferences[WAKE_WORDS_KEY] = json.encodeToString(updatedWords)
        }
    }
    
    /**
     * Toggle wake word enabled state
     */
    suspend fun toggleWakeWord(id: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[WAKE_WORDS_KEY] ?: "[]"
            val currentWords = try {
                json.decodeFromString<List<SerializableWakeWord>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updatedWords = currentWords.map { word ->
                if (word.id == id) {
                    word.copy(isEnabled = !word.isEnabled)
                } else {
                    word
                }
            }
            preferences[WAKE_WORDS_KEY] = json.encodeToString(updatedWords)
        }
    }
    
    /**
     * Clear all wake words
     */
    suspend fun clearAllWakeWords() {
        context.dataStore.edit { preferences ->
            preferences[WAKE_WORDS_KEY] = "[]"
        }
    }
}

private fun WakeWord.toSerializable() = SerializableWakeWord(
    id = id,
    word = word,
    isEnabled = isEnabled,
    createdAt = createdAt
)

private fun SerializableWakeWord.toWakeWord() = WakeWord(
    id = id,
    word = word,
    isEnabled = isEnabled,
    createdAt = createdAt
)
