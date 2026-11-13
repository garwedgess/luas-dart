#!/usr/bin/env python3
"""Align, sign and verify a release APK without exposing passwords to command-line arguments or Gradle."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


def android_tools(version: str) -> tuple[Path, Path]:
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        properties = Path("local.properties")
        if properties.exists():
            sdk = next((line.partition("=")[2].strip() for line in properties.read_text().splitlines()
                        if line.startswith("sdk.dir=")), None)
    if not sdk:
        raise ValueError("Set ANDROID_HOME to the Android SDK directory.")
    tools = Path(sdk) / "build-tools" / version
    apksigner, zipalign = tools / "apksigner", tools / "zipalign"
    if not apksigner.is_file() or not zipalign.is_file():
        raise ValueError(f"Install Android SDK build-tools;{version} before signing.")
    return apksigner, zipalign


def signing_config(path: Path) -> dict[str, str]:
    config = json.loads(path.read_text()) if path.exists() else {}
    for field, variable in {
        "keystore": "RELEASE_KEYSTORE_PATH", "key_alias": "RELEASE_KEY_ALIAS",
        "store_password": "RELEASE_STORE_PASSWORD", "key_password": "RELEASE_KEY_PASSWORD",
    }.items():
        if os.environ.get(variable):
            config[field] = os.environ[variable]
    missing = [key for key in ("key_alias", "store_password", "key_password") if not config.get(key)]
    if missing:
        raise ValueError("Missing release signing credentials. Configure the release environment secrets.")
    return config


def sign(args: argparse.Namespace) -> None:
    source, output = args.input.resolve(), args.output.resolve()
    if source == output:
        raise ValueError("The signed output must not overwrite the unsigned input.")
    if not source.is_file():
        raise ValueError("Unsigned APK not found. Build assembleRelease first.")
    apksigner, zipalign = android_tools(args.build_tools_version)
    config = signing_config(args.config)
    with tempfile.TemporaryDirectory(prefix="luas-release-signing-") as temporary:
        work = Path(temporary)
        encoded = os.environ.get("RELEASE_KEYSTORE_BASE64")
        if encoded:
            keystore = work / "release.p12"
            keystore.write_bytes(base64.b64decode(encoded, validate=True))
            keystore.chmod(0o600)
        else:
            if not config.get("keystore"):
                raise ValueError("Set RELEASE_KEYSTORE_BASE64 or RELEASE_KEYSTORE_PATH.")
            keystore = Path(config["keystore"]).expanduser().resolve()
        if not keystore.is_file():
            raise ValueError("Release keystore not found.")
        aligned, signed = work / "aligned.apk", work / "signed.apk"
        subprocess.run([str(zipalign), "-P", "16", "-f", "4", str(source), str(aligned)], check=True)
        signer_env = dict(os.environ, LUAS_SIGN_STORE_PASSWORD=config["store_password"],
                          LUAS_SIGN_KEY_PASSWORD=config["key_password"])
        # The private material never enters a Gradle build, its configuration cache, or an artifact upload.
        subprocess.run([
            str(apksigner), "sign", "--ks", str(keystore), "--ks-type", "PKCS12",
            "--ks-key-alias", config["key_alias"], "--ks-pass", "env:LUAS_SIGN_STORE_PASSWORD",
            "--key-pass", "env:LUAS_SIGN_KEY_PASSWORD", "--v4-signing-enabled", "false",
            "--out", str(signed), str(aligned),
        ], env=signer_env, check=True)
        subprocess.run([str(apksigner), "verify", "--verbose", "--print-certs", str(signed)], check=True)
        subprocess.run([str(zipalign), "-c", "-P", "16", "4", str(signed)], check=True)
        output.parent.mkdir(parents=True, exist_ok=True)
        # Replace only after verification. Copy across filesystems so RUNNER_TEMP need not share the checkout disk.
        import shutil
        shutil.copyfile(signed, output)
    digest = hashlib.sha256()
    with output.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    output.with_suffix(".apk.sha256").write_text(f"{digest.hexdigest()}  {output.name}\n")
    print(f"Signed and verified APK: {output}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--config", type=Path,
                        default=Path.home() / ".android/keystores/luas-dart/signing.json")
    parser.add_argument("--build-tools-version", default="35.0.0")
    args = parser.parse_args()
    try:
        sign(args)
    except (ValueError, OSError, subprocess.CalledProcessError):
        # Do not dump configuration values or environment variables on signing failure.
        raise SystemExit("Release signing failed. Check the input APK, SDK tools and signing credentials.") from None


if __name__ == "__main__":
    main()
