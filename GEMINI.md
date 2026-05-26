# MotionGaurd Project

## Project Overview

MotionGaurd is an Android application built using modern Android development practices. It is a Kotlin-based project that utilizes Jetpack Compose for its User Interface. The project relies on Gradle with Kotlin DSL (`.kts` files) for build configuration and manages its dependencies via a Gradle Version Catalog (`gradle/libs.versions.toml`). It is configured to target Android SDK 36.

## Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Build System:** Gradle (Kotlin DSL)
- **Dependency Management:** Version Catalogs (`libs.versions.toml`)

## Building and Running

You can use standard Gradle Wrapper commands to build, test, and run the project:

- **Build the project:**
  ```bash
  ./gradlew build
  ```
- **Install the Debug APK on an attached device or emulator:**
  ```bash
  ./gradlew installDebug
  ```
- **Run Unit Tests:**
  ```bash
  ./gradlew test
  ```
- **Run Instrumented UI Tests:**
  ```bash
  ./gradlew connectedAndroidTest
  ```

## Development Conventions

- **Version Catalog:** All dependency versions and definitions should be maintained in `gradle/libs.versions.toml`.
- **UI:** The project uses Jetpack Compose for UI. New UI components should be built as Compose functions.
- **Java/Kotlin Version:** The project uses Java 11 compatibility.
- **Package Name:** `com.gayan.motiongaurd`
