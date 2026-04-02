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


TICKETS_PATH = PROJECT_ROOT / "rag_files" / "support" / "tickets.json"


def load_tickets() -> list[dict]:
    try:
        with open(TICKETS_PATH, encoding="utf-8") as f:
            data = json.load(f)
        return data.get("tickets", [])
    except Exception as exc:
        return []


def handle_tool_call(name: str, arguments: dict) -> dict:
    if name == "get_ticket":
        ticket_id = arguments.get("ticket_id", "").strip()
        tickets = load_tickets()
        ticket = next((t for t in tickets if t.get("id") == ticket_id), None)
        if ticket is None:
            return make_text_result(
                f"Ticket '{ticket_id}' not found.",
                {"found": False, "ticketId": ticket_id},
            )
        text = (
            f"Ticket {ticket['id']}: {ticket['subject']}\n"
            f"User: {ticket['userName']} | Status: {ticket['status']} | Category: {ticket['category']}\n"
            f"App version: {ticket['appVersion']} | Created: {ticket['createdAt']}\n"
            f"Description: {ticket['description']}"
        )
        return make_text_result(text, {"found": True, "ticket": ticket})

    if name == "list_tickets":
        tickets = load_tickets()
        if not tickets:
            return make_text_result("No tickets found.", {"tickets": []})
        lines = [f"{t['id']} [{t['status']}] {t['subject']} — {t['userName']}" for t in tickets]
        text = "Support tickets:\n" + "\n".join(lines)
        summary = [{"id": t["id"], "subject": t["subject"], "status": t["status"], "userName": t["userName"]} for t in tickets]
        return make_text_result(text, {"tickets": summary})

    if name == "read_file":
        rel_path = arguments.get("path", "").strip()
        target = (PROJECT_ROOT / rel_path).resolve()
        if not str(target).startswith(str(PROJECT_ROOT)):
            return make_text_result("Access denied: path outside project root.", {"error": "outside_root"})
        if not target.exists() or not target.is_file():
            return make_text_result(f"File not found: {rel_path}", {"error": "not_found"})
        try:
            content = target.read_text(encoding="utf-8", errors="replace")
            lines = content.splitlines()
            max_lines = int(arguments.get("max_lines", 300))
            truncated = len(lines) > max_lines
            result_lines = lines[:max_lines]
            text = "\n".join(result_lines)
            if truncated:
                text += f"\n... [truncated, showing {max_lines}/{len(lines)} lines]"
            return make_text_result(text, {"path": rel_path, "lines": len(lines), "truncated": truncated})
        except Exception as exc:
            return make_text_result(f"Error reading file: {exc}", {"error": str(exc)})

    if name == "search_in_files":
        query = arguments.get("query", "").strip()
        extensions = arguments.get("extensions", [".kt", ".py", ".java", ".xml", ".md", ".json"])
        if isinstance(extensions, str):
            extensions = [e.strip() for e in extensions.split(",")]
        max_results = int(arguments.get("max_results", 50))
        exclude_dirs = {".git", "build", ".gradle", ".kotlin", "__pycache__", "node_modules"}
        matches = []
        try:
            for path in PROJECT_ROOT.rglob("*"):
                if any(part in exclude_dirs for part in path.parts):
                    continue
                if not path.is_file():
                    continue
                if extensions and path.suffix not in extensions:
                    continue
                try:
                    text = path.read_text(encoding="utf-8", errors="replace")
                    for i, line in enumerate(text.splitlines(), 1):
                        if query.lower() in line.lower():
                            rel = str(path.relative_to(PROJECT_ROOT)).replace("\\", "/")
                            matches.append({"file": rel, "line": i, "text": line.strip()})
                            if len(matches) >= max_results:
                                break
                except Exception:
                    pass
                if len(matches) >= max_results:
                    break
        except Exception as exc:
            return make_text_result(f"Search error: {exc}", {"error": str(exc)})
        if not matches:
            return make_text_result(f"No matches for '{query}'.", {"matches": [], "total": 0})
        lines_out = [f"{m['file']}:{m['line']}: {m['text']}" for m in matches]
        summary = f"Found {len(matches)} match(es) for '{query}':\n" + "\n".join(lines_out)
        return make_text_result(summary, {"matches": matches, "total": len(matches)})

    if name == "write_file":
        rel_path = arguments.get("path", "").strip()
        content = arguments.get("content", "")
        target = (PROJECT_ROOT / rel_path).resolve()
        if not str(target).startswith(str(PROJECT_ROOT)):
            return make_text_result("Access denied: path outside project root.", {"error": "outside_root"})
        try:
            target.parent.mkdir(parents=True, exist_ok=True)
            existed = target.exists()
            target.write_text(content, encoding="utf-8")
            action = "updated" if existed else "created"
            return make_text_result(
                f"File {action}: {rel_path} ({len(content)} chars)",
                {"path": rel_path, "action": action, "size": len(content)}
            )
        except Exception as exc:
            return make_text_result(f"Error writing file: {exc}", {"error": str(exc)})

    if name == "list_files":
        pattern = arguments.get("pattern", "**/*.kt")
        exclude_dirs = {".git", "build", ".gradle", ".kotlin", "__pycache__"}
        try:
            files = []
            for path in PROJECT_ROOT.glob(pattern):
                if any(part in exclude_dirs for part in path.parts):
                    continue
                if path.is_file():
                    rel = str(path.relative_to(PROJECT_ROOT)).replace("\\", "/")
                    files.append(rel)
            files.sort()
            text = f"Files matching '{pattern}' ({len(files)}):\n" + "\n".join(files[:100])
            return make_text_result(text, {"files": files[:100], "total": len(files)})
        except Exception as exc:
            return make_text_result(f"Error listing files: {exc}", {"error": str(exc)})

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
            "name": "get_ticket",
            "description": "Get a support ticket by ID from tickets.json.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "ticket_id": {
                        "type": "string",
                        "description": "The ticket ID, e.g. T-001"
                    }
                },
                "required": ["ticket_id"],
                "additionalProperties": False,
            },
        },
        {
            "name": "list_tickets",
            "description": "List all support tickets (id, subject, status, userName).",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
        {
            "name": "read_file",
            "description": "Read a file from the project by relative path.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "path": {"type": "string", "description": "Relative path from project root, e.g. app/src/main/.../Foo.kt"},
                    "max_lines": {"type": "integer", "description": "Max lines to return (default 300)", "default": 300}
                },
                "required": ["path"],
                "additionalProperties": False,
            },
        },
        {
            "name": "search_in_files",
            "description": "Search for a text query across project files. Returns file:line:text matches.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Text to search for (case-insensitive)"},
                    "extensions": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "File extensions to include, e.g. [\".kt\", \".py\"]. Default: .kt .py .java .xml .md .json"
                    },
                    "max_results": {"type": "integer", "description": "Max matches to return (default 50)", "default": 50}
                },
                "required": ["query"],
                "additionalProperties": False,
            },
        },
        {
            "name": "write_file",
            "description": "Create or overwrite a file in the project with given content.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "path": {"type": "string", "description": "Relative path from project root"},
                    "content": {"type": "string", "description": "File content to write"}
                },
                "required": ["path", "content"],
                "additionalProperties": False,
            },
        },
        {
            "name": "list_files",
            "description": "List project files matching a glob pattern.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "pattern": {"type": "string", "description": "Glob pattern, e.g. **/*.kt or rag_files/**/*.md", "default": "**/*.kt"}
                },
                "additionalProperties": False,
            },
        },
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
