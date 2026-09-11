import requests
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import json

URL = "http://localhost:8081/api/v1/auth/refresh"

# IMPORTANT: Put the SAME original refresh token here for all 100 requests.
REFRESH_TOKEN = "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiIwMTczODAzYy0wNWI4LTQ5OTMtODU1Ni02NjczZWM2Y2ZlNjkiLCJzdWIiOiIzNTIiLCJpc3MiOiJhcGkubG9kaGkuY29tIiwiaWF0IjoxNzg5MTI1NDI2LCJleHAiOjE3ODkyMTE4MjYsImVtYWlsIjoiekBnbWFpbC5jb20iLCJ0eXBlIjoicmVmcmVzaCJ9.HpE0D1iNC9fzgWNS9ZYkxafiV6Bgmlm1bNhin3De9qQTGbDVqAETc1dq0C6uVQgO"

TOTAL_REQUESTS = 100
MAX_WORKERS = 100


def refresh(request_number):
    started = datetime.now(timezone.utc).isoformat()

    try:
        response = requests.post(
            URL,
            json={"refreshToken": REFRESH_TOKEN},
            timeout=10
        )

        return {
            "request": request_number,
            "status": response.status_code,
            "success": response.status_code == 200,
            "startedAt": started,
            "response": response.text
        }

    except Exception as e:
        return {
            "request": request_number,
            "status": None,
            "success": False,
            "startedAt": started,
            "error": str(e)
        }


results = []

with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
    futures = [
        executor.submit(refresh, i)
        for i in range(1, TOTAL_REQUESTS + 1)
    ]

    for future in as_completed(futures):
        results.append(future.result())


results.sort(key=lambda x: x["request"])

success_count = sum(1 for r in results if r["success"])
failure_count = TOTAL_REQUESTS - success_count

output = {
    "test": "Refresh Token Race Condition",
    "timestamp": datetime.now(timezone.utc).isoformat(),
    "url": URL,
    "totalRequests": TOTAL_REQUESTS,
    "maxWorkers": MAX_WORKERS,
    "successCount": success_count,
    "failureCount": failure_count,
    "expectedSuccessCount": 1,
    "raceConditionPassed": success_count == 1,
    "requests": results
}

with open("output.json", "w", encoding="utf-8") as f:
    json.dump(output, f, indent=2)

with open("output.txt", "w", encoding="utf-8") as f:
    f.write("========== REFRESH TOKEN RACE TEST ==========\n")
    f.write(f"Timestamp       : {output['timestamp']}\n")
    f.write(f"Total requests  : {TOTAL_REQUESTS}\n")
    f.write(f"Success         : {success_count}\n")
    f.write(f"Failed          : {failure_count}\n")
    f.write("Expected success: 1\n")
    f.write(f"PASSED          : {success_count == 1}\n")
    f.write("\n========== REQUEST RESULTS ==========\n")

    for r in results:
        line = (
            f"Request {r['request']:03d} | "
            f"Status: {r['status']} | "
            f"Success: {r['success']}"
        )

        if "error" in r:
            line += f" | Error: {r['error']}"

        f.write(line + "\n")


print("========== RESULT ==========")
print(f"Success: {success_count}")
print(f"Failed : {failure_count}")
print(f"Passed : {success_count == 1}")
print("\nOutput written to:")
print("output.txt")
print("output.json")
