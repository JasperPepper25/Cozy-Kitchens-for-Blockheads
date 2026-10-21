#!/usr/bin/env python3
"""Native cabinet models using credited Farmer's Delight art and an original pale oak design."""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/cozykitchens"
DATA = RES / "data/cozykitchens"
WOODS = "oak spruce birch jungle acacia dark_oak mangrove cherry bamboo crimson warped pale_oak".split()
KINDS = "counter cabinet cooking_table sink corner hanging_corner".split()
FINISHES = {"ivory": "white_terracotta", "sage": "green_terracotta", "blush": "pink_terracotta", "charcoal": "gray_terracotta"}
FAUCET_FINISHES = {
    **dict.fromkeys('spruce jungle dark_oak mangrove cherry crimson'.split(), 'gold'),
    **dict.fromkeys('oak birch acacia bamboo warped pale_oak'.split(), 'black'),
}
FAUCET_TEXTURES = {'gold': 'minecraft:block/gold_block', 'black': 'minecraft:block/black_concrete'}
assert set(FAUCET_FINISHES) == set(WOODS)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def cube(a, b, texture="wood", uv=None):
    # Minecraft samples the stitched atlas, not a repeating standalone texture.
    # Implicit UVs outside 0..16 can read neighbouring sprites (notably on faucets).
    x0, y0, z0 = a; x1, y1, z1 = b
    defaults = {
        'north': [16-x1, 16-y1, 16-x0, 16-y0], 'south': [x0, 16-y1, x1, 16-y0],
        'east': [16-z1, 16-y1, 16-z0, 16-y0], 'west': [z0, 16-y1, z1, 16-y0],
        'up': [x0, z0, x1, z1], 'down': [x0, 16-z1, x1, 16-z0],
    }
    faces = {}
    for side in ["north", "south", "east", "west", "up", "down"]:
        coords = list(uv if uv is not None else defaults[side])
        if any(v < 0 or v > 16 for v in coords):
            coords = [0, 0, abs(coords[2]-coords[0]), abs(coords[3]-coords[1])]
        face = {"texture": "#" + texture, "uv": coords}
        faces[side] = face
    return {"from": list(a), "to": list(b), "faces": faces}


def remove_covered_faces(elements):
    """Drop whole hidden faces, including the shared ends of joined faucet pieces."""
    result = copy.deepcopy(elements)
    directions = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
                  'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}
    for index, part in enumerate(result):
        for side in list(part['faces']):
            axis, sign = directions[side]
            plane = part['to' if sign > 0 else 'from'][axis]
            others = [e for i, e in enumerate(result) if i != index and
                      e['from'][axis] - 1e-7 < plane + sign * 1e-5 < e['to'][axis] + 1e-7]
            axes = [i for i in range(3) if i != axis]
            # Boundaries partition the face into rectangles with uniform coverage.
            cuts = [sorted({part['from'][a], part['to'][a]} |
                           {v for e in others for v in [e['from'][a], e['to'][a]]
                            if part['from'][a] < v < part['to'][a]}) for a in axes]
            covered = bool(others) and all(any(all(e['from'][a] <= p <= e['to'][a]
                for a, p in zip(axes, ((u0+u1)/2, (v0+v1)/2))) for e in others)
                for u0, u1 in zip(cuts[0], cuts[0][1:])
                for v0, v1 in zip(cuts[1], cuts[1][1:]))
            if covered: del part['faces'][side]
    return result


def model(elements, wood="minecraft:block/oak_planks", enamel="cozykitchens:block/ceramic/ivory"):
    return {"parent": "minecraft:block/block", "render_type": "minecraft:solid",
            "textures": {"wood": wood, "panel": wood, "top": "minecraft:block/quartz_block_bottom",
                         "metal": "minecraft:block/gold_block", "enamel": enamel,
                         "dark": "cozykitchens:block/ceramic/dark", "glass": "minecraft:block/gray_stained_glass",
                         "fire": "minecraft:block/magma", "water": "minecraft:block/water_still",
                         "particle": wood}, "elements": remove_covered_faces(elements)}


def shell(cabinet=False):
    low, floor, high, front = (2, 2.75, 15.25, 4) if cabinet else (0, 1, 14, 1.75)
    parts = [cube((0, floor, front), (1, high, 16)), cube((15, floor, front), (16, high, 16)),
             cube((1, floor, 15), (15, high, 16)),
             cube((0, low, 3 if cabinet else front), (16, floor, 16)),
             cube((1, 6.5, max(front, 3.5)), (15, 7, 15))]
    if cabinet:
        parts += [cube((0, 15.25, 3), (16, 16, 16))]
    else:
        parts += [cube((0, 14, 0), (16, 16, 16), "top")]
    return parts


def door(cabinet=False, flipped=False, material="wood", half=None):
    lo, hi, z = (2.75, 15.25, 2.75) if cabinet else (1, 14, .5)
    parts = []
    for name, x0, x1, u0, u1 in [('left', .75, 7.96875, 0, 8), ('right', 8.03125, 15.25, 8, 16)]:
        if half is not None and name != half: continue
        panel = cube((x0, lo, z), (x1, hi, z + 1), material)
        panel['name'] = 'cabinet_door_' + name
        panel['faces']['north'] = {'uv': [u0, 0, u1, 16], 'texture': '#front'}
        parts.append(panel)
    return mirror(parts) if flipped else parts


def mirror(elements):
    result = copy.deepcopy(elements)
    for element in result:
        x1, x2 = element["from"][0], element["to"][0]
        element["from"][0], element["to"][0] = 16 - x2, 16 - x1
    return result


def sink():
    # A genuine open basin: no counter slab or cabinet roof crosses the water volume.
    parts = shell()
    parts = [e for e in parts if e["from"] != [0, 14, 0]]
    parts += [cube((0, 14, 0), (3.5, 16, 16), "top"), cube((12.5, 14, 0), (16, 16, 16), "top"),
              cube((3.5, 14, 0), (12.5, 16, 3.5), "top"), cube((3.5, 14, 12.5), (12.5, 16, 16), "top"),
              ]
    basin = [cube((2.5, 7.5, 2.5), (13.5, 8, 13.5), "top"),
             cube((2.5, 8, 2.5), (3.5, 14, 13.5), "top"),
             cube((12.5, 8, 2.5), (13.5, 14, 13.5), "top"),
             cube((3.5, 8, 2.5), (12.5, 14, 3.5), "top"),
             cube((3.5, 8, 12.5), (12.5, 14, 13.5), "top")]
    for i, piece in enumerate(basin): piece['name'] = f'basin_{i}'
    parts += basin
    # A mounted collar, solid riser, continuous arm, downspout and side lever.
    faucet = [cube((7.125, 16, 13.625), (8.875, 16.375, 15.375), 'faucet'),
              cube((7.5, 16.375, 14), (8.5, 20.25, 15), 'faucet'),
              cube((7.5, 20.25, 10), (8.5, 21.25, 15), 'faucet'),
              cube((7.5, 19.25, 10), (8.5, 20.25, 11), 'faucet'),
              cube((8.5, 17, 14.125), (9.75, 17.625, 14.875), 'faucet')]
    for i, piece in enumerate(faucet): piece['name'] = f'faucet_{i}'
    parts += faucet
    parts += door()
    return parts


def sink_liquid(wood):
    water = cube((3.55, 8, 3.55), (12.45, 13.75, 12.45), 'water')
    water['name'] = 'basin_water'
    for face in water['faces'].values(): face['tintindex'] = 0
    result = cabinet_model([water], wood)
    result['render_type'] = 'minecraft:translucent'
    return result


def cabinet_model(parts, wood):
    result = model(parts, 'minecraft:block/' + wood + '_planks')
    base = 'cozykitchens:block/cabinet/' + wood + '_cabinet_'
    result['textures']['front'] = base + 'front'
    result['textures']['wood'] = base + 'side'
    result['textures']['particle'] = base + 'side'
    result['textures']['cabinet_top'] = base + 'top'
    if any(p.get('name', '').startswith('faucet_') for p in parts):
        result['textures']['faucet'] = FAUCET_TEXTURES[FAUCET_FINISHES[wood]]
    for piece in result['elements']:
        for side in ['up', 'down']:
            if side in piece['faces'] and piece['faces'][side]['texture'] == '#wood':
                piece['faces'][side]['texture'] = '#cabinet_top'
    return result


def table():
    parts = shell() + door()
    parts += [cube((3, 16, 3), (13, 16.35, 12), "board")]
    return parts


def corner(hanging=False):
    low = 2 if hanging else 0
    # Six enclosed faces. Walls stop beneath the cap instead of intersecting it.
    parts = [cube((0, low, 0), (16, low + 1, 16)),
             cube((0, low + 1, 0), (1, 15, 16)), cube((15, low + 1, 0), (16, 15, 16)),
             cube((1, low + 1, 0), (15, 15, 1)), cube((1, low + 1, 15), (15, 15, 16))]
    parts += [cube((0, 15, 0), (16, 16, 16), "wood" if hanging else "top")]
    return parts


def states(base, properties=None):
    variants = {}
    for facing, angle in {"north": 0, "east": 90, "south": 180, "west": 270}.items():
        for values, suffix in properties or [("", "")]:
            key = "facing=" + facing + ("," + values if values else "")
            variants[key] = {"model": "cozykitchens:block/" + base + suffix, "y": angle}
    return {"variants": variants}


LANG = {"itemGroup.cozykitchens": "Cozy Kitchens for Blockheads"}
PREVIEW = []


def entry(finish, kind, body, variants=None, item_body=None):
    name = finish + "_" + kind
    write(ASSETS / f"models/block/{name}.json", body)
    write(ASSETS / f"blockstates/{name}.json", states(name, variants))
    if item_body is not None:
        write(ASSETS / f"models/block/{name}_inventory.json", item_body)
    write(ASSETS / f"models/item/{name}.json", {"parent": f"cozykitchens:block/{name}" + ("_inventory" if item_body is not None else "")})
    loot_path = DATA / f"loot_tables/blocks/{name}.json"
    if finish == 'pale_oak':
        # Loot entry deserialization resolves item IDs even when a pool condition would be false.
        # Load these six tables only with Backport via an automatic built-in data pack.
        loot_path.unlink(missing_ok=True)
        loot_path = RES / f'resourcepacks/pale_oak/data/cozykitchens/loot_tables/blocks/{name}.json'
    write(loot_path, {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "cozykitchens:" + name}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(DATA / f"recipes/{name}.json", {"type": "minecraft:crafting_shapeless", "group": "cozykitchens_" + kind,
        **({"conditions": [{"type": "forge:mod_loaded", "modid": "vanillabackport"}]} if finish == "pale_oak" else {}),
        "ingredients": [{"tag": "cozykitchens:reskinnable_" + kind},
                        {"item": "minecraft:" + (finish + "_planks" if kind in KINDS else {"ivory": "white_dye", "sage": "green_dye", "blush": "pink_dye", "charcoal": "black_dye"}[finish])}],
        "result": {"item": "cozykitchens:" + name}})
    LANG["block.cozykitchens." + name] = finish.replace("_", " ").title() + " " + {"cabinet": "Wall Cabinet", "counter": "Kitchen Counter", "corner": "Kitchen Corner", "hanging_corner": "Hanging Corner"}.get(kind, kind.replace("_", " ").title())
    PREVIEW.append({"name": LANG["block.cozykitchens." + name], "id": name, "model": item_body or body})


def generate():
    write(RES / 'resourcepacks/pale_oak/pack.mcmeta', {'pack': {'pack_format': 15, 'description': 'Cozy Kitchens pale oak support'}})
    for wood in WOODS:
        for kind in KINDS:
            parts = {"counter": shell, "cabinet": lambda: shell(True), "cooking_table": table,
                     "sink": sink, "corner": corner, "hanging_corner": lambda: corner(True)}[kind]()
            body = cabinet_model(parts, wood)
            body['textures']['board'] = 'minecraft:block/' + wood + '_planks'
            inv = None
            if kind in ["counter", "cabinet"]:
                inv = cabinet_model(copy.deepcopy(parts) + door(kind == "cabinet"), wood)
                for flipped in [False, True]:
                    path = wood + "_" + kind + "_door" + ("_flipped" if flipped else "")
                    write(ASSETS / f"models/block/{path}.json", cabinet_model(door(kind == "cabinet", flipped), wood))
                for half in ['left', 'right']:
                    write(ASSETS / f'models/block/{wood}_{kind}_door_{half}.json',
                          cabinet_model(door(kind == 'cabinet', half=half), wood))
            entry(wood, kind, body, item_body=inv)
        write(ASSETS / f'models/block/{wood}_sink_liquid.json', sink_liquid(wood))

    for finish, tex in FINISHES.items():
        enamel = "cozykitchens:block/ceramic/" + finish
        def appliance(parts):
            m = model(parts, enamel, enamel)
            m["textures"]["panel"] = enamel
            return m
        oven_body = [cube((0, 2, 2), (1, 14, 16), "enamel"), cube((15, 2, 2), (16, 14, 16), "enamel"),
                     cube((1, 2, 15), (15, 14, 16), "enamel"), cube((0, 1, 2), (16, 2, 16), "enamel"),
                     cube((0, 14, 0), (16, 16, 16), "enamel"), cube((1, 12, 0), (15, 14, 2), "enamel")]
        for x in [3.5, 7.5, 11.5]: oven_body.append(cube((x, 12.5, -.3), (x + 1, 13.5, .2), "metal"))
        for x in [2, 9]:
            for z in [2, 9]:
                oven_body.append(cube((x, 16, z), (x + 5, 16.15, z + 5), "dark"))
                oven_body.append(cube((x + 1, 16.15, z + 1), (x + 4, 16.3, z + 4), "metal"))
        def oven_door(active=False):
            return [cube((1, 2, .8), (15, 12, 2), "enamel"), cube((3, 3.5, .55), (13, 9, .8), "fire" if active else "dark"),
                    cube((3, 10.2, -.2), (13, 11, .55), "metal")]
        entry(finish, "oven", appliance(oven_body), [("active=false", ""), ("active=true", "_active")], appliance(oven_body + oven_door()))
        write(ASSETS / f"models/block/{finish}_oven_active.json", appliance(oven_body))
        for part, mesh in [("oven_door", oven_door()), ("oven_door_active", oven_door(True)), ("oven_door_handle", [])]:
            write(ASSETS / f"models/block/{finish}_{part}.json", appliance(mesh))

        fridge_body = [cube((0, 1, 1.5), (1, 15, 16), "enamel"), cube((15, 1, 1.5), (16, 15, 16), "enamel"),
                       cube((1, 1, 15), (15, 15, 16), "enamel"), cube((0, 0, 1.5), (16, 1, 16), "enamel"),
                       cube((0, 15, 1.5), (16, 16, 16), "enamel"), cube((1, 8, 2), (15, 8.5, 15), "top")]
        def fridge_door(flipped=False, upper=False):
            p = [cube((.5, .5, .75), (15.5, 15.5, 2), "enamel"),
                 cube((1.3, 1.3, .5), (14.7, 14.7, .75), "enamel"),
                 cube((2, 7 if upper else 6, -.2), (2.8, 13 if upper else 12, .5), "metal"),
                 cube((10, 12, .3), (13.5, 13, .5), "metal")]
            return mirror(p) if flipped else p
        entry(finish, "fridge", appliance(fridge_body), [("model=small", ""), ("model=large_lower", "_large_lower"), ("model=large_upper", "_large_upper")], appliance(fridge_body + fridge_door()))
        for part in ["large_lower", "large_upper"]:
            write(ASSETS / f"models/block/{finish}_fridge_{part}.json", appliance(fridge_body))
        for part in ["door", "large_door_lower", "large_door_upper"]:
            for flipped in [False, True]:
                path = finish + "_fridge_" + part + ("_flipped" if flipped else "")
                write(ASSETS / f"models/block/{path}.json", appliance(fridge_door(flipped, part.endswith("upper"))))

        toaster = [cube((4.5, .5, 4.5), (11.5, 5.5, 11.5), "enamel"),
                   cube((4.2, 0, 4.2), (11.8, .5, 11.8), "metal"), cube((5, 5.5, 5), (11, 6, 11), "metal"),
                   cube((5.5, 6, 6), (10.5, 6.1, 7), "dark"), cube((5.5, 6, 9), (10.5, 6.1, 10), "dark"),
                   cube((11.5, 2, 7), (12.4, 3, 8), "metal")]
        entry(finish, "toaster", appliance(toaster), [("active=false", ""), ("active=true", "_active")])
        active = copy.deepcopy(toaster)
        active[-1] = cube((11.5, .7, 7), (12.4, 1.7, 8), "metal")
        write(ASSETS / f"models/block/{finish}_toaster_active.json", appliance(active))

    for kind in KINDS + ["oven", "fridge", "toaster"]:
        values = ["cookingforblockheads:" + kind]
        for style in (WOODS if kind in KINDS else FINISHES):
            v = {"id": "cozykitchens:" + style + "_" + kind, "required": style != "pale_oak"}
            values.append(v)
        write(DATA / f"tags/items/reskinnable_{kind}.json", {"replace": False, "values": values})
    wood_blocks = ["cozykitchens:" + wood + "_" + kind for wood in WOODS if wood != "pale_oak" for kind in KINDS]
    pale_blocks = [{"id": "cozykitchens:pale_oak_" + kind, "required": False} for kind in KINDS]
    write(RES / "data/minecraft/tags/blocks/mineable/axe.json", {"replace": False, "values": wood_blocks + pale_blocks})
    write(RES / "data/minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": ["cozykitchens:" + finish + "_" + kind for finish in FINISHES for kind in ["oven", "fridge", "toaster"]]})
    write(ASSETS / "lang/en_us.json", LANG)
    write(ASSETS / 'models/block/empty.json', model([]))
    write(ROOT / "research/preview-models.json", PREVIEW)
    print(f"Generated {len(PREVIEW)} block variants with models, recipes, loot, tags and names.")


if __name__ == "__main__":
    generate()
