"""Fetch the API-28 and API-34 Robolectric SDKs with pinned checksums and bounded retries.

Robolectric's own downloader has failed with Maven 429 responses before any test
could run. Populate its normal Maven cache; never substitute SDKs or skip tests.
"""
import argparse
import hashlib
import pathlib
import time
import urllib.error
import urllib.request

SDK_CHECKSUMS = {
    "14-robolectric-10818077-i7": {
        "pom": "fb97a3028bb1eb4c4b3d1ab9c23c8b6b245842139f76194afc16afe002a212c2f93baff400a83cb6f35e525d0753e5b13aeda172fbdf0f628b25d21aa4a065d6",
        "jar": "7cdc0b1306e7865f1acd25a363073149654caee5b8e278c5ae37ff4724185724eff39f0b0111800b521e640bb28698d9d2e655134a801569a0f07470e7b5f455",
    },
    "9-robolectric-4913185-2-i7": {
        "pom": "0c0048dd6cb9dc1fc81038ae9ef77383c842941e17ca5e961d0f9d3f0dda91c2ce31ca8ca11634e613927dbcc75259e39ed5e39ad5406a0ed4316576e0dc7a77",
        "jar": "24687b9ea919eb20f5e27e47274f2aa711e98eb332d4769d34044a3b4952a71393831d0a3f61ba726dc8fe220673a76138c11a0716f304999e5b029146fe88eb",
    },
}
REPOSITORIES = ("https://repo.maven.apache.org/maven2", "https://repo1.maven.org/maven2")


def checksum(path):
    digest = hashlib.sha512()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def prefetch(repository_root, version, checksums):
    directory = repository_root / "org/robolectric/android-all-instrumented" / version
    directory.mkdir(parents=True, exist_ok=True)
    for extension, expected in checksums.items():
        name = f"android-all-instrumented-{version}.{extension}"
        target = directory / name
        if target.is_file() and checksum(target) == expected:
            target.with_name(name + ".sha512").write_text(expected + "\n")
            print(f"Verified cached Robolectric SDK: {name}")
            continue
        temporary = directory / (name + ".part")
        last_error = None
        for attempt in range(3):
            for repository in REPOSITORIES:
                try:
                    url = f"{repository}/org/robolectric/android-all-instrumented/{version}/{name}"
                    with urllib.request.urlopen(url, timeout=120) as response, temporary.open("wb") as out:
                        for block in iter(lambda: response.read(1024 * 1024), b""):
                            out.write(block)
                    if checksum(temporary) != expected:
                        raise ValueError(f"SDK checksum mismatch: {name}")
                    temporary.replace(target)
                    target.with_name(name + ".sha512").write_text(expected + "\n")
                    print(f"Downloaded and SHA512-verified Robolectric SDK: {name}")
                    last_error = None
                    break
                except (OSError, ValueError) as error:
                    last_error = error
                    temporary.unlink(missing_ok=True)
                    print(f"SDK download retry {attempt + 1}: {type(error).__name__}: {error}")
            if last_error is None:
                break
            if attempt < 2:
                time.sleep(5 * (attempt + 1))
        if last_error is not None:
            raise RuntimeError(f"Cannot fetch verified Robolectric SDK: {name}") from last_error


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository-root", type=pathlib.Path,
                        default=pathlib.Path.home() / ".m2/repository")
    repository_root = parser.parse_args().repository_root
    for version, checksums in SDK_CHECKSUMS.items():
        prefetch(repository_root, version, checksums)
