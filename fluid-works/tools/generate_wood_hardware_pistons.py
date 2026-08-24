#!/usr/bin/env python3
"""Generate twelve-wood furniture/stairs, iron pistons, and iron/netherite fluid hardware."""
from __future__ import annotations

import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fluidworks"
DATA = ROOT / "src/main/resources/data/fluidworks"
UPLOADS = ROOT.parents[1] / "upload"

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
         "cherry", "pale_oak", "bamboo", "crimson", "warped"]
FURNITURE = ["chair", "four_legged_table", "one_legged_table"]
DEVICES = ["fluid_sprinkler", "vacuum_drain", "fluid_cannon", "spill_tray",
           "pressure_sensor", "emergency_shutoff", "sampling_valve", "fluid_router",
           "heat_exchanger", "fluid_separator", "mist_nozzle", "drain_grate",
           "pipe_cover", "fluid_trap", "remote_tank_link"]
PIPES = ["fluid_pipe", "redstone_fluid_valve", "extraction_fluid_pipe", "high_pressure_pipe",
         "meter_pipe", "overflow_valve", "pulse_valve", "priority_junction", "fluid_diode",
         "filter_pipe", "mixing_junction"]
MATERIAL_TEXTURES = {
    "iron": (UPLOADS / "F4A45BB0-66DE-46D9-9FB9-ECAE1910E9C4.png", "fluid_hardware_iron"),
    "netherite": (UPLOADS / "04AEDB6F-C48A-4582-9D05-316089F359CD.png", "fluid_hardware_netherite"),
}
SHAPES = ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]
HALVES = ["bottom", "top"]


def write(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def faces(texture: str = "#texture", cull: bool = False) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    for direction in ("down", "up", "north", "south", "west", "east"):
        result[direction] = {"texture": texture}
        if cull:
            result[direction]["cullface"] = direction
    return result


def element(start: list[float], end: list[float], texture: str = "#texture",
            cull: bool = False) -> dict[str, object]:
    return {"from": start, "to": end, "faces": faces(texture, cull)}


def item_files(block_id: str, block_model: str | None = None) -> None:
    block_model = block_model or block_id
    write(ASSETS / f"models/item/{block_id}.json", {"parent": f"fluidworks:block/{block_model}"})
    write(ASSETS / f"items/{block_id}.json", {
        "model": {"type": "minecraft:model", "model": f"fluidworks:item/{block_id}"}
    })


def loot(block_id: str) -> None:
    write(DATA / f"loot_table/blocks/{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item",
                                              "name": f"fluidworks:{block_id}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })


def horizontal_blockstate(model: str) -> dict[str, object]:
    return {"variants": {
        "facing=north": {"model": f"fluidworks:block/{model}"},
        "facing=east": {"model": f"fluidworks:block/{model}", "y": 90},
        "facing=south": {"model": f"fluidworks:block/{model}", "y": 180},
        "facing=west": {"model": f"fluidworks:block/{model}", "y": 270},
    }}


def six_way_rotations() -> dict[str, dict[str, int]]:
    return {"north": {}, "south": {"y": 180}, "west": {"y": 270},
            "east": {"y": 90}, "up": {"x": 270}, "down": {"x": 90}}


def wood_title(wood: str) -> str:
    return wood.replace("_", " ").title()


def generate_wood_furniture() -> None:
    recipe_data = {
        "chair": (["PPP", " S ", "S S"], 2),
        "four_legged_table": (["PPP", "S S", "S S"], 1),
        "one_legged_table": (["PPP", " S ", "SSS"], 1),
    }
    for wood in WOODS:
        plank = f"minecraft:{wood}_planks"
        texture = f"minecraft:block/{wood}_planks"
        for furniture in FURNITURE:
            block_id = f"{wood}_{furniture}"
            write(ASSETS / f"models/block/{block_id}.json", {
                "parent": f"fluidworks:block/{furniture}",
                "textures": {"texture": texture, "particle": texture},
            })
            write(ASSETS / f"blockstates/{block_id}.json", horizontal_blockstate(block_id))
            item_files(block_id)
            loot(block_id)
            pattern, count = recipe_data[furniture]
            write(DATA / f"recipe/{block_id}.json", {
                "type": "minecraft:crafting_shaped", "category": "decorations", "pattern": pattern,
                "key": {"P": plank, "S": "minecraft:stick"},
                "result": {"id": f"fluidworks:{block_id}", "count": count},
            })


def rise(direction: str, x: int, z: int) -> int:
    return {"north": 16 - z, "south": z + 1, "west": 16 - x, "east": x + 1}[direction]


def stair_height(shape: str, x: int, z: int) -> int:
    primary = rise("north", x, z)
    corner_direction = "west" if shape.endswith("left") else "east"
    corner = rise(corner_direction, x, z)
    if shape == "straight":
        return primary
    if shape.startswith("inner"):
        return max(primary, corner)
    return min(primary, corner)


def rectangle_cover(mask: list[list[bool]]) -> list[tuple[int, int, int, int]]:
    used = [[False] * 16 for _ in range(16)]
    rectangles: list[tuple[int, int, int, int]] = []
    for z in range(16):
        for x in range(16):
            if not mask[z][x] or used[z][x]:
                continue
            width = 0
            while x + width < 16 and mask[z][x + width] and not used[z][x + width]:
                width += 1
            depth = 1
            while z + depth < 16 and all(mask[z + depth][column]
                                         and not used[z + depth][column]
                                         for column in range(x, x + width)):
                depth += 1
            for row in range(z, z + depth):
                for column in range(x, x + width):
                    used[row][column] = True
            rectangles.append((x, z, width, depth))
    return rectangles


def micro_stair_elements(shape: str, half: str) -> list[dict[str, object]]:
    elements: list[dict[str, object]] = []
    for y in range(16):
        threshold = y + 1 if half == "bottom" else 16 - y
        mask = [[stair_height(shape, x, z) >= threshold for x in range(16)] for z in range(16)]
        for x, z, width, depth in rectangle_cover(mask):
            elements.append(element([x, y, z], [x + width, y + 1, z + depth], "#wood"))
    return elements


def generate_micro_stairs() -> None:
    for shape in SHAPES:
        for half in HALVES:
            write(ASSETS / f"models/block/micro_stairs/{shape}_{half}.json", {
                "textures": {"wood": "minecraft:block/oak_planks", "particle": "#wood"},
                "elements": micro_stair_elements(shape, half),
            })

    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    for wood in WOODS:
        texture = f"minecraft:block/{wood}_planks"
        block_id = f"{wood}_16_step_stairs"
        for shape in SHAPES:
            for half in HALVES:
                model_id = f"{block_id}_{shape}_{half}"
                write(ASSETS / f"models/block/{model_id}.json", {
                    "parent": f"fluidworks:block/micro_stairs/{shape}_{half}",
                    "textures": {"wood": texture, "particle": texture},
                })
        write(ASSETS / f"models/block/{block_id}.json", {
            "parent": f"fluidworks:block/{block_id}_straight_bottom"
        })
        variants: dict[str, object] = {}
        for facing, rotation in rotations.items():
            for shape in SHAPES:
                for half in HALVES:
                    key = f"facing={facing},half={half},shape={shape}"
                    value: dict[str, object] = {
                        "model": f"fluidworks:block/{block_id}_{shape}_{half}"
                    }
                    if rotation:
                        value["y"] = rotation
                    variants[key] = value
        write(ASSETS / f"blockstates/{block_id}.json", {"variants": variants})
        item_files(block_id, f"{block_id}_straight_bottom")
        loot(block_id)
        write(DATA / f"recipe/{block_id}.json", {
            "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["P  ", "PP ", "PPP"],
            "key": {"P": f"minecraft:{wood}_planks"},
            "result": {"id": f"fluidworks:{block_id}", "count": 6},
        })


def piston_model(width: int, extended: bool) -> dict[str, object]:
    texture = "fluidworks:block/fluid_hardware_iron"
    elements = [element([0, 0, 0], [16, 16, 16], "#iron", True)]
    if extended:
        inset = (16 - width) / 2
        elements = [element([0, 0, 4], [16, 16, 16], "#iron"),
                    element([inset, inset, 0], [16 - inset, 16 - inset, 4], "#iron")]
    return {"textures": {"iron": texture, "particle": texture}, "elements": elements}


def generate_pistons() -> None:
    rotations = six_way_rotations()
    for width in (12, 14):
        block_id = f"iron_piston_{width}"
        write(ASSETS / f"models/block/{block_id}.json", piston_model(width, False))
        write(ASSETS / f"models/block/{block_id}_extended.json", piston_model(width, True))
        variants: dict[str, object] = {}
        for facing, rotation in rotations.items():
            for extended in (False, True):
                model = f"fluidworks:block/{block_id}{'_extended' if extended else ''}"
                variants[f"extended={str(extended).lower()},facing={facing}"] = {"model": model, **rotation}
        write(ASSETS / f"blockstates/{block_id}.json", {"variants": variants})
        item_files(block_id)
        loot(block_id)
    write(DATA / "recipe/iron_piston_12.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": [" I ", "IPI", " I "],
        "key": {"I": "minecraft:iron_ingot", "P": "minecraft:piston"},
        "result": {"id": "fluidworks:iron_piston_12", "count": 1},
    })
    write(DATA / "recipe/iron_piston_14.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["III", "IPI", "III"],
        "key": {"I": "minecraft:iron_ingot", "P": "minecraft:piston"},
        "result": {"id": "fluidworks:iron_piston_14", "count": 1},
    })


def device_blockstate(model: str) -> dict[str, object]:
    return {"multipart": [
        {"when": {"facing": facing}, "apply": {"model": f"fluidworks:block/{model}", **rotation}}
        for facing, rotation in six_way_rotations().items()
    ]}


def connected_pipe_blockstate(block_id: str) -> dict[str, object]:
    rotations = six_way_rotations()
    multipart = [
        {"when": {"facing": facing},
         "apply": {"model": f"fluidworks:block/{block_id}_core", **rotation}}
        for facing, rotation in rotations.items()
    ]
    multipart.extend(
        {"when": {direction: "true"},
         "apply": {"model": f"fluidworks:block/{block_id}_arm", **rotation}}
        for direction, rotation in rotations.items()
    )
    return {"multipart": multipart}


def iron_upgrade_recipe(block_id: str, base_id: str) -> None:
    write(DATA / f"recipe/{block_id}.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["III", "IBI", "III"],
        "key": {"I": "minecraft:iron_ingot", "B": f"fluidworks:{base_id}"},
        "result": {"id": f"fluidworks:{block_id}", "count": 1},
    })


def netherite_upgrade_recipe(block_id: str, iron_id: str) -> None:
    write(DATA / f"recipe/{block_id}.json", {
        "type": "minecraft:smithing_transform",
        "template": "minecraft:netherite_upgrade_smithing_template",
        "base": f"fluidworks:{iron_id}",
        "addition": "minecraft:netherite_ingot",
        "result": {"id": f"fluidworks:{block_id}"},
    })


def generate_hardware_variants() -> None:
    texture_dir = ASSETS / "textures/block"
    texture_dir.mkdir(parents=True, exist_ok=True)
    for _, (source, target_name) in MATERIAL_TEXTURES.items():
        shutil.copy2(source, texture_dir / f"{target_name}.png")

    for material, (_, texture_name) in MATERIAL_TEXTURES.items():
        texture = f"fluidworks:block/{texture_name}"
        for base_id in DEVICES:
            block_id = f"{material}_{base_id}"
            write(ASSETS / f"models/block/{block_id}.json", {
                "parent": f"fluidworks:block/{base_id}",
                "textures": {"device": texture, "particle": texture},
            })
            write(ASSETS / f"blockstates/{block_id}.json", device_blockstate(block_id))
            item_files(block_id)
            loot(block_id)
            if material == "iron":
                iron_upgrade_recipe(block_id, base_id)
            else:
                netherite_upgrade_recipe(block_id, f"iron_{base_id}")

        for base_id in PIPES:
            block_id = f"{material}_{base_id}"
            for suffix in ("", "_core", "_arm"):
                write(ASSETS / f"models/block/{block_id}{suffix}.json", {
                    "parent": f"fluidworks:block/{base_id}{suffix}",
                    "textures": {"texture": texture, "particle": texture},
                })
            write(ASSETS / f"models/block/preview/{block_id}_straight.json", {
                "parent": f"fluidworks:block/preview/{base_id}_straight",
                "textures": {"texture": texture, "particle": texture},
            })
            write(ASSETS / f"blockstates/{block_id}.json", connected_pipe_blockstate(block_id))
            item_files(block_id)
            loot(block_id)
            if material == "iron":
                iron_upgrade_recipe(block_id, base_id)
            else:
                netherite_upgrade_recipe(block_id, f"iron_{base_id}")


def update_tags() -> None:
    axe_ids = [f"fluidworks:{wood}_{kind}" for wood in WOODS for kind in FURNITURE]
    axe_ids += [f"fluidworks:{wood}_16_step_stairs" for wood in WOODS]
    axe_path = ROOT / "src/main/resources/data/minecraft/tags/block/mineable/axe.json"
    axe = {"replace": False, "values": axe_ids}
    if axe_path.exists():
        existing = json.loads(axe_path.read_text(encoding="utf-8"))
        axe["values"] = list(dict.fromkeys(existing.get("values", []) + axe_ids))
    write(axe_path, axe)

    pickaxe_path = ROOT / "src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json"
    pickaxe = json.loads(pickaxe_path.read_text(encoding="utf-8"))
    new_ids = ["fluidworks:iron_piston_12", "fluidworks:iron_piston_14"]
    new_ids += [f"fluidworks:{material}_{base_id}"
                for material in MATERIAL_TEXTURES for base_id in DEVICES + PIPES]
    pickaxe["values"] = list(dict.fromkeys(pickaxe.get("values", []) + new_ids))
    write(pickaxe_path, pickaxe)

    device_ids = [f"fluidworks:{material}_{base_id}"
                  for material in MATERIAL_TEXTURES for base_id in DEVICES]
    for namespace in ("block", "item"):
        path = ROOT / f"src/main/resources/data/c/tags/{namespace}/fluid_devices.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
        data["values"] = list(dict.fromkeys(data.get("values", []) + device_ids))
        write(path, data)


def update_language() -> None:
    path = ASSETS / "lang/en_us.json"
    language = json.loads(path.read_text(encoding="utf-8"))
    furniture_titles = {"chair": "Chair", "four_legged_table": "4-Legged Table",
                        "one_legged_table": "1-Legged Table"}
    for wood in WOODS:
        for furniture, title in furniture_titles.items():
            language[f"block.fluidworks.{wood}_{furniture}"] = f"{wood_title(wood)} {title}"
        language[f"block.fluidworks.{wood}_16_step_stairs"] = f"{wood_title(wood)} 16-Step Stairs"
    language["block.fluidworks.iron_piston_12"] = "12-Wide Iron Piston"
    language["block.fluidworks.iron_piston_14"] = "14-Wide Iron Piston"
    for material in MATERIAL_TEXTURES:
        material_title = material.title()
        for base_id in DEVICES + PIPES:
            base_key = f"block.fluidworks.{base_id}"
            base_title = language.get(base_key, base_id.replace("_", " ").title())
            language[f"block.fluidworks.{material}_{base_id}"] = f"{material_title} {base_title}"
    write(path, language)


def main() -> None:
    generate_wood_furniture()
    generate_micro_stairs()
    generate_pistons()
    generate_hardware_variants()
    update_tags()
    update_language()


if __name__ == "__main__":
    main()
