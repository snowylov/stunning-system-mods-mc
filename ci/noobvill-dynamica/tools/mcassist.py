#!/usr/bin/env python3
"""Deterministic discovery and resource validation for Fabric projects."""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any

BUILD_KEYS = ("minecraft_version", "yarn_mappings", "loader_version", "fabric_api_version", "mod_version", "maven_group", "archives_base_name")


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def detect_java(root: Path) -> int | None:
    for path in (root / "build.gradle", root / "build.gradle.kts"):
        if path.is_file():
            text = path.read_text(encoding="utf-8")
            for pattern in (r"options\.release\s*=\s*(\d+)", r"JavaVersion\.VERSION_(\d+)"):
                match = re.search(pattern, text)
                if match:
                    return int(match.group(1))
    return None


def source_jars(root: Path) -> list[str]:
    cache = root / ".gradle" / "loom-cache" / "minecraftMaven"
    return sorted(str(path) for path in cache.rglob("minecraft-merged-*-sources.jar")) if cache.exists() else []


def build_profile(root: Path) -> dict[str, Any]:
    props = read_properties(root / "gradle.properties")
    mod_id = None
    metadata = root / "src/main/resources/fabric.mod.json"
    if metadata.is_file():
        try:
            mod_id = json.loads(metadata.read_text(encoding="utf-8")).get("id")
        except json.JSONDecodeError:
            pass
    return {"root": str(root.resolve()), "mod_id": mod_id, "java_release": detect_java(root), "versions": {key: props[key] for key in BUILD_KEYS if key in props}, "mapped_source_jars": source_jars(root)}


def profile(root: Path) -> int:
    data = build_profile(root)
    directory = root / ".mccodeassist"
    directory.mkdir(exist_ok=True)
    (directory / "profile.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(data, indent=2))
    return 0


def find_source(root: Path, symbol: str) -> int:
    matches: list[str] = []
    suffix = f"/{symbol}.java"
    for jar_name in source_jars(root):
        with zipfile.ZipFile(jar_name) as archive:
            matches.extend(f"{jar_name}!/{entry}" for entry in archive.namelist() if entry.endswith(suffix) or entry == f"{symbol}.java")
    if not matches:
        print(f"No mapped source found for {symbol}", file=sys.stderr)
        return 1
    print("\n".join(matches))
    return 0


def png_dimensions(data: bytes) -> tuple[int, int] | None:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")


def validate(root: Path) -> int:
    errors: list[str] = []
    resources = root / "src/main/resources"
    json_count = png_count = 0
    if not resources.exists():
        errors.append("Missing src/main/resources")
    else:
        for path in resources.rglob("*.json"):
            json_count += 1
            try:
                json.loads(path.read_text(encoding="utf-8"))
            except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
                errors.append(f"Invalid JSON {path.relative_to(root)}: {exc}")
        for path in resources.rglob("*.png"):
            png_count += 1
            try:
                dimensions = png_dimensions(path.read_bytes())
            except OSError as exc:
                errors.append(f"Unreadable PNG {path.relative_to(root)}: {exc}")
                continue
            if dimensions is None or min(dimensions) <= 0:
                errors.append(f"Invalid PNG {path.relative_to(root)}")
    if not build_profile(root)["versions"].get("minecraft_version"):
        errors.append("minecraft_version missing from gradle.properties")
    result = {"ok": not errors, "json_files": json_count, "png_files": png_count, "errors": errors}
    print(json.dumps(result, indent=2))
    return 0 if not errors else 1


def main() -> int:
    parser = argparse.ArgumentParser(prog="mcassist")
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("profile", "validate"):
        sub = commands.add_parser(name)
        sub.add_argument("root", type=Path)
    source = commands.add_parser("find-source")
    source.add_argument("root", type=Path)
    source.add_argument("symbol")
    args = parser.parse_args()
    root = args.root.resolve()
    if args.command == "profile":
        return profile(root)
    if args.command == "find-source":
        return find_source(root, args.symbol)
    return validate(root)


if __name__ == "__main__":
    raise SystemExit(main())
