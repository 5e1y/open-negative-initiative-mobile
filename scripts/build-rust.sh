#!/usr/bin/env bash
# Builds oni-link-ffi for iPhone and the Apple-silicon simulator into Frameworks/OniLinkFFI.xcframework,
# then regenerates the Swift bindings. Runs on a Mac; ONI_LINK names the oni-link workspace, else a sibling.
set -euo pipefail

here=$(cd "$(dirname "$0")/.." && pwd)
link=${ONI_LINK:-$here/../oni-link}
targets=(aarch64-apple-ios aarch64-apple-ios-sim)
library=liboni_link_ffi.a

rustup target add "${targets[@]}"
cd "$link"
for target in "${targets[@]}"; do
    # ring compiles C through the cc crate, which reads the same floor as rustc.
    IPHONEOS_DEPLOYMENT_TARGET=17.0 cargo build -p oni-link-ffi --release --target "$target"
done

generated=$here/ONIMobile/Generated
scripts/swift-bindings.sh "$generated" "target/aarch64-apple-ios/release/$library"

# The XCFramework carries the C header under the module name the bindings import.
headers=$(mktemp -d)
trap 'rm -rf "$headers"' EXIT
cp "$generated/oni_link_ffiFFI.h" "$headers/"
cp "$generated/oni_link_ffiFFI.modulemap" "$headers/module.modulemap"

framework=$here/Frameworks/OniLinkFFI.xcframework
rm -rf "$framework"
mkdir -p "$here/Frameworks"
xcodebuild -create-xcframework \
    -library "target/aarch64-apple-ios/release/$library" -headers "$headers" \
    -library "target/aarch64-apple-ios-sim/release/$library" -headers "$headers" \
    -output "$framework"
echo "Built $framework"
