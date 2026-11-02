"""Run on the owner's computer: generate a persistent key, optionally upload GitHub secrets.
No password/private key is printed. Keep the output directory outside every Git checkout.
"""
import argparse
import base64
import getpass
import hashlib
import os
from pathlib import Path
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, default=Path.home() / '.trainpilot-signing')
    parser.add_argument('--upload', action='store_true', help='Set encrypted repository secrets using your own authenticated gh CLI')
    args = parser.parse_args()
    if not sys.stdin.isatty():
        parser.error("Use an interactive terminal so password input cannot echo")
    directory = args.directory.resolve()
    for parent in (directory, *directory.parents):
        if (parent / '.git').exists():
            parser.error('Choose a directory outside Git repositories')
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    directory.chmod(0o700)
    key = directory / 'trainpilot-release.jks'
    encoded = directory / 'TRAINPILOT_RELEASE_KEYSTORE_BASE64.txt'
    if key.exists() or key.is_symlink() or encoded.exists() or encoded.is_symlink():
        parser.error('Existing key found: refusing to replace it')
    password = getpass.getpass('New keystore/key password (at least 16 characters): ')
    if len(password) < 16 or password != getpass.getpass('Repeat password: '):
        parser.error('Password too short or confirmation differs')
    environment = os.environ.copy()
    environment['TRAINPILOT_NEW_KEY_PASSWORD'] = password
    os.umask(0o077)
    subprocess.run(['keytool', '-genkeypair', '-keystore', str(key), '-storetype', 'JKS',
        '-alias', 'upload', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
        '-dname', 'CN=TrainPilot,OU=Android,O=TrainPilot',
        '-storepass:env', 'TRAINPILOT_NEW_KEY_PASSWORD', '-keypass:env', 'TRAINPILOT_NEW_KEY_PASSWORD'],
        env=environment, check=True, capture_output=True)
    key.chmod(0o600)
    certificate = subprocess.check_output(['keytool', '-exportcert', '-keystore', str(key),
        '-alias', 'upload', '-storepass:env', 'TRAINPILOT_NEW_KEY_PASSWORD'], env=environment, stderr=subprocess.PIPE)
    sha1 = hashlib.sha1(certificate).hexdigest()
    sha256 = hashlib.sha256(certificate).hexdigest()
    encoded.write_bytes(base64.b64encode(key.read_bytes()))
    encoded.chmod(0o600)
    (directory / 'PUBLIC_CERTIFICATE.txt').write_text(f'Package: com.TrainPilot.com\nSHA1: {sha1}\nSHA256: {sha256}\n')
    print(f'Key saved at {key}. Back up this directory securely and save the password in your password manager.')
    print(f'PUBLIC SHA1: {sha1}\nPUBLIC SHA256: {sha256}')
    if args.upload:
        values = {'TRAINPILOT_RELEASE_KEYSTORE_BASE64': encoded.read_text(),
            'STORE_PASSWORD': password, 'KEY_PASSWORD': password, 'TRAINPILOT_RELEASE_CERT_SHA256': sha256}
        for name, value in values.items():
            result = subprocess.run(['gh', 'secret', 'set', name, '--repo', 'fuloplevente1998/Native-AI-Studio', '--env', 'android-signing'],
                input=value, text=True, capture_output=True)
            if result.returncode:
                raise RuntimeError(f'Could not set {name}; check your gh authentication/permissions. Local key preserved.')
            print(f'Encrypted android-signing environment secret set: {name}')
    else:
        print('GitHub Secrets: paste the encoded key file into TRAINPILOT_RELEASE_KEYSTORE_BASE64,')
        print('your password into STORE_PASSWORD and KEY_PASSWORD; the public SHA256 into TRAINPILOT_RELEASE_CERT_SHA256.')


if __name__ == '__main__':
    main()
