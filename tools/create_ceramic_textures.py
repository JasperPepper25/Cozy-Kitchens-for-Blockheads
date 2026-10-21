#!/usr/bin/env python3
"""Export original, uniform ceramic finishes from editable SVG color swatches.

The colors match the rounded mean RGB of the previously used vanilla terracotta.
No Minecraft texture pixels are copied: all 256 pixels have the chosen glaze color.
The model's lighting supplies the shading rather than baked-in clay flecks or seams.
"""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PALETTE = {'ivory': '#d2b2a1', 'sage': '#4c532a', 'blush': '#a24e4f', 'charcoal': '#3a2a24',
           'dark': '#251710'}


def export():
    target = ROOT / 'src/main/resources/assets/cozykitchens/textures/block/ceramic'
    target.mkdir(parents=True, exist_ok=True)
    for finish, color in PALETTE.items():
        source = ROOT / f'art/ceramic_{finish}.svg'
        source.write_text('<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16">\n'
                          f'  <rect width="16" height="16" fill="{color}"/>\n</svg>\n')
        rect = ET.parse(source).getroot()[0]
        Image.new('RGB', (16, 16), rect.attrib['fill']).save(target / f'{finish}.png')
    print('Exported four smooth ceramic finishes and a dark interior; original appliance palette retained.')


if __name__ == '__main__':
    export()
