# Wrapper and build guide

This project includes the wrapper scripts and `gradle-wrapper.properties`, but the binary wrapper runtime JAR must be supplied by a normal Android/Gradle environment if it is not already present.

After opening the project in Android Studio or another Android build environment, restore/generate the standard Gradle wrapper runtime, then run:

```bash
./gradlew --no-daemon clean assembleDebug
```

Do not treat this guide as proof that an APK has already been built.
