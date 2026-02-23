# AlertO - Quick Start Guide

## What is AlertO?

AlertO helps you never miss important calls during attendance or meetings. Just add your name (or other important keywords) as wake words, and the app will alert you with vibration and notifications when those words are spoken nearby.

## 🚀 Getting Started (3 Minutes)

### Step 1: Build & Install (1 min)
```bash
# Build and install the app
./gradlew installDebug

# Or just build the APK
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Load the Speech Model (1 min)
1. Launch the app
2. Tap **"Speech"** card from home
3. Tap **"Load Model"** button
4. Wait for ~75 MB download
5. Model loads automatically
6. Press back arrow

### Step 3: Set Up AlertO (1 min)
1. From home screen, tap **"AlertO"** card (red)
2. Type your name in the text field
3. Tap the **+** button
4. Add more wake words: "important", "attention", etc.
5. Tap **"Start Listening"**
6. Grant microphone permission → **Allow**
7. Grant notification permission → **Allow** (Android 13+)
8. Battery optimization prompt → **Open Settings** → **Don't optimize**

### Step 4: Test It! (30 seconds)
1. With AlertO listening (green card shows "Listening Active")
2. Speak one of your wake words clearly
3. Wait 3-5 seconds
4. Feel vibration + see notification!
5. Tap notification to return to app

## ✅ You're All Set!

The app now runs in the background monitoring for your wake words. You can:
- Minimize the app - keeps working ✓
- Lock the screen - keeps working ✓
- Use other apps - keeps working ✓
- Toggle wake words on/off
- Delete wake words
- Stop/restart listening anytime

## 📱 How to Use During Class

**Before Class:**
1. Open AlertO
2. Add your name as wake word
3. Add "important", "urgent" if needed
4. Tap "Start Listening"
5. Minimize app

**During Class:**
- Use your phone normally
- When your name is called → vibration + notification
- Check notification for context
- Never miss attendance again!

**After Class:**
- Open AlertO
- Tap "Stop Listening" to save battery

## 🔧 Common Issues

### "Model not loaded" error
**Fix:** Go to Speech-to-Text screen → Tap "Load Model"

### Service stops after screen lock
**Fix:** Disable battery optimization (prompt appears automatically)

### No vibration when wake word spoken
**Check:** 
- Phone not on silent mode
- Wake word is enabled (toggle is blue)
- Speaking clearly and loudly enough

### No notification appears
**Fix:** Grant notification permission in Android settings

## 📊 Performance

- **Battery**: ~10% per hour (continuous monitoring)
- **Latency**: 3-5 seconds from speech to alert
- **Accuracy**: Depends on speech clarity and pronunciation

## 🎯 Pro Tips

1. **Test your wake words** using Speech-to-Text screen to see how they're transcribed
2. **Use simple, clear words** like your first name rather than full name
3. **Only run during needed hours** to save battery
4. **Keep volume up** for better detection in noisy environments
5. **Disable wake words** you're not using instead of deleting them

## 📚 Full Documentation

- **User Guide**: `ALERTO_README.md`
- **Technical Details**: `IMPLEMENTATION_SUMMARY.md`
- **Testing Guide**: `CHECKLIST.md`
- **Project Structure**: `PROJECT_STRUCTURE.md`

## 🎉 That's It!

You're ready to never miss an attendance call again. Enjoy AlertO!

---

**Questions or Issues?**
- Check the documentation files above
- Review the troubleshooting section in `ALERTO_README.md`
- Test wake word transcription in Speech-to-Text screen
