# ONI Mobile — working notes

The phone companion of Open Negative Initiative (ONI): a remote holding the desktop panel's Roll, Color
and Details tabs on the Scanner or Pro face, and import of scans to the desktop. `README.md` says how to build. The protocol and every hard part live in Rust, in
the `oni-link`, `oni-localsend` and `oni-link-ffi` crates of `5e1y/open-negative-initiative-rs`, checked
out next to this repository; this repository holds only thin native interfaces over them (UniFFI).

## Decisions

- **One design system with the desktop.** `android/.../ds/Tokens.kt` is generated from the desktop's
  `crates/oni-ui/ui/ds/tokens.slint` by `scripts/ds-tokens.py`; screens use no literal size or colour, and
  a unit test fails when the copy drifts. The launcher icon is the desktop app's.

- **Android first** (Kotlin and Compose, `android/`, arm64 only, minSdk 29). The iPhone app (SwiftUI,
  `ONIMobile/`) builds and runs in the simulator but is parked.
- **The desktop decides, the phone asks.** A key press is a relative command (step, reset, machine, walk,
  undo, redo), never a value, so two hands add up and the desktop keeps the history; the desktop answers
  with the full scanner state after every change. Keep the commands aligned with the Rust app's command
  layer (`oni_link::Host`, `HostEvents::next()`, `Host::publish(State)`).
- **Transport**: local network by default; Bluetooth LE later as a fallback, with the phone as the
  peripheral and the desktop as the central. Noise IK, paired once by a QR code (`psk1` for pairing).
- **Import speaks LocalSend v2 unchanged**, so any LocalSend app can also send to the desktop. Phone scans
  on a light table come first (a ProRAW scan arrives as its DNG), lab files take the same path. Files land
  untouched in an inbox folder chosen once in the desktop's settings: no roll created, no setting applied.
  Each paired phone has its own import code; LocalSend fingerprints are lowercase hex.
- **The UniFFI Kotlin bindings are committed** and checked against the Rust output. Renaming a Rust item
  exposed to Kotlin changes the bindings (`close` became `hangUp`, `Failed.message` became `reason`).
- **The debug keystore is committed on purpose**, so every APK installs over the last and keeps pairing.

## Build and release

- **Local first**: the APK is built on a Mac (`scripts/build-android-rust.sh`, then Gradle). The
  **Android** workflow is `workflow_dispatch` only and checks out the Rust repository with the deploy key
  `ONI_RS_DEPLOY_KEY`; never block on it.
- Without an accepted Xcode licence, `xcodebuild` and XcodeGen refuse to run: `scripts/run-simulator.sh`
  builds the iPhone app straight from the toolchain, and `cargo` needs `CC`, `CXX`, `AR` and the linker
  pointed into `XcodeDefault.xctoolchain`.
- A debug APK is published as a GitHub prerelease `android-debug-<sha>`.
- **Nothing private goes into this public repository**: no email address, no machine or account detail.

## Known gaps

Bluetooth not started; a desktop whose IP changes needs re-pairing; "forget" on the phone does not revoke
the pairing on the desktop; never tried against the official LocalSend app; discovery joins only the
default network interface (`DiscoveryConfig.interface`, where LocalSend joins all); the desktop side is
not wired into the Rust app's core yet.

## Roadmap

M1 the Android remote against the demo host (`oni-link-demo`); M2 import from the phone, once the Rust
app has its archive; M3 the remote on the Rust app, once it has the Scanner face; M4 Bluetooth. Later:
sending exported images from the desktop to the phone, and tablets.
