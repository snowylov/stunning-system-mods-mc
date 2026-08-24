#!/usr/bin/env python3
"""Generate deterministic JSON models/resources for furniture, channels, connected pipes and tanks."""
from __future__ import annotations

import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fluidworks"
DATA = ROOT / "src/main/resources/data/fluidworks"

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
         "cherry", "pale_oak", "bamboo", "crimson", "warped"]
CHANNELS = WOODS + ["copper", "iron", "gold", "stone", "cobblestone"]
PIPE_IDS = ["fluid_pipe", "redstone_fluid_valve", "extraction_fluid_pipe", "high_pressure_pipe",
            "meter_pipe", "overflow_valve", "pulse_valve", "priority_junction", "fluid_diode",
            "filter_pipe", "mixing_junction"]


def write(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def cube_faces(texture="#texture", *, cull=False):
    result = {}
    for face in ("down", "up", "north", "south", "west", "east"):
        result[face] = {"texture": texture}
        if cull:
            result[face]["cullface"] = face
    return result


def element(start, end, texture="#texture", *, cull=False):
    return {"from": start, "to": end, "faces": cube_faces(texture, cull=cull)}


def item_files(block_id: str, model_id: str | None = None):
    model_id = model_id or block_id
    write(ASSETS / "models/item" / f"{block_id}.json", {"parent": f"fluidworks:block/{model_id}"})
    write(ASSETS / "items" / f"{block_id}.json", {
        "model": {"type": "minecraft:model", "model": f"fluidworks:item/{block_id}"}
    })


def loot(block_id: str):
    write(DATA / "loot_table/blocks" / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"fluidworks:{block_id}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]
    })


def directional_blockstate(model: str):
    return {"variants": {
        "facing=north": {"model": f"fluidworks:block/{model}"},
        "facing=east": {"model": f"fluidworks:block/{model}", "y": 90},
        "facing=south": {"model": f"fluidworks:block/{model}", "y": 180},
        "facing=west": {"model": f"fluidworks:block/{model}", "y": 270},
    }}


def furniture():
    models = {
        "chair": [
            element([2, 7, 2], [14, 10, 14]),
            element([2, 0, 2], [5, 7, 5]), element([11, 0, 2], [14, 7, 5]),
            element([2, 0, 11], [5, 7, 14]), element([11, 0, 11], [14, 7, 14]),
            element([2, 10, 12], [14, 16, 15]),
        ],
        "four_legged_table": [
            element([0, 12, 0], [16, 16, 16], cull=True),
            element([1, 0, 1], [4, 12, 4]), element([12, 0, 1], [15, 12, 4]),
            element([1, 0, 12], [4, 12, 15]), element([12, 0, 12], [15, 12, 15]),
        ],
        "one_legged_table": [
            element([0, 12, 0], [16, 16, 16], cull=True),
            element([6, 1, 6], [10, 12, 10]), element([3, 0, 3], [13, 2, 13]),
        ],
    }
    recipes = {
        "chair": (["PPP", " S ", "S S"], {"P": "minecraft:oak_planks", "S": "minecraft:stick"}, 2),
        "four_legged_table": (["PPP", "S S", "S S"], {"P": "minecraft:oak_planks", "S": "minecraft:stick"}, 1),
        "one_legged_table": (["PPP", " S ", "SSS"], {"P": "minecraft:oak_planks", "S": "minecraft:stick"}, 1),
    }
    for block_id, elements in models.items():
        write(ASSETS / "models/block" / f"{block_id}.json", {
            "textures": {"texture": "minecraft:block/oak_planks", "particle": "minecraft:block/oak_planks"},
            "elements": elements,
        })
        write(ASSETS / "blockstates" / f"{block_id}.json", directional_blockstate(block_id))
        item_files(block_id)
        loot(block_id)
        pattern, key, count = recipes[block_id]
        write(DATA / "recipe" / f"{block_id}.json", {
            "type": "minecraft:crafting_shaped", "category": "decorations", "pattern": pattern,
            "key": key, "result": {"id": f"fluidworks:{block_id}", "count": count}
        })


def channel_texture(material: str) -> str:
    if material in WOODS:
        return f"minecraft:block/{material}_planks"
    return {
        "copper": "minecraft:block/copper_block", "iron": "minecraft:block/iron_block",
        "gold": "minecraft:block/gold_block", "stone": "minecraft:block/stone",
        "cobblestone": "minecraft:block/cobblestone",
    }[material]


def channel_ingredient(material: str) -> str:
    if material in WOODS:
        return f"minecraft:{material}_planks"
    return {
        "copper": "minecraft:copper_ingot", "iron": "minecraft:iron_ingot",
        "gold": "minecraft:gold_ingot", "stone": "minecraft:stone",
        "cobblestone": "minecraft:cobblestone",
    }[material]


def channels():
    flow = {
        "textures": {"flow": "fluidworks:block/fluid_channel_flow", "particle": "fluidworks:block/fluid_channel_flow"},
        "elements": [{"from": [5, 5.05, 0], "to": [11, 5.15, 16], "shade": False,
                      "faces": {"up": {"texture": "#flow", "uv": [0, 0, 6, 16]}}}],
    }
    write(ASSETS / "models/block/fluid_channel_flow.json", flow)
    for material in CHANNELS:
        block_id = f"{material}_fluid_channel"
        texture = channel_texture(material)
        write(ASSETS / "models/block" / f"{block_id}.json", {
            "textures": {"texture": texture, "particle": texture},
            "elements": [
                element([2, 2, 0], [14, 5, 16]),
                element([2, 5, 0], [5, 12, 16]),
                element([11, 5, 0], [14, 12, 16]),
            ],
        })
        write(ASSETS / "models/block/preview" / f"{block_id}_filled.json", {
            "textures": {"texture": texture, "flow": "fluidworks:block/fluid_channel_flow",
                         "particle": texture},
            "elements": [
                element([2, 2, 0], [14, 5, 16]),
                element([2, 5, 0], [5, 12, 16]),
                element([11, 5, 0], [14, 12, 16]),
                {"from": [5, 5.05, 0], "to": [11, 5.15, 16], "shade": False,
                 "faces": {"up": {"texture": "#flow", "uv": [0, 0, 6, 16]}}},
            ],
        })
        multipart = []
        rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
        for facing, rotation in rotations.items():
            base = {"model": f"fluidworks:block/{block_id}", **rotation}
            liquid = {"model": "fluidworks:block/fluid_channel_flow", **rotation}
            multipart.append({"when": {"facing": facing}, "apply": base})
            multipart.append({"when": {"facing": facing, "filled": "true"}, "apply": liquid})
        write(ASSETS / "blockstates" / f"{block_id}.json", {"multipart": multipart})
        item_files(block_id)
        loot(block_id)
        write(DATA / "recipe" / f"{block_id}.json", {
            "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["M M", "MMM"],
            "key": {"M": channel_ingredient(material)},
            "result": {"id": f"fluidworks:{block_id}", "count": 6},
        })

    # Four vertically stacked frames make the surface visibly move in the flow direction.
    image = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for frame in range(4):
        for y in range(16):
            for x in range(16):
                wave = ((y + frame * 4) % 8) in (0, 1)
                color = (74, 174, 255, 190) if wave else (44, 126, 224, 180)
                image.putpixel((x, frame * 16 + y), color)
    texture_path = ASSETS / "textures/block/fluid_channel_flow.png"
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    image.save(texture_path)
    write(Path(str(texture_path) + ".mcmeta"), {"animation": {"frametime": 2, "interpolate": True}})


def stackable_tank():
    textures = {"side": "minecraft:block/glass", "top": "minecraft:block/glass",
                "particle": "minecraft:block/glass"}
    body = element([1, 0, 1], [15, 16, 15], "#side")
    cap_top = element([0, 14, 0], [16, 16, 16], "#top")
    cap_bottom = element([0, 0, 0], [16, 2, 16], "#top")
    write(ASSETS / "models/block/stackable_tank_body.json", {"textures": textures, "elements": [body]})
    write(ASSETS / "models/block/stackable_tank_top_cap.json", {"textures": textures, "elements": [cap_top]})
    write(ASSETS / "models/block/stackable_tank_bottom_cap.json", {"textures": textures, "elements": [cap_bottom]})
    write(ASSETS / "models/block/stackable_tank.json", {"textures": textures,
          "elements": [body, cap_top, cap_bottom]})
    write(ASSETS / "blockstates/stackable_tank.json", {"multipart": [
        {"apply": {"model": "fluidworks:block/stackable_tank_body"}},
        {"when": {"connected_up": "false"}, "apply": {"model": "fluidworks:block/stackable_tank_top_cap"}},
        {"when": {"connected_down": "false"}, "apply": {"model": "fluidworks:block/stackable_tank_bottom_cap"}},
    ]})
    item_files("stackable_tank")
    loot("stackable_tank")
    write(DATA / "recipe/stackable_tank.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["IGI", "G G", "IGI"],
        "key": {"I": "minecraft:iron_ingot", "G": "fluidworks:double_smelted_glass"},
        "result": {"id": "fluidworks:stackable_tank", "count": 1},
    })


def reservoir_top():
    path = ASSETS / "models/block/reservoir_tank.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    model["textures"] = {"side": "fluidworks:block/tank_side", "top": "fluidworks:block/tank_top",
                         "particle": "fluidworks:block/tank_side"}
    for face, value in model["elements"][0]["faces"].items():
        value["texture"] = "#top" if face in ("up", "down") else "#side"
    write(path, model)


def connected_pipes():
    rotations = {
        "north": {}, "south": {"y": 180}, "west": {"y": 270}, "east": {"y": 90},
        "up": {"x": 270}, "down": {"x": 90},
    }
    for block_id in PIPE_IDS:
        old = json.loads((ASSETS / "models/block" / f"{block_id}.json").read_text(encoding="utf-8"))
        texture = next(value for key, value in old["textures"].items() if key != "particle")
        textures = {"texture": texture, "particle": texture}
        write(ASSETS / "models/block" / f"{block_id}_core.json", {
            "textures": textures, "elements": [element([5, 5, 5], [11, 11, 11])]
        })
        write(ASSETS / "models/block" / f"{block_id}_arm.json", {
            "textures": textures, "elements": [
                element([6, 6, 0], [10, 10, 5]), element([4, 4, 0], [12, 12, 2])]
        })
        write(ASSETS / "models/block/preview" / f"{block_id}_straight.json", {
            "textures": textures, "elements": [
                element([5, 5, 5], [11, 11, 11]),
                element([6, 6, 0], [10, 10, 5]), element([4, 4, 0], [12, 12, 2]),
                element([6, 6, 11], [10, 10, 16]), element([4, 4, 14], [12, 12, 16]),
            ]
        })
        multipart = []
        for facing, rotation in rotations.items():
            multipart.append({"when": {"facing": facing},
                              "apply": {"model": f"fluidworks:block/{block_id}_core", **rotation}})
        for direction, rotation in rotations.items():
            multipart.append({"when": {direction: "true"},
                              "apply": {"model": f"fluidworks:block/{block_id}_arm", **rotation}})
        write(ASSETS / "blockstates" / f"{block_id}.json", {"multipart": multipart})


def language():
    path = ASSETS / "lang/en_us.json"
    lang = json.loads(path.read_text(encoding="utf-8"))
    lang.update({
        "block.fluidworks.chair": "Chair",
        "block.fluidworks.four_legged_table": "4-Legged Table",
        "block.fluidworks.one_legged_table": "1-Legged Table",
        "block.fluidworks.stackable_tank": "Stackable Tank",
    })
    display = {name: name.replace("_", " ").title() for name in CHANNELS}
    display["dark_oak"] = "Dark Oak"
    display["pale_oak"] = "Pale Oak"
    for material, title in display.items():
        lang[f"block.fluidworks.{material}_fluid_channel"] = f"{title} Fluid Channel"
    write(path, lang)


def connected_furniture_and_crates():
    """Connection-aware benches/tables plus seamless six-direction crate structures."""
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    all_materials = [("", "minecraft:block/oak_planks")] + [
        (wood + "_", f"minecraft:block/{wood}_planks") for wood in WOODS
    ]
    for material_index, (prefix, texture) in enumerate(all_materials):
        # Bench core always spans the block. Only exposed ends receive legs and a distinctive border.
        chair_id = prefix + "chair"
        core_id = chair_id + "_bench_core"
        left_id, right_id = chair_id + "_bench_left", chair_id + "_bench_right"
        textures = {"texture": texture, "particle": texture}
        write(ASSETS / "models/block" / f"{core_id}.json", {"textures": textures, "elements": [
            element([0, 7, 2], [16, 10, 14]), element([0, 10, 12], [16, 15, 15]),
            element([0, 13, 11.5], [16, 14, 16]),
        ]})
        border_width = 1 + (material_index % 2)
        cap_y = 15 if material_index % 3 else 14
        left_elements = [
            element([0, 0, 2], [3, 7, 5]), element([0, 0, 11], [3, 7, 14]),
            element([0, 9, 11], [border_width + 1, 16, 16]),
            element([0, cap_y, 9], [4 + material_index % 3, 16, 16]),
        ]
        right_elements = [
            element([13, 0, 2], [16, 7, 5]), element([13, 0, 11], [16, 7, 14]),
            element([15 - border_width, 9, 11], [16, 16, 16]),
            element([12 - material_index % 3, cap_y, 9], [16, 16, 16]),
        ]
        write(ASSETS / "models/block" / f"{left_id}.json", {"textures": textures, "elements": left_elements})
        write(ASSETS / "models/block" / f"{right_id}.json", {"textures": textures, "elements": right_elements})
        multipart = []
        for facing, rotation in rotations.items():
            multipart += [
                {"when": {"facing": facing}, "apply": {"model": f"fluidworks:block/{core_id}", **rotation}},
                {"when": {"facing": facing, "left": "false"}, "apply": {"model": f"fluidworks:block/{left_id}", **rotation}},
                {"when": {"facing": facing, "right": "false"}, "apply": {"model": f"fluidworks:block/{right_id}", **rotation}},
            ]
        write(ASSETS / "blockstates" / f"{chair_id}.json", {"multipart": multipart})
        write(ASSETS / "models/block" / f"{chair_id}.json", {"textures": textures,
            "elements": json.loads(json.dumps((json.loads((ASSETS / "models/block" / f"{core_id}.json").read_text()))["elements"] + left_elements + right_elements))})

        for kind in ("four_legged_table", "one_legged_table"):
            table_id = prefix + kind
            top_id = table_id + "_connected_top"
            write(ASSETS / "models/block" / f"{top_id}.json", {"textures": textures,
                "elements": [element([0, 12, 0], [16, 16, 16], cull=True)]})
            corners = {
                "nw": ([1, 0, 1], [4, 12, 4], {"north": "false", "west": "false"}),
                "ne": ([12, 0, 1], [15, 12, 4], {"north": "false", "east": "false"}),
                "sw": ([1, 0, 12], [4, 12, 15], {"south": "false", "west": "false"}),
                "se": ([12, 0, 12], [15, 12, 15], {"south": "false", "east": "false"}),
            }
            parts = [{"apply": {"model": f"fluidworks:block/{top_id}"}}]
            full_elements = [element([0, 12, 0], [16, 16, 16], cull=True)]
            for corner, (start, end, condition) in corners.items():
                model = table_id + "_leg_" + corner
                leg = element(start, end)
                write(ASSETS / "models/block" / f"{model}.json", {"textures": textures, "elements": [leg]})
                parts.append({"when": condition, "apply": {"model": f"fluidworks:block/{model}"}})
                full_elements.append(leg)
            write(ASSETS / "blockstates" / f"{table_id}.json", {"multipart": parts})
            write(ASSETS / "models/block" / f"{table_id}.json", {"textures": textures, "elements": full_elements})

    for wood in WOODS:
        block_id = f"{wood}_crate"
        texture = f"minecraft:block/{wood}_planks"
        textures = {"texture": texture, "particle": texture}
        core_id, face_id = block_id + "_core", block_id + "_face"
        write(ASSETS / "models/block" / f"{core_id}.json", {"textures": textures,
            "elements": [element([0, 0, 0], [16, 16, 16], cull=True)]})
        face_elements = [element([0, 0, 0], [16, 2, 2]), element([0, 14, 0], [16, 16, 2]),
                         element([0, 2, 0], [2, 14, 2]), element([14, 2, 0], [16, 14, 2]),
                         element([2, 2, 0], [14, 3, 1]), element([2, 13, 0], [14, 14, 1])]
        write(ASSETS / "models/block" / f"{face_id}.json", {"textures": textures, "elements": face_elements})
        directions = {"north": {}, "south": {"y": 180}, "west": {"y": 270}, "east": {"y": 90},
                      "up": {"x": 90}, "down": {"x": 270}}
        multipart = [{"apply": {"model": f"fluidworks:block/{core_id}"}}]
        for direction, rotation in directions.items():
            multipart.append({"when": {direction: "false"},
                              "apply": {"model": f"fluidworks:block/{face_id}", **rotation}})
        write(ASSETS / "blockstates" / f"{block_id}.json", {"multipart": multipart})
        full = [element([0, 0, 0], [16, 16, 16])] + face_elements
        write(ASSETS / "models/block" / f"{block_id}.json", {"textures": textures, "elements": full})
        item_files(block_id)
        loot(block_id)
        write(DATA / "recipe" / f"{block_id}.json", {
            "type": "minecraft:crafting_shaped", "category": "decorations", "pattern": ["PPP", "PCP", "PPP"],
            "key": {"P": f"minecraft:{wood}_planks", "C": "minecraft:chest"},
            "result": {"id": f"fluidworks:{block_id}", "count": 1},
        })

    axe_path = DATA.parent / "minecraft/tags/block/mineable/axe.json"
    axe = json.loads(axe_path.read_text(encoding="utf-8"))
    for wood in WOODS:
        value = f"fluidworks:{wood}_crate"
        if value not in axe["values"]: axe["values"].append(value)
    write(axe_path, axe)
    lang_path = ASSETS / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for wood in WOODS:
        name = wood.replace("_", " ").title()
        lang[f"block.fluidworks.{wood}_crate"] = f"{name} Crate"
    lang["container.fluidworks.scaled_crate"] = "%s — page %s/%s (%s total slots)"
    write(lang_path, lang)


def main():
    furniture()
    channels()
    stackable_tank()
    reservoir_top()
    connected_pipes()
    language()
    connected_furniture_and_crates()


if __name__ == "__main__":
    main()
