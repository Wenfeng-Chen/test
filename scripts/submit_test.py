#!/usr/bin/env python3
"""示例：通过 Python 脚本提交压测任务"""
import json
import urllib.request

MASTER = "http://localhost:8080"

plan = {
    "name": "demo-ping-test",
    "mode": "STANDALONE",
    "load": {"concurrency": 20, "durationSeconds": 10},
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
    print("Task submitted:", result)
    task_id = result["data"]["taskId"]
    print(f"Open dashboard: {MASTER}/index.html and enter task ID: {task_id}")
