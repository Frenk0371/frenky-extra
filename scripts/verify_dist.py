"""Validate the actual built packages, never mistake an empty catalogue for playback."""
import json
from pathlib import Path
from zipfile import ZipFile
import sys
root = Path(sys.argv[1])
expected = {'FilmPerTutti', 'CasaCinema', 'GuardoSerie'}
entries = json.loads((root / 'plugins.json').read_text())
assert len(entries) == 3, entries
packages = list(root.glob('*.cs3'))
assert {p.stem for p in packages} == expected
for package in packages:
    with ZipFile(package) as archive:
        assert archive.testzip() is None
        assert archive.read('classes.dex').startswith(b'dex\n')
        manifest = json.loads(archive.read('manifest.json'))
        assert manifest['version'] == 8, manifest
        assert package.stem.lower() in manifest['pluginClassName'].lower(), manifest
    matching = [e for e in entries if e['url'].endswith('/' + package.name)]
    assert len(matching) == 1, entries
    assert matching[0]['version'] == 8
    assert matching[0]['fileSize'] == package.stat().st_size
repo = json.loads((root / 'repo.json').read_text())
assert repo['pluginLists'] == ['https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/plugins.json']
print('Verified: 3 independent version-8 DEX packages and matching manifests.')
