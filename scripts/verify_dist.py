"""Validate the actual built packages, never mistake an empty catalogue for playback."""
import json
from pathlib import Path
from zipfile import ZipFile
import sys
import re
root = Path(sys.argv[1])
expected = {'FilmPerTutti', 'CasaCinema', 'GuardoSerie'}
source_root = Path(__file__).resolve().parents[1]
version_matches = re.findall(r"(?m)^version\s*=\s*(\d+)\s*$", (source_root / 'GuardoSerie/build.gradle.kts').read_text())
assert len(version_matches) == 1, 'Expected one integer GuardoSerie source version'
versions = {'FilmPerTutti': 8, 'CasaCinema': 8, 'GuardoSerie': int(version_matches[0])}
entries = json.loads((root / 'plugins.json').read_text())
assert len(entries) == 3, entries
packages = list(root.glob('*.cs3'))
assert {p.stem for p in packages} == expected
for package in packages:
    with ZipFile(package) as archive:
        assert archive.testzip() is None
        assert archive.read('classes.dex').startswith(b'dex\n')
        manifest = json.loads(archive.read('manifest.json'))
        assert manifest['version'] == versions[package.stem], manifest
        assert package.stem.lower() in manifest['pluginClassName'].lower(), manifest
    matching = [e for e in entries if e['url'].endswith('/' + package.name)]
    assert len(matching) == 1, entries
    assert matching[0]['version'] == versions[package.stem]
    assert matching[0]['fileSize'] == package.stat().st_size
repo = json.loads((root / 'repo.json').read_text())
assert repo['pluginLists'] == ['https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/plugins.json']
if len(sys.argv) > 2:
    previous = Path(sys.argv[2])
    old_entries = json.loads((previous / 'plugins.json').read_text())
    for name in ('FilmPerTutti', 'CasaCinema'):
        assert (root / (name + '.cs3')).read_bytes() == (previous / (name + '.cs3')).read_bytes()
        assert [e for e in entries if e['internalName'] == name] == [e for e in old_entries if e['internalName'] == name]
print(f"Verified: package manifests match expected versions {versions}; ZIP/DEX and catalogue sizes valid.")
