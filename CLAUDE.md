# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android password manager app built with Kotlin, Jetpack Compose, and Clean Architecture. Uses AES-256 encryption via Android Keystore for secure password storage.

**Target**: Android 8.0+ (API 26-35)  
**Language**: Kotlin with JDK 17  
**UI**: Jetpack Compose + Material 3

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (with ProGuard)
./gradlew assembleRelease

# Install and run on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Architecture

**Pattern**: MVVM + Clean Architecture with Hilt DI

### Layer Structure

```
data/
  ├── local/          Room database (PasswordDao, PasswordDatabase)
  ├── model/          Data entities (PasswordEntry)
  ├── repository/     Data access layer (PasswordRepository)
  └── security/       Encryption services (CryptoManager, MasterPasswordManager)

domain/
  ├── auth/           Authentication logic (AuthManager)
  └── generator/      Password generation (PasswordGenerator)

ui/
  ├── auth/           Login/setup screens + AuthViewModel
  ├── passwords/      Password list/detail screens + PasswordViewModel
  └── theme/          Material 3 theme configuration

di/                   Hilt modules (DatabaseModule)
```

### Key Components

**CryptoManager**: Handles AES-256-GCM encryption using Android Keystore. All password fields are encrypted before Room storage. The key (`password_manager_master_key`) is hardware-backed when available.

**MasterPasswordManager**: Stores SHA-256 hash of master password in EncryptedSharedPreferences. Used for authentication, not encryption.

**AuthManager**: Coordinates master password verification and biometric authentication (BiometricPrompt API).

**Navigation**: Single-activity architecture with Jetpack Navigation Compose. Routes: `setup` → `login` → `passwords` → `password_detail/{id}`.

## Security Considerations

- Master password hash is stored separately from encryption keys
- Encryption keys never leave Android Keystore
- Database uses `fallbackToDestructiveMigration()` - schema changes will wipe data
- No password recovery mechanism - forgotten master password means data loss
- BiometricPrompt integration requires `FragmentActivity` (MainActivity extends it)

## Dependencies

- **Hilt**: Constructor injection via `@Inject`, modules in `di/`
- **Room**: Database version managed in PasswordDatabase, migrations not implemented
- **Compose**: BOM version 2024.12.01, Material 3 components
- **Security**: `security-crypto` for EncryptedSharedPreferences, `biometric` for fingerprint/face auth

## Development Notes

- All ViewModels use Hilt's `@HiltViewModel` and are retrieved via `hiltViewModel()` in Composables
- Encrypted data stored as `EncryptedData(ciphertext: ByteArray, iv: ByteArray)` - both fields required for decryption
- Password generation defaults: 16 chars, uppercase + lowercase + digits + symbols
- Search functionality filters by title, username, and website fields
