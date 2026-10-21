#!/usr/bin/env python3
"""Export the original, editable 16-pixel pale oak front, side and end artwork."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art'
TEXTURES = ROOT / 'src/main/resources/assets/cozykitchens/textures/block/cabinet'
SVG = ART / 'pale_oak_cabinet_front.svg'

def create_source():
    # Pale painted framing, two diamond lattice panels and cool iron handles.
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">']
    def rect(x,y,w,h,c): parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{c}"/>')
    rect(0,0,16,16,'#aaa69e');rect(1,1,14,14,'#e5e0d4')
    rect(2,2,5,12,'#bdbbb3');rect(9,2,5,12,'#bdbbb3')
    rect(2,2,5,1,'#f0ebdf');rect(9,2,5,1,'#f0ebdf')
    rect(7,1,2,14,'#96938d');rect(7,1,1,14,'#ddd8cc')
    for start in [2,9]:
        for x,y in [(2,4),(1,5),(0,6),(1,7),(2,8),(3,9),(4,10),(3,11),(2,12),(1,11),(0,10),(1,9),(3,7),(4,6),(3,5)]:
            rect(start+x,y,1,1,'#eae5d9')
        rect(start,13,5,1,'#d1cbbc')
    rect(5,7,1,3,'#595c5b');rect(10,7,1,3,'#595c5b')
    rect(5,7,1,1,'#969b98');rect(10,7,1,1,'#969b98')
    rect(1,14,14,1,'#c7c1b5');rect(1,1,14,1,'#f2eee3')
    parts.append('</svg>');SVG.parent.mkdir(exist_ok=True);SVG.write_text('\n'.join(parts)+'\n')

def create_panel_source(kind):
    # Hand-placed joinery and grain in the accepted lattice front's own palette.
    # These are new panels, not recolored vanilla planks or third-party bitmaps.
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">']
    def rect(x,y,w,h,c): parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{c}"/>')
    rect(0,0,16,16,'#aaa69e')
    rect(1,1,14,14,'#ddd8cc')
    rect(2,2,12,12,'#96938d')
    rect(3,3,10,10,'#bdbbb3')
    rect(1,1,14,1,'#f2eee3');rect(1,2,1,12,'#e5e0d4')
    rect(14,2,1,12,'#c7c1b5');rect(1,14,14,1,'#c7c1b5')
    rect(2,13,12,1,'#e5e0d4');rect(13,3,1,10,'#d1cbbc')
    if kind == 'side':
        # Recessed vertical boards with a quiet centre seam and long pale grain.
        rect(3,3,4,10,'#c7c3b9');rect(8,3,5,10,'#bdbbb3')
        rect(7,3,1,10,'#aaa69e');rect(8,3,1,10,'#ddd8cc')
        for x,y,w,h,c in [
            (4,4,1,3,'#ddd8cc'),(5,7,1,3,'#d1cbbc'),(3,10,1,2,'#b3b0a7'),
            (6,4,1,2,'#b3b0a7'),(4,11,2,1,'#ddd8cc'),(9,5,1,4,'#d1cbbc'),
            (10,3,1,2,'#aaa69e'),(11,8,1,3,'#aaa69e'),(10,11,2,1,'#d1cbbc'),
            (12,4,1,2,'#c7c3b9'),(9,10,1,2,'#c7c3b9')]: rect(x,y,w,h,c)
    elif kind == 'top':
        # Bordered end panel: three joined boards and short cross-grain marks.
        rect(3,3,10,3,'#c7c3b9');rect(3,7,10,3,'#bdbbb3');rect(3,11,10,2,'#c7c3b9')
        rect(3,6,10,1,'#aaa69e');rect(3,7,10,1,'#ddd8cc')
        rect(3,10,10,1,'#aaa69e');rect(3,11,10,1,'#ddd8cc')
        for x,y,w,h,c in [
            (4,4,3,1,'#ddd8cc'),(9,3,2,1,'#b3b0a7'),(8,5,4,1,'#d1cbbc'),
            (4,8,4,1,'#d1cbbc'),(9,9,3,1,'#aaa69e'),(10,8,2,1,'#c7c3b9'),
            (5,12,3,1,'#b3b0a7'),(10,12,2,1,'#e5e0d4')]: rect(x,y,w,h,c)
    else: raise ValueError(kind)
    # Small corner joints distinguish the frame from a plain plank block.
    rect(2,1,1,1,'#d1cbbc');rect(13,14,1,1,'#aaa69e')
    parts.append('</svg>')
    ART.mkdir(exist_ok=True)
    (ART / f'pale_oak_cabinet_{kind}.svg').write_text('\n'.join(parts)+'\n')

def export():
    if not SVG.exists(): create_source()
    for kind in ['front', 'side', 'top']:
        source = ART / f'pale_oak_cabinet_{kind}.svg'
        if not source.exists(): create_panel_source(kind)
        root=ET.fromstring(source.read_text());im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        for r in root:
            x,y,w,h=[int(r.get(k)) for k in ['x','y','width','height']]
            d.rectangle((x,y,x+w-1,y+h-1),fill=r.get('fill'))
        TEXTURES.mkdir(parents=True,exist_ok=True)
        im.save(TEXTURES / f'pale_oak_cabinet_{kind}.png')
    print('Exported original pale oak front, side and top/bottom artwork from SVG')

if __name__=='__main__':export()
