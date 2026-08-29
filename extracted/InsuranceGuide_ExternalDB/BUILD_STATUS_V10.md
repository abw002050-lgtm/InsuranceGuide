# V10 Build Readiness

This source package has been repaired for the issues found during static inspection:
- fixed `InsuranceDatabase.get()` singleton return logic;
- added the missing Android `AppTheme` resource;
- enabled Java 17 compile settings;
- enabled core-library desugaring required by `java.time` calculators on Android API 24-25;
- retained local SQLite database initialization and search modules.

A real APK build still requires a machine with the Android SDK and Gradle/Gradle Wrapper runtime available.
