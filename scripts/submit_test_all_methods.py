#!/usr/bin/env python3
"""全 HTTP 方法压测：轮询 GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS"""
import json
import urllib.request

MASTER = "http://localhost:8080"
BASE = f"{MASTER}/api/demo/resource"

plan = {
    "name": "all-http-methods",
    "mode": "STANDALONE",
    "load": {"concurrency": 10, "durationSeconds": 10},
    "requests": [
        {"method": "GET", "url": BASE},
        {"method": "POST", "url": BASE, "headers": {"Content-Type": "application/json"}, "body": "{\"k\":\"v\"}"},
        {"method": "PUT", "url": BASE, "headers": {"Content-Type": "application/json"}, "body": "{\"k\":\"v\"}"},
        {"method": "PATCH", "url": BASE, "headers": {"Content-Type": "application/json"}, "body": "{\"k\":\"v\"}"},
        {"method": "DELETE", "url": BASE},
        {"method": "HEAD", "url": BASE},
        {"method": "OPTIONS", "url": BASE},
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
    print(f"[ALL_METHODS] taskId={task_id}, methods=7, dashboard={MASTER}/index.html")
