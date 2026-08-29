#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA_VER=$(java -version 2>&1 | sed -n '1p')
echo "Java: $JAVA_VER"
if ! command -v java >/dev/null 2>&1; then echo "ERROR: Java is required" >&2; exit 1; fi
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "ERROR: Set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK installation." >&2
  exit 1
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT}}"
[ -d "$SDK" ] || { echo "ERROR: Android SDK path does not exist: $SDK" >&2; exit 1; }
if [ ! -d "$SDK/platforms/android-35" ]; then
  echo "ERROR: Android SDK Platform 35 is required at $SDK/platforms/android-35" >&2
  exit 1
fi
if [ ! -d "$SDK/build-tools" ]; then
  echo "ERROR: Android SDK Build-Tools are required" >&2
  exit 1
fi
echo "Android SDK: $SDK"
echo "Environment OK"
