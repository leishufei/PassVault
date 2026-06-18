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

### Key Components

**CryptoManager**: Handles AES-256-GCM encryption using Android Keystore. All password fields are encrypted before Room storage. The key (`password_manager_master_key`) is hardware-backed when available.

**CloudCryptoManager**: Separate encryption layer for cloud sync. Encrypts entire backup JSON with a second AES-256-GCM key before uploading to Google Drive.

**MasterPasswordManager**: Stores SHA-256 hash of master password in EncryptedSharedPreferences. Used for authentication, not encryption.

**AuthManager**: Coordinates master password verification and biometric authentication (BiometricPrompt API).

**SessionManager**: Manages authentication state across app lifecycle. Handles auto-lock timeout and session invalidation on background.

**SyncManager**: Coordinates cloud sync with two-way merge algorithm. Uses UUID + timestamp for conflict resolution, tombstone mechanism for deletions. Implements optimistic locking via Google Drive `modifiedTime` field.

**PassVaultAutofillService**: Implements Android Autofill Framework. Parses autofill contexts, matches saved passwords by domain, and builds fill/save responses. Requires authentication via separate activity.

**Navigation**: Single-activity architecture with Jetpack Navigation Compose. Routes: `setup` → `login` → `main` (bottom nav with Passwords/Generator/Settings tabs) → `password_detail/{id}`, `category_management`, etc.

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
- **Google Play Services**: `play-services-auth` and `play-services-drive` for OAuth 2.0 and Drive API cloud sync

## Development Notes

- All ViewModels use Hilt's `@HiltViewModel` and are retrieved via `hiltViewModel()` in Composables
- Encrypted data stored as `EncryptedData(ciphertext: ByteArray, iv: ByteArray)` - both fields required for decryption
- Password generation defaults: 16 chars, uppercase + lowercase + digits + symbols
- Search functionality filters by title, username, and website fields
- Category system uses separate `CategoryDao` and `CategoryRepository`, supports custom categories with color coding
- Autofill service requires user to explicitly enable in system settings (Settings → Autofill services)
- Cloud sync uses UUID-based identity for entries, `lastModified` timestamp for conflict resolution, and tombstone records for deletions

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

## Build Verification

After completing changes, do **not** run a build automatically. Always ask the user whether they want to verify via compilation — the user will typically build and test themselves, and will only request help if a build error occurs.
