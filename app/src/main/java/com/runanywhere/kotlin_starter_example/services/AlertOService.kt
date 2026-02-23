package com.runanywhere.kotlin_starter_example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.runanywhere.kotlin_starter_example.MainActivity
import com.runanywhere.kotlin_starter_example.R
import com.runanywhere.kotlin_starter_example.data.WakeWordRepository
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/**
 * Background service that continuously listens for wake words using Whisper STT
 */
class AlertOService : Service() {
    
    companion object {
        private const val TAG = "AlertOService"
        private const val NOTIFICATION_CHANNEL_ID = "alerto_service"
        private const val NOTIFICATION_CHANNEL_NAME = "AlertO Listening Service"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_DURATION_SECONDS = 3 // 3 seconds of audio for each transcription
        
        const val ACTION_START_LISTENING = "com.runanywhere.alerto.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.runanywhere.alerto.STOP_LISTENING"
        
        fun startService(context: Context) {
            val intent = Intent(context, AlertOService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AlertOService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listeningJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private lateinit var wakeWordRepository: WakeWordRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var vibrator: Vibrator
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private var isListening = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlertOService created")
        
        wakeWordRepository = WakeWordRepository(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlertO::ListeningWakeLock"
        )
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification())
                startListening()
            }
            ACTION_STOP_LISTENING -> {
                stopListening()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlertOService destroyed")
        stopListening()
        serviceScope.cancel()
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Service notification channel (low priority)
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when AlertO is listening for wake words"
                setShowBadge(false)
            }
            
            // Alert notification channel (high priority for heads-up)
            val alertChannel = NotificationChannel(
                "alerto_alerts",
                "AlertO Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when your alert words are detected"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AlertO is listening")
            .setContentText("Monitoring for your alert words")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening")
            return
        }
        
        isListening = true
        wakeLock.acquire(10 * 60 * 60 * 1000L) // 10 hours max
        
        listeningJob = serviceScope.launch {
            try {
                Log.d(TAG, "Starting continuous listening")
                continuousListen()
            } catch (e: Exception) {
                Log.e(TAG, "Error in listening loop", e)
                isListening = false
            }
        }
    }
    
    private fun stopListening() {
        Log.d(TAG, "Stopping listening")
        isListening = false
        listeningJob?.cancel()
        listeningJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    private suspend fun continuousListen() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * 2
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }
            
            audioRecord?.startRecording()
            Log.d(TAG, "AudioRecord started")
            
            val samplesPerBuffer = SAMPLE_RATE * BUFFER_DURATION_SECONDS
            val audioBuffer = ShortArray(samplesPerBuffer)
            var currentPosition = 0
            
            while (coroutineContext.isActive && isListening) {
                val readSize = minOf(bufferSize / 2, samplesPerBuffer - currentPosition)
                val read = audioRecord?.read(
                    audioBuffer,
                    currentPosition,
                    readSize
                ) ?: -1
                
                if (read > 0) {
                    currentPosition += read
                    
                    // When we have enough audio (3 seconds), process it
                    if (currentPosition >= samplesPerBuffer) {
                        // Convert short array to byte array for Whisper
                        val audioData = convertToByteArray(audioBuffer)
                        
                        // Process in background
                        processAudioChunk(audioData)
                        
                        // Reset buffer
                        currentPosition = 0
                    }
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord ERROR_INVALID_OPERATION")
                    delay(100)
                } else if (read == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord ERROR_BAD_VALUE")
                    delay(100)
                }
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in continuous listening", e)
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }
    
    private fun convertToByteArray(shortArray: ShortArray): ByteArray {
        val byteArray = ByteArray(shortArray.size * 2)
        for (i in shortArray.indices) {
            val value = shortArray[i]
            byteArray[i * 2] = (value.toInt() and 0xFF).toByte()
            byteArray[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
        }
        return byteArray
    }
    
    private suspend fun processAudioChunk(audioData: ByteArray) {
        try {
            // Get current wake words
            val wakeWords = wakeWordRepository.wakeWords.first()
            val enabledWakeWords = wakeWords.filter { it.isEnabled }
            
            if (enabledWakeWords.isEmpty()) {
                Log.d(TAG, "No enabled wake words")
                return
            }
            
            // Transcribe audio using Whisper
            val transcription = withContext(Dispatchers.IO) {
                try {
                    RunAnywhere.transcribe(audioData)
                } catch (e: Exception) {
                    Log.e(TAG, "Transcription error", e)
                    null
                }
            }
            
            if (transcription.isNullOrBlank()) {
                return
            }
            
            Log.d(TAG, "Transcription: $transcription")
            
            // Check for wake words (case-insensitive, word boundary aware)
            val lowerTranscription = transcription.lowercase()
            val detectedWords = mutableListOf<String>()
            
            for (wakeWord in enabledWakeWords) {
                val pattern = "\\b${Regex.escape(wakeWord.word)}\\b".toRegex()
                if (pattern.containsMatchIn(lowerTranscription)) {
                    detectedWords.add(wakeWord.word)
                    Log.d(TAG, "Wake word detected: ${wakeWord.word}")
                }
            }
            
            // Trigger alert if any wake word was detected
            if (detectedWords.isNotEmpty()) {
                triggerAlert(detectedWords, transcription)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio chunk", e)
        }
    }
    
    private fun triggerAlert(detectedWords: List<String>, fullText: String) {
        Log.d(TAG, "Triggering alert for: $detectedWords")
        
        // Vibrate
        vibrateAlert()
        
        // Show notification
        showAlertNotification(detectedWords, fullText)
    }
    
    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms
                val pattern = longArrayOf(0, 500, 200, 500)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e)
        }
    }
    
    private fun showAlertNotification(detectedWords: List<String>, fullText: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            ALERT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val wordsText = detectedWords.joinToString(", ")
        
        // Use high-priority notification channel for heads-up display
        val notification = NotificationCompat.Builder(this, "alerto_alerts")
            .setContentTitle("🔔 Alert Word Detected!")
            .setContentText("Heard: $wordsText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Alert words: $wordsText\n\nFull conversation: $fullText"))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Alarm category for heads-up
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibrate, lights
            .setFullScreenIntent(pendingIntent, true) // Full-screen on older devices
            .build()
        
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}
