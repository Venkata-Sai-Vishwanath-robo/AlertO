# AlertO - Wake Word Detection App

**AlertO** is an Android app designed to help students (and anyone else) who miss important calls for attendance or other notifications while using their phone. The app continuously listens in the background for user-defined "wake words" (like your name or the word "important") and alerts you with vibration and notifications when those words are detected.

## Features

- **Continuous Background Listening**: Runs as a foreground service to monitor audio continuously
- **On-Device Speech Recognition**: Uses Whisper Tiny model via RunAnywhere SDK for 100% private, offline speech-to-text
- **Custom Wake Words**: Add unlimited wake words (your name, "important", "attention", etc.)
- **Multiple Alert Methods**: 
  - Strong vibration pattern (500ms-200ms-500ms)
  - High-priority notification with detected words
- **Battery Optimization Handling**: Prompts user to disable battery optimization for reliable background operation
- **Word Boundary Detection**: Smart matching that only triggers on complete word matches (won't trigger "John" when hearing "Johnny")
- **Enable/Disable Wake Words**: Toggle individual wake words on/off without deleting them

## Architecture

### Core Components

#### 1. **AlertOService** (`services/AlertOService.kt`)
- Foreground service that handles continuous audio monitoring
- Captures 3-second audio chunks using Android's `AudioRecord` API
- Transcribes audio using RunAnywhere's Whisper STT model
- Detects wake words using regex word-boundary matching
- Triggers alerts (vibration + notification) when wake words are detected
- Maintains partial wake lock for reliable background operation

#### 2. **WakeWordRepository** (`data/WakeWordRepository.kt`)
- Manages wake word storage using AndroidX DataStore
- Provides Flow-based reactive data access
- Handles CRUD operations: add, remove, toggle wake words
- Uses kotlinx.serialization for JSON persistence
- Validates for duplicates and empty values

#### 3. **AlertOScreen** (`ui/screens/AlertOScreen.kt`)
- Main UI for managing wake words and controlling the listening service
- Material 3 design with Jetpack Compose
- Shows STT model status with download/load capability
- Service control with start/stop buttons
- Add wake word input with validation
- Scrollable list of wake words with toggle and delete actions

#### 4. **AlertOViewModel** (`ui/screens/AlertOViewModel.kt`)
- Manages UI state and business logic
- Handles permission requests (microphone, notifications)
- Controls service lifecycle
- Checks and prompts for battery optimization settings
- Exposes wake words as StateFlow for reactive UI updates

### Data Flow

```
User Speaks → AudioRecord → 3-second chunks → Whisper STT → Text
                                                              ↓
                                              Wake Word Detection (Regex)
                                                              ↓
                                              Vibration + Notification
```

## Setup & Usage

### 1. Load the STT Model

Before using AlertO, you need to load the Whisper STT model:

1. Navigate to the **Speech-to-Text** screen from the home screen
2. Tap **"Load Model"** (downloads ~75 MB Whisper Tiny model)
3. Wait for download and loading to complete
4. Return to AlertO screen

Alternatively, load the model directly from AlertO screen using the **"Load Model"** button in the model status card.

### 2. Add Wake Words

1. Open the **AlertO** screen from the home menu
2. In the "Add Wake Word" section, type a wake word (e.g., your name, "important", "attention")
3. Tap the **+** button or press Done on the keyboard
4. The wake word appears in the list below
5. Add as many wake words as you need

**Tips:**
- Use lowercase for consistency
- Use single words or short phrases
- Common examples: your name, "important", "urgent", "attendance", "roll call"

### 3. Start Listening

1. Ensure the STT model is loaded (green card shows "Ready to detect wake words")
2. Tap **"Start Listening"** button
3. Grant microphone and notification permissions when prompted
4. If prompted, disable battery optimization for reliable background operation
5. A persistent notification appears: "AlertO is listening"
6. The service continues running even if you minimize the app or lock your screen

### 4. When a Wake Word is Detected

- Your phone vibrates with a strong pattern (500ms - pause - 500ms)
- A high-priority notification appears showing:
  - "Wake Word Detected!"
  - Which wake words were heard
  - Full transcribed text for context
- Tap the notification to open AlertO

### 5. Stop Listening

1. Open AlertO screen or tap the persistent notification
2. Tap **"Stop Listening"** button
3. Service stops and notifications disappear

## Permissions

AlertO requires the following permissions:

- **RECORD_AUDIO**: To capture audio for speech recognition
- **POST_NOTIFICATIONS**: To show alert notifications (Android 13+)
- **FOREGROUND_SERVICE**: To run continuously in the background
- **FOREGROUND_SERVICE_MICROPHONE**: To access microphone while in background
- **VIBRATE**: To vibrate the device for alerts
- **WAKE_LOCK**: To keep processing audio even when screen is off

All permissions are requested at runtime when you tap "Start Listening".

## Battery Optimization

For AlertO to work reliably in the background, you should disable battery optimization:

1. When starting the service, AlertO checks battery optimization status
2. If enabled, a dialog prompts you to disable it
3. Tap **"Open Settings"** to go directly to battery settings
4. Select "Don't optimize" or "Allow" for AlertO
5. Return to AlertO and start listening again

**Note**: This is necessary because Android aggressively kills background services on battery-optimized apps.

## Technical Details

### Audio Processing

- **Sample Rate**: 16kHz (optimal for Whisper)
- **Format**: PCM 16-bit mono
- **Chunk Size**: 3 seconds per transcription
- **Continuous**: Overlapping chunks ensure no speech is missed

### Wake Word Detection

- Case-insensitive matching
- Word boundary detection using regex: `\b{word}\b`
- Handles multiple wake words in a single transcription
- Shows all detected words in the notification

### Background Service

- Runs as a **Foreground Service** with persistent notification
- Service type: `microphone` (Android 14+ requirement)
- Survives app closure and screen lock
- Automatically restarts if killed by system (START_STICKY)

### Privacy

- **100% On-Device Processing**: All speech recognition happens locally using Whisper
- **No Network Access Required**: After model download, works completely offline
- **No Data Collection**: Audio is processed in real-time and not stored anywhere
- **No Third-Party Services**: Direct integration with RunAnywhere SDK

## Code Structure

```
app/src/main/java/com/runanywhere/kotlin_starter_example/
├── data/
│   ├── WakeWord.kt              # Wake word data model
│   └── WakeWordRepository.kt    # DataStore persistence layer
├── services/
│   ├── AlertOService.kt         # Background listening service
│   └── ModelService.kt          # AI model management (existing)
└── ui/screens/
    ├── AlertOScreen.kt          # Main UI screen
    ├── AlertOViewModel.kt       # UI state management
    └── AlertOViewModelFactory.kt # ViewModel factory
```

## Dependencies

- **RunAnywhere SDK**: On-device AI inference
  - `runanywhere-kotlin`: Core SDK
  - `runanywhere-onnx`: Whisper STT backend
- **Jetpack Compose**: Modern UI framework
- **AndroidX DataStore**: Persistent key-value storage
- **Kotlinx Serialization**: JSON serialization for wake words
- **Coroutines**: Asynchronous operations

## Building the App

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Build and install
./gradlew clean assembleDebug installDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Service Stops After Some Time
- Disable battery optimization for AlertO
- Check that microphone permission is granted
- Ensure STT model is loaded
- Check for memory constraints (Whisper requires ~75 MB)

### Wake Words Not Detected
- Speak clearly and at normal volume
- Ensure wake word is spelled correctly (lowercase)
- Check that wake word is enabled (toggle is on)
- Test wake word pronunciation (STT may transcribe differently)
- Try the Speech-to-Text screen to test transcription accuracy

### Notification Not Showing
- Grant POST_NOTIFICATIONS permission (Android 13+)
- Check notification settings for AlertO in system settings
- Ensure notifications are not muted/blocked

### High Battery Usage
- This is expected for continuous audio monitoring
- Reduce usage by stopping the service when not needed
- Consider using only during specific times (e.g., class hours)

### Model Download Fails
- Check internet connection
- Verify sufficient storage space (~75 MB for STT model)
- Try downloading again from Speech-to-Text screen

## Performance

### Battery Impact
- Continuous audio recording: ~5-10% per hour
- Whisper inference: ~2-5% per hour
- Total: ~7-15% per hour depending on device

### Memory Usage
- Base app: ~50 MB
- Whisper Tiny model: ~75 MB
- Total: ~125 MB RAM

### Latency
- Audio chunk: 3 seconds
- Transcription: ~0.5-2 seconds (device dependent)
- Total delay: ~3.5-5 seconds from speech to alert

## Future Enhancements

Possible improvements:
- [ ] Adjustable audio chunk duration (1-5 seconds)
- [ ] Confidence threshold for wake word detection
- [ ] Custom vibration patterns per wake word
- [ ] Different notification sounds per wake word
- [ ] Schedule listening hours (auto-start/stop)
- [ ] Wake word usage statistics
- [ ] Export/import wake word lists
- [ ] Voice training for better accuracy
- [ ] Multiple language support

## License

This app is built on top of the RunAnywhere Kotlin SDK Starter. See the LICENSE file for details.

## Credits

- **RunAnywhere SDK**: On-device AI inference framework
- **Whisper**: OpenAI's speech recognition model
- **Jetpack Compose**: Android's modern UI toolkit

---

**Built with RunAnywhere SDK v0.16.0-test.39**

For issues and questions:
- GitHub Issues: [runanywhere-sdks/issues](https://github.com/RunanywhereAI/runanywhere-sdks/issues)
- Documentation: [RunAnywhere Docs](https://github.com/RunanywhereAI/runanywhere-sdks)
