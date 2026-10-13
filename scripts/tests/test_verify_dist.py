"""Regression checks for source-version drift and preserved release packages."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]

class DistributionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.dist = self.root / 'dist'
        self.previous = self.root / 'previous'
        self.dist.mkdir()
        self.previous.mkdir()
        import re
        self.version = int(re.search(r'(?m)^version\s*=\s*(\d+)', (ROOT / 'GuardoSerie/build.gradle.kts').read_text())[1])
        self.entries = []
        for name, version in [('FilmPerTutti', 8), ('CasaCinema', 8), ('GuardoSerie', self.version)]:
            package = self.dist / (name + '.cs3')
            with ZipFile(package, 'w') as archive:
                archive.writestr('classes.dex', b'dex\nsynthetic-validator-fixture')
                archive.writestr('manifest.json', json.dumps({'version': version, 'pluginClassName': name + 'Plugin'}))
            entry = {'internalName': name, 'version': version, 'fileSize': package.stat().st_size, 'url': 'https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/' + package.name}
            self.entries.append(entry)
            (self.previous / package.name).write_bytes(package.read_bytes())
        self.write_entries()
        (self.previous / 'plugins.json').write_text(json.dumps(self.entries))
        (self.dist / 'repo.json').write_text((ROOT / 'repo.json').read_text())

    def write_entries(self):
        (self.dist / 'plugins.json').write_text(json.dumps(self.entries))

    def validate(self):
        return subprocess.run([sys.executable, str(ROOT / 'scripts/verify_dist.py'), str(self.dist), str(self.previous)], capture_output=True)

    def test_current_source_version_is_accepted(self):
        self.assertEqual(self.validate().returncode, 0)

    def test_stale_catalogue_version_is_rejected(self):
        self.entries[-1]['version'] = self.version - 1
        self.write_entries()
        self.assertNotEqual(self.validate().returncode, 0)

    def test_modified_movie_package_is_rejected(self):
        package = self.dist / 'CasaCinema.cs3'
        with ZipFile(package, 'a') as archive:
            archive.writestr('unexpected.txt', 'change')
        self.entries[1]['fileSize'] = package.stat().st_size
        self.write_entries()
        self.assertNotEqual(self.validate().returncode, 0)

    def test_stale_manifest_version_is_rejected(self):
        package = self.dist / 'GuardoSerie.cs3'
        with ZipFile(package, 'w') as archive:
            archive.writestr('classes.dex', b'dex\nfixture')
            archive.writestr('manifest.json', json.dumps({'version': self.version - 1, 'pluginClassName': 'GuardoSeriePlugin'}))
        self.entries[-1]['fileSize'] = package.stat().st_size
        self.write_entries()
        self.assertNotEqual(self.validate().returncode, 0)

if __name__ == '__main__':
    unittest.main()
