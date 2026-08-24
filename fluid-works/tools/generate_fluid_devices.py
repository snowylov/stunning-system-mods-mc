#!/usr/bin/env python3
"""Generate the repetitive Fluid Works device assets from one audited definition table."""

from __future__ import annotations

import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fluidworks"
DATA = ROOT / "src/main/resources/data"
TEXTURE_SOURCE = ROOT.parents[1] / "upload/IMG_5299.png"
TEXTURE_TARGET = ASSETS / "textures/block/fluid_device_copper.png"
INPUT_TARGET = ROOT / "tools/device_texture_inputs/IMG_5299.png"

DEVICES = {
    "fluid_sprinkler": ("Fluid Sprinkler", [
        ([3, 3, 12], [13, 13, 16]), ([6, 6, 4], [10, 10, 12]), ([3, 3, 1], [13, 13, 4])]),
    "vacuum_drain": ("Vacuum Drain", [
        ([2, 2, 0], [14, 14, 4]), ([5, 5, 4], [11, 11, 16]), ([0, 0, 0], [3, 16, 2]),
        ([13, 0, 0], [16, 16, 2])]),
    "fluid_cannon": ("Fluid Cannon", [
        ([3, 3, 6], [13, 13, 15]), ([5, 5, 0], [11, 11, 8]), ([4, 4, 0], [12, 12, 2])]),
    "spill_tray": ("Spill Tray", [
        ([1, 1, 13], [15, 15, 16]), ([0, 0, 12], [2, 16, 16]), ([14, 0, 12], [16, 16, 16]),
        ([2, 0, 12], [14, 2, 16]), ([2, 14, 12], [14, 16, 16])]),
    "pressure_sensor": ("Pressure Sensor", [
        ([2, 2, 13], [14, 14, 16]), ([5, 5, 10], [11, 11, 13]), ([7, 2, 9], [9, 8, 10])]),
    "emergency_shutoff": ("Emergency Shutoff", [
        ([5, 5, 0], [11, 11, 16]), ([2, 2, 5], [14, 14, 11]), ([7, 0, 7], [9, 16, 9])]),
    "sampling_valve": ("Sampling Valve", [
        ([5, 5, 0], [11, 11, 16]), ([3, 3, 6], [13, 13, 10]), ([7, 0, 7], [9, 7, 9])]),
    "fluid_router": ("Fluid Router", [
        ([3, 3, 3], [13, 13, 13]), ([5, 5, 0], [11, 11, 16]), ([0, 5, 5], [16, 11, 11])]),
    "heat_exchanger": ("Heat Exchanger", [
        ([2, 2, 3], [14, 14, 13]), ([4, 4, 0], [7, 7, 16]), ([9, 9, 0], [12, 12, 16]),
        ([1, 1, 6], [15, 15, 10])]),
    "fluid_separator": ("Fluid Separator", [
        ([2, 2, 4], [14, 14, 14]), ([5, 5, 0], [11, 11, 5]), ([0, 5, 8], [16, 11, 12]),
        ([7, 0, 7], [9, 16, 9])]),
    "mist_nozzle": ("Mist Nozzle", [
        ([4, 4, 12], [12, 12, 16]), ([6, 6, 3], [10, 10, 12]), ([4, 4, 1], [12, 12, 4])]),
    "drain_grate": ("Drain Grate", [
        ([1, 1, 13], [15, 3, 16]), ([1, 5, 13], [15, 7, 16]), ([1, 9, 13], [15, 11, 16]),
        ([1, 13, 13], [15, 15, 16]), ([6, 6, 10], [10, 10, 13])]),
    "pipe_cover": ("Pipe Cover", [
        ([2, 2, 13], [14, 5, 16]), ([2, 11, 13], [14, 14, 16]), ([2, 5, 13], [5, 11, 16]),
        ([11, 5, 13], [14, 11, 16]), ([6, 6, 0], [10, 10, 13])]),
    "fluid_trap": ("Fluid Trap", [
        ([2, 2, 12], [14, 14, 16]), ([4, 4, 5], [12, 12, 12]), ([6, 6, 1], [10, 10, 5])]),
    "remote_tank_link": ("Remote Tank Link", [
        ([3, 3, 3], [13, 13, 13]), ([6, 6, 0], [10, 10, 16]), ([1, 7, 7], [15, 9, 9]),
        ([7, 1, 7], [9, 15, 9])]),
}

RECIPES = {
    "fluid_sprinkler": (["CIC", " P ", "CIC"], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_bars", "P": "fluidworks:fluid_pipe"}, 1),
    "vacuum_drain": (["CHC", " P ", "CHC"], {"C": "minecraft:copper_ingot", "H": "minecraft:hopper", "P": "fluidworks:fluid_pipe"}, 1),
    "fluid_cannon": (["CDC", "PHP", "CCC"], {"C": "minecraft:copper_ingot", "D": "minecraft:dispenser", "H": "fluidworks:high_pressure_pipe", "P": "fluidworks:fluid_pipe"}, 1),
    "spill_tray": (["C C", "CIC", "CCC"], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_bars"}, 1),
    "pressure_sensor": ([" C ", "CMC", " R "], {"C": "minecraft:copper_ingot", "M": "fluidworks:meter_pipe", "R": "minecraft:comparator"}, 1),
    "emergency_shutoff": (["CIC", "RVR", "CIC"], {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone_torch", "V": "fluidworks:redstone_fluid_valve"}, 1),
    "sampling_valve": ([" C ", "BVB", " C "], {"C": "minecraft:copper_ingot", "B": "minecraft:glass_bottle", "V": "fluidworks:redstone_fluid_valve"}, 1),
    "fluid_router": (["CPC", "PJP", "CPC"], {"C": "minecraft:copper_ingot", "P": "fluidworks:fluid_pipe", "J": "fluidworks:priority_junction"}, 1),
    "heat_exchanger": (["CIC", "PBP", "CIC"], {"C": "minecraft:copper_block", "I": "minecraft:iron_bars", "P": "fluidworks:fluid_pipe", "B": "minecraft:blast_furnace"}, 1),
    "fluid_separator": (["CHC", "FSF", "CHC"], {"C": "minecraft:copper_ingot", "H": "minecraft:hopper", "F": "fluidworks:filter_pipe", "S": "minecraft:comparator"}, 1),
    "mist_nozzle": ([" C ", "BPC", " C "], {"C": "minecraft:copper_ingot", "B": "minecraft:glass_bottle", "P": "fluidworks:fluid_pipe"}, 1),
    "drain_grate": (["III", "CHC", " C "], {"I": "minecraft:iron_bars", "C": "minecraft:copper_ingot", "H": "minecraft:hopper"}, 1),
    "pipe_cover": ([" C ", "CPC", " C "], {"C": "minecraft:copper_ingot", "P": "fluidworks:fluid_pipe"}, 4),
    "fluid_trap": (["CTC", "DPD", "CCC"], {"C": "minecraft:copper_ingot", "T": "minecraft:tripwire_hook", "D": "minecraft:dispenser", "P": "fluidworks:fluid_pipe"}, 1),
    "remote_tank_link": (["CEC", "PQP", "CEC"], {"C": "minecraft:copper_ingot", "E": "minecraft:ender_pearl", "P": "fluidworks:fluid_pipe", "Q": "minecraft:quartz"}, 2),
}


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def faces() -> dict[str, dict[str, str]]:
    return {side: {"texture": "#device"} for side in ("down", "up", "north", "south", "west", "east")}


def blockstate(name: str) -> dict[str, object]:
    rotations = {
        "north": {}, "south": {"y": 180}, "west": {"y": 270}, "east": {"y": 90},
        "up": {"x": 270}, "down": {"x": 90},
    }
    return {"multipart": [
        {"when": {"facing": facing}, "apply": {"model": f"fluidworks:block/{name}", **rotation}}
        for facing, rotation in rotations.items()
    ]}


def main() -> None:
    TEXTURE_TARGET.parent.mkdir(parents=True, exist_ok=True)
    INPUT_TARGET.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(TEXTURE_SOURCE, TEXTURE_TARGET)
    shutil.copy2(TEXTURE_SOURCE, INPUT_TARGET)

    for name, (_, elements) in DEVICES.items():
        write_json(ASSETS / f"blockstates/{name}.json", blockstate(name))
        write_json(ASSETS / f"models/block/{name}.json", {
            "textures": {"device": "fluidworks:block/fluid_device_copper", "particle": "#device"},
            "elements": [{"from": start, "to": end, "faces": faces()} for start, end in elements],
        })
        write_json(ASSETS / f"models/item/{name}.json", {"parent": f"fluidworks:block/{name}"})
        write_json(ASSETS / f"items/{name}.json", {
            "model": {"type": "minecraft:model", "model": f"fluidworks:block/{name}"}
        })
        write_json(DATA / f"fluidworks/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{
                "type": "minecraft:item", "name": f"fluidworks:{name}",
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }]}],
        })
        pattern, keys, count = RECIPES[name]
        write_json(DATA / f"fluidworks/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern,
            "key": keys, "result": {"id": f"fluidworks:{name}", "count": count},
        })

    device_ids = [f"fluidworks:{name}" for name in DEVICES]
    for namespace in ("block", "item"):
        write_json(DATA / f"c/tags/{namespace}/fluid_devices.json", {
            "replace": False, "values": device_ids,
        })

    pickaxe_path = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    pickaxe = json.loads(pickaxe_path.read_text(encoding="utf-8"))
    for device_id in device_ids:
        if device_id not in pickaxe["values"]:
            pickaxe["values"].append(device_id)
    write_json(pickaxe_path, pickaxe)

    language_path = ASSETS / "lang/en_us.json"
    language = json.loads(language_path.read_text(encoding="utf-8"))
    for name, (display_name, _) in DEVICES.items():
        language[f"block.fluidworks.{name}"] = display_name
    language.update({
        "message.fluidworks.device.enabled": "Fluid device enabled.",
        "message.fluidworks.device.disabled": "Fluid device disabled.",
        "message.fluidworks.device.status": "%s — %s, %s mB stored, %s mB last operation",
    })
    write_json(language_path, language)


if __name__ == "__main__":
    main()
