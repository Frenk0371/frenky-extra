#!/usr/bin/env python3
"""Monitor every provider listed in config/domains.json. Never auto-trust a new host."""
import json
import os
import ssl
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit

CONFIG = Path("config/domains.json")
OUTPUT = Path("domain-health-report.json")

class StrictRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        endpoint = urlsplit(newurl)
        if endpoint.scheme != "https" or not endpoint.hostname or endpoint.username or endpoint.password:
            raise ValueError("Unsafe redirect")
        return super().redirect_request(req, fp, code, msg, headers, newurl)

def check(source, opener):
    url = source.get("current_url", "")
    p = urlsplit(url)
    result = {"id": source.get("id", "unknown"), "configured_host": p.hostname, "status": "unknown"}
    if p.scheme != "https" or not p.hostname or p.username or p.password:
        result["status"] = "invalid_config"
        return result
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "FrenkyHealthCheck/1.0"}, method="GET")
        with opener.open(request, timeout=12) as response:
            final_host = urlsplit(response.geturl()).hostname
            result.update({"http_status": response.status, "final_host": final_host,
                           "status": "redirect_review_required" if final_host != p.hostname else "reachable"})
    except urllib.error.HTTPError as error:
        result.update({"status": "http_error", "http_status": error.code})
    except Exception as error:
        result.update({"status": "connection_error", "error_type": type(error).__name__})
    return result

def main():
    data = json.loads(CONFIG.read_text(encoding="utf-8"))
    sources = data.get("sources")
    if not isinstance(sources, list) or not sources:
        raise SystemExit("No sources in registry")
    opener = urllib.request.build_opener(StrictRedirect(), urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    results = [check(source, opener) for source in sources]
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
    # A redirect to a different host needs manual review, but is not by itself
    # a monitor execution failure. Never automatically trust or update that host.
    failing_statuses = {"invalid_config", "http_error", "connection_error"}
    if any(item["status"] in failing_statuses for item in results):
        raise SystemExit(1)

if __name__ == "__main__":
    main()
