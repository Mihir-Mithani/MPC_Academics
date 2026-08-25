# MPC Android Experiments - Multi-Module Project

This project contains all 10 Android experiments as Gradle modules in a single multi-module project.

## Project Structure

```
Experiments/
├── build.gradle.kts              # Root build configuration (shared settings)
├── settings.gradle.kts           # Module inclusion + version catalog
├── gradle.properties             # Gradle performance settings
├── gradle/
│   └── libs.versions.toml        # Version catalog (TOML format)
├── Experiment1_HelloWorld/       # Experiment 1: Hello World
├── Experiment6_QuoteApp/         # Experiment 6: HTTP Quote App
├── Experiment7_SMSAutoReply/     # Experiment 7: SMS Auto-Reply
├── Experiment8_BluetoothScanner/ # Experiment 8: Bluetooth Scanner
├── Experiment9_BluetoothChat/    # Experiment 9: Bluetooth Chat
├── Experiment10_WiFiSignal/      # Experiment 10: WiFi Signal Strength
├── Experiment11_GPSMaps/         # Experiment 11: GPS on Google Maps
├── Experiment12_SensorDemo/      # Experiment 12: Sensor Demo
├── Experiment13_FirebaseChat/    # Experiment 13: Firebase Chat
└── Experiment14_MLKitFaceDetection/ # Experiment 14: ML Kit Face Detection
```

## Opening in Android Studio

1. **Open the parent folder**: `File → Open → Select the `Experiments` folder`
2. **Wait for Gradle Sync** - All 10 modules will be loaded
3. **Run any experiment** - Select the module in the run configuration dropdown

## Module Configuration

Each module has:
- `build.gradle.kts` - Module-specific dependencies (using version catalog)
- `src/main/` - Source code (MainActivity.kt, etc.)
- `AndroidManifest.xml` - Permissions & configuration

## Version Catalog

All dependency versions are centralized in `gradle/libs.versions.toml`. To update a library version, edit this file.

## Experiments Requiring Additional Setup

| Experiment | Setup Required |
|------------|----------------|
| **11 - GPS Maps** | Add Google Maps API key in `Experiment11_GPSMaps/AndroidManifest.xml` |
| **13 - Firebase Chat** | Replace `google-services.json` in `Experiment13_FirebaseChat/` with your Firebase config |
| **14 - ML Kit Face Detection** | Requires physical device with camera (emulator limited) |

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :Experiment1_HelloWorld:assembleDebug

# Run tests for all modules
./gradlew test

# Clean build
./gradlew clean
```

## Gradle Performance

The `gradle.properties` includes optimized settings:
- Parallel builds enabled
- Configuration cache enabled
- Increased JVM heap (4GB)
- R8 full mode enabled

## Notes

- Experiments 2-5 were blank in the original PDF
- Each module is a complete, runnable Android application
- All use Jetpack Compose + Material3 + Kotlin
- Minimum SDK: 24 (Android 7.0)
- Target/Compile SDK: 34 (Android 14)