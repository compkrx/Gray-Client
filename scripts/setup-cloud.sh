#!/usr/bin/env bash
set -euo pipefail
cd /workspace/Gray-Client
mkdir -p /workspace/toolchains
jdk_dir=/workspace/toolchains/jdk-21
if [[ ! -x "$jdk_dir/bin/javac" ]]; then
    archive=/workspace/toolchains/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz
    curl --fail --location --retry 2 --max-time 240 \
      'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz' \
      --output "$archive"
    printf '%s  %s\n' ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94 "$archive" | sha256sum --check -
    mkdir -p "$jdk_dir"
    tar --extract --gzip --file "$archive" --directory "$jdk_dir" --strip-components=1
fi
export JAVA_HOME="$jdk_dir"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle
javac -version
python3 scripts/cloud-build.py ./gradlew build --no-daemon --max-workers=2
