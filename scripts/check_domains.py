#!/usr/bin/env python3
"""Monitor domains; optionally accept HTTPS migrations with identity checks."""
import argparse
import ipaddress
import json
import os
import ssl
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit

CONFIG = Path("config/domains.json")
OUTPUT = Path("domain-health-report.json")

def valid_url(url):
    try:
        p = urlsplit(url)
        if (p.scheme != "https" or not p.hostname or p.username or p.password
                or p.port not in (None, 443) or p.query or p.fragment):
            return False
        host = p.hostname.lower()
        if "." not in host or host.endswith((".local", ".localhost", ".internal")):
            return False
        try:
            ipaddress.ip_address(host)
            return False
        except ValueError:
            return True
    except (TypeError, ValueError):
        return False


class IdentityPage(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_title = False
        self.title = ""
        self.links = 0

    def handle_starttag(self, tag, attrs):
        if tag == "title":
            self.in_title = True
        if tag == "form" or (tag == "a" and any(k == "href" for k, _ in attrs)):
            self.links += 1

    def handle_endtag(self, tag):
        if tag == "title":
            self.in_title = False

    def handle_data(self, data):
        if self.in_title:
            self.title += data


class StrictRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if not valid_url(newurl):
            raise ValueError("Unsafe redirect")
        return super().redirect_request(req, fp, code, msg, headers, newurl)

def check(source, opener, update=False):
    url = source.get("current_url", "")
    p = urlsplit(url)
    result = {"id": source.get("id", "unknown"), "configured_host": p.hostname, "status": "unknown"}
    if not valid_url(url):
        result["status"] = "invalid_config"
        return result
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "FrenkyHealthCheck/1.0"}, method="GET")
        with opener.open(request, timeout=12) as response:
            final_url = response.geturl()
            if not valid_url(final_url):
                raise ValueError("Unsafe final URL")
            final = urlsplit(final_url)
            final_host = final.hostname
            result.update({"http_status": response.status, "final_host": final_host,
                           "status": "redirect_review_required" if final_host != p.hostname else "reachable"})
            if final_host != p.hostname:
                # Only a server redirect from the configured host can nominate a
                # replacement. No search-engine guesses or arbitrary new TLDs.
                body = response.read(600001)
                page = IdentityPage()
                page.feed(body.decode("utf-8", errors="replace"))
                normalize = lambda value: "".join(c for c in value.lower() if c.isalnum())
                expected = normalize(source.get("name", ""))
                original = source.get("original_url", "")
                previous_review = source.get("identity_review", {})
                anchored = (urlsplit(original).hostname == p.hostname or
                            (previous_review.get("approved") is True and
                             previous_review.get("verified_domain") == p.hostname and
                             urlsplit(previous_review.get("official_evidence_url", "")).hostname
                             == urlsplit(original).hostname))
                matches = (response.status == 200 and len(body) <= 600000 and
                           "text/html" in response.headers.get("Content-Type", "").lower() and
                           final.path in ("", "/") and anchored and valid_url(original) and
                           bool(expected) and expected in normalize(page.title) and page.links > 10)
                result["identity_verified"] = matches
                if matches:
                    result["status"] = "verified_redirect"
                    if update:
                        source["current_url"] = final_url.rstrip("/") + "/"
                        source["identity_review"] = {
                            "approved": True,
                            "verified_domain": final_host,
                            "official_evidence_url": original,
                            "reviewed_at": datetime.now(timezone.utc).isoformat(),
                            "method": "https_redirect_and_site_identity",
                            "redirect_from": url,
                        }
                        result["status"] = "updated"
    except urllib.error.HTTPError as error:
        result.update({"status": "http_error", "http_status": error.code})
    except Exception as error:
        result.update({"status": "connection_error", "error_type": type(error).__name__})
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--update", action="store_true", help="Persist verified HTTPS migrations")
    args = parser.parse_args()
    data = json.loads(CONFIG.read_text(encoding="utf-8"))
    sources = data.get("sources")
    if not isinstance(sources, list) or not sources:
        raise SystemExit("No sources in registry")
    opener = urllib.request.build_opener(StrictRedirect(), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    results = [check(source, opener, args.update) for source in sources]
    if any(item["status"] == "updated" for item in results):
        data["updated_at"] = datetime.now(timezone.utc).isoformat()
        temporary = CONFIG.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        temporary.replace(CONFIG)
    report = {"checked_at": datetime.now(timezone.utc).isoformat(), "sources": results}
    OUTPUT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for item in results:
        print(f"{item['id']}: {item['status']}")
    summary = os.getenv("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write("## Controllo sorgenti Frenky\n\n| Sorgente | Stato | Host configurato | Host finale |\n|---|---|---|---|\n")
            for item in results:
                f.write(f"| {item['id']} | {item['status']} | {item.get('configured_host', '')} | {item.get('final_host', '')} |\n")
    failing_statuses = {"invalid_config", "http_error", "connection_error", "redirect_review_required"}
    if any(item["status"] in failing_statuses for item in results):
        raise SystemExit(1)

if __name__ == "__main__":
    main()
