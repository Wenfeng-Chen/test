#!/usr/bin/env python3
"""通用提交脚本：从 JSON 文件加载 TestPlan 并提交"""
import json
import sys
import urllib.request
from pathlib import Path

MASTER = "http://localhost:8080"


def submit(plan_path: str) -> None:
    plan = json.loads(Path(plan_path).read_text(encoding="utf-8"))
    req = urllib.request.Request(
        f"{MASTER}/api/tasks",
        data=json.dumps(plan).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
        task_id = result["data"]["taskId"]
        print(f"[{plan.get('mode')}] {plan.get('name')} -> taskId={task_id}")
        print(f"dashboard: {MASTER}/index.html")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python submit_from_json.py scripts/testplans/standalone.json")
        sys.exit(1)
    submit(sys.argv[1])
