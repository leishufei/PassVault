# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android password manager app built with Kotlin, Jetpack Compose, and Clean Architecture. Uses AES-256 encryption via Android Keystore for secure password storage.

**Target**: Android 8.0+ (API 26-35)  
**Language**: Kotlin with JDK 17  
**UI**: Jetpack Compose + Material 3

## Build Commands

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Build release APK (with ProGuard)
.\gradlew.bat assembleRelease

# Install and run on connected device
.\gradlew.bat installDebug

# Run all tests
.\gradlew.bat test

# Run instrumented tests (requires device/emulator)
.\gradlew.bat connectedAndroidTest

# Clean build
.\gradlew.bat clean
```

## Architecture

**Pattern**: MVVM + Clean Architecture with Hilt DI

### Layer Structure

```
data/
  ├── local/          Room database (PasswordDao, CategoryDao, PasswordDatabase)
  ├── model/          Data entities (PasswordEntry, Category)
  ├── repository/     Data access layer (PasswordRepository, CategoryRepository)
  ├── security/       Encryption services (CryptoManager, CloudCryptoManager, MasterPasswordManager)
  └── sync/           Cloud storage implementation (GoogleDriveProvider)

domain/
  ├── auth/           Authentication logic (AuthManager, SessionManager)
  ├── generator/      Password generation (PasswordGenerator)
  └── sync/           Sync coordination (SyncManager, SyncScheduler, CloudStorageProvider)

ui/
  ├── auth/           Login/setup screens + AuthViewModel
  ├── main/           Main container with bottom navigation
  ├── passwords/      Password list/detail screens + PasswordViewModel
  ├── generator/      Password generator screen
  ├── settings/       Settings, backup, category management + ViewModels
  └── theme/          Material 3 theme configuration

autofill/             Android Autofill Service integration (PassVaultAutofillService, matching engine)
di/                   Hilt modules (DatabaseModule)
```

## Devlopment Note

1. Compile and verify after completing function development or problem repair.
2. If there is a compilation problem, solve the compilation problem yourself.
3. After the compilation is passed, use the sub-agent to check whether all the modifications meet the user's requirements, and execute the test first if there are unit tests.


## UI Design Guidelines

Any UI-related changes must follow the design system defined in [`DESIGN.md`](./DESIGN.md).

## Room Schema Changes

Modifying any `@Entity` class **always** requires bumping the `version` in `PasswordDatabase`. The project currently uses `fallbackToDestructiveMigration()` — a version mismatch without a bump will silently wipe all user data at runtime. When in doubt, ask before touching entity fields.

## Code Style & Patterns

Follow the conventions already established in the codebase:

- **State**: Use `StateFlow` + `MutableStateFlow` in ViewModels, not `LiveData`.
- **Error handling**: Represent UI states with sealed classes (e.g., `UiState.Loading / Success / Error`).
- **ViewModel scope**: Extend existing ViewModels before creating new ones. Each screen should have at most one ViewModel.
- **DI**: Add new dependencies via Hilt constructor injection (`@Inject`). New bindings go in `di/DatabaseModule` or a new module under `di/` — never use manual instantiation.
- **Coroutines**: Use `viewModelScope` for UI-related work; use `withContext(Dispatchers.IO)` for database and network calls.
- **Composables**: Keep Composables stateless where possible — lift state to the ViewModel. Avoid business logic inside Composables.
- **No Chinese comments**: Do not write Chinese characters in code comments or log messages.

## Scope of Changes

Only modify files directly relevant to the requested task. Do **not** refactor, reformat, or "improve" unrelated code — even if it looks messy. If you notice a problem outside the task scope, mention it in a comment rather than fixing it unilaterally.
<!-- 
## Build Verification

After completing changes, do **not** run a build automatically. Always ask the user whether they want to verify via compilation — the user will typically build and test themselves, and will only request help if a build error occurs. -->
