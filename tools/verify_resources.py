#!/usr/bin/env python3
"""Resolve production assets and check the rendering regressions against geometric invariants."""
import hashlib
import io
import json
from pathlib import Path
import zipfile
from PIL import Image, ImageStat
from generate_resources import WOODS, KINDS, FINISHES, FAUCET_FINISHES, FAUCET_TEXTURES, ROOT, RES

client = zipfile.ZipFile(ROOT / 'research/client.jar')
backport = zipfile.ZipFile(ROOT / 'vendor/VanillaBackport-forge-1.20.1-1.1.7.10.jar')
available = set(client.namelist()) | set(backport.namelist()) | {p.relative_to(RES).as_posix() for p in RES.rglob('*.png')}
DIRECTIONS = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
              'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}


def load(name):
    return json.loads((RES / f'assets/cozykitchens/models/block/{name}.json').read_text())


def intersects(a, b):
    return all(min(a['to'][i], b['to'][i]) > max(a['from'][i], b['from'][i]) + 1e-7 for i in range(3))


def face_area(e, side):
    axis, _ = DIRECTIONS[side]
    axes = [i for i in range(3) if i != axis]
    return (e['to'][axes[0]] - e['from'][axes[0]]) * (e['to'][axes[1]] - e['from'][axes[1]])


def check_surfaces(m, label):
    # Two outward faces in the same plane must never cover the same positive area.
    # Shared edges and opposing faces where solid pieces meet are allowed.
    planes = {}
    for e in m.get('elements', []):
        for side, f in e['faces'].items():
            assert len(f.get('uv', [])) == 4 and all(0 <= v <= 16 for v in f['uv']), (label, e.get('name'), side, f)
            axis, sign = DIRECTIONS[side]
            plane = e['to' if sign > 0 else 'from'][axis]
            axes = [i for i in range(3) if i != axis]
            for other in planes.get((side, plane), []):
                assert not all(min(e['to'][a], other['to'][a]) > max(e['from'][a], other['from'][a]) + 1e-7
                               for a in axes), ('Coplanar overlapping surfaces', label, side, e, other)
            planes.setdefault((side, plane), []).append(e)


checked = 0
for p in (RES / 'assets/cozykitchens/models').rglob('*.json'):
    m = json.loads(p.read_text())
    assert m['parent'] == 'minecraft:block/block' or m['parent'].startswith('cozykitchens:block/'), p
    for t in m.get('textures', {}).values():
        if t.startswith('#'): continue
        namespace, path = t.split(':')
        assert f'assets/{namespace}/textures/{path}.png' in available, (p, t)
    for e in m.get('elements', []):
        assert all(a < b for a, b in zip(e['from'], e['to'])), p
        for face in e['faces'].values():
            assert face['texture'].lstrip('#') in m['textures'], (p, face)
    check_surfaces(m, p.name)
    parent = m.get('parent', '')
    if parent.startswith('cozykitchens:'):
        assert (RES / 'assets/cozykitchens/models' / (parent.split(':')[1] + '.json')).exists(), p
    checked += 1

recipes = list((RES / 'data/cozykitchens/recipes').glob('*.json'))
assert len(recipes) == 84
for p in recipes:
    r = json.loads(p.read_text()); name = p.stem
    assert r['result']['item'] == 'cozykitchens:' + name
    assert (RES / f'assets/cozykitchens/blockstates/{name}.json').exists()
    loot_prefix = 'resourcepacks/pale_oak/' if name.startswith('pale_oak_') else ''
    assert (RES / f'{loot_prefix}data/cozykitchens/loot_tables/blocks/{name}.json').exists()
    assert (RES / f'assets/cozykitchens/models/item/{name}.json').exists()
    if name.startswith('pale_oak_'):
        assert r['conditions'] == [{'type': 'forge:mod_loaded', 'modid': 'vanillabackport'}]
        assert not (RES / f'data/cozykitchens/loot_tables/blocks/{name}.json').exists()
    state = json.loads((RES / f'assets/cozykitchens/blockstates/{name}.json').read_text())
    for variant in state['variants'].values():
        assert (RES / 'assets/cozykitchens/models' / (variant['model'].split(':')[1] + '.json')).exists()

fronts = []
for wood in WOODS:
    front = RES / f'assets/cozykitchens/textures/block/cabinet/{wood}_cabinet_front.png'
    fronts.append(hashlib.sha256(front.read_bytes()).hexdigest())
    sink = load(wood + '_sink')
    doors = [e for e in sink['elements'] if e.get('name', '').startswith('cabinet_door_')]
    basin = [e for e in sink['elements'] if e.get('name', '').startswith('basin_')]
    assert len(basin) == 5 and len(doors) == 2
    assert all(not intersects(door, piece) for door in doors for piece in basin), wood
    assert min(p['from'][2] for p in basin) - max(d['to'][2] for d in doors) >= 1, wood
    assert sink['textures']['top'] == 'minecraft:block/quartz_block_bottom'
    faucet = [e for e in sink['elements'] if e.get('name', '').startswith('faucet_')]
    assert len(faucet) == 5
    assert all(f['texture'] == '#faucet' for e in faucet for f in e['faces'].values())
    assert sink['textures']['faucet'] == FAUCET_TEXTURES[FAUCET_FINISHES[wood]]
    # Every component touches the assembly over a real face, not just an edge/point.
    connected = {0}
    for _ in faucet:
        for i, e in enumerate(faucet):
            for j in list(connected):
                other = faucet[j]
                overlap = [min(e['to'][a], other['to'][a])-max(e['from'][a], other['from'][a]) for a in range(3)]
                if all(v >= -1e-7 for v in overlap) and sum(v > 1e-7 for v in overlap) >= 2: connected.add(i)
    assert len(connected) == len(faucet), ('Disconnected faucet', wood)
    for kind in KINDS:
        cabinet = load(wood + '_' + kind)
        assert cabinet['textures']['wood'] == f'cozykitchens:block/cabinet/{wood}_cabinet_side'
        assert cabinet['textures']['cabinet_top'] == f'cozykitchens:block/cabinet/{wood}_cabinet_top'
        assert any(f['texture'] == '#cabinet_top' for e in cabinet['elements']
                   for side, f in e['faces'].items() if side in ['up', 'down']), (wood, kind)
    for kind in ['corner', 'hanging_corner']:
        corner = load(wood + '_' + kind)
        low = 2 if kind == 'hanging_corner' else 0
        for side, (axis, sign) in DIRECTIONS.items():
            plane = 16 if sign > 0 else (low if axis == 1 else 0)
            area = sum(face_area(e, side) for e in corner['elements'] if side in e['faces'] and
                       e['to' if sign > 0 else 'from'][axis] == plane)
            assert abs(area - (256 if axis == 1 else 16 * (16-low))) < 1e-7, ('Open corner side', wood, kind, side, area)
    for kind in ['counter', 'cabinet']:
        for half, x0, x1, uv in [('left', .75, 7.96875, [0,0,8,16]), ('right', 8.03125, 15.25, [8,0,16,16])]:
            leaf = load(f'{wood}_{kind}_door_{half}')['elements']
            assert len(leaf) == 1 and leaf[0]['from'][0] == x0 and leaf[0]['to'][0] == x1
            assert leaf[0]['faces']['north'] == {'uv': uv, 'texture': '#front'}
    water = load(wood + '_sink_liquid')['elements'][0]
    assert all(f['tintindex'] == 0 for f in water['faces'].values())
    for fill in [0, .1, .25, .5, .75, 1]:
        scaled = dict(water, **{'from': list(water['from']), 'to': list(water['to'])})
        scaled['from'][1] = 8 + (water['from'][1] - 8) * fill
        scaled['to'][1] = 8 + (water['to'][1] - 8) * fill
        assert scaled['from'][1] >= 8 and scaled['to'][1] < 14, (wood, fill)
        assert scaled['from'][0] > 3.5 and scaled['from'][2] > 3.5
        assert scaled['to'][0] < 12.5 and scaled['to'][2] < 12.5
        assert all(not intersects(door, scaled) for door in doors)
        assert all(not intersects(scaled, piece) for piece in basin)
assert len(set(fronts)) == 12

for finish, terracotta in {**FINISHES, 'dark': 'black_terracotta'}.items():
    image = Image.open(RES / f'assets/cozykitchens/textures/block/ceramic/{finish}.png').convert('RGB')
    assert image.size == (16, 16) and len(image.getcolors()) == 1, ('Nonuniform ceramic', finish)
    original = Image.open(io.BytesIO(client.read(f'assets/minecraft/textures/block/{terracotta}.png'))).convert('RGB')
    assert image.getpixel((0,0)) == tuple(round(v) for v in ImageStat.Stat(original).mean), ('Palette changed', finish)
    if finish not in FINISHES: continue
    for p in (RES / 'assets/cozykitchens/models/block').glob(f'{finish}_*.json'):
        m = json.loads(p.read_text())
        assert m['textures']['enamel'] == f'cozykitchens:block/ceramic/{finish}'
        assert not any('terracotta' in t for t in m['textures'].values()), p
    for suffix in ['', '_large_lower', '_large_upper']:
        fridge = load(finish + '_fridge' + suffix)
        for e in fridge['elements']:
            if e['from'][1] == 0 or e['to'][1] == 16:
                assert all(f['texture'] == '#enamel' for f in e['faces'].values()), ('White fridge seam', finish, suffix)

# Preserve every accepted cabinet texture, including the custom pale oak panels.
approved = json.loads((ROOT / 'art/approved-cabinet-textures.json').read_text())['sha256']
assert len(approved) == 36
for name, expected in approved.items():
    assert hashlib.sha256((RES / name).read_bytes()).hexdigest() == expected, ('Accepted cabinet artwork changed', name)

assert checked == 361, checked
print(f'PASS: {checked} model references/UV bounds and coplanar surfaces; 84 recipes, loot, blockstates and items; pale oak gating.')
print('PASS: 24 fully enclosed corners; 48 separately textured door leaves; five connected, opaque faucet components per sink.')
print('PASS: six gold/six black faucets, original wood artwork and quartz worktops; water clearance at six fill levels.')
print('PASS: four original smooth ceramic finishes match the previous palette; all fridge caps and stacking seams use their finish.')
