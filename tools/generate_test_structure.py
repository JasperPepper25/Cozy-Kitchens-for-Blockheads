#!/usr/bin/env python3
"""Create the empty structure used by the isolated Forge GameTests."""
import gzip
from pathlib import Path
import struct

def name(s):
    d = s.encode(); return struct.pack('>H', len(d)) + d
def integer(n): return struct.pack('>i', n)
def named(tag, n, data): return bytes([tag]) + name(n) + data
data = b'\x0a\x00\x00'
data += named(3, 'DataVersion', integer(3465))
data += named(9, 'size', b'\x03' + integer(3) + integer(16) + integer(8) + integer(16))
data += named(9, 'palette', b'\x0a' + integer(1) + named(8, 'Name', name('minecraft:air')) + b'\x00')
data += named(9, 'blocks', b'\x0a' + integer(0))
data += named(9, 'entities', b'\x0a' + integer(0)) + b'\x00'
p = Path(__file__).resolve().parents[1] / 'src/gametest/resources/data/cozykitchens/structures/empty.nbt'
p.parent.mkdir(parents=True, exist_ok=True)
p.write_bytes(gzip.compress(data, mtime=0))
