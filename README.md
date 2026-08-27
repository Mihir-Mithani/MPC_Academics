# MPC_Academics

A comprehensive repository containing academic materials for the **Mobile and Pervasive Computing (MPC)** course (Semester 7). This collection includes unit notes, assignments, question banks, previous year papers, teaching schemes, and **14 complete Android experiments** implemented as a multi-module Gradle project — organized for easy reference, study, and hands-on practice.

---

## 📚 Repository Contents

### 📁 Root Directory Structure

```
MPC_Academics/
├── README.md                              # This file
├── LICENSE                                # MIT License
├── CODE_OF_CONDUCT.md                     # Contributor Covenant Code of Conduct
├── CONTRIBUTING.md                        # Contribution guidelines
├── SECURITY.md                            # Security policy
├── .gitattributes                         # Git LFS configuration for zip files
├── Experiments.zip                        # Archived experiments (Git LFS)
├── Assignment/                            # Course assignments & submissions
├── Experiments/                           # Android experiments (multi-module Gradle project)
├── Git Files/                             # GitHub community files
└── Study Material/                        # Course syllabus, units & teaching scheme
```

---

### 📖 Study Material

| File | Description | Format | Size |
|------|-------------|--------|------|
| `01CT0716_Mobileandpervasivecomputingpdf__2026_06_24_14_06_27.pdf` | Course syllabus and overview | PDF | 244 KB |
| `TeachingSchemepdf__2026_06_24_14_16_20.pdf` | Teaching scheme and course structure | PDF | 571 KB |
| `Unit_1pdf__2026_06_17_13_56_11.pdf` | Unit 1: Introduction to Mobile Computing, Wireless Communication Fundamentals | PDF | 2.4 MB |
| `Unit_2pdf__2026_06_24_14_11_32.pdf` | Unit 2: Mobile IP, Wireless LAN, Bluetooth, Mobile Transport Layer | PDF | 848 KB |
| `Chapter3finalpdf__2026_07_01_11_03_05.pdf` | Unit 3: Cellular Concepts, Frequency Reuse, Handoff Strategies | PDF | 571 KB |
| `Unit3_Cellular_Concept_Answers.md.pdf` | Answers for Unit 3 Cellular Concept questions | PDF | 421 KB |

---

### 📝 Assignments

| Assignment | File | Description | Format | Size |
|------------|------|-------------|--------|------|
| **Assignment 1** | `Assignment_1docx__2026_06_24_14_04_52.docx` | Assignment 1 questions (editable) | DOCX | 71 KB |
| **Assignment 2** | `Assignment_2.docx` | Assignment 2 questions (editable) | DOCX | 310 KB |
| **Assignment 2** | `Assignment_2.pdf` | Assignment 2 questions (reference) | PDF | 288 KB |
| **Assignment 2 (alt)** | `Assignment_2docx__2026_06_24_14_13_14.docx` | Alternative version of Assignment 2 | DOCX | 71 KB |

---

### ❓ Question Banks & Practice

| File | Description | Format | Size |
|------|-------------|--------|------|
| `Questionbank_MPCpdf__2026_07_01_08_46_05.pdf` | Comprehensive question bank for MPC | PDF | 204 KB |
| `Previous_Year_Paper.pdf` | Previous year examination paper | PDF | 216 KB |
| `Answers1.pdf` | Answer key for practice questions | PDF | 7.0 MB |

---

### 👨‍🎓 Student Submissions

| File | Description | Format | Size |
|------|-------------|--------|------|
| `Mihir_Mithani_(2301733025.docx` | Student assignment submission (editable) | DOCX | 561 KB |
| `Mihir_Mithani_(2301733025.pdf` | Student assignment submission (reference) | PDF | 589 KB |

---

## 📱 Experiments — Android Multi-Module Project

The `Experiments/` directory contains a **complete multi-module Android project** with 14 experiments, each as an independent Gradle module. Built with **Kotlin**, **Jetpack Compose**, **Material3**, and modern Android development practices.

### Project Architecture

```
Experiments/
├── build.gradle.kts              # Root build configuration (shared settings)
├── settings.gradle.kts           # Module inclusion + version catalog
├── gradle.properties             # Gradle performance settings
├── gradle/
│   └── libs.versions.toml        # Version catalog (TOML format)
├── gradlew / gradlew.bat         # Gradle wrapper scripts
├── local.properties              # Local SDK path (gitignored)
├── Experiment1_HelloWorld/       # Experiment 1: Hello World
├── Experiment2_LoginActivity/    # Experiment 2: Login Activity
├── Experiment3_Calculator/       # Experiment 3: Calculator
├── Experiment4_MultimediaPlayer/ # Experiment 4: Multimedia Player
├── Experiment5_DataPersistence/  # Experiment 5: Data Persistence (Room)
├── Experiment6_QuoteApp/         # Experiment 6: HTTP Quote App (Retrofit)
├── Experiment7_SMSAutoReply/     # Experiment 7: SMS Auto-Reply
├── Experiment8_BluetoothScanner/ # Experiment 8: Bluetooth Scanner
├── Experiment9_BluetoothChat/    # Experiment 9: Bluetooth Chat
├── Experiment10_WiFiSignal/      # Experiment 10: WiFi Signal Strength
├── Experiment11_GPSMaps/         # Experiment 11: GPS on Google Maps
├── Experiment12_SensorDemo/      # Experiment 12: Sensor Demo
├── Experiment13_FirebaseChat/    # Experiment 13: Firebase Chat
└── Experiment14_MLKitFaceDetection/ # Experiment 14: ML Kit Face Detection
```

### Experiment Details

| # | Experiment | Key Technologies | Special Setup |
|---|------------|------------------|---------------|
| 1 | **Hello World** | Compose, Material3 | — |
| 2 | **Login Activity** | Compose, State management | — |
| 3 | **Calculator** | Compose, UI logic | — |
| 4 | **Multimedia Player** | Media3 (ExoPlayer), Compose | — |
| 5 | **Data Persistence** | Room Database, Coroutines, Flow | — |
| 6 | **Quote App (HTTP)** | Retrofit, Gson, Coroutines | — |
| 7 | **SMS Auto-Reply** | SMS permissions, BroadcastReceiver | Requires SMS permission |
| 8 | **Bluetooth Scanner** | Bluetooth APIs, Permissions | Bluetooth hardware |
| 9 | **Bluetooth Chat** | Bluetooth sockets, Threading | Two devices needed |
| 10 | **WiFi Signal Strength** | WifiManager, ScanResults | Location permission |
| 11 | **GPS Maps** | Google Maps SDK, Play Services Location | **Google Maps API key required** |
| 12 | **Sensor Demo** | SensorManager, SensorEventListener | Physical device recommended |
| 13 | **Firebase Chat** | Firebase Auth, Realtime Database, Analytics | **google-services.json required** |
| 14 | **ML Kit Face Detection** | ML Kit, CameraX | **Physical device with camera required** |

### Technology Stack

| Category | Libraries |
|----------|-----------|
| **Language** | Kotlin 2.0.21 |
| **Build** | AGP 8.5.0, Gradle KTS, Version Catalogs (TOML) |
| **UI** | Jetpack Compose (BOM 2024.08.00), Material3 |
| **Architecture** | Coroutines, Flow, ViewModel-ready |
| **Networking** | Retrofit 2.11.0, Gson 2.11.0 |
| **Database** | Room 2.6.1 (KSP) |
| **Maps/Location** | Play Services Maps 18.2.0, Location 21.2.0 |
| **Backend** | Firebase BOM 33.1.0 (Auth, Database, Analytics) |
| **ML/Computer Vision** | ML Kit Face Detection 16.1.7 |
| **Camera** | CameraX 1.3.4 |
| **Media** | Media3 (ExoPlayer) 1.4.1 |
| **Testing** | JUnit 4.13.2, Espresso 3.5.1, Compose UI Test |

### Build Configuration Highlights

- **Centralized versions**: All dependencies managed via `gradle/libs.versions.toml`
- **Shared configuration**: Root `build.gradle.kts` applies common settings to all modules
- **Performance optimized**: Parallel builds, configuration cache, 4GB JVM heap, R8 full mode
- **Modern standards**: Min SDK 24 (Android 7.0), Target/Compile SDK 34 (Android 14), Java 17

---

## 🚀 Getting Started

### Opening in Android Studio

1. **Open the parent folder**: `File → Open → Select the \`Experiments\` folder`
2. **Wait for Gradle Sync** — All 14 modules will be loaded
3. **Run any experiment** — Select the module in the run configuration dropdown

### Build Commands

```bash
# Navigate to Experiments directory
cd Experiments

# Build all modules
./gradlew build

# Build specific module
./gradlew :Experiment1_HelloWorld:assembleDebug

# Run tests for all modules
./gradlew test

# Clean build
./gradlew clean
```

### Required Setup for Specific Experiments

| Experiment | Setup Instructions |
|------------|-------------------|
| **11 - GPS Maps** | Add Google Maps API key in `Experiment11_GPSMaps/src/main/AndroidManifest.xml` |
| **13 - Firebase Chat** | Replace `Experiment13_FirebaseChat/google-services.json` with your Firebase project config |
| **14 - ML Kit Face Detection** | Requires physical device with camera (emulator has limited support) |

---

## 📋 Prerequisites

- **Android Studio** Koala (2024.1.2) or later
- **JDK 17** (included with Android Studio)
- **Android SDK** 34 (API Level 34)
- **Git LFS** for zip file handling (`git lfs install`)

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:
- How to contribute fixes, new materials, or improvements
- File naming conventions
- Pull request process
- Code of conduct (see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md))

---

## 📄 License

This repository is licensed under the [MIT License](LICENSE). See the license file for details.

---

## ⚠️ Academic Integrity

- Only submit material you have the right to share (your own notes, properly cited sources, or freely shareable educational content)
- Do not submit content that violates academic integrity policies (restricted answer keys, plagiarized material)
- When in doubt, open an issue to ask before submitting

---

## 📞 Contact

For questions, issues, or suggestions, please [open an issue](https://github.com/Mihir-Mithani/MPC_Academics/issues) or contact the maintainer.

---

## 📅 Last Updated

**August 2026** — Semester 7, Mobile and Pervasive Computing Course

---

*This repository serves as a comprehensive academic resource and practical Android development portfolio for the MPC course. All experiments are production-ready, demonstrating modern Android development practices with Jetpack Compose, coroutines, and industry-standard libraries.*