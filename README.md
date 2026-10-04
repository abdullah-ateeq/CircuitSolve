# CircuitSolve ⚡

**CircuitSolve** is an Android application built with **Jetpack Compose** and **Material Design 3** that calculates equivalent resistance for electrical circuits in both **Series** and **Parallel** configurations. Designed with high mathematical precision, intuitive UI, and modern Android development best practices.

---

## 🌟 Key Features

- ⚡ **Series & Parallel Calculation**: Easily toggle between series and parallel circuit modes with real-time recalculation.
- 📐 **High-Precision Calculation Engine**: Built using Kotlin's `BigDecimal` (34 significant digits) ensuring exact math without floating-point precision loss.
- 🎛️ **Flexible Unit Support**: Enter resistance values in **milliohms (mΩ)**, **ohms (Ω)**, **kilohms (kΩ)**, **megohms (MΩ)**, and **gigohms (GΩ)** with seamless automatic scaling.
- 🎨 **Visual Circuit Schematic**: Dynamic visual schematic rendering series and parallel resistor layouts according to selected values and units.
- 📊 **Formula & Step-by-Step Summary**: Live expression breakdown (e.g., `100 Ω + 220 Ω + 330 Ω` or `100 Ω || 220 Ω`) along with named textbook formulas.
- 📜 **Calculation History**: Automatically saves calculations with timestamps, allowing you to restore previous inputs, search records, or export/copy results.
- ⚙️ **Customizable Settings**:
  - Decimal precision adjustment (0–8 decimal places)
  - Auto-unit scaling preference
  - Material You Dynamic Color integration
  - Light, Dark, or System default theme support
- 🛡️ **Input Validation & Guidance**: Real-time validation preventing invalid values, empty fields, or calculations outside limits (supports 2 to 10 resistors).

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture**: Modern Android Architecture (MVVM, Unidirectional Data Flow, StateFlow, ViewModel)
- **Data Persistence**: [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore) with Kotlinx Serialization for type-safe history management
- **Navigation**: Jetpack Navigation Compose
- **Build System**: Gradle Version Catalogs (`libs.versions.toml`)
- **Testing**: JUnit 4 for domain calculator, unit conversion, and persistence tests

---

## 📂 Project Structure

```
com.example.circuitsolve/
├── CircuitSolveApplication.kt   # Application entry point
├── MainActivity.kt              # Main Activity with Compose theme host
├── data/                        # Repositories & DataStore persistence
│   ├── HistoryRepository.kt
│   └── SettingsRepository.kt
├── domain/                      # Pure Kotlin business logic & calculation engine
│   ├── CircuitEvaluation.kt
│   ├── CircuitModels.kt
│   ├── CircuitSolveCalculator.kt
│   ├── ResistanceInputParser.kt
│   └── ResistanceUnits.kt
└── ui/                          # Jetpack Compose UI layer
    ├── CircuitSolveApp.kt       # Navigation host & scaffolding
    ├── CircuitSolveViewModel.kt # State management
    ├── calculator/              # Main calculator screen & visual diagram
    ├── history/                 # History screen
    ├── settings/                # App settings screen
    ├── about/                   # About screen
    └── theme/                   # Material 3 typography, colors, and theme
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug / Meerkat (2024.x) or newer
- **JDK**: Version 11 or higher
- **Android SDK**: Compile SDK 35+, Minimum SDK 24 (Android 7.0+)

### Building and Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/CircuitSolve.git
   cd CircuitSolve
   ```

2. **Open in Android Studio**:
   Open Android Studio and select **Open** -> choose the `CircuitSolve` directory.

3. **Build the project**:
   Run `./gradlew assembleDebug` or click **Build > Make Project** in Android Studio.

4. **Run unit tests**:
   ```bash
   ./gradlew test
   ```

---

## 🧪 Testing

The app includes unit test coverage for:
- **Circuit Engine**: Verifies series sum and parallel product-over-sum / conductance calculations (`CircuitSolveCalculatorTest.kt`).
- **Unit Parsing & Validation**: Tests formatting, parsing edge cases, prefix conversions, and boundary checks (`UnitsAndValidationTest.kt`).
- **Persistence**: Validates settings and calculation history DataStore serialization (`PersistenceTest.kt`).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
