#!/usr/bin/env sh

set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
gradle_version=8.9
cache_dir="$project_dir/.gradle-local"
gradle_dir="$cache_dir/gradle-$gradle_version"
gradle_bin="$gradle_dir/bin/gradle"

if [ ! -x "$gradle_bin" ]; then
    mkdir -p "$cache_dir"
    archive="$cache_dir/gradle-$gradle_version-bin.zip"

    if [ ! -f "$archive" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl -fL "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip" -o "$archive"
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$archive" "https://services.gradle.org/distributions/gradle-$gradle_version-bin.zip"
        else
            echo "Necesitas curl o wget para descargar Gradle." >&2
            exit 1
        fi
    fi

    unzip -q -o "$archive" -d "$cache_dir"
fi

exec "$gradle_bin" "$@"
