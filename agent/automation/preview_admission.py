#!/usr/bin/env python3
"""Preview-only admission: describe what would launch, never launch it."""
from __future__ import annotations
import argparse, json, sqlite3

def preview(db: str, allowlist: set[str]) -> dict:
    with sqlite3.connect(db) as conn:
        row = conn.execute("select event_id, issue_key, payload from events order by rowid desc limit 1").fetchone()
    if not row:
        return {"eligible": False, "reason": "no events"}
    event_id, issue_key, payload = row
    data = json.loads(payload)
    eligible = issue_key in allowlist
    return {"eligible": eligible, "reason": "pilot allowlist" if eligible else "issue is not allowlisted", "event_id": event_id, "issue_key": issue_key, "action": "would claim and launch one bounded worker" if eligible else "no action"}

if __name__ == "__main__":
    p = argparse.ArgumentParser(); p.add_argument("db"); p.add_argument("--allow", action="append", default=[])
    print(json.dumps(preview(p.parse_args().db, set(p.parse_args().allow)), indent=2))
