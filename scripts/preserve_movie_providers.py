"""Stage GuardaSerie beta while preserving the confirmed movie providers exactly."""
import json
import shutil
import sys
from pathlib import Path

dist, previous = map(Path, sys.argv[1:3])
old = json.loads((previous / 'plugins.json').read_text())
current = json.loads((dist / 'plugins.json').read_text())
for name in ('FilmPerTutti', 'CasaCinema'):
    old_entry, = [e for e in old if e['internalName'] == name]
    assert old_entry['version'] == 8
    shutil.copyfile(previous / (name + '.cs3'), dist / (name + '.cs3'))
    current = [e for e in current if e['internalName'] != name]
    current.append(old_entry)
(dist / 'plugins.json').write_text(json.dumps(current, indent=4) + '\n')
print('Preserved FilmPerTutti and CasaCinema published packages and metadata byte-for-byte.')
