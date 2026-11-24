#!/usr/bin/env python3
"""Prove a refactor only MOVED Kotlin code (REFACTOR_PLAN.md).

Usage: python3 scripts/verify_move.py [--dedup N] <base-ref> <dir> [<dir> ...]
--dedup N: accept exactly N missing lines whose identical copy still exists (a removed duplicate
private helper, e.g. a second `tr`); NEW lines are never accepted.
Compares every non-blank, non-package/import line of the *.kt files under the dirs at <base-ref>
with the working tree, as a multiset. Visibility changes private/internal are ignored, as are
file header KDoc lines starting with "// Moved from". Exit 1 and list lines on any difference.
"""
import collections, pathlib, re, subprocess, sys

VIS = re.compile(r"\b(private|internal)\s+")

def norm(line: str):
    s = line.strip()
    if not s or s.startswith(("package ", "import ", "// Moved from")) or s.startswith("@file:"):
        return None
    return VIS.sub("", s)

def count(texts):
    c = collections.Counter()
    for t in texts:
        for line in t.splitlines():
            n = norm(line)
            if n is not None:
                c[n] += 1
    return c

def main():
    if len(sys.argv) < 3:
        print(__doc__); return 2
    args = sys.argv[1:]
    dedup = 0
    if args[0] == "--dedup":
        dedup, args = int(args[1]), args[2:]
    base, dirs = args[0], args[1:]
    files = subprocess.run(["git", "ls-tree", "-r", "--name-only", base, "--", *dirs],
                           capture_output=True, text=True, check=True).stdout.split()
    before = count(subprocess.run(["git", "show", f"{base}:{f}"], capture_output=True, text=True, check=True).stdout
                   for f in files if f.endswith(".kt"))
    after = count(p.read_text(encoding="utf-8") for d in dirs for p in pathlib.Path(d).rglob("*.kt"))
    lost, added = before - after, after - before
    for label, diff in (("MISSING (was in base)", lost), ("NEW (not in base)", added)):
        if diff:
            print(f"{label}: {sum(diff.values())} line(s)")
            for line, n in list(diff.items())[:40]:
                print(f"  {n}x {line[:140]}")
    deduped = all(after[line] > 0 for line in lost) and sum(lost.values()) == dedup
    if lost and deduped:
        print(f"accepted: {dedup} removed duplicate line(s), each still present elsewhere")
    ok = (not lost or deduped) and not added
    print("verify_move: OK – code only moved" if ok else "verify_move: FAILED")
    return 0 if ok else 1

if __name__ == "__main__":
    sys.exit(main())
