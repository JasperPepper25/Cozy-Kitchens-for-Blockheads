#!/usr/bin/env python3
"""Resolve real model/texture references and check production recipes, loot and door assets."""
import json
import hashlib
from pathlib import Path
import zipfile
from generate_resources import WOODS, KINDS, FINISHES, FAUCET_FINISHES, FAUCET_TEXTURES, ROOT, RES

client = zipfile.ZipFile(ROOT / 'research/client.jar')
backport = zipfile.ZipFile(ROOT / 'vendor/VanillaBackport-forge-1.20.1-1.1.7.10.jar')
available = set(client.namelist()) | set(backport.namelist()) | {p.relative_to(RES).as_posix() for p in RES.rglob('*.png')}
checked = 0
for p in (RES / 'assets/cozykitchens/models').rglob('*.json'):
    m = json.loads(p.read_text())
    assert m['parent'] == 'minecraft:block/block' or m['parent'].startswith('cozykitchens:block/'), p
    for t in m.get('textures', {}).values():
        if t.startswith('#'): continue
        namespace, path = t.split(':')
        assert f'assets/{namespace}/textures/{path}.png' in available, (p, t)
    for e in m.get('elements', []):
        assert all(a <= b for a, b in zip(e['from'], e['to'])), p
        for face in e['faces'].values():
            assert face['texture'].lstrip('#') in m['textures'], (p, face)
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
        assert not (RES / f'data/cozykitchens/loot_tables/blocks/{name}.json').exists(), 'Pale oak loot escaped its optional data pack'
    state = json.loads((RES / f'assets/cozykitchens/blockstates/{name}.json').read_text())
    for variant in state['variants'].values():
        assert (RES / 'assets/cozykitchens/models' / (variant['model'].split(':')[1] + '.json')).exists()

for wood in WOODS:
    for door in ['counter_door', 'cabinet_door']:
        for suffix in ['', '_flipped']:
            assert (RES / f'assets/cozykitchens/models/block/{wood}_{door}{suffix}.json').exists()

def load(name):
    return json.loads((RES / f'assets/cozykitchens/models/block/{name}.json').read_text())

def intersects(a, b):
    return all(min(a['to'][i], b['to'][i]) > max(a['from'][i], b['from'][i]) + 1e-7 for i in range(3))

fronts = []
for wood in WOODS:
    front = RES / f'assets/cozykitchens/textures/block/cabinet/{wood}_cabinet_front.png'
    fronts.append(hashlib.sha256(front.read_bytes()).hexdigest())
    sink = load(wood + '_sink')
    door = next(e for e in sink['elements'] if e.get('name') == 'cabinet_door')
    basin = [e for e in sink['elements'] if e.get('name', '').startswith('basin_')]
    assert len(basin) == 5
    assert all(not intersects(door, piece) for piece in basin), wood
    assert min(p['from'][2] for p in basin) - door['to'][2] >= 1, wood
    assert sink['textures']['top'] == 'minecraft:block/quartz_block_bottom'
    assert door['faces']['north']['texture'] == '#front'
    faucet = [e for e in sink['elements'] if e.get('name', '').startswith('faucet_')]
    assert len(faucet) == 3
    assert all(f['texture'] == '#faucet' for e in faucet for f in e['faces'].values())
    assert sink['textures']['faucet'] == FAUCET_TEXTURES[FAUCET_FINISHES[wood]]
    for kind in KINDS:
        cabinet = load(wood + '_' + kind)
        assert cabinet['textures']['wood'] == f'cozykitchens:block/cabinet/{wood}_cabinet_side'
        assert cabinet['textures']['cabinet_top'] == f'cozykitchens:block/cabinet/{wood}_cabinet_top'
        assert any(f['texture'] == '#cabinet_top' for e in cabinet['elements']
                   for side, f in e['faces'].items() if side in ['up', 'down']), (wood, kind)
    water = load(wood + '_sink_liquid')['elements'][0]
    # SinkRenderer anchors at y=8 and scales the height by the native fill fraction.
    for fill in [0, .1, .25, .5, .75, 1]:
        scaled = dict(water, **{'from': list(water['from']), 'to': list(water['to'])})
        scaled['from'][1] = 8 + (water['from'][1] - 8) * fill
        scaled['to'][1] = 8 + (water['to'][1] - 8) * fill
        assert scaled['from'][1] >= 8 and scaled['to'][1] < 14, (wood, fill)
        assert scaled['from'][0] > 3.5 and scaled['from'][2] > 3.5
        assert scaled['to'][0] < 12.5 and scaled['to'][2] < 12.5
        assert not intersects(door, scaled)
        assert all(not intersects(scaled, piece) for piece in basin)
assert len(set(fronts)) == 12, 'Wood fronts must each be distinct'

# The approved appliance assets must remain exactly as released in 0.1.0.
baseline = ROOT / 'research/baseline.jar'
if baseline.exists():
    with zipfile.ZipFile(baseline) as z:
        preserved = [n for n in z.namelist() if n.endswith('.json') and
                     any(n.rsplit('/', 1)[-1].startswith(finish + '_') for finish in FINISHES) and
                     n.startswith('assets/cozykitchens/')]
        for name in preserved:
            assert (RES / name).read_bytes() == z.read(name), ('Approved appliance changed', name)
    print(f'PASS: {len(preserved)} approved appliance asset files are byte-identical to 0.1.0.')

baseline = ROOT / 'research/baseline-0.2.0.jar'
if baseline.exists():
    with zipfile.ZipFile(baseline) as z:
        for name in z.namelist():
            if not name.startswith('assets/cozykitchens/') or name.endswith('/'): continue
            revised_model = name.startswith('assets/cozykitchens/models/block/') and (
                name.rsplit('/', 1)[-1].startswith('pale_oak_') or
                any(name.endswith('/' + wood + '_sink.json') for wood in WOODS))
            if revised_model:
                old = json.loads(z.read(name)); new = json.loads((RES / name).read_text())
                # Existing cuboid geometry must not change in this texture-only patch.
                for elements in [old['elements'], new['elements']]:
                    for e in elements:
                        e.pop('name', None)
                        for face in e['faces'].values():
                            if face['texture'] == '#faucet': face['texture'] = '#top'
                assert old['elements'] == new['elements'], ('Geometry changed', name)
            else:
                assert (RES / name).read_bytes() == z.read(name), ('Unrelated asset changed', name)
    print('PASS: original cabinet fronts and all unrelated assets are unchanged; every revised model retains its 0.2.0 geometry.')

assert checked == 312, checked
print(f'PASS: {checked} models resolved; 84 recipes, loot tables, blockstates and item models; 12 distinct cabinet fronts and animated door sets; pale oak recipes gated.')
print('PASS: all 12 basins clear the doors by one pixel; water fits inside the basin at six native fill levels; quartz countertop material preserved.')
print('PASS: six gold and six black faucet sets; all wood sets use custom side and top/bottom textures, including pale oak.')
