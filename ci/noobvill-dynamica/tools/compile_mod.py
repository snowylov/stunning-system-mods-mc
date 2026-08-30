#!/usr/bin/env python3
"""Profile, compile, and verify Fabric mod projects without guessing versions."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return ""


def properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in read_text(path).splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def first_match(patterns: list[str], text: str) -> str | None:
    for pattern in patterns:
        match = re.search(pattern, text, re.MULTILINE)
        if match:
            return match.group(1)
    return None


def required_java(root: Path, minecraft: str | None) -> int | None:
    build = "\n".join(read_text(p) for p in (root / "build.gradle", root / "build.gradle.kts"))
    found = first_match(
        [
            r"JavaLanguageVersion\.of\((\d+)\)",
            r"JavaVersion\.VERSION_(\d+)",
            r"(?:sourceCompatibility|targetCompatibility)\s*=\s*[\"']?(\d+)",
            r"languageVersion\.set\([^\n]*?(\d+)\)",
        ],
        build,
    )
    if found:
        return int(found)
    if minecraft:
        nums = tuple(int(x) for x in re.findall(r"\d+", minecraft)[:3])
        if nums >= (1, 20, 5):
            return 21
        if nums >= (1, 18):
            return 17
        if nums >= (1, 17):
            return 16
        return 8
    return None


def java_major(java: Path) -> int | None:
    try:
        proc = subprocess.run([str(java), "-version"], capture_output=True, text=True, timeout=8, check=False)
    except (OSError, subprocess.TimeoutExpired):
        return None
    match = re.search(r'version "(?:1\.)?(\d+)', proc.stderr + proc.stdout)
    return int(match.group(1)) if match else None


def java_homes() -> list[dict[str, object]]:
    candidates: list[Path] = []
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]))
    current = shutil.which("java")
    if current:
        candidates.append(Path(current).resolve().parent.parent)
    for base in (Path("/usr/lib/jvm"), Path("/opt/java"), Path("/opt/jdk")):
        if base.is_dir():
            candidates.extend(p for p in base.iterdir() if p.is_dir())
    seen: set[Path] = set()
    found: list[dict[str, object]] = []
    for home in candidates:
        try:
            home = home.resolve()
        except OSError:
            continue
        if home in seen:
            continue
        seen.add(home)
        binary = home / "bin" / "java"
        if binary.is_file() and (major := java_major(binary)) is not None:
            found.append({"home": str(home), "major": major})
    return sorted(found, key=lambda item: int(item["major"]))


def fabric_metadata(root: Path) -> tuple[Path | None, dict[str, object] | None, str | None]:
    matches = list(root.glob("src/**/fabric.mod.json"))
    if not matches:
        return None, None, None
    path = matches[0]
    try:
        return path, json.loads(read_text(path)), None
    except json.JSONDecodeError as exc:
        return path, None, f"Invalid fabric.mod.json: {exc}"


def profile(root: Path) -> dict[str, object]:
    props = properties(root / "gradle.properties")
    build = "\n".join(read_text(p) for p in (root / "build.gradle", root / "build.gradle.kts"))
    wrapper = properties(root / "gradle" / "wrapper" / "gradle-wrapper.properties")
    metadata_path, metadata, metadata_error = fabric_metadata(root)
    minecraft = props.get("minecraft_version") or first_match(
        [r'minecraft\s+["\']com\.mojang:minecraft:([^"\']+)', r'minecraft\s*=\s*["\']([^"\']+)'], build
    )
    java = required_java(root, minecraft)
    jdks = java_homes()
    exact = next((j for j in jdks if j["major"] == java), None) if java else None
    compatible = exact or (next((j for j in jdks if int(j["major"]) > java), None) if java else None)
    distribution = wrapper.get("distributionUrl")
    gradle_version = first_match([r"gradle-([0-9.]+)-(?:bin|all)\.zip"], distribution or "")
    return {
        "root": str(root),
        "minecraft": minecraft,
        "yarn_mappings": props.get("yarn_mappings"),
        "fabric_loader": props.get("loader_version"),
        "fabric_api": props.get("fabric_api_version") or props.get("fabric_version"),
        "terrablender": props.get("terrablender_version"),
        "required_java": java,
        "installed_jdks": jdks,
        "selected_jdk": compatible,
        "gradle": gradle_version,
        "fabric_mod_json": str(metadata_path) if metadata_path else None,
        "mod_id": metadata.get("id") if metadata else None,
        "metadata_error": metadata_error,
        "ready": bool((root / "gradlew").is_file() and compatible and not metadata_error),
    }


def verify_jar(path: Path) -> dict[str, object]:
    result: dict[str, object] = {"jar": str(path), "valid": False, "errors": [], "warnings": []}
    errors = result["errors"]
    warnings = result["warnings"]
    assert isinstance(errors, list) and isinstance(warnings, list)
    if not path.is_file():
        errors.append("JAR does not exist")
        return result
    try:
        with zipfile.ZipFile(path) as archive:
            names = set(archive.namelist())
            if "fabric.mod.json" not in names:
                errors.append("fabric.mod.json is missing")
                return result
            try:
                metadata = json.loads(archive.read("fabric.mod.json"))
            except (json.JSONDecodeError, UnicodeDecodeError) as exc:
                errors.append(f"fabric.mod.json is invalid: {exc}")
                return result
            classes: list[str] = []
            entrypoints = metadata.get("entrypoints", {})
            if isinstance(entrypoints, dict):
                for values in entrypoints.values():
                    if not isinstance(values, list):
                        values = [values]
                    for value in values:
                        target = value.get("value") if isinstance(value, dict) else value
                        if isinstance(target, str):
                            classes.append(target.split("::", 1)[0])
            for target in classes:
                if target.replace(".", "/") + ".class" not in names:
                    errors.append(f"Declared entrypoint class is missing: {target}")
            if not any(name.endswith(".class") for name in names):
                errors.append("JAR contains no compiled classes")
            if not any(name.startswith("assets/") for name in names):
                warnings.append("JAR contains no assets directory")
            result.update({"mod_id": metadata.get("id"), "version": metadata.get("version"), "entrypoints": classes, "entries": len(names), "valid": not errors})
    except zipfile.BadZipFile:
        errors.append("File is not a valid ZIP/JAR")
    return result


def build(root: Path, task: str, offline: bool, timeout: int) -> dict[str, object]:
    info = profile(root)
    if info.get("metadata_error"):
        return {"ok": False, "profile": info, "error": info["metadata_error"]}
    selected = info.get("selected_jdk")
    if not isinstance(selected, dict):
        return {"ok": False, "profile": info, "error": f"Required Java {info.get('required_java')} is not installed"}
    wrapper = root / "gradlew"
    if not wrapper.is_file():
        return {"ok": False, "profile": info, "error": "Gradle wrapper is missing"}
    cache_key = hashlib.sha256(str(root.resolve()).encode()).hexdigest()[:12]
    gradle_home = Path(tempfile.gettempdir()) / "minecraft-mod-compiler" / cache_key
    gradle_home.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    env["JAVA_HOME"] = str(selected["home"])
    env["GRADLE_USER_HOME"] = str(gradle_home)
    cmd = [str(wrapper), task, "--no-daemon"] + (["--offline"] if offline else [])
    try:
        proc = subprocess.run(cmd, cwd=root, env=env, capture_output=True, text=True, timeout=timeout, check=False)
    except subprocess.TimeoutExpired as exc:
        return {"ok": False, "profile": info, "command": cmd, "error": f"Build timed out after {timeout}s", "output": str(exc.stdout or "")[-12000:]}
    output = (proc.stdout + "\n" + proc.stderr).strip()
    lib_dir = root / "build" / "libs"
    jars = sorted(
        (p for p in lib_dir.glob("*.jar") if not re.search(r"(?:-sources|-dev|-shadow|-javadoc)\.jar$", p.name)),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    ) if lib_dir.is_dir() else []
    verification = verify_jar(jars[0]) if proc.returncode == 0 and jars else None
    ok = proc.returncode == 0 and bool(verification and verification.get("valid"))
    return {
        "ok": ok,
        "profile": info,
        "command": cmd,
        "exit_code": proc.returncode,
        "output_tail": output[-16000:],
        "artifact": str(jars[0]) if jars else None,
        "verification": verification,
        "error": None if ok else ("Build failed" if proc.returncode else "No verified installable JAR was produced"),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    p_profile = sub.add_parser("profile")
    p_profile.add_argument("root", type=Path)
    p_build = sub.add_parser("build")
    p_build.add_argument("root", type=Path)
    p_build.add_argument("--task", default="build")
    p_build.add_argument("--offline", action="store_true")
    p_build.add_argument("--timeout", type=int, default=900)
    p_verify = sub.add_parser("verify-jar")
    p_verify.add_argument("jar", type=Path)
    args = parser.parse_args()
    if args.command == "profile":
        result = profile(args.root.resolve())
        code = 0 if result.get("ready") else 2
    elif args.command == "build":
        result = build(args.root.resolve(), args.task, args.offline, args.timeout)
        code = 0 if result.get("ok") else 2
    else:
        result = verify_jar(args.jar.resolve())
        code = 0 if result.get("valid") else 2
    print(json.dumps(result, indent=2))
    return code


if __name__ == "__main__":
    raise SystemExit(main())
