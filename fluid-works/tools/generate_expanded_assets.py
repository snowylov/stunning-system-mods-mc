#!/usr/bin/env python3
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/fluidworks"
DATA = RES / "data"

WOODS = ["oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","bamboo","crimson","warped"]
COLORS = ["white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"]
CONCRETES = [f"{c}_concrete" for c in COLORS]
TERRACOTTAS = ["terracotta"] + [f"{c}_terracotta" for c in COLORS]
NORMAL_BUTTONS = WOODS + ["stone", "polished_blackstone"]
STEPS = [4, 5, 6, 16]
SHAPES = ["straight","inner_left","inner_right","outer_left","outer_right"]
HALVES = ["bottom","top"]
LEVER_SPECS = [
    ("copper_gear_lever", "copper", 1), ("copper_valve_lever", "copper", 2),
    ("iron_safety_lever", "iron", 3), ("iron_breaker_lever", "iron", 4),
    ("gold_precision_lever", "gold", 5), ("gold_toggle_lever", "gold", 6),
    ("trimetal_control_lever", "trimetal", 7),
]
FLUID_COLORS = {"gold":(255,198,41), "iron":(216,216,216), "diamond":(77,230,219),
                "copper":(216,115,73), "rose_gold":(240,154,139)}

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")

def faces(texture="#material"):
    return {d:{"texture":texture} for d in ("down","up","north","south","west","east")}

def title(identifier):
    return " ".join(word.capitalize() for word in identifier.split("_"))

def group_for(material):
    if material in WOODS: return "wood"
    if material in CONCRETES: return "concrete"
    if material in TERRACOTTAS: return "terracotta"
    return "normal"

def texture_for(material):
    return f"minecraft:block/{material}_planks" if material in WOODS else f"minecraft:block/{material}"

def runs(steps):
    values=[]
    start=0
    last=None
    for p in range(16):
        level=min(steps, p*steps//16+1)
        if last is not None and level != last:
            values.append((start,p,last))
            start=p
        last=level
    values.append((start,16,last))
    return values

def rise_north(z, steps):
    level=min(steps,(15-z)*steps//16+1)
    return math.ceil(level*16/steps)

def rise_west(x, steps):
    level=min(steps,(15-x)*steps//16+1)
    return math.ceil(level*16/steps)

def rise_east(x, steps):
    level=min(steps,x*steps//16+1)
    return math.ceil(level*16/steps)

def stair_elements(steps, shape, half):
    elements=[]
    intervals=runs(steps)
    for xs,xe,_ in intervals:
        for zs,ze,_ in intervals:
            x=(xs+xe-1)//2; z=(zs+ze-1)//2
            primary=rise_north(z,steps)
            corner=rise_west(x,steps) if shape.endswith("left") else rise_east(x,steps)
            height = primary if shape == "straight" else (max(primary,corner) if shape.startswith("inner") else min(primary,corner))
            lo,hi=(0,height) if half=="bottom" else (16-height,16)
            elements.append({"from":[xs,lo,zs],"to":[xe,hi,ze],"faces":faces()})
    return elements

def generate_stairs(lang):
    materials=[(m,"wood") for m in WOODS]+[(m,"concrete") for m in CONCRETES]+[(m,"terracotta") for m in TERRACOTTAS]
    for steps in STEPS:
        for shape in SHAPES:
            for half in HALVES:
                write(ASSETS/f"models/block/stepped_stairs/{steps}/{shape}_{half}.json",
                      {"textures":{"particle":"#material"},"elements":stair_elements(steps,shape,half)})
    for material,group in materials:
        for steps in STEPS:
            block_id=f"{material}_{steps}_step_stairs"
            variants={}
            for facing,y in (("north",0),("east",90),("south",180),("west",270)):
                for half in HALVES:
                    for shape in SHAPES:
                        model=f"fluidworks:block/stairs/{group}/{material}/{steps}/{shape}_{half}"
                        value={"model":model}
                        if y:value["y"]=y
                        variants[f"facing={facing},half={half},shape={shape}"]=value
            write(ASSETS/f"blockstates/{block_id}.json",{"variants":variants})
            for shape in SHAPES:
                for half in HALVES:
                    write(ASSETS/f"models/block/stairs/{group}/{material}/{steps}/{shape}_{half}.json",
                          {"parent":f"fluidworks:block/stepped_stairs/{steps}/{shape}_{half}",
                           "textures":{"material":texture_for(material),"particle":texture_for(material)}})
            item_model=f"fluidworks:block/stairs/{group}/{material}/{steps}/straight_bottom"
            write(ASSETS/f"items/{block_id}.json",{"model":{"type":"minecraft:model","model":item_model}})
            self_drop(block_id)
            ingredient = f"minecraft:{material}_planks" if material in WOODS else f"minecraft:{material}"
            write(DATA/f"fluidworks/recipe/{block_id}.json",{"type":"minecraft:crafting_shaped",
                "category":"building","pattern":["  M"," MM","MMM"],"key":{"M":ingredient},
                "result":{"id":f"fluidworks:{block_id}","count":4}})
            lang[f"block.fluidworks.{block_id}"]=f"{title(material)} {steps}-Step Stairs"

def button_elements(size, face, pressed):
    inset=(16-size)/2; thick=1 if pressed else 2
    if face=="floor": bounds=([inset,0,inset],[16-inset,thick,16-inset])
    elif face=="ceiling": bounds=([inset,16-thick,inset],[16-inset,16,16-inset])
    else: bounds=([inset,inset,16-thick],[16-inset,16-inset,16])
    return [{"from":bounds[0],"to":bounds[1],"faces":faces()}]

def generate_buttons(lang):
    materials=NORMAL_BUTTONS+CONCRETES+TERRACOTTAS
    for size in (12,8):
        for face in ("wall","floor","ceiling"):
            for powered in (False,True):
                state="pressed" if powered else "normal"
                write(ASSETS/f"models/block/square_button/{size}/{face}_{state}.json",
                      {"textures":{"particle":"#material"},"elements":button_elements(size,face,powered)})
    for material in materials:
        group=group_for(material)
        for size in (12,8):
            block_id=f"{material}_{size}x{size}_button"
            variants={}
            for face in ("wall","floor","ceiling"):
                for facing,y in (("north",0),("east",90),("south",180),("west",270)):
                    for powered in (False,True):
                        state="pressed" if powered else "normal"
                        model=f"fluidworks:block/buttons/{group}/{material}/{size}/{face}_{state}"
                        value={"model":model}
                        if y:value["y"]=y
                        variants[f"face={face},facing={facing},powered={str(powered).lower()}"]=value
            write(ASSETS/f"blockstates/{block_id}.json",{"variants":variants})
            for face in ("wall","floor","ceiling"):
                for state in ("normal","pressed"):
                    write(ASSETS/f"models/block/buttons/{group}/{material}/{size}/{face}_{state}.json",
                          {"parent":f"fluidworks:block/square_button/{size}/{face}_{state}",
                           "textures":{"material":texture_for(material),"particle":texture_for(material)}})
            item_model=f"fluidworks:block/buttons/{group}/{material}/{size}/floor_normal"
            write(ASSETS/f"items/{block_id}.json",{"model":{"type":"minecraft:model","model":item_model}})
            self_drop(block_id)
            ingredient=f"minecraft:{material}_planks" if material in WOODS else f"minecraft:{material}"
            write(DATA/f"fluidworks/recipe/{block_id}.json",{"type":"minecraft:crafting_shaped",
                "category":"redstone","pattern":["MM","MM"],"key":{"M":ingredient},
                "result":{"id":f"fluidworks:{block_id}","count":4 if size==8 else 2}})
            lang[f"block.fluidworks.{block_id}"]=f"{title(material)} {size}x{size} Button"

def lever_model(material, design, face, powered):
    tex={"base":f"minecraft:block/{material}_block" if material!="trimetal" else "minecraft:block/iron_block",
         "handle":"minecraft:block/copper_block" if material=="trimetal" else f"minecraft:block/{material}_block",
         "accent":"minecraft:block/gold_block","particle":"#base"}
    if face=="floor":
        base=([3,0,3],[13,3,13]); handle=([6,3,6 if not powered else 4],[10,13,10 if not powered else 8])
    elif face=="ceiling":
        base=([3,13,3],[13,16,13]); handle=([6,3,6 if powered else 8],[10,13,10 if powered else 12])
    else:
        base=([3,3,13],[13,13,16]); handle=([6,6 if powered else 8,3],[10,10 if powered else 12,13])
    elems=[{"from":base[0],"to":base[1],"faces":faces("#base")},
           {"from":handle[0],"to":handle[1],"faces":faces("#handle")}]
    if design in (1,3,5,7): elems.append({"from":[2,2,14] if face=="wall" else [2,1,2],"to":[5,5,16] if face=="wall" else [5,3,5],"faces":faces("#accent")})
    if design in (2,4,6,7): elems.append({"from":[11,11,14] if face=="wall" else [11,1,11],"to":[14,14,16] if face=="wall" else [14,3,14],"faces":faces("#accent")})
    return {"textures":tex,"elements":elems}

def generate_levers(lang):
    for lever_id,material,design in LEVER_SPECS:
        variants={}
        for face in ("wall","floor","ceiling"):
            for facing,y in (("north",0),("east",90),("south",180),("west",270)):
                for powered in (False,True):
                    state="on" if powered else "off"
                    model=f"fluidworks:block/levers/{material}/{lever_id}/{face}_{state}"
                    value={"model":model}
                    if y:value["y"]=y
                    variants[f"face={face},facing={facing},powered={str(powered).lower()}"]=value
        write(ASSETS/f"blockstates/{lever_id}.json",{"variants":variants})
        for face in ("wall","floor","ceiling"):
            for powered in (False,True):
                state="on" if powered else "off"
                write(ASSETS/f"models/block/levers/{material}/{lever_id}/{face}_{state}.json",
                      lever_model(material,design,face,powered))
        item_model=f"fluidworks:block/levers/{material}/{lever_id}/floor_off"
        write(ASSETS/f"items/{lever_id}.json",{"model":{"type":"minecraft:model","model":item_model}})
        self_drop(lever_id)
        write(DATA/f"fluidworks/recipe/{lever_id}.json",{"type":"minecraft:crafting_shaped",
              "category":"redstone","pattern":[" I ","CMC"," R "],"key":{"I":"minecraft:iron_ingot",
              "C":"minecraft:copper_ingot","M":"minecraft:gold_ingot","R":"minecraft:redstone"},
              "result":{"id":f"fluidworks:{lever_id}"}})
        lang[f"block.fluidworks.{lever_id}"]=title(lever_id)

def self_drop(block_id):
    write(DATA/f"fluidworks/loot_table/blocks/{block_id}.json",{"type":"minecraft:block","pools":[{
        "rolls":1,"entries":[{"type":"minecraft:item","name":f"fluidworks:{block_id}"}],
        "conditions":[{"condition":"minecraft:survives_explosion"}]}]})

def generate_machines_and_fluids(lang):
    make_metal_block_texture(ASSETS/"textures/block/rose_gold_block.png", FLUID_COLORS["rose_gold"])
    write(ASSETS/"blockstates/rose_gold_block.json",{"variants":{"":{"model":"fluidworks:block/rose_gold_block"}}})
    write(ASSETS/"models/block/rose_gold_block.json",{"parent":"minecraft:block/cube_all","textures":{"all":"fluidworks:block/rose_gold_block"}})
    write(ASSETS/"items/rose_gold_block.json",{"model":{"type":"minecraft:model","model":"fluidworks:block/rose_gold_block"}})
    self_drop("rose_gold_block")
    write(DATA/"c/tags/block/storage_blocks/rose_gold.json",{"replace":False,"values":["fluidworks:rose_gold_block"]})
    write(DATA/"c/tags/item/storage_blocks/rose_gold.json",{"replace":False,"values":["fluidworks:rose_gold_block"]})
    cauldron_elems=[{"from":[0,0,0],"to":[16,4,16],"faces":faces("#metal")},
        {"from":[0,4,0],"to":[3,16,16],"faces":faces("#metal")},
        {"from":[13,4,0],"to":[16,16,16],"faces":faces("#metal")},
        {"from":[3,4,0],"to":[13,16,3],"faces":faces("#metal")},
        {"from":[3,4,13],"to":[13,16,16],"faces":faces("#metal")}]
    write(ASSETS/"models/block/cooling_cauldron.json",{"textures":{"metal":"minecraft:block/iron_block","particle":"#metal"},"elements":cauldron_elems})
    write(ASSETS/"blockstates/cooling_cauldron.json",{"variants":{"":{"model":"fluidworks:block/cooling_cauldron"}}})
    write(ASSETS/"items/cooling_cauldron.json",{"model":{"type":"minecraft:model","model":"fluidworks:block/cooling_cauldron"}})
    self_drop("cooling_cauldron")
    write(DATA/"fluidworks/recipe/cooling_cauldron.json",{"type":"minecraft:crafting_shaped","category":"misc",
        "pattern":["I I","ICI","III"],"key":{"I":"minecraft:iron_ingot","C":"minecraft:cauldron"},
        "result":{"id":"fluidworks:cooling_cauldron"}})
    lang["block.fluidworks.rose_gold_block"]="Rose Gold Block"
    lang["block.fluidworks.cooling_cauldron"]="Cooling Cauldron"
    for name,color in FLUID_COLORS.items():
        block_id=f"liquid_{name}_block"; bucket_id=f"liquid_{name}_bucket"
        write(ASSETS/f"blockstates/{block_id}.json",{"variants":{"":{"model":"minecraft:block/water"}}})
        write(ASSETS/f"items/{bucket_id}.json",{"model":{"type":"minecraft:model","model":f"fluidworks:item/{bucket_id}"}})
        write(ASSETS/f"models/item/{bucket_id}.json",{"parent":"minecraft:item/generated","textures":{"layer0":f"fluidworks:item/{bucket_id}"}})
        make_bucket_texture(ASSETS/f"textures/item/{bucket_id}.png",color)
        lang[f"item.fluidworks.{bucket_id}"]=f"Liquid {title(name)} Bucket"
        lang[f"fluid.fluidworks.liquid_{name}"]=f"Liquid {title(name)}"

def make_bucket_texture(path,color):
    path.parent.mkdir(parents=True,exist_ok=True)
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    dark=tuple(max(0,c-70) for c in color)+(255,); light=tuple(min(255,c+45) for c in color)+(255,)
    d.polygon([(3,3),(12,3),(11,14),(4,14)],fill=(145,145,150,255))
    d.rectangle((4,5,11,12),fill=color+(255,)); d.line((4,5,11,5),fill=light,width=1)
    d.line((4,13,11,13),fill=dark,width=1); d.line((2,3,4,1,11,1,13,3),fill=(205,205,210,255),width=1)
    im.save(path)

def make_metal_block_texture(path,color):
    path.parent.mkdir(parents=True,exist_ok=True)
    im=Image.new("RGBA",(16,16),color+(255,)); d=ImageDraw.Draw(im)
    light=tuple(min(255,c+32) for c in color)+(255,)
    dark=tuple(max(0,c-40) for c in color)+(255,)
    d.line((0,0,15,0,15,15),fill=light,width=1)
    d.line((0,15,0,0),fill=dark,width=1)
    d.rectangle((3,3,12,12),outline=light)
    d.line((4,12,12,4),fill=dark,width=1)
    im.save(path)

def update_pistons():
    for width in (12,14):
        inset=(16-width)/2
        model={"textures":{"iron":"fluidworks:block/fluid_hardware_iron","particle":"#iron"},"elements":[
            {"from":[0,0,6],"to":[16,16,16],"faces":faces("#iron")},
            {"from":[inset,inset,1],"to":[16-inset,16-inset,6],"faces":faces("#iron")},
            {"from":[0,0,0],"to":[16,16,1],"faces":faces("#iron")} ]}
        write(ASSETS/f"models/block/iron_piston_{width}_extended.json",model)

def main():
    lang_path=ASSETS/"lang/en_us.json"
    lang=json.loads(lang_path.read_text())
    lang["itemGroup.fluidworks.custom_stairs"]="Fluid Works: Custom Stairs"
    generate_stairs(lang); generate_buttons(lang); generate_levers(lang)
    generate_machines_and_fluids(lang); update_pistons()
    write(lang_path,dict(sorted(lang.items())))

if __name__ == "__main__": main()
