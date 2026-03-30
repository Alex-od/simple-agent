#!/usr/bin/env python3
import json
import subprocess
import sys
from pathlib import Path


SERVER_NAME = "simpleagent-project-mcp"
SERVER_VERSION = "0.1.0"
PROJECT_ROOT = Path(__file__).resolve().parent.parent


def send_message(payload: dict) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    header = f"Content-Length: {len(body)}\r\n\r\n".encode("ascii")
    sys.stdout.buffer.write(header)
    sys.stdout.buffer.write(body)
    sys.stdout.buffer.flush()


def read_message() -> dict | None:
    headers: dict[str, str] = {}
    while True:
        line = sys.stdin.buffer.readline()
        if not line:
            return None
        if line in (b"\r\n", b"\n"):
            break
        key, value = line.decode("utf-8").split(":", 1)
        headers[key.strip().lower()] = value.strip()

    content_length = int(headers.get("content-length", "0"))
    if content_length <= 0:
        return None

    body = sys.stdin.buffer.read(content_length)
    if not body:
        return None
    return json.loads(body.decode("utf-8"))


def make_text_result(text: str, structured_content: dict | None = None) -> dict:
    result = {
        "content": [
            {
                "type": "text",
                "text": text,
            }
        ]
    }
    if structured_content is not None:
        result["structuredContent"] = structured_content
    return result


def run_git(*args: str) -> tuple[bool, str]:
    try:
        completed = subprocess.run(
            ["git.exe", *args],
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            timeout=10,
            check=False,
        )
    except Exception as exc:
        return False, str(exc)

    output = (completed.stdout or completed.stderr).strip()
    return completed.returncode == 0, output


def handle_tool_call(name: str, arguments: dict) -> dict:
    if name == "git_branch":
        ok, output = run_git("branch", "--show-current")
        branch = output if ok and output else "unknown"
        return make_text_result(
            f"Current git branch: {branch}",
            {"branch": branch, "projectRoot": str(PROJECT_ROOT)},
        )

    if name == "git_status":
        limit = int(arguments.get("limit", 50))
        ok, output = run_git("status", "--short")
        if not ok:
            return make_text_result(
                f"Failed to read git status: {output}",
                {"entries": [], "projectRoot": str(PROJECT_ROOT)},
            )
        entries = [line for line in output.splitlines() if line.strip()][:limit]
        text = "Changed files:\n" + ("\n".join(entries) if entries else "Clean working tree")
        return make_text_result(
            text,
            {"entries": entries, "projectRoot": str(PROJECT_ROOT)},
        )

    raise ValueError(f"Unknown tool: {name}")


def tools_definition() -> list[dict]:
    return [
        {
            "name": "git_branch",
            "description": "Return the current git branch for the project.",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
        {
            "name": "git_status",
            "description": "Return changed files from git status --short.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "limit": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 200,
                        "description": "Maximum number of status entries to return."
                    }
                },
                "additionalProperties": False,
            },
        },
    ]


def main() -> int:
    while True:
        message = read_message()
        if message is None:
            return 0

        method = message.get("method")
        msg_id = message.get("id")
        params = message.get("params", {})

        try:
            if method == "initialize":
                if msg_id is not None:
                    send_message(
                        {
                            "jsonrpc": "2.0",
                            "id": msg_id,
                            "result": {
                                "protocolVersion": "2024-11-05",
                                "capabilities": {
                                    "tools": {}
                                },
                                "serverInfo": {
                                    "name": SERVER_NAME,
                                    "version": SERVER_VERSION,
                                },
                            },
                        }
                    )
                continue

            if method == "notifications/initialized":
                continue

            if method == "ping":
                if msg_id is not None:
                    send_message({"jsonrpc": "2.0", "id": msg_id, "result": {}})
                continue

            if method == "tools/list":
                send_message(
                    {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "result": {
                            "tools": tools_definition()
                        },
                    }
                )
                continue

            if method == "tools/call":
                result = handle_tool_call(params.get("name", ""), params.get("arguments", {}))
                send_message({"jsonrpc": "2.0", "id": msg_id, "result": result})
                continue

            if msg_id is not None:
                send_message(
                    {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "error": {
                            "code": -32601,
                            "message": f"Method not found: {method}",
                        },
                    }
                )
        except Exception as exc:
            if msg_id is not None:
                send_message(
                    {
                        "jsonrpc": "2.0",
                        "id": msg_id,
                        "error": {
                            "code": -32000,
                            "message": str(exc),
                        },
                    }
                )


if __name__ == "__main__":
    raise SystemExit(main())
