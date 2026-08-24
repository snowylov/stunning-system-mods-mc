#!/usr/bin/env python3
"""Regenerate Fluid Works pipe resources and the pixel-art mod icon."""

from __future__ import annotations

import json
from pathlib import Path

import matplotlib.image as mpimg
import matplotlib.pyplot as plt
import numpy as np


PROJECT = Path(__file__).resolve().parents[1]
RESOURCES = PROJECT / "src/main/resources"
ASSETS = RESOURCES / "assets/fluidworks"
DATA = RESOURCES / "data/fluidworks"
UPLOADS = PROJECT / "tools/icon_inputs"

PIPE_DEFINITIONS = {
    "fluid_pipe": ("Fluid Pipe", (184, 115, 51), False),
    "redstone_fluid_valve": ("Redstone Fluid Valve", (190, 38, 38), False),
    "extraction_fluid_pipe": ("Extraction Fluid Pipe", (151, 105, 79), True),
    "high_pressure_pipe": ("High-Pressure Pipe", (185, 195, 205), False),
    "meter_pipe": ("Meter Pipe", (45, 194, 221), False),
    "overflow_valve": ("Overflow Valve", (235, 126, 35), False),
    "pulse_valve": ("Pulse Valve", (238, 55, 74), True),
    "priority_junction": ("Priority Junction", (246, 190, 42), False),
    "fluid_diode": ("Fluid Diode", (239, 239, 231), True),
    "filter_pipe": ("Filter Pipe", (105, 205, 74), False),
    "mixing_junction": ("Mixing Junction", (161, 75, 205), False),
}

RECIPES = {
    "extraction_fluid_pipe": ("PRP", {"P": "minecraft:piston", "R": "minecraft:redstone_torch"}, 1),
    "high_pressure_pipe": ("IPI", {"I": "minecraft:iron_ingot", "P": "fluidworks:fluid_pipe"}, 4),
    "meter_pipe": ("GPG", {"G": "minecraft:glass_pane", "P": "minecraft:comparator"}, 1),
    "overflow_valve": ("HPH", {"H": "minecraft:hopper", "P": "fluidworks:fluid_pipe"}, 1),
    "pulse_valve": ("RPR", {"R": "minecraft:repeater", "P": "fluidworks:fluid_pipe"}, 1),
    "priority_junction": ("GPG", {"G": "minecraft:gold_ingot", "P": "fluidworks:fluid_pipe"}, 1),
    "fluid_diode": ("QPR", {"Q": "minecraft:quartz", "P": "fluidworks:fluid_pipe", "R": "minecraft:repeater"}, 1),
    "filter_pipe": ("LPL", {"L": "minecraft:lime_dye", "P": "fluidworks:fluid_pipe"}, 1),
    "mixing_junction": ("PBP", {"P": "fluidworks:fluid_pipe", "B": "minecraft:brewing_stand"}, 1),
}


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def pipe_texture(color: tuple[int, int, int], output: Path) -> None:
    pixels = np.zeros((16, 16, 4), dtype=np.uint8)
    pixels[:, :, :3] = color
    pixels[:, :, 3] = 255
    dark = tuple(max(0, channel - 70) for channel in color)
    light = tuple(min(255, channel + 55) for channel in color)
    pixels[[0, 1, 14, 15], :, :3] = dark
    pixels[:, [0, 1, 14, 15], :3] = dark
    pixels[3:6, 3:6, :3] = light
    pixels[10:13, 10:13, :3] = light
    pixels[7:9, :, :3] = dark
    output.parent.mkdir(parents=True, exist_ok=True)
    plt.imsave(output, pixels)


def faces(texture: str = "#pipe") -> dict[str, dict[str, str]]:
    return {face: {"texture": texture} for face in ("down", "up", "north", "south", "west", "east")}


def element(start: list[int], end: list[int]) -> dict[str, object]:
    return {"from": start, "to": end, "faces": faces()}


def junction_model(identifier: str) -> dict[str, object]:
    return {
        "textures": {
            "pipe": f"fluidworks:block/{identifier}",
            "particle": f"fluidworks:block/{identifier}",
        },
        "elements": [
            element([5, 5, 5], [11, 11, 11]),
            element([6, 6, 0], [10, 10, 16]),
            element([0, 6, 6], [16, 10, 10]),
            element([6, 0, 6], [10, 16, 10]),
            element([4, 4, 0], [12, 12, 2]),
            element([4, 4, 14], [12, 12, 16]),
            element([0, 4, 4], [2, 12, 12]),
            element([14, 4, 4], [16, 12, 12]),
            element([4, 0, 4], [12, 2, 12]),
            element([4, 14, 4], [12, 16, 12]),
        ],
    }


def directional_model(identifier: str) -> dict[str, object]:
    return {
        "textures": {
            "pipe": f"fluidworks:block/{identifier}",
            "particle": f"fluidworks:block/{identifier}",
        },
        "elements": [
            element([6, 6, 0], [10, 10, 16]),
            element([4, 4, 0], [12, 12, 2]),
            element([4, 4, 14], [12, 12, 16]),
            element([4, 4, 6], [12, 12, 10]),
        ],
    }


def directional_blockstate(identifier: str) -> dict[str, object]:
    model = f"fluidworks:block/{identifier}"
    rotations = {
        "north": {},
        "south": {"y": 180},
        "west": {"y": 270},
        "east": {"y": 90},
        "up": {"x": 270},
        "down": {"x": 90},
    }
    return {
        "multipart": [
            {"when": {"facing": facing}, "apply": {"model": model, **rotation}}
            for facing, rotation in rotations.items()
        ]
    }


def generate_pipe_resources() -> None:
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for identifier, (display_name, color, directional) in PIPE_DEFINITIONS.items():
        lang[f"block.fluidworks.{identifier}"] = display_name
        pipe_texture(color, ASSETS / f"textures/block/{identifier}.png")
        write_json(ASSETS / f"models/block/{identifier}.json",
                   directional_model(identifier) if directional else junction_model(identifier))
        write_json(ASSETS / f"blockstates/{identifier}.json",
                   directional_blockstate(identifier) if directional
                   else {"multipart": [{"apply": {"model": f"fluidworks:block/{identifier}"}}]})
        write_json(ASSETS / f"models/item/{identifier}.json",
                   {"parent": f"fluidworks:block/{identifier}"})
        write_json(ASSETS / f"items/{identifier}.json",
                   {"model": {"type": "minecraft:model", "model": f"fluidworks:block/{identifier}"}})
        write_json(DATA / f"loot_table/blocks/{identifier}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"fluidworks:{identifier}",
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }],
            }],
        })
    write_json(lang_path, lang)

    for identifier, (pattern, keys, count) in RECIPES.items():
        write_json(DATA / f"recipe/{identifier}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "redstone",
            "pattern": [pattern],
            "key": {symbol: item for symbol, item in keys.items()},
            "result": {"id": f"fluidworks:{identifier}", "count": count},
        })

    pipe_tag = RESOURCES / "data/c/tags/block/fluid_pipes.json"
    pipe_values = [f"fluidworks:{identifier}" for identifier in PIPE_DEFINITIONS]
    write_json(pipe_tag, {"replace": False, "values": pipe_values})
    write_json(RESOURCES / "data/c/tags/item/fluid_pipes.json",
               {"replace": False, "values": pipe_values})

    pickaxe_tag = RESOURCES / "data/minecraft/tags/block/mineable/pickaxe.json"
    pickaxe = json.loads(pickaxe_tag.read_text(encoding="utf-8"))
    values = pickaxe.setdefault("values", [])
    for value in pipe_values:
        if value not in values:
            values.append(value)
    write_json(pickaxe_tag, pickaxe)


def overlay(canvas: np.ndarray, source: np.ndarray, top: int, left: int) -> None:
    rgba = source
    if rgba.dtype != np.uint8:
        rgba = np.rint(rgba * 255).astype(np.uint8)
    height, width = rgba.shape[:2]
    alpha = rgba[:, :, 3:4] / 255.0
    target = canvas[top:top + height, left:left + width]
    target[:, :, :3] = np.rint(rgba[:, :, :3] * alpha + target[:, :, :3] * (1 - alpha)).astype(np.uint8)
    target[:, :, 3] = 255


def generate_icon() -> None:
    canvas = np.zeros((64, 64, 4), dtype=np.uint8)
    canvas[:, :, :] = (5, 12, 22, 255)
    for y in range(0, 64, 8):
        for x in range(0, 64, 8):
            if (x // 8 + y // 8) % 2 == 0:
                canvas[y:y + 8, x:x + 8, :3] = (8, 22, 34)

    lime = np.array((121, 255, 42, 255), dtype=np.uint8)
    copper = np.array((196, 116, 54, 255), dtype=np.uint8)
    cyan = np.array((31, 198, 255, 255), dtype=np.uint8)
    canvas[1:4, 1:63] = lime
    canvas[60:63, 1:63] = lime
    canvas[1:63, 1:4] = lime
    canvas[1:63, 60:63] = lime

    # A compact industrial pipe network behind the three supplied fluid symbols.
    canvas[28:36, 8:56] = copper
    canvas[8:56, 28:36] = copper
    canvas[30:34, 8:56] = cyan
    canvas[8:56, 30:34] = cyan
    canvas[24:40, 24:40] = copper
    canvas[28:36, 28:36] = cyan

    panels = [(5, 5), (5, 43), (43, 24)]
    sources = ["IMG_5289.png", "IMG_5278(2).png", "IMG_5279(1).png"]
    for (top, left), filename in zip(panels, sources, strict=True):
        canvas[top - 1:top + 17, left - 1:left + 17, :3] = (2, 7, 14)
        overlay(canvas, mpimg.imread(UPLOADS / filename), top, left)

    icon = np.repeat(np.repeat(canvas, 2, axis=0), 2, axis=1)
    output = ASSETS / "icon.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    plt.imsave(output, icon)


def update_metadata() -> None:
    metadata_path = RESOURCES / "fabric.mod.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    metadata["icon"] = "assets/fluidworks/icon.png"
    metadata["description"] = (
        "Industrial fluid tanks, portable containers, multiblock reservoirs, "
        "and configurable BuildCraft-style fluid pipes."
    )
    write_json(metadata_path, metadata)


if __name__ == "__main__":
    generate_pipe_resources()
    generate_icon()
    update_metadata()
