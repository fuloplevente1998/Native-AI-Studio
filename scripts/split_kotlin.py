#!/usr/bin/env python3
"""Split one Kotlin file into several by top-level declaration (REFACTOR_PLAN.md).

Usage: python3 scripts/split_kotlin.py <source.kt> <plan.txt>
plan.txt lines: "<decl-line> <target-path>" (1-based line of a top-level declaration; its KDoc /
annotations / comments directly above are taken with it). Each section runs to the next listed
declaration. Every target gets the source's header (package + imports, unused imports are harmless)
and its sections in source order. Column-0 "private " becomes "internal " so split files still see
each other. The source file is deleted unless it is itself a target.
"""
import pathlib, re, sys

OPERATORS = {"getValue", "setValue", "provideDelegate", "plus", "minus", "times", "div", "rem", "unaryMinus",
             "unaryPlus", "not", "contains", "get", "set", "invoke", "compareTo", "rangeTo", "rangeUntil", "iterator",
             "next", "hasNext", "plusAssign", "minusAssign", "inc", "dec"} | {f"component{i}" for i in range(1, 10)}

def prune(header, body):
    """Drop imports whose simple name never appears in the body. Wildcards, aliases resolved by
    their alias, and operator/delegate functions (used without their name) are always kept."""
    kept = []
    for line in header.split("\n"):
        m = re.match(r"^import\s+([\w.`]+)(?:\s+as\s+(\w+))?\s*$", line)
        if m:
            name = m.group(2) or m.group(1).split(".")[-1].strip("`")
            delegate = name in {"getValue", "setValue", "provideDelegate"}
            if delegate and " by " not in body:
                continue
            if name != "*" and name not in OPERATORS and not re.search(r"\b" + re.escape(name) + r"\b", body):
                continue
        kept.append(line)
    text = re.sub(r"\n{3,}", "\n\n", "\n".join(kept))
    return text.rstrip() + "\n"

def main(src, plan):
    lines = pathlib.Path(src).read_text(encoding="utf-8").split("\n")
    entries = []
    for raw in pathlib.Path(plan).read_text().splitlines():
        raw = raw.split("#", 1)[0].strip()
        if raw:
            n, target = raw.split(None, 1)
            entries.append((int(n), target))
    entries.sort()
    def lead(i):  # include doc/annotation/comment lines directly above (0-based index)
        while i > 0 and re.match(r"^(\s*(/\*\*|\*|\*/|//|@)|/\*)", lines[i - 1]):
            i -= 1
        return i
    starts = [lead(n - 1) for n, _ in entries]
    header_end = starts[0]
    header = "\n".join(lines[:header_end]).rstrip() + "\n"
    out = {}
    for k, (n, target) in enumerate(entries):
        end = starts[k + 1] if k + 1 < len(entries) else len(lines)
        body = [re.sub(r"^private ", "internal ", l) for l in lines[starts[k]:end]]
        out.setdefault(target, []).append("\n".join(body).strip("\n"))
    srcp = pathlib.Path(src).resolve()
    if str(srcp) not in {str(pathlib.Path(t).resolve()) for t in out}:
        pathlib.Path(src).unlink()
    for target, parts in out.items():
        p = pathlib.Path(target); p.parent.mkdir(parents=True, exist_ok=True)
        body = "\n\n".join(parts)
        p.write_text(prune(header, body) + "\n" + body + "\n", encoding="utf-8")
        print(f"{target}: {len((header + chr(10).join(parts)).splitlines())} lines")

if __name__ == "__main__":
    if len(sys.argv) != 3: print(__doc__); sys.exit(2)
    main(*sys.argv[1:])
