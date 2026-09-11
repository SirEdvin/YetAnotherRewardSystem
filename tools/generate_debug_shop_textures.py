#!/usr/bin/env python3
"""Write YARS debug shop textures pixel by pixel; Python 3 stdlib only."""
import argparse
import struct
import zlib
from pathlib import Path

PALETTES = {
    "debug_reward_shop": ["201c24", "49302b", "704735", "a36b43", "d6a45a", "ffe09a", "8d632f", "36262c"],
    "debug_automatic_reward_box": ["18232d", "293d49", "42606a", "678c92", "44c6b5", "b4f7da", "288a87", "203039"],
}
COIN = ["...4444...", "..455554..", ".45644654.", ".45455454.", ".45445454.", ".45644654.", "..455554..", "...4444..."]
HOPPER = ["4444444444", ".45555554.", "..466664..", "...4664...", "....44....", "....44....", "..4.44.4..", "...4444...", "....44...."]


def png(pixels, scale=1):
    size = len(pixels)
    raw = b"".join(b"\0" + b"".join(color * scale for color in row) for row in pixels for _ in range(scale))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    # Stored DEFLATE blocks keep the bytes stable across compression-library versions.
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size * scale, size * scale, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, level=0)) + chunk(b"IEND", b"")


def texture(name, face):
    colors = [bytes.fromhex(color) for color in PALETTES[name]]
    grid = [[1 for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                grid[y][x] = 0
            elif y == 1 or x == 1:
                grid[y][x] = 3
            elif y == 14 or x == 14:
                grid[y][x] = 7
            else:
                grid[y][x] = 2 if (x + y // 4) % 5 else 1
    if face == "side":
        motif = COIN if name == "debug_reward_shop" else HOPPER
        for y, row in enumerate(motif, 3):
            for x, value in enumerate(row, 3):
                if value != ".":
                    grid[y][x] = int(value)
        for x in range(2, 14):
            grid[12][x] = 4
            grid[13][x] = 6
    elif face == "top":
        for y in range(4, 12):
            for x in range(4, 12):
                grid[y][x] = 0 if 5 <= x <= 10 and 5 <= y <= 10 else 4
        for x in range(6, 10):
            grid[7][x] = 6
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        grid[y][x] = 5
    return [[colors[index] for index in row] for row in grid]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / "projects/core/src/main/resources/assets/yars/textures/block")
    parser.add_argument("--preview", type=Path, help="Optional directory for enlarged nearest-neighbor PNGs")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.preview:
        args.preview.mkdir(parents=True, exist_ok=True)
    for name in PALETTES:
        for face in ("side", "top", "bottom"):
            pixels = texture(name, face)
            filename = f"{name}_{face}.png"
            (args.output / filename).write_bytes(png(pixels))
            if args.preview:
                (args.preview / filename).write_bytes(png(pixels, 20))
            print(args.output / filename)


if __name__ == "__main__":
    main()
