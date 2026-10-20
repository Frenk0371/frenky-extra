import copy
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('domains', Path(__file__).parents[1] / 'check_domains.py')
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)

SOURCE = {'id': 'example', 'name': 'Example', 'original_url': 'https://old.example/',
          'current_url': 'https://old.example/'}
HTML = '<title>Example - Catalogo</title>' + '<a href="/film">Film</a>' * 11

class Response(io.BytesIO):
    status = 200
    headers = {'Content-Type': 'text/html; charset=utf-8'}
    def __init__(self, url='https://new.example/', body=HTML):
        super().__init__(body.encode())
        self.url = url
    def geturl(self):
        return self.url

class Opener:
    def __init__(self, **kw): self.kw = kw
    def open(self, *args, **kwargs): return Response(**self.kw)

class DomainTests(unittest.TestCase):
    def test_verified_migration_is_compatible_with_existing_resolver(self):
        source = copy.deepcopy(SOURCE)
        self.assertEqual(d.check(source, Opener(), True)['status'], 'updated')
        self.assertEqual(source['current_url'], 'https://new.example/')
        review = source['identity_review']
        self.assertTrue(review['approved'])
        self.assertEqual(review['verified_domain'], 'new.example')
        self.assertEqual(review['official_evidence_url'], SOURCE['original_url'])
        self.assertTrue(review['reviewed_at'])

    def test_monitor_does_not_mutate_registry(self):
        source = copy.deepcopy(SOURCE)
        self.assertEqual(d.check(source, Opener())['status'], 'verified_redirect')
        self.assertEqual(source, SOURCE)

    def test_repeated_migration_preserves_original_trust_anchor(self):
        source = copy.deepcopy(SOURCE)
        d.check(source, Opener(), True)
        result = d.check(source, Opener(url='https://third.example/'), True)
        self.assertEqual(result['status'], 'updated')
        self.assertEqual(source['identity_review']['official_evidence_url'], SOURCE['original_url'])

    def test_unrelated_page_and_non_root_redirect_are_rejected(self):
        for kwargs in ({'body': HTML.replace('Example', 'Advertisement')},
                       {'body': '<title>Example</title>'},
                       {'body': HTML + 'x' * 600001},
                       {'url': 'https://new.example/login'}):
            with self.subTest(kwargs=list(kwargs)):
                source = copy.deepcopy(SOURCE)
                self.assertEqual(d.check(source, Opener(**kwargs), True)['status'], 'redirect_review_required')
                self.assertEqual(source, SOURCE)

    def test_unapproved_intermediate_host_is_rejected(self):
        source = dict(SOURCE, current_url='https://unreviewed.example/')
        self.assertEqual(d.check(source, Opener(), True)['status'], 'redirect_review_required')

    def test_unsafe_urls(self):
        for url in ('http://new.example/', 'https://a:b@new.example/',
                    'https://new.example:444/', 'https://127.0.0.1/',
                    'https://[::1]/', 'https://localhost/', 'https://a.local/',
                    'https://new.example/?token=a', 'https://new.example/#x'):
            with self.subTest(url=url):
                self.assertFalse(d.valid_url(url))
                with self.assertRaises(ValueError):
                    d.StrictRedirect().redirect_request(None, None, 301, '', {}, url)

    def test_valid_updates_saved_even_when_another_provider_fails(self):
        with tempfile.TemporaryDirectory() as folder:
            config, output = Path(folder) / 'domains.json', Path(folder) / 'report.json'
            config.write_text(json.dumps({'sources': [dict(SOURCE), dict(SOURCE, id='broken')]}))
            real_check = d.check
            def check(source, opener, update):
                if source['id'] == 'broken':
                    return {'id': 'broken', 'status': 'connection_error'}
                return real_check(source, Opener(), update)
            with patch.object(d, 'CONFIG', config), patch.object(d, 'OUTPUT', output), \
                 patch.object(d, 'check', side_effect=check), patch('sys.argv', ['check', '--update']), \
                 patch.dict(d.os.environ, {'GITHUB_STEP_SUMMARY': ''}):
                with self.assertRaises(SystemExit): d.main()
            self.assertEqual(json.loads(config.read_text())['sources'][0]['current_url'], 'https://new.example/')
            self.assertTrue(output.exists())

if __name__ == '__main__': unittest.main()
