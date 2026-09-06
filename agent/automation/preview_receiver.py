#!/usr/bin/env python3
"""Authenticated, durable Jira event receiver for preview mode."""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import sqlite3
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path


def record_event(db: str, body: bytes, signature: str, secret: str, token: str = "") -> tuple[int, str]:
    if token:
        if not hmac.compare_digest(signature, token):
            return 401, "invalid token"
    else:
        expected = hmac.new(secret.encode(), body, hashlib.sha256).hexdigest()
        if not hmac.compare_digest(signature.removeprefix("sha256="), expected):
            return 401, "invalid signature"
    try:
        payload = json.loads(body)
        event_id = str(payload.get("eventId") or payload.get("webhookEvent"))
        issue = payload.get("issue", {})
        key = issue.get("key")
        if not event_id or not key:
            return 400, "eventId and issue.key are required"
    except (ValueError, TypeError):
        return 400, "invalid JSON"
    Path(db).parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(db) as conn:
        conn.execute("CREATE TABLE IF NOT EXISTS events (event_id TEXT PRIMARY KEY, issue_key TEXT NOT NULL, payload TEXT NOT NULL)")
        conn.execute("INSERT OR IGNORE INTO events VALUES (?, ?, ?)", (event_id, key, body.decode()))
        conn.commit()
    return 200, "recorded"


class Handler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:  # noqa: N802
        length = int(self.headers.get("Content-Length", "0"))
        status, message = record_event(os.environ["WAYPOINT_PREVIEW_DB"], self.rfile.read(length), self.headers.get("X-Preview-Token", self.headers.get("X-Webhook-Signature", "")), os.environ.get("WAYPOINT_PREVIEW_SECRET", ""), os.environ.get("WAYPOINT_PREVIEW_TOKEN", ""))
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": message, "preview": True}).encode())

    def log_message(self, *_: object) -> None:
        return


if __name__ == "__main__":
    HTTPServer((os.getenv("WAYPOINT_PREVIEW_HOST", "127.0.0.1"), int(os.getenv("WAYPOINT_PREVIEW_PORT", "8088"))), Handler).serve_forever()
