"""Verify shipped static fonts and Hungarian/German/Romanian glyphs, using only stdlib."""
import hashlib
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parent.parent
REQUIRED = set(map(ord, "áéíóöőúüűÁÉÍÓÖŐÚÜŰäöüÄÖÜßăâîșțĂÂÎȘȚ")) | set(range(32, 127)) | {0x0301, 0x0302, 0x0306, 0x0308, 0x030B, 0x0326}

def font_info(path):
    data = path.read_bytes()
    tables = {}
    for index in range(struct.unpack_from(">H", data, 4)[0]):
        tag, _, offset, length = struct.unpack_from(">4sIII", data, 12 + 16 * index)
        tables[tag] = data[offset:offset + length]
    cmap = tables[b"cmap"]
    present = set()
    for index in range(struct.unpack_from(">H", cmap, 2)[0]):
        _, _, offset = struct.unpack_from(">HHI", cmap, 4 + 8 * index)
        fmt = struct.unpack_from(">H", cmap, offset)[0]
        if fmt == 12:
            for group in range(struct.unpack_from(">I", cmap, offset + 12)[0]):
                start, end, first = struct.unpack_from(">III", cmap, offset + 16 + 12 * group)
                present.update(cp for cp in REQUIRED if start <= cp <= end and first + cp - start)
        elif fmt == 4:
            count = struct.unpack_from(">H", cmap, offset + 6)[0] // 2
            ends = struct.unpack_from(">" + str(count) + "H", cmap, offset + 14)
            starts = struct.unpack_from(">" + str(count) + "H", cmap, offset + 16 + 2 * count)
            deltas = struct.unpack_from(">" + str(count) + "h", cmap, offset + 16 + 4 * count)
            ranges = struct.unpack_from(">" + str(count) + "H", cmap, offset + 16 + 6 * count)
            for cp in REQUIRED:
                for segment, (start, end) in enumerate(zip(starts, ends)):
                    if start <= cp <= end:
                        if ranges[segment] == 0:
                            glyph = (cp + deltas[segment]) % 65536
                        else:
                            pos = offset + 16 + 6 * count + 2 * segment + ranges[segment] + 2 * (cp - start)
                            glyph = struct.unpack_from(">H", cmap, pos)[0]
                            glyph = (glyph + deltas[segment]) % 65536 if glyph else 0
                        if glyph:
                            present.add(cp)
                        break
    return present, struct.unpack_from(">H", tables[b"OS/2"], 4)[0], b"fvar" in tables

def main():
    proof = json.loads((ROOT / "docs/FONT_SOURCES_AND_GLYPHS.json").read_text())
    expected = set()
    for family in proof["families"]:
        license_path = ROOT / "app/src/main/assets/font-licenses" / (family["family"] + "-OFL.txt")
        assert "SIL OPEN FONT LICENSE" in license_path.read_text(), license_path
        for item in family["instances"]:
            path = ROOT / item["path"]
            expected.add(path)
            assert hashlib.sha256(path.read_bytes()).hexdigest() == item["sha256"], path
            glyphs, weight, variable = font_info(path)
            missing = REQUIRED - glyphs
            assert not missing, (path, [f"U+{cp:04X}" for cp in sorted(missing)])
            assert not variable and weight == item["weight"], (path, weight, variable)
    assert expected == set((ROOT / "app/src/main/res/font").glob("*.ttf"))
    assert len(expected) == 12
    print(f"PASS: {len(expected)} static font faces; actual weights, hashes, licenses and HU/DE/RO/ASCII glyphs verified.")

if __name__ == "__main__":
    main()
