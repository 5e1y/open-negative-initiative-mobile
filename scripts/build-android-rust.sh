#!/usr/bin/env bash
# Builds oni-link-ffi for Android phones into android/app/src/main/jniLibs, then regenerates the Kotlin
# bindings. Needs cargo-ndk and an NDK in ANDROID_NDK_HOME; ONI_RS names the Rust repository, else a sibling.
set -euo pipefail

here=$(cd "$(dirname "$0")/.." && pwd)
link=${ONI_RS:-$here/../open-negative-initiative-rs}
abi=arm64-v8a
target=aarch64-linux-android

rustup target add "$target"
cd "$link"
# The platform is the app's minSdk: the library may call nothing newer than the phones it installs on.
cargo ndk -t "$abi" --platform 29 -o "$here/android/app/src/main/jniLibs" build -p oni-link-ffi --release
cargo run -p oni-link-ffi --features cli --bin uniffi-bindgen --release -- \
    generate --library "target/$target/release/liboni_link_ffi.so" --language kotlin --no-format \
    --out-dir "$here/android/app/src/main/java"
echo "Built $here/android/app/src/main/jniLibs/$abi/liboni_link_ffi.so"
