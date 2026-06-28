#!/usr/bin/env python3
"""单机模式压测：Master 本地发压"""
import json
import urllib.request

MASTER = "http://localhost:8080"

plan = {
    "name": "standalone-ping",
    "mode": "STANDALONE",
    "load": {"concurrency": 30, "durationSeconds": 10},
    "requests": [
        {"method": "GET", "url": f"{MASTER}/api/demo/ping"}
    ]
}

req = urllib.request.Request(
    f"{MASTER}/api/tasks",
    data=json.dumps(plan).encode(),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req) as resp:
    result = json.loads(resp.read())
    task_id = result["data"]["taskId"]
    print(f"[STANDALONE] taskId={task_id}, dashboard={MASTER}/index.html")
