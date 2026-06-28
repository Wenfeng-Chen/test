#!/usr/bin/env python3
"""RPS 模式压测：固定每秒请求数"""
import json
import urllib.request

MASTER = "http://localhost:8080"

plan = {
    "name": "fixed-rps-ping",
    "mode": "STANDALONE",
    "load": {
        "mode": "FIXED_RPS",
        "targetRps": 50,
        "durationSeconds": 10,
    },
    "requests": [
        {"method": "GET", "url": f"{MASTER}/api/demo/ping"}
    ],
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
    print(f"[FIXED_RPS] taskId={task_id}, dashboard={MASTER}/index.html")
