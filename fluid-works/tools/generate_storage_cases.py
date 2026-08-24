#!/usr/bin/env python3
"""Generate deterministic portable cooler/item-case resources."""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/fluidworks"
DATA = ROOT / "src/main/resources/data/fluidworks"

WOODS = ["oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","bamboo","crimson","warped"]
GLASS = ["glass", "tinted_glass"] + [f"{c}_stained_glass" for c in
    ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]]

def write(path: Path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")

def element(frm, to, texture="#material"):
    return {"from": frm, "to": to, "faces": {face: {"texture": texture} for face in
        ("north","east","south","west","up","down")}}

def closed_model(texture: str, glass: bool):
    parts = [
        element([0,0,5],[16,1,11]), element([0,13,5],[16,14,11]),
        element([0,1,5],[1,13,11]), element([15,1,5],[16,13,11]),
        element([1,1,5],[15,13,5.25]), element([1,1,10.75],[15,13,11]),
        element([6,14,6.5],[7,16,9.5], "#trim"), element([9,14,6.5],[10,16,9.5], "#trim"),
        element([7,15,6.5],[9,16,9.5], "#trim"),
        element([0,6.5,4.75],[16,7.5,5.25], "#trim"),
    ]
    if not glass:
        parts += [element([1,1,5.25],[15,6.5,10.75]), element([1,7.5,5.25],[15,13,10.75])]
    return {"ambientocclusion": not glass, "textures":{"particle":texture,"material":texture,
        "trim":"minecraft:block/iron_block"}, "elements":parts}

def open_model(texture: str, glass: bool):
    # Lower tray remains upright; the upper half is folded down in front of it.
    parts = [
        element([0,0,5],[16,1,11]), element([0,1,5],[1,7,11]), element([15,1,5],[16,7,11]),
        element([1,1,10.75],[15,7,11]),
        element([0,0,0],[16,1,5]), element([0,1,0],[1,6,5]), element([15,1,0],[16,6,5]),
        element([1,1,0],[15,1.25,5]),
        element([6,1,1.5],[7,3,4.5], "#trim"), element([9,1,1.5],[10,3,4.5], "#trim"),
        element([7,2,1.5],[9,3,4.5], "#trim"),
    ]
    if not glass:
        parts += [element([1,1,5.25],[15,6.5,10.75]), element([1,1.25,0.25],[15,1.5,4.75])]
    return {"ambientocclusion": not glass, "textures":{"particle":texture,"material":texture,
        "trim":"minecraft:block/iron_block"}, "elements":parts}

def base_model(texture: str, glass: bool):
    # The interpolated upper lid is submitted by FoodCoolerBlockEntityRenderer.
    parts = [element([0,0,5],[16,1,11]), element([0,1,5],[1,7,11]),
        element([15,1,5],[16,7,11]), element([1,1,10.75],[15,7,11]),
        element([1,1,5],[15,7,5.25])]
    if not glass: parts.append(element([1,1,5.25],[15,6.5,10.75]))
    return {"ambientocclusion":not glass,"textures":{"particle":texture,"material":texture,
        "trim":"minecraft:block/iron_block"},"elements":parts}

def blockstate(base: str):
    variants = {}
    rotations = {"north":0,"east":90,"south":180,"west":270}
    for facing, y in rotations.items():
        for opened in (False, True):
            entry = {"model": f"fluidworks:block/{base}_{'open' if opened else 'closed'}"}
            if y: entry["y"] = y
            variants[f"facing={facing},open={str(opened).lower()}"] = entry
    return {"variants":variants}

def item_defs(base: str):
    write(ASSETS/"models/item"/f"{base}.json", {"parent":f"fluidworks:block/{base}_closed"})
    write(ASSETS/"items"/f"{base}.json", {"model":{"type":"minecraft:model","model":f"fluidworks:item/{base}"}})

def empty_loot(base: str):
    # Java creates one component-bearing drop. A data-driven self drop would duplicate it.
    write(DATA/"loot_table/blocks"/f"{base}.json", {"type":"minecraft:block","pools":[]})

def recipe(base: str, material: str, center: str, pattern=None, key_extra=None):
    pattern = pattern or ["MMM","MCM","MMM"]
    key = {"M":material,"C":center}
    if key_extra: key.update(key_extra)
    write(DATA/"recipe"/f"{base}.json", {"type":"minecraft:crafting_shaped","category":"decorations",
        "pattern":pattern,"key":key,"result":{"id":f"fluidworks:{base}","count":1}})

entries = {}
for glass in GLASS:
    base = f"{glass}_portable_food_cooler"
    texture = f"minecraft:block/{glass}"
    write(ASSETS/"models/block"/f"{base}_closed.json", base_model(texture, True))
    write(ASSETS/"models/block"/f"{base}_open.json", base_model(texture, True))
    write(ASSETS/"models/block/preview"/f"{base}_closed.json", closed_model(texture, True))
    write(ASSETS/"models/block/preview"/f"{base}_open.json", open_model(texture, True))
    write(ASSETS/"blockstates"/f"{base}.json", blockstate(base)); item_defs(base); empty_loot(base)
    recipe(base, f"minecraft:{glass}", "minecraft:chest")
    display = "Clear Glass" if glass == "glass" else glass.replace("_", " ").title()
    entries[f"block.fluidworks.{base}"] = f"{display} Portable Food Cooler"

for wood in WOODS:
    base = f"{wood}_item_case"; texture = f"minecraft:block/{wood}_planks"
    write(ASSETS/"models/block"/f"{base}_closed.json", base_model(texture, False))
    write(ASSETS/"models/block"/f"{base}_open.json", base_model(texture, False))
    write(ASSETS/"models/block/preview"/f"{base}_closed.json", closed_model(texture, False))
    write(ASSETS/"models/block/preview"/f"{base}_open.json", open_model(texture, False))
    write(ASSETS/"blockstates"/f"{base}.json", blockstate(base)); item_defs(base); empty_loot(base)
    recipe(base, f"minecraft:{wood}_planks", "minecraft:chest", ["MIM","MCM","MMM"], {"I":"minecraft:iron_nugget"})
    entries[f"block.fluidworks.{base}"] = f"{wood.replace('_',' ').title()} Item Case"

for metal in ("iron", "gold"):
    base=f"{metal}_item_case"; texture=f"minecraft:block/{metal}_block"
    write(ASSETS/"models/block"/f"{base}_closed.json", base_model(texture, False))
    write(ASSETS/"models/block"/f"{base}_open.json", base_model(texture, False))
    write(ASSETS/"models/block/preview"/f"{base}_closed.json", closed_model(texture, False))
    write(ASSETS/"models/block/preview"/f"{base}_open.json", open_model(texture, False))
    write(ASSETS/"blockstates"/f"{base}.json", blockstate(base)); item_defs(base); empty_loot(base)
    recipe(base, f"minecraft:{metal}_ingot", "minecraft:chest")
    entries[f"block.fluidworks.{base}"] = f"{metal.title()} Item Case"

lang_path = ASSETS/"lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update(entries)
lang["container.fluidworks.case_padding"] = "Unused"
lang["container.fluidworks.crate_page_selected"] = "Crate page %s/%s selected"
write(lang_path, dict(sorted(lang.items())))

def merge_tag(path: Path, values):
    value = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace":False,"values":[]}
    value["values"] = sorted(set(value.get("values", [])) | set(values))
    write(path, value)

merge_tag(DATA/"../minecraft/tags/block/mineable/axe.json",
    [f"fluidworks:{wood}_item_case" for wood in WOODS])
merge_tag(DATA/"../minecraft/tags/block/mineable/pickaxe.json",
    ["fluidworks:iron_item_case", "fluidworks:gold_item_case"])
print(f"Generated {len(GLASS)} food coolers and {len(WOODS)+2} item cases")
