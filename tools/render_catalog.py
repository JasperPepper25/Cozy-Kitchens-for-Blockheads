#!/usr/bin/env python3
"""Depth-tested orthographic previews of the exact resource-pack cuboids and textures."""
import io
import json
from pathlib import Path
import zipfile
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from generate_resources import ROOT, RES, WOODS, FINISHES, FAUCET_FINISHES

client = zipfile.ZipFile(ROOT / 'research/client.jar')
backport = zipfile.ZipFile(ROOT / 'vendor/VanillaBackport-forge-1.20.1-1.1.7.10.jar')
entries = {e['id']: e for e in json.loads((ROOT / 'research/preview-models.json').read_text())}
fontroot = Path('/usr/share/fonts/truetype/dejavu')
def font(size, bold=False):
    return ImageFont.truetype(str(fontroot / ('DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf')), size)

textures = {}
def texture(path):
    if path not in textures:
        ns, name = path.split(':'); p = f'assets/{ns}/textures/{name}.png'
        if (RES / p).is_file(): data = (RES / p).read_bytes()
        else: data = client.read(p) if p in client.namelist() else backport.read(p)
        im = Image.open(io.BytesIO(data)).convert('RGBA')
        textures[path] = im.crop((0, 0, im.width, im.width))
    return textures[path]

def geometry(model, offset=(0, 0, 0)):
    parts = []
    for e in model['elements']:
        a = np.array(e['from'], dtype=float) + offset
        b = np.array(e['to'], dtype=float) + offset
        x0, y0, z0 = a; x1, y1, z1 = b
        lx0, ly0, lz0 = e['from']; lx1, ly1, lz1 = e['to']
        faces = {
            'north': [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)],
            'east': [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)],
            'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]}
        default_uv = {'north': [16-lx1, 16-ly1, 16-lx0, 16-ly0],
                      'east': [16-lz1, 16-ly1, 16-lz0, 16-ly0],
                      'up': [lx0, lz0, lx1, lz1]}
        for side, points in faces.items():
            face = e['faces'].get(side)
            if not face: continue
            t = face['texture'].lstrip('#'); path = model['textures'][t]
            pts = np.array(points)
            parts.append((side, pts, texture(path), face.get('uv', default_uv[side]), e.get('name', 'body')))
    return parts

def render(items, size=(420, 230), scale=6):
    parts = []
    for model, offset in items: parts.extend(geometry(model, offset))
    def project(pts):
        return np.stack(((pts[:, 0] + pts[:, 2]) * .7071,
                         pts[:, 0] * .30 - pts[:, 1] * .905 - pts[:, 2] * .30), axis=1) * scale
    allpts = np.concatenate([project(p[1]) for p in parts])
    lo = allpts.min(axis=0); hi = allpts.max(axis=0)
    shift = np.array([(size[0] - (hi[0] - lo[0])) / 2, (size[1] - (hi[1] - lo[1])) / 2]) - lo
    pixels = np.zeros((size[1], size[0], 4), dtype=np.uint8)
    depth_buffer = np.full((size[1], size[0]), -np.inf)
    # Orthogonal to both projection axes; larger values are nearer the camera.
    view = np.cross([.7071, 0, .7071], [.30, -.905, -.30])
    for side, pts, tex, uv, name in parts:
        q = project(pts) + shift
        left, top = np.maximum(np.floor(q.min(axis=0)).astype(int), [0, 0])
        right, bottom = np.minimum(np.ceil(q.max(axis=0)).astype(int), size)
        if right <= left or bottom <= top: continue
        mat = np.column_stack((q[1] - q[0], q[3] - q[0]))
        if abs(np.linalg.det(mat)) < 1e-6: continue
        yy, xx = np.mgrid[top:bottom, left:right]
        coords = np.stack((xx + .5 - q[0,0], yy + .5 - q[0,1]), axis=-1) @ np.linalg.inv(mat).T
        u, v = coords[...,0], coords[...,1]
        inside = (u >= -1e-7) & (u <= 1+1e-7) & (v >= -1e-7) & (v <= 1+1e-7)
        depths = pts @ view
        depth = depths[0] + u*(depths[1]-depths[0]) + v*(depths[3]-depths[0])
        w, h = tex.size
        tx = np.floor((uv[0] + u*(uv[2]-uv[0])) * w/16).astype(int) % w
        ty = np.floor((uv[1] + v*(uv[3]-uv[1])) * h/16).astype(int) % h
        colors = np.array(tex)[ty, tx].copy()
        colors[...,:3] = np.clip(colors[...,:3].astype(float) * {'up':1.05, 'north':.94, 'east':.73}[side],0,255).astype(np.uint8)
        region = depth_buffer[top:bottom,left:right]
        visible = inside & (depth > region+1e-8) & (colors[...,3] > 0)
        region[visible] = depth[visible]
        pixels[top:bottom,left:right][visible] = colors[visible]
    return Image.fromarray(pixels)

def main():
    canvas = Image.new('RGB', (1680, 1580), '#f3efe7'); draw = ImageDraw.Draw(canvas)
    draw.text((55, 35), 'COZY KITCHENS', fill='#2d3d39', font=font(44, True))
    draw.text((57, 92), 'REVISION 0.2.1  /  GOLD + BLACK FAUCETS / COMPLETE PALE OAK CABINET TEXTURES', fill='#7b715f', font=font(18))
    for i, wood in enumerate(WOODS):
        col, row = i % 4, i // 4; x = 40 + col * 410; y = 143 + row * 304
        draw.rounded_rectangle((x, y, x + 390, y + 284), radius=16, fill='#fffdf8')
        objects = [(entries[wood + '_counter']['model'], (0, 0, 0)),
                   (entries[wood + '_cooking_table']['model'], (16, 0, 0)),
                   (entries[wood + '_sink']['model'], (32, 0, 0)),
                   (entries[wood + '_cabinet']['model'], (16, 28, 2))]
        image = render(objects, (390, 233), 5.2)
        canvas.paste(image, (x, y + 5), image)
        draw.text((x + 20, y + 246), wood.replace('_', ' ').title(), fill='#3b4438', font=font(20, True))
        if wood == 'pale_oak': draw.text((x + 140, y + 250), 'Vanilla Backport', fill='#887d6e', font=font(14))
        finish = FAUCET_FINISHES[wood]
        draw.text((x + 283, y + 251), finish.title() + ' faucet', fill='#997634' if finish == 'gold' else '#414643', font=font(13))
    y = 1077
    draw.text((56, y), 'SOFT ENAMEL + WARM METAL', fill='#2d3d39', font=font(24, True))
    for i, finish in enumerate(FINISHES):
        x = 40 + i * 410
        draw.rounded_rectangle((x, y + 49, x + 390, y + 333), radius=16, fill='#fffdf8')
        objects = [(entries[finish + '_oven']['model'], (0, 0, 0)),
                   (entries[finish + '_fridge']['model'], (16, 0, 0)),
                   (entries[finish + '_toaster']['model'], (0, 16.4, 0))]
        image = render(objects, (390, 230), 6.8)
        canvas.paste(image, (x, y + 55), image)
        draw.text((x + 20, y + 295), finish.title(), fill='#3b4438', font=font(20, True))
    draw.text((56, 1452), 'Custom pale oak door, side and top/bottom textures. White basins and quartz worktops. Coordinated faucet finishes.', fill='#7b715f', font=font(18))
    draw.text((56, 1485), 'Farmer’s Delight textures © vectorwing / MIT. Pale oak art and add-on geometry © Rafe Robichaud / MIT.', fill='#7b715f', font=font(15))
    draw.text((56, 1517), 'Depth-tested model preview using the exact resource files. Minecraft lighting and resource packs will vary.', fill='#7b715f', font=font(15))
    out = ROOT / 'dist/Cozy-Kitchens-Catalog.png'; out.parent.mkdir(exist_ok=True)
    canvas.save(out)
    print(out)

if __name__ == '__main__': main()
