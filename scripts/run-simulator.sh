#!/usr/bin/env bash
# Builds ONI Mobile for the Apple-silicon simulator without xcodebuild or XcodeGen, then runs it on an
# "ONI Mobile Test" device. For a Mac whose Xcode licence is unaccepted; ONI_RS names the Rust repository.
set -euo pipefail

here=$(cd "$(dirname "$0")/.." && pwd)
link=${ONI_RS:-$here/../open-negative-initiative-rs}
device_name="ONI Mobile Test"
device_type=${ONI_SIM_DEVICE:-iPhone 17 Pro}
target=aarch64-apple-ios-sim

# The /usr/bin shims refuse while the licence is unaccepted: every tool is called inside the toolchain.
developer=$(xcode-select -p)
toolchain=$developer/Toolchains/XcodeDefault.xctoolchain/usr/bin
sim_sdk=$developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator.sdk
mac_sdk=$developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk
simctl=$developer/usr/bin/simctl

# One value of project.yml's settings, quotes dropped; the first match wins.
setting() {
    sed -n "s/^ *$1: *\"\{0,1\}\([^\"]*\)\"\{0,1\} *$/\1/p" "$here/project.yml" | head -n 1
}
ios=$(setting iOS)
swift_version=$(setting SWIFT_VERSION)
product=$(setting PRODUCT_NAME)

scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT

# Host links (build scripts, proc macros) keep the macOS SDK while SDKROOT names the simulator's.
# The wrapper's path is the same on every run, else cargo rebuilds every host crate.
wrapper=${TMPDIR:-/tmp}/oni-mobile-host-clang
cat > "$wrapper" <<EOF
#!/bin/sh
SDKROOT="$mac_sdk" exec "$toolchain/clang" "\$@"
EOF
chmod +x "$wrapper"
host=$(rustc -vV | sed -n 's/^host: //p')
host_linker=CARGO_TARGET_$(echo "$host" | tr 'a-z-' 'A-Z_')_LINKER

rustup target add "$target"
(
    cd "$link"
    export CC="$toolchain/clang" CXX="$toolchain/clang++" AR="$toolchain/ar"
    export CARGO_TARGET_AARCH64_APPLE_IOS_SIM_LINKER="$toolchain/clang" "$host_linker=$wrapper"
    SDKROOT="$sim_sdk" IPHONEOS_DEPLOYMENT_TARGET="$ios" cargo build -p oni-link-ffi --release --target "$target"
)
library=$link/target/$target/release/liboni_link_ffi.a

# The bindings import the C header under its module name, as in the XCFramework.
generated=$here/ONIMobile/Generated
cp "$generated/oni_link_ffiFFI.h" "$scratch/"
cp "$generated/oni_link_ffiFFI.modulemap" "$scratch/module.modulemap"

build=$here/build-sim
app=$build/$product.app
rm -rf "$build"
mkdir -p "$app"
sources=()
while IFS= read -r file; do sources+=("$file"); done < <(find "$here/ONIMobile" -name '*.swift' | sort)
"$toolchain/swiftc" -swift-version "${swift_version%%.*}" -Onone -g -parse-as-library -module-name "$product" \
    -sdk "$sim_sdk" -target "arm64-apple-ios$ios-simulator" -I "$scratch" \
    -emit-executable -o "$build/$product" "${sources[@]}" \
    "$library" -framework Security -framework CoreFoundation -liconv
mv "$build/$product" "$app/"

# Xcode's $(...) values come from project.yml; EXECUTABLE_NAME is Xcode's default, the product name.
plist=$(cat "$here/ONIMobile/Info.plist")
for key in $(grep -o '\$([A-Z_]*)' "$here/ONIMobile/Info.plist" | sort -u | tr -d '$()'); do
    value=$([[ $key == EXECUTABLE_NAME ]] && echo "$product" || setting "$key")
    [[ -n $value ]] || { echo "project.yml has no $key for Info.plist" >&2; exit 1; }
    plist=${plist//\$($key)/$value}
done
printf '%s\n' "$plist" > "$app/Info.plist"
plutil -lint "$app/Info.plist" > /dev/null
printf 'APPL????' > "$app/PkgInfo"
# KeycapSound looks for the clicks in a Keycap folder inside the bundle.
mkdir -p "$app/Keycap"
cp "$here/ONIMobile/Resources/Keycap/"*.wav "$app/Keycap/"
codesign --sign - --force "$app"

# simctl warns about the licence on stderr and still works: that one line is dropped, other errors kept.
sim() { "$simctl" "$@" 2> >(grep -v "agreed to the Xcode license" >&2); }
runtime=$(sim list runtimes available | sed -n 's/^iOS .* - \(com\.apple\.CoreSimulator\.SimRuntime\.iOS-[0-9-]*\).*/\1/p' | tail -n 1)
[[ -n $runtime ]] || { echo "No iOS simulator runtime is installed" >&2; exit 1; }
udid=$(sim list devices available | sed -n "s/^ *$device_name (\([0-9A-F-]*\)).*/\1/p" | head -n 1)
if [[ -z $udid ]]; then
    udid=$(sim create "$device_name" "$device_type" "$runtime")
fi
sim boot "$udid" 2> /dev/null || true
sim bootstatus "$udid" -b > /dev/null
# Shows the device when a Simulator app is installed; the device runs headless otherwise.
open -b com.apple.iphonesimulator --args -CurrentDeviceUDID "$udid" 2> /dev/null || true

bundle_id=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app/Info.plist")
sim install "$udid" "$app"
sim launch --terminate-running-process "$udid" "$bundle_id"
echo "Running $bundle_id on $device_name ($udid)"
