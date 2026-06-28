#!/usr/bin/env python3
"""分布式模式压测：Master 拆分任务给 Worker"""
import json
import urllib.request

MASTER = "http://localhost:8080"

plan = {
    "name": "distributed-ping",
    "mode": "DISTRIBUTED",
    "load": {"concurrency": 60, "durationSeconds": 15},
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
    print(f"[DISTRIBUTED] taskId={task_id}, dashboard={MASTER}/index.html")
