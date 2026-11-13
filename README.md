# Luas Tracker - Android App

[![Android CI](https://github.com/garwedgess/luas-dart/actions/workflows/android.yml/badge.svg?branch=master)](https://github.com/garwedgess/luas-dart/actions/workflows/android.yml)
[![codecov](https://codecov.io/gh/garwedgess/luas-dart/graph/badge.svg)](https://app.codecov.io/gh/garwedgess/luas-dart)

**Luas Dart** is an Android application designed to provide realtime data related to the luas and dart.

## Continuous integration

[Android CI](.github/workflows/android.yml) runs on branch pushes, pull requests and manual dispatches. It checks
formatting, Detekt, Android lint, SQLDelight migrations and unit tests; generates JaCoCo XML/HTML coverage; builds
debug and unsigned release APKs; and verifies 16 KB alignment of the packaged 64-bit native libraries. Emulator
UI tests are not part of this workflow. Successful builds on the default branch also produce an installable,
signed release APK in the protected `release` environment.

Quality checks, unit tests/JaCoCo and APK builds run as three parallel jobs, with shared build-tool setup.
Codecov depends only on the tests job, so a lint or APK build failure does not prevent coverage upload.

Gradle dependencies, wrapper distributions and cacheable task outputs are cached using `setup-gradle`.
Each job saves its build state on the default branch; feature branches and pull requests restore it read-only.
Robolectric's downloaded Android SDK jars have a separate cache in the tests job. The first run populates the
caches; subsequent runs can reuse them. The three parallel jobs do not share a live cache within the same run.

### Release signing

Download **android-release-signed** for the installable release APK and its SHA-256 checksum, or
**android-debug-apk** for a debug build. **android-release-unsigned** is an intermediate signing input and cannot
be installed on Android.

The signing job waits for quality checks, unit tests and APK builds to pass, and runs only on the default branch.
Configure the GitHub `release` environment to allow deployments from `master` only and add these environment
secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.
The Base64 value must encode the PKCS12 release keystore. Signing material is decoded into a temporary directory,
never passed into Gradle or its caches, and deleted after signing. Only the verified APK and checksum are uploaded.

For local signing, the private configuration is stored outside the checkout at
`~/.android/keystores/luas-dart/signing.json`, alongside `release.p12`. Back up both privately: future updates
must use the same signing key. Do not commit these files or share the passwords in chat.

```sh
python3 scripts/sign_release.py \
  --input app/build/outputs/apk/release/app-release-unsigned.apk \
  --output app/build/outputs/signed-release/app-release.apk
```

The helper aligns native libraries to 16 KB, signs the APK, and verifies its signatures and ZIP alignment.
A release signed with this key cannot update an existing installation signed with a different key (including
the Android Studio debug key); uninstall that installation first if Android reports a signature conflict.

Test results, analysis reports, coverage and APKs are available as workflow artifacts. Codecov uploads use GitHub
OIDC authentication, so no `CODECOV_TOKEN` secret is needed. Uploads run for trusted branches and pull requests;
fork and Dependabot pull requests still run all build and test checks. Coverage policy lives in [codecov.yml](codecov.yml).

To run the same checks locally, use Java 21 and an Android SDK with platform 35 and build tools 34.0.0:

```sh
bash gradlew --no-daemon --stacktrace --continue --max-workers=2 \
  :app:spotlessCheck :app:detekt :app:lintDebug :app:verifySqlDelightMigration \
  :app:testDebugUnitTest :app:jacocoTestDebugUnitTestReport \
  :app:assembleDebug :app:assembleRelease :app:verify16KbNativeLibraries
```

## Contributing

Contributions are welcome! Please feel free to open issues or submit pull requests.
