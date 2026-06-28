#!/usr/bin/env python3
"""阶梯加压模式：并发随阶段递增"""
import json
import urllib.request

MASTER = "http://localhost:8080"

plan = {
    "name": "step-ramp-ping",
    "mode": "STANDALONE",
    "load": {
        "mode": "STEP_RAMP",
        "stages": [
            {"concurrency": 10, "durationSeconds": 5},
            {"concurrency": 30, "durationSeconds": 5},
            {"concurrency": 60, "durationSeconds": 5},
        ],
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
    print(f"[STEP_RAMP] taskId={task_id}, dashboard={MASTER}/index.html")
