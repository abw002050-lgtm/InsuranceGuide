# GitHub Actions APK Build

This workflow provisions JDK 17 and Android SDK Platform 35 on a GitHub-hosted Ubuntu runner, then runs `./gradlew clean assembleDebug`. The generated debug APK is uploaded as the artifact `InsuranceGuide-debug-apk`.

You can start it manually from GitHub: **Actions → Build Android APK → Run workflow**.
