"""Render report PDF pages to PNG so the layout can be inspected visually.

    python scripts/render_pdf_pages.py <pdf> <outdir> [--dpi 110] [--pages 1,2,20-23]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import pymupdf


def parse_pages(spec: str, total: int) -> list[int]:
    if not spec or spec == "all":
        return list(range(1, total + 1))
    pages: list[int] = []
    for part in spec.split(","):
        part = part.strip()
        if "-" in part:
            lo, hi = part.split("-", 1)
            pages.extend(range(int(lo), int(hi) + 1))
        elif part:
            pages.append(int(part))
    return [p for p in pages if 1 <= p <= total]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf")
    ap.add_argument("outdir")
    ap.add_argument("--dpi", type=int, default=110)
    ap.add_argument("--pages", default="all")
    ap.add_argument("--prefix", default="page")
    args = ap.parse_args()

    pdf_path = Path(args.pdf)
    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    doc = pymupdf.open(pdf_path)
    wanted = parse_pages(args.pages, doc.page_count)
    scale = args.dpi / 72

    print(f"{pdf_path.name}: {doc.page_count} pages, rendering {len(wanted)} at {args.dpi} dpi")
    for number in wanted:
        page = doc[number - 1]
        pixmap = page.get_pixmap(matrix=pymupdf.Matrix(scale, scale))
        target = outdir / f"{args.prefix}-{number:02d}.png"
        pixmap.save(target)
        # Report whether the page carries any embedded raster content, which is
        # how a figure page can be told apart from a prose page at a glance.
        images = page.get_images(full=True)
        print(f"  p{number:>2}  {pixmap.width}x{pixmap.height}  images={len(images):>2}  "
              f"text={len(page.get_text().strip()):>5}ch  {target.name}")
    doc.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())