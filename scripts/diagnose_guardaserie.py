#!/usr/bin/env python3
"""Read-only diagnostics for candidate GuardaSerie domain. No bypasses or domain changes."""
import json
import urllib.request
import urllib.error
from html.parser import HTMLParser
from urllib.parse import urlparse

URL = "https://guardaseries.wiki/"
class Elements(HTMLParser):
    def __init__(self):
        super().__init__()
        self.counts = {}
        self.title = ""
        self.in_title = False
    def handle_starttag(self, tag, attrs):
        self.counts[tag] = self.counts.get(tag, 0) + 1
        if tag == "title":
            self.in_title = True
    def handle_endtag(self, tag):
        if tag == "title":
            self.in_title = False
    def handle_data(self, data):
        if self.in_title:
            self.title += data

def run():
    out = {"url": URL, "result": "unknown"}
    try:
        req = urllib.request.Request(URL, headers={"User-Agent": "Frenky-Domain-Diagnostic/1.0", "Accept": "text/html"}, method="GET")
        with urllib.request.urlopen(req, timeout=15) as response:
            final = response.geturl()
            out["http_status"] = response.status
            out["final_host"] = urlparse(final).hostname
            if urlparse(final).scheme != "https":
                out["result"] = "unsafe_redirect"
            else:
                raw = response.read(300000)
                parser = Elements()
                parser.feed(raw.decode("utf-8", errors="replace"))
                out.update({"result": "html_received", "page_title": parser.title.strip()[:120],
                            "html_counts": {k: parser.counts.get(k, 0) for k in ["a", "img", "form", "script", "iframe"]}})
    except urllib.error.HTTPError as e:
        out.update({"result": "http_error", "http_status": e.code})
    except Exception as e:
        out.update({"result": "connection_error", "error_type": type(e).__name__})
    with open("guardaserie-diagnostic.json", "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
    print(json.dumps(out, ensure_ascii=False))

if __name__ == "__main__":
    run()
