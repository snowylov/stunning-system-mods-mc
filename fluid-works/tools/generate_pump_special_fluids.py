#!/usr/bin/env python3
"""Generate deterministic resources for wooden drain grates, pump, and special fluids."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/fluidworks"
DATA = ROOT / "data"
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
         "cherry", "pale_oak", "bamboo", "crimson", "warped"]


def write(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def directional_blockstate(model: str) -> dict:
    return {"multipart": [
        {"when": {"facing": "north"}, "apply": {"model": model}},
        {"when": {"facing": "south"}, "apply": {"model": model, "y": 180}},
        {"when": {"facing": "west"}, "apply": {"model": model, "y": 270}},
        {"when": {"facing": "east"}, "apply": {"model": model, "y": 90}},
        {"when": {"facing": "up"}, "apply": {"model": model, "x": 270}},
        {"when": {"facing": "down"}, "apply": {"model": model, "x": 90}},
    ]}


def self_loot(block_id: str) -> dict:
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"fluidworks:{block_id}",
        "conditions": [{"condition": "minecraft:survives_explosion"}]
    }]}]}


def cube(from_xyz: list[int], to_xyz: list[int], texture: str = "#device") -> dict:
    return {"from": from_xyz, "to": to_xyz, "faces": {
        side: {"texture": texture} for side in ("down", "up", "north", "south", "west", "east")
    }}


for wood in WOODS:
    block_id = f"{wood}_drain_grate"
    write(ASSETS / f"blockstates/{block_id}.json",
          directional_blockstate(f"fluidworks:block/{block_id}"))
    write(ASSETS / f"models/block/{block_id}.json", {
        "parent": "fluidworks:block/drain_grate",
        "textures": {"device": f"minecraft:block/{wood}_planks",
                     "particle": f"minecraft:block/{wood}_planks"}
    })
    write(ASSETS / f"models/item/{block_id}.json", {"parent": f"fluidworks:block/{block_id}"})
    write(ASSETS / f"items/{block_id}.json", {"model": {"type": "minecraft:model",
          "model": f"fluidworks:item/{block_id}"}})
    write(DATA / f"fluidworks/loot_table/blocks/{block_id}.json", self_loot(block_id))
    write(DATA / f"fluidworks/recipe/{block_id}.json", {
        "type": "minecraft:crafting_shaped", "category": "redstone",
        "pattern": ["PPP", "PHP", "PPP"],
        "key": {"P": f"minecraft:{wood}_planks", "H": "minecraft:hopper"},
        "result": {"id": f"fluidworks:{block_id}", "count": 1}
    })

pump_elements = [
    cube([3, 2, 3], [13, 14, 13]), cube([5, 5, 0], [11, 11, 3]),
    cube([3, 3, 0], [13, 13, 2]), cube([5, 5, 13], [11, 11, 16]),
    cube([3, 3, 14], [13, 13, 16]), cube([6, 14, 6], [10, 16, 10]),
    cube([2, 0, 4], [14, 2, 12]),
    cube([4, 6, 2], [12, 10, 4], "#accent"), cube([6, 4, 2], [10, 12, 4], "#accent")
]
write(ASSETS / "models/block/fluid_pump.json", {
    "textures": {"device": "fluidworks:block/fluid_device_copper",
                 "accent": "fluidworks:block/fluid_hardware_iron", "particle": "#device"},
    "elements": pump_elements
})
write(ASSETS / "blockstates/fluid_pump.json",
      directional_blockstate("fluidworks:block/fluid_pump"))
write(ASSETS / "models/item/fluid_pump.json", {"parent": "fluidworks:block/fluid_pump"})
write(ASSETS / "items/fluid_pump.json", {"model": {"type": "minecraft:model",
      "model": "fluidworks:item/fluid_pump"}})
write(DATA / "fluidworks/loot_table/blocks/fluid_pump.json", self_loot("fluid_pump"))
write(DATA / "fluidworks/recipe/fluid_pump.json", {
    "type": "minecraft:crafting_shaped", "category": "redstone",
    "pattern": ["ICI", "PRP", "ICI"],
    "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot",
            "P": "fluidworks:fluid_pipe", "R": "minecraft:redstone_block"},
    "result": {"id": "fluidworks:fluid_pump", "count": 1}
})

bucket_texture = {
    "ender": "fluidworks:item/liquid_rose_gold_bucket",
    "nitrogen": "fluidworks:item/liquid_diamond_bucket",
    "cryogen": "fluidworks:item/liquid_copper_bucket",
}
for fluid in ("ender", "nitrogen", "cryogen"):
    write(ASSETS / f"blockstates/liquid_{fluid}_block.json",
          {"variants": {"": {"model": "minecraft:block/water"}}})
    write(ASSETS / f"models/item/liquid_{fluid}_bucket.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": bucket_texture[fluid]}})
    write(ASSETS / f"items/liquid_{fluid}_bucket.json", {"model": {"type": "minecraft:model",
          "model": f"fluidworks:item/liquid_{fluid}_bucket"}})

write(DATA / "fluidworks/fluid_mixing/rose_gold.json", {
    "inputs": ["fluidworks:liquid_gold", "fluidworks:liquid_copper"],
    "result": "fluidworks:liquid_rose_gold"
})
write(DATA / "fluidworks/fluid_mixing/cryogen.json", {
    "inputs": ["fluidworks:liquid_nitrogen", "fluidworks:liquid_ender"],
    "result": "fluidworks:liquid_cryogen"
})
write(DATA / "c/tags/fluid/cold_liquids.json", {"replace": False, "values": [
    "fluidworks:liquid_nitrogen", "fluidworks:flowing_liquid_nitrogen",
    "fluidworks:liquid_cryogen", "fluidworks:flowing_liquid_cryogen"]})
write(DATA / "c/tags/fluid/hot_liquids.json", {"replace": False, "values": [
    "minecraft:lava", "minecraft:flowing_lava",
    *[f"fluidworks:{prefix}liquid_{metal}" for metal in ("iron", "copper", "gold")
      for prefix in ("", "flowing_")]]})

lang_path = ASSETS / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
for wood in WOODS:
    display = wood.replace("_", " ").title()
    lang[f"block.fluidworks.{wood}_drain_grate"] = f"{display} Drain Grate"
lang.update({
    "block.fluidworks.fluid_pump": "Fluid Pump",
    "item.fluidworks.liquid_ender_bucket": "Liquid Ender Bucket",
    "item.fluidworks.liquid_nitrogen_bucket": "Liquid Nitrogen Bucket",
    "item.fluidworks.liquid_cryogen_bucket": "Cryogen Bucket",
})
write(lang_path, lang)

for namespace, path, additions in (
    ("minecraft", "tags/block/mineable/axe.json", [f"fluidworks:{w}_drain_grate" for w in WOODS]),
    ("minecraft", "tags/block/mineable/pickaxe.json", ["fluidworks:fluid_pump"]),
    ("c", "tags/block/fluid_devices.json", ["fluidworks:fluid_pump", *[f"fluidworks:{w}_drain_grate" for w in WOODS]]),
    ("c", "tags/item/fluid_devices.json", ["fluidworks:fluid_pump", *[f"fluidworks:{w}_drain_grate" for w in WOODS]]),
):
    target = DATA / namespace / path
    current = json.loads(target.read_text(encoding="utf-8")) if target.exists() else {"replace": False, "values": []}
    current.setdefault("values", [])
    for value in additions:
        if value not in current["values"]:
            current["values"].append(value)
    write(target, current)

print("Generated pump, special fluid, and 12 wooden drain-grate resources")
