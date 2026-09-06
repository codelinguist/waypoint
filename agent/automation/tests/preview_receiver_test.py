import hashlib
import hmac
import json
import sqlite3
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).parents[1]))
from preview_receiver import record_event


def test_records_and_deduplicates(tmp_path):
    body = json.dumps({"eventId": "evt-1", "issue": {"key": "WAP-1"}}).encode()
    sig = hmac.new(b"secret", body, hashlib.sha256).hexdigest()
    assert record_event(str(tmp_path / "events.db"), body, sig, "secret") == (200, "recorded")
    assert record_event(str(tmp_path / "events.db"), body, sig, "secret") == (200, "recorded")
    with sqlite3.connect(tmp_path / "events.db") as db:
        assert db.execute("select count(*) from events").fetchone()[0] == 1


def test_rejects_bad_signature_and_shape(tmp_path):
    assert record_event(str(tmp_path / "events.db"), b"{}", "bad", "secret")[0] == 401
    body = json.dumps({"eventId": "evt-2"}).encode()
    sig = hmac.new(b"secret", body, hashlib.sha256).hexdigest()
    assert record_event(str(tmp_path / "events.db"), body, sig, "secret")[0] == 400
