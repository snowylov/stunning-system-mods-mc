#!/usr/bin/env python3
"""Create and verify deterministic validity evidence for Flower Fabric builds."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path
from typing import Any, Iterable


IGNORED_PARTS = {
    ".git",
    ".gradle",
    ".idea",
    ".mccodeassist",
    ".vscode",
    "build",
    "logs",
    "out",
    "run",
}
ALLOWED_ARTIFACT_ROLES = {
    "api",
    "deobf",
    "game-ready",
    "sources-jar",
    "source-zip",
}
PROFILE_FIELDS = (
    "minecraft",
    "yarn_mappings",
    "fabric_loader",
    "fabric_api",
    "required_java",
    "gradle",
    "mod_id",
)
IMPORT_PATTERN = re.compile(
    r"^\s*import\s+(?:static\s+)?((?:net\.minecraft|net\.fabricmc)\.[^;]+);",
    re.MULTILINE,
)


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)

    fingerprint = commands.add_parser("fingerprint")
    fingerprint.add_argument("mod_root", type=Path)
    fingerprint.add_argument("--output", type=Path)

    verify = commands.add_parser("verify")
    verify.add_argument("mod_root", type=Path)
    verify.add_argument("--compiler-profile", required=True, type=Path)
    verify.add_argument("--static-result", required=True, type=Path)
    verify.add_argument("--build-result", required=True, type=Path)
    verify.add_argument("--artifact", action="append", default=[])
    verify.add_argument("--changed-java", action="append", default=[], type=Path)
    verify.add_argument("--api-evidence", type=Path)
    verify.add_argument("--expected-minecraft")
    verify.add_argument("--expected-yarn")
    verify.add_argument("--expected-loader")
    verify.add_argument("--expected-fabric-api")
    verify.add_argument("--expected-java", type=int)
    verify.add_argument("--output", type=Path)
    return parser.parse_args()


def resolve_mod_root(raw_root: Path) -> Path:
    root = raw_root.expanduser().resolve(strict=True)
    if not root.is_dir():
        raise SystemExit(f"Mod root is not a directory: {root}")
    if not any((root / name).is_file() for name in ("gradlew", "gradlew.bat")):
        raise SystemExit(f"Gradle wrapper is missing from: {root}")
    return root


def load_json(path: Path, label: str, errors: list[str]) -> dict[str, Any]:
    try:
        value = json.loads(path.expanduser().read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        errors.append(f"{label} is unreadable or invalid JSON: {exc}")
        return {}
    if not isinstance(value, dict):
        errors.append(f"{label} must be a JSON object")
        return {}
    return value


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def stable_digest(items: Iterable[tuple[str, str]]) -> str:
    digest = hashlib.sha256()
    for key, value in sorted(items):
        encoded_key = key.encode("utf-8")
        encoded_value = value.encode("utf-8")
        digest.update(len(encoded_key).to_bytes(8, "big"))
        digest.update(encoded_key)
        digest.update(len(encoded_value).to_bytes(8, "big"))
        digest.update(encoded_value)
    return digest.hexdigest()


def included_files(root: Path) -> list[Path]:
    candidates = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        relative = path.relative_to(root)
        if any(part in IGNORED_PARTS for part in relative.parts):
            continue
        candidates.append(path)
    return sorted(candidates, key=lambda path: path.relative_to(root).as_posix())


def project_fingerprint(root: Path) -> dict[str, Any]:
    files = included_files(root)
    file_hashes = [
        (path.relative_to(root).as_posix(), sha256_file(path))
        for path in files
    ]
    return {
        "schema": 1,
        "project": root.name,
        "source_fingerprint": stable_digest(file_hashes),
        "file_count": len(file_hashes),
    }


def project_metadata(root: Path, errors: list[str]) -> dict[str, Any]:
    matches = sorted(root.glob("src/**/fabric.mod.json"))
    if not matches:
        errors.append("Project fabric.mod.json is missing")
        return {}
    try:
        metadata = json.loads(matches[0].read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        errors.append(f"Project fabric.mod.json is invalid: {exc}")
        return {}
    if not isinstance(metadata, dict):
        errors.append("Project fabric.mod.json must contain an object")
        return {}
    return metadata


def parse_artifact(value: str) -> tuple[str, Path]:
    role, separator, raw_path = value.partition("=")
    role = role.strip()
    if not separator or role not in ALLOWED_ARTIFACT_ROLES or not raw_path.strip():
        choices = ", ".join(sorted(ALLOWED_ARTIFACT_ROLES))
        raise SystemExit(f"--artifact must use one of {choices} as role=/absolute/path")
    return role, Path(raw_path).expanduser().resolve(strict=True)


def artifact_inputs(values: list[str], build_result: dict[str, Any]) -> list[tuple[str, Path]]:
    artifacts = []
    seen_roles = set()
    for value in values:
        role, path = parse_artifact(value)
        if role in seen_roles:
            raise SystemExit(f"Duplicate artifact role: {role}")
        seen_roles.add(role)
        artifacts.append((role, path))
    built_artifact = build_result.get("artifact")
    if "game-ready" not in seen_roles and isinstance(built_artifact, str) and built_artifact:
        artifacts.append(("game-ready", Path(built_artifact).expanduser().resolve(strict=True)))
    return sorted(artifacts, key=lambda item: item[0])


def archive_names(path: Path, errors: list[str], role: str) -> set[str]:
    try:
        with zipfile.ZipFile(path) as archive:
            return set(archive.namelist())
    except (OSError, zipfile.BadZipFile) as exc:
        errors.append(f"{role} artifact is not a valid ZIP/JAR: {path.name}: {exc}")
        return set()


def read_archive_json(
    path: Path,
    entry: str,
    errors: list[str],
) -> dict[str, Any]:
    try:
        with zipfile.ZipFile(path) as archive:
            value = json.loads(archive.read(entry))
    except (OSError, KeyError, UnicodeDecodeError, json.JSONDecodeError, zipfile.BadZipFile) as exc:
        errors.append(f"Invalid {entry} in {path.name}: {exc}")
        return {}
    if not isinstance(value, dict):
        errors.append(f"{entry} in {path.name} must contain an object")
        return {}
    return value


def entrypoint_classes(metadata: dict[str, Any]) -> list[str]:
    classes = []
    entrypoints = metadata.get("entrypoints", {})
    if not isinstance(entrypoints, dict):
        return classes
    for values in entrypoints.values():
        if not isinstance(values, list):
            values = [values]
        for value in values:
            target = value.get("value") if isinstance(value, dict) else value
            if isinstance(target, str):
                classes.append(target.split("::", 1)[0])
    return sorted(set(classes))


def inspect_artifact(
    role: str,
    path: Path,
    metadata: dict[str, Any],
    errors: list[str],
    warnings: list[str],
) -> dict[str, Any]:
    names = archive_names(path, errors, role)
    if not names:
        return {
            "role": role,
            "filename": path.name,
            "size_bytes": path.stat().st_size,
            "sha256": sha256_file(path),
            "valid": False,
        }

    source_role = role in {"sources-jar", "source-zip"}
    if source_role:
        if not any(name.endswith(".java") for name in names):
            errors.append(f"{role} artifact contains no Java sources: {path.name}")
    else:
        if not any(name.endswith(".class") for name in names):
            errors.append(f"{role} artifact contains no compiled classes: {path.name}")

    artifact_metadata: dict[str, Any] = {}
    if role == "game-ready":
        if "fabric.mod.json" not in names:
            errors.append(f"game-ready artifact lacks fabric.mod.json: {path.name}")
        else:
            artifact_metadata = read_archive_json(path, "fabric.mod.json", errors)
            project_id = metadata.get("id")
            if project_id and artifact_metadata.get("id") != project_id:
                errors.append(
                    f"Artifact mod ID {artifact_metadata.get('id')!r} does not match project {project_id!r}"
                )
            project_version = metadata.get("version")
            if (
                isinstance(project_version, str)
                and "${" not in project_version
                and artifact_metadata.get("version") != project_version
            ):
                errors.append(
                    f"Artifact version {artifact_metadata.get('version')!r} does not match project {project_version!r}"
                )
            for target in entrypoint_classes(artifact_metadata):
                class_entry = target.replace(".", "/") + ".class"
                if class_entry not in names:
                    errors.append(f"Declared entrypoint class is missing from {path.name}: {target}")
            if not any(name.startswith("assets/") for name in names):
                warnings.append(f"game-ready artifact has no assets directory: {path.name}")

    return {
        "role": role,
        "filename": path.name,
        "size_bytes": path.stat().st_size,
        "sha256": sha256_file(path),
        "entry_count": len(names),
        "mod_id": artifact_metadata.get("id"),
        "version": artifact_metadata.get("version"),
        "valid": True,
    }


def resolve_wrapper(root: Path, command: Any, errors: list[str]) -> list[str]:
    if not isinstance(command, list) or not command or not all(isinstance(item, str) for item in command):
        errors.append("Build command must be a nonempty string array")
        return []
    wrapper_index = 0
    if Path(command[0]).name.lower() in {"bash", "sh", "cmd", "cmd.exe"} and len(command) > 1:
        wrapper_index = 1
    raw_wrapper = Path(command[wrapper_index])
    actual = raw_wrapper.resolve() if raw_wrapper.is_absolute() else (root / raw_wrapper).resolve()
    expected = {(root / "gradlew").resolve(), (root / "gradlew.bat").resolve()}
    if actual not in expected:
        errors.append(f"Build command does not use this repository's Gradle wrapper: {raw_wrapper}")
    return [Path(item).name if index == wrapper_index else item for index, item in enumerate(command)]


def compare_profiles(
    compiler_profile: dict[str, Any],
    build_profile: dict[str, Any],
    errors: list[str],
) -> None:
    for field in PROFILE_FIELDS:
        if compiler_profile.get(field) != build_profile.get(field):
            errors.append(
                f"Build profile field {field} changed: "
                f"{compiler_profile.get(field)!r} != {build_profile.get(field)!r}"
            )


def compare_expected(
    args: argparse.Namespace,
    profile: dict[str, Any],
    errors: list[str],
) -> None:
    expectations = {
        "minecraft": args.expected_minecraft,
        "yarn_mappings": args.expected_yarn,
        "fabric_loader": args.expected_loader,
        "fabric_api": args.expected_fabric_api,
        "required_java": args.expected_java,
    }
    for field, expected in expectations.items():
        if expected is not None and profile.get(field) != expected:
            errors.append(
                f"Expected {field} {expected!r}, detected {profile.get(field)!r}"
            )


def changed_imports(
    root: Path,
    changed_files: list[Path],
    errors: list[str],
) -> set[str]:
    imports = set()
    for raw_path in changed_files:
        path = raw_path.expanduser()
        path = path.resolve() if path.is_absolute() else (root / path).resolve()
        try:
            path.relative_to(root)
        except ValueError:
            errors.append(f"Changed Java file is outside the mod root: {path}")
            continue
        if not path.is_file() or path.suffix != ".java":
            errors.append(f"Changed Java file is missing or not Java: {path.name}")
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError) as exc:
            errors.append(f"Changed Java file cannot be read: {path.name}: {exc}")
            continue
        imports.update(IMPORT_PATTERN.findall(text))
    return imports


def verify_api_evidence(
    imports: set[str],
    evidence_path: Path | None,
    errors: list[str],
) -> list[dict[str, str]]:
    if not imports:
        return []
    if evidence_path is None:
        errors.append("Changed version-sensitive imports require --api-evidence")
        return []
    evidence = load_json(evidence_path, "API evidence", errors)
    rows = evidence.get("imports", [])
    if not isinstance(rows, list):
        errors.append("API evidence imports must be an array")
        return []
    indexed = {
        row.get("symbol"): row
        for row in rows
        if isinstance(row, dict) and isinstance(row.get("symbol"), str)
    }
    verified = []
    for symbol in sorted(imports):
        row = indexed.get(symbol)
        if not isinstance(row, dict):
            errors.append(f"Missing exact API evidence for changed import: {symbol}")
            continue
        archive_value = row.get("source_archive")
        entry = row.get("entry")
        if not isinstance(archive_value, str) or not isinstance(entry, str):
            errors.append(f"Incomplete API evidence for changed import: {symbol}")
            continue
        archive_path = Path(archive_value).expanduser().resolve()
        if not archive_path.is_file():
            errors.append(f"API source archive is missing for {symbol}: {archive_path.name}")
            continue
        try:
            with zipfile.ZipFile(archive_path) as archive:
                names = set(archive.namelist())
        except (OSError, zipfile.BadZipFile) as exc:
            errors.append(f"API source archive is invalid for {symbol}: {exc}")
            continue
        found = any(name.startswith(entry) for name in names) if entry.endswith("/") else entry in names
        if not found:
            errors.append(f"API source entry is missing for {symbol}: {entry}")
            continue
        verified.append(
            {
                "symbol": symbol,
                "source_archive": archive_path.name,
                "entry": entry,
            }
        )
    return verified


def write_payload(payload: dict[str, Any], output: Path | None) -> None:
    rendered = json.dumps(payload, indent=2, sort_keys=True) + "\n"
    if output is None:
        print(rendered, end="")
        return
    destination = output.expanduser().resolve()
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(rendered, encoding="utf-8")
    print(destination)


def command_fingerprint(args: argparse.Namespace) -> int:
    root = resolve_mod_root(args.mod_root)
    write_payload(project_fingerprint(root), args.output)
    return 0


def command_verify(args: argparse.Namespace) -> int:
    root = resolve_mod_root(args.mod_root)
    errors: list[str] = []
    warnings: list[str] = []
    fingerprint = project_fingerprint(root)
    compiler_profile = load_json(args.compiler_profile, "Compiler profile", errors)
    static_result = load_json(args.static_result, "Static result", errors)
    build_result = load_json(args.build_result, "Build result", errors)
    metadata = project_metadata(root, errors)

    profile_root = compiler_profile.get("root")
    if not isinstance(profile_root, str) or Path(profile_root).resolve() != root:
        errors.append("Compiler profile does not belong to the current mod root")
    if not compiler_profile.get("ready"):
        errors.append("Compiler profile is not ready for an exact build")
    if compiler_profile.get("metadata_error"):
        errors.append(f"Compiler metadata error: {compiler_profile['metadata_error']}")
    selected_jdk = compiler_profile.get("selected_jdk")
    if not isinstance(selected_jdk, dict) or selected_jdk.get("major") != compiler_profile.get("required_java"):
        errors.append("Selected Java runtime does not exactly match the project requirement")
    compare_expected(args, compiler_profile, errors)

    if not static_result.get("ok") or static_result.get("errors"):
        errors.append("Static Fabric resource validation did not pass")
    if not build_result.get("ok") or build_result.get("exit_code") != 0:
        errors.append("Gradle build result is not successful")
    build_profile = build_result.get("profile")
    if not isinstance(build_profile, dict):
        errors.append("Build result lacks a compiler profile")
        build_profile = {}
    compare_profiles(compiler_profile, build_profile, errors)
    sanitized_command = resolve_wrapper(root, build_result.get("command"), errors)
    if "clean" in sanitized_command:
        warnings.append("Build command used clean; confirm stale output or an explicit request justified it")
    if build_result.get("source_fingerprint") != fingerprint["source_fingerprint"]:
        errors.append("Source/toolchain fingerprint changed after build dispatch")
    verification = build_result.get("verification")
    if not isinstance(verification, dict) or not verification.get("valid"):
        errors.append("Compiler JAR verification did not pass")

    artifacts = []
    parsed_artifacts = artifact_inputs(args.artifact, build_result)
    if not parsed_artifacts:
        errors.append("No build artifact was supplied")
    built_artifact = build_result.get("artifact")
    if isinstance(built_artifact, str) and built_artifact:
        built_path = Path(built_artifact).expanduser().resolve()
        explicit_game = next((path for role, path in parsed_artifacts if role == "game-ready"), None)
        if explicit_game is not None and explicit_game != built_path:
            errors.append("Explicit game-ready artifact differs from the compiler build artifact")
    for role, path in parsed_artifacts:
        before = len(errors)
        artifact = inspect_artifact(role, path, metadata, errors, warnings)
        artifact["valid"] = len(errors) == before
        artifacts.append(artifact)

    imports = changed_imports(root, args.changed_java, errors)
    api_evidence = verify_api_evidence(imports, args.api_evidence, errors)

    toolchain = {field: compiler_profile.get(field) for field in PROFILE_FIELDS}
    receipt = {
        "schema": 1,
        "verdict": "PASS" if not errors else "FAIL",
        "project": root.name,
        "source_fingerprint": fingerprint["source_fingerprint"],
        "file_count": fingerprint["file_count"],
        "toolchain": toolchain,
        "command": sanitized_command,
        "checks": {
            "profile_consistent": not any("profile" in error.lower() for error in errors),
            "static_validation": bool(static_result.get("ok") and not static_result.get("errors")),
            "gradle_exit_zero": build_result.get("exit_code") == 0,
            "compiler_jar_verification": bool(
                isinstance(verification, dict) and verification.get("valid")
            ),
            "source_fingerprint_current": build_result.get("source_fingerprint")
            == fingerprint["source_fingerprint"],
            "api_evidence_count": len(api_evidence),
        },
        "api_evidence": api_evidence,
        "artifacts": artifacts,
        "warnings": warnings,
        "errors": errors,
    }
    write_payload(receipt, args.output)
    return 0 if not errors else 2


def main() -> int:
    args = parse_arguments()
    if args.command == "fingerprint":
        return command_fingerprint(args)
    if args.command == "verify":
        return command_verify(args)
    raise SystemExit(f"Unknown command: {args.command}")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("Interrupted", file=sys.stderr)
        raise SystemExit(130)
