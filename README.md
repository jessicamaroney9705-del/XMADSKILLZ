# X MADSKILLZ

An advanced Android application featuring voice activation and floating bubble overlay capabilities.

## Features

- 🎤 **Voice Activation Engine** - Continuous background voice listening
- 🫧 **Floating Bubble UI** - Customizable overlay interface
- 📱 **Android 8.0+** - Supports API 26 and above
- 🔒 **Secure Permissions** - Proper permission handling for microphone and overlay

## Requirements

- Android Studio 2023.1 or later
- Kotlin 1.9.10+
- Android SDK 34 (Compile)
- Android 8.0+ (Runtime)

## Permissions

- `RECORD_AUDIO` - Microphone access for voice activation
- `SYSTEM_ALERT_WINDOW` - Draw overlay bubbles
- `FOREGROUND_SERVICE` - Background service execution
- `FOREGROUND_SERVICE_MICROPHONE` - Audio foreground service type
- `POST_NOTIFICATIONS` - Notifications (Android 13+)

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew installDebug
```

## Project Structure

```
app/
├── src/main/
│   ├── kotlin/com/kbko/xmadskillz/
│   │   ├── MainActivity.kt
│   │   └── services/
│   │       ├── VoiceActivationService.kt
│   │       └── FloatingBubbleService.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   └── mipmap/
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## License

MIT License - See LICENSE file for details
