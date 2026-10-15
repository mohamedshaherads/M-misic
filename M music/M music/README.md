# Nocturne Music (Android starter)

A dark, minimal local-music-player starter inspired by modern music apps. It is an original implementation and does not include Samsung or Apple proprietary assets.

## Included
- Android project written in Kotlin
- `gradlew` and `gradlew.bat` bootstrap scripts (Gradle 8.9)
- GitHub Actions workflow that builds a debug APK on push to `main` or manually
- Local audio library listing and basic play/pause/previous/next controls
- Optional dim ambient screen labelled AOD in the app

## Important AOD note
The included ambient screen is an in-app low-light screen, **not** the phone manufacturer's system Always-On Display. Third-party apps generally cannot replace the system AOD globally. Actual lock-screen media controls and metadata on the system AOD depend on Android version, device manufacturer, and system settings. A future iteration should add a foreground playback service plus MediaSession/MediaStyle notification for proper background playback and lock-screen controls.

## Build on GitHub
1. Create a new GitHub repository.
2. Upload/push the contents of this folder to the repository's `main` branch.
3. Open **Actions** and enable workflows if prompted.
4. Run **Build Android APK**, or push a commit to `main`.
5. Open the completed workflow run and download the `nocturne-music-debug-apk` artifact.
6. Extract the artifact ZIP to get `app-debug.apk`, then install it on your Android phone.

## Build locally
Requires JDK 17 and an internet connection for the first Gradle distribution/dependency download:
```bash
chmod +x ./gradlew
./gradlew assembleDebug
```
On Windows:
```bat
gradlew.bat assembleDebug
```

## Current MVP limitations
- This is a starter build, not yet a full Samsung Music / Apple Music replacement.
- Playback currently uses a basic in-activity MediaPlayer; background playback, media-session integration, playlists, album art, queue, lyrics, and equalizer remain next steps.
- The first Android build may require dependency resolution from Google Maven and Maven Central.
