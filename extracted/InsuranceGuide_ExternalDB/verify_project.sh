#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
required=(
  "settings.gradle.kts"
  "build.gradle.kts"
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/assets/databases/Domw.zip"
  "gradle/wrapper/gradle-wrapper.properties"
)
missing=0
for f in "${required[@]}"; do
  if [[ -f "$root/$f" ]]; then echo "OK   $f"; else echo "MISS $f"; missing=1; fi
done
kt_count=$(find "$root/app/src/main/java" -name '*.kt' | wc -l | tr -d ' ')
echo "Kotlin files: $kt_count"
if [[ -f "$root/gradle/wrapper/gradle-wrapper.jar" ]]; then
  echo "OK   gradle/wrapper/gradle-wrapper.jar"
else
  echo "MISS gradle/wrapper/gradle-wrapper.jar (required for standard ./gradlew execution)"
  missing=1
fi
if [[ -n "${ANDROID_HOME:-}" || -n "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Android SDK environment variable detected"
else
  echo "Android SDK environment variable not detected in current shell"
fi
exit "$missing"
