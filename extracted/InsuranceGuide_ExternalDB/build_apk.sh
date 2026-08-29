#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$DIR"
./verify_build_environment.sh
./gradlew clean assembleDebug
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  echo "APK built successfully: $DIR/$APK"
else
  echo "Build finished but APK was not found" >&2
  exit 1
fi
