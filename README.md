# Wordy (Android)

A word-guessing game: find the mystery word in six tries, with hints, definitions, history and
bookmarks. The iOS version lives at [hansololz/word-guesser](https://github.com/hansololz/word-guesser).

## Build

Requires JDK 21 and the Android SDK (API 37 platform). Open the project in Android Studio, or:

```sh
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests (JVM + Robolectric)
./gradlew lintDebug            # Android lint
./gradlew bundleRelease        # minified release bundle (sign before uploading)
```

Dependency versions are in `gradle/libs.versions.toml`.

## Structure

Single module, Kotlin and Jetpack Compose, under `app/src/main/java/com/deezus/wordy`:

| Package | Contents |
|---|---|
| `core` | The rules of the game as pure functions over an immutable `GameState`. No Android dependencies. |
| `data` | Repositories: bundled word lists, settings and scores (DataStore), saved games (DataStore), history and bookmarks (Room), definitions (Retrofit). |
| `data/legacy` | One-time import of data written by versions before 3.0. |
| `di` | `AppContainer`, the manually wired object graph. |
| `ui` | One package per screen, each with a ViewModel exposing a single `StateFlow` of UI state. Navigation 3 back stack in `WordyApp.kt`. |

## Things that must not change

Existing installs depend on these:

- The application id `com.deezus.wordy` and the launcher activity name `com.deezus.wordy.ui.MainActivity`.
- The persisted ids on `GameMode` and `ScoreDisplay`, and the names of the `GameOutcome` and
  `GameStatus` entries.
- The preference key names in `SettingsRepository`, which match the pre-3.0 SharedPreferences file.
- The Room schema without a migration. Schemas are exported to `app/schemas`; bump the database
  version and add a migration for any entity change.
