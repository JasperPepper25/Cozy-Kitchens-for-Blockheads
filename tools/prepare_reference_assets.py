#!/usr/bin/env python3
"""Fetch checksum-verified development inputs; none are bundled with the mod."""
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def metadata(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'CozyKitchens-development/0.3.0'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def download(url, path, algorithm, digest):
    if path.is_file() and hashlib.new(algorithm, path.read_bytes()).hexdigest() == digest:
        print('Verified cached reference:', path.relative_to(ROOT))
        return
    with urllib.request.urlopen(url, timeout=120) as response:
        data = response.read()
    assert hashlib.new(algorithm, data).hexdigest() == digest, (path, 'Checksum mismatch')
    path.parent.mkdir(parents=True, exist_ok=True)
    # Replace a local cache symlink rather than modifying the Gradle cache through it.
    path.unlink(missing_ok=True)
    path.write_bytes(data)
    print('Downloaded verified reference:', path.relative_to(ROOT))


def main():
    manifest = metadata('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
    version = next(v for v in manifest['versions'] if v['id'] == '1.20.1')
    client = metadata(version['url'])['downloads']['client']
    download(client['url'], ROOT / 'research/client.jar', 'sha1', client['sha1'])
    backport = metadata('https://api.modrinth.com/v2/project/vanillabackport/version/1.1.7.10')
    jar = next(f for f in backport['files'] if f['primary'])
    download(jar['url'], ROOT / 'vendor/VanillaBackport-forge-1.20.1-1.1.7.10.jar', 'sha512', jar['hashes']['sha512'])


if __name__ == '__main__':
    main()
