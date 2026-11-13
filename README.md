# Luas Tracker - Android App

[![Android CI](https://github.com/garwedgess/luas-dart/actions/workflows/android.yml/badge.svg?branch=master)](https://github.com/garwedgess/luas-dart/actions/workflows/android.yml)
[![codecov](https://codecov.io/gh/garwedgess/luas-dart/graph/badge.svg)](https://app.codecov.io/gh/garwedgess/luas-dart)

**Luas Dart** is an Android application designed to provide realtime data related to the luas and dart.

## Continuous integration

[Android CI](.github/workflows/android.yml) runs on branch pushes, pull requests and manual dispatches. It checks
formatting, Detekt, Android lint, SQLDelight migrations and unit tests; generates JaCoCo XML/HTML coverage; builds
debug and unsigned release APKs; and verifies 16 KB alignment of the packaged 64-bit native libraries. Emulator
UI tests are not part of this workflow.

Quality checks, unit tests/JaCoCo and APK builds run as three parallel jobs, with shared build-tool setup.
Codecov depends only on the tests job, so a lint or APK build failure does not prevent coverage upload.

Gradle dependencies, wrapper distributions and cacheable task outputs are cached using `setup-gradle`.
Each job saves its build state on the default branch; feature branches and pull requests restore it read-only.
Robolectric's downloaded Android SDK jars have a separate cache in the tests job. The first run populates the
caches; subsequent runs can reuse them. The three parallel jobs do not share a live cache within the same run.

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
