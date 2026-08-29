# BUILD STATUS V13

## Direct build bootstrap
This version removes the hard dependency on a pre-existing `gradle-wrapper.jar` for Unix builds. `./gradlew` now:

1. Uses the standard wrapper when `gradle-wrapper.jar` exists.
2. Otherwise downloads Gradle 8.7 from the official Gradle distribution service using curl or wget.
3. Unpacks it into the user's Gradle directory and runs the project from there.

`build_apk.sh` runs `clean assembleDebug` and verifies that `app/build/outputs/apk/debug/app-debug.apk` exists.

## Current verification
The project structure and scripts were checked locally. A real Android APK build still requires a machine with a JDK 17, Android SDK platform 35, build-tools, and network access for dependencies/Gradle if they are not already cached. No successful APK build is claimed in this package.
