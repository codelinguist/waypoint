import json, sqlite3, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parents[1]))
from preview_admission import preview

def test_allowlisted_event_is_preview_only(tmp_path):
    db = tmp_path / "events.db"
    with sqlite3.connect(db) as c:
        c.execute("create table events (event_id text primary key, issue_key text, payload text)")
        c.execute("insert into events values ('e1','WAP-5',?)", (json.dumps({'issue': {'key':'WAP-5'}}),))
    result = preview(str(db), {'WAP-5'})
    assert result['eligible'] is True
    assert 'would claim' in result['action']
