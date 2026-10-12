"""Structural layout checks for the generated deck.

Catches the failure modes that are easy to miss by eye: a shape pushed off the
canvas, a picture buried under a panel, or text that overflows the card it was
placed in. Runs against the .pptx directly, so it works in CI without PowerPoint.

    python scripts/check_deck_layout.py
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import ImageFont
from pptx import Presentation
from pptx.util import Emu

REPO = Path(__file__).resolve().parents[1]
# Deliverables are hoisted to the repository root; REPO is the source tree.
ROOT = REPO.parent
DECK = ROOT / "LogInsight_Analyzer_Final_Presentation.pptx"

FONT_PATH = Path(r"C:\Windows\Fonts\segoeui.ttf")
BOLD_PATH = Path(r"C:\Windows\Fonts\segoeuib.ttf")

# A panel whose edges sit within this distance of a picture on all four sides is
# that picture's own border/frame, which is drawn slightly larger on purpose.
FRAME_MARGIN = 0.16
# Bottom of the content area; the footer band lives below this.
SAFE_BOTTOM = 7.02

failures: list[str] = []
checks = 0


def check(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        failures.append(message)


def inches(value) -> float:
    return Emu(int(value)).inches


def text_width(text: str, size_pt: float, bold: bool) -> float:
    try:
        font = ImageFont.truetype(str(BOLD_PATH if bold else FONT_PATH), int(size_pt * 4))
    except OSError:
        return len(text) * size_pt * 0.5 / 72
    return font.getlength(text) / 4 / 72


def est_height(frame, box_w_in: float) -> float:
    """Approximate rendered height of a text frame, in inches."""
    usable = max(box_w_in - 0.09, 0.4)
    total = 0.0
    for paragraph in frame.paragraphs:
        runs = paragraph.runs
        size = max((r.font.size.pt for r in runs if r.font.size), default=18)
        bold = any(bool(r.font.bold) for r in runs)
        line = "".join(r.text for r in runs)
        if line:
            width = text_width(line, size, bold)
            nlines = max(1, -(-int(width * 1000) // int(usable * 1000)))
        else:
            nlines = 1
        spacing = paragraph.line_spacing if isinstance(paragraph.line_spacing, float) else 1.0
        line_h = size * 1.2 * spacing / 72
        gap = (paragraph.space_after.pt if paragraph.space_after else 0) / 72
        gap += (paragraph.space_before.pt if paragraph.space_before else 0) / 72
        total += nlines * line_h + gap
    return total


def contains(outer, inner, margin: float) -> bool:
    l, t, r, b = outer
    il, it, ir, ib = inner
    return (il - l <= margin and it - t <= margin
            and r - ir <= margin and b - ib <= margin)


def main() -> int:
    if not DECK.exists():
        raise SystemExit(f"Deck not found: {DECK}\nRun scripts/build_presentation.py first.")

    prs = Presentation(DECK)
    sw, sh = prs.slide_width, prs.slide_height
    ratio = sw / sh
    check(abs(ratio - 16 / 9) < 0.01, f"slide ratio {ratio:.4f} is not 16:9")
    print(f"Deck: {len(prs.slides.__iter__.__self__._sldIdLst)} slides, "
          f"{inches(sw):.2f}in x {inches(sh):.2f}in, ratio {ratio:.4f} "
          f"({'16:9 ok' if abs(ratio - 16 / 9) < 0.01 else 'NOT 16:9'})")

    for index, slide in enumerate(prs.slides, start=1):
        pictures: list[tuple] = []
        panels: list[tuple] = []
        texts: list[tuple] = []

        for shape in slide.shapes:
            if shape.left is None or shape.top is None:
                continue
            l, t = inches(shape.left), inches(shape.top)
            w, h = inches(shape.width), inches(shape.height)
            box = (l, t, l + w, t + h)

            check(l >= -0.02 and t >= -0.02 and box[2] <= inches(sw) + 0.02 and box[3] <= inches(sh) + 0.02,
                  f"slide {index}: shape off-canvas {box}")
            check(box[3] <= SAFE_BOTTOM + 0.30,
                  f"slide {index}: shape bottom {box[3]:.2f}in runs into the footer band")

            if shape.shape_type == 13:
                pictures.append(box)
            elif shape.has_text_frame and shape.text_frame.text.strip():
                texts.append((box, est_height(shape.text_frame, w),
                              shape.text_frame.text.strip().splitlines()[0][:44]))
            else:
                panels.append(box)

        # A picture's own border is a panel hugging it; anything else covering a
        # picture is a genuine overlap.
        for pic in pictures:
            for panel in panels:
                if contains(panel, pic, FRAME_MARGIN):
                    continue
                overlap_w = min(pic[2], panel[2]) - max(pic[0], panel[0])
                overlap_h = min(pic[3], panel[3]) - max(pic[1], panel[1])
                if overlap_w > 0.05 and overlap_h > 0.05:
                    check(False,
                          f"slide {index}: picture at ({pic[0]:.2f},{pic[1]:.2f}) covered by panel "
                          f"({panel[0]:.2f},{panel[1]:.2f}) over {overlap_w * overlap_h:.2f} sq in")

        for i, pic in enumerate(pictures):
            for other in pictures[i + 1:]:
                overlap_w = min(pic[2], other[2]) - max(pic[0], other[0])
                overlap_h = min(pic[3], other[3]) - max(pic[1], other[1])
                check(overlap_w <= 0.05 or overlap_h <= 0.05,
                      f"slide {index}: pictures overlap at ({pic[0]:.2f},{pic[1]:.2f})")

        # Text must stay inside the card it visually belongs to. The card is the
        # smallest *card-sized* panel containing the text box; thin accent rules
        # are excluded because they span a whole row rather than holding content.
        for (box, need, label) in texts:
            if box[1] >= SAFE_BOTTOM - 0.05:      # footer row, handled separately
                continue
            owner = None
            for panel in panels:
                pw, ph = panel[2] - panel[0], panel[3] - panel[1]
                if pw < 1.0 or ph < 0.5:
                    continue
                # The text must *start* inside the panel, otherwise a card in the
                # row above would claim it.
                if (panel[0] <= box[0] <= panel[2]) and (panel[1] <= box[1] <= panel[3]):
                    if owner is None or pw * ph < (owner[2] - owner[0]) * (owner[3] - owner[1]):
                        owner = panel
            if owner is not None:
                check(box[1] + need <= owner[3] + 0.04,
                      f"slide {index}: text {label!r} needs {need:.2f}in but its card ends at "
                      f"{owner[3]:.2f}in (text top {box[1]:.2f}in)")
            check(box[1] + need <= SAFE_BOTTOM,
                  f"slide {index}: text {label!r} ends at {box[1] + need:.2f}in, past the safe bottom "
                  f"{SAFE_BOTTOM}in")

        print(f"  slide {index:>2}: {len(slide.shapes):>3} shapes, {len(pictures)} pictures, "
              f"{len(texts)} text boxes, {len(panels)} panels")

    print(f"\n{'=' * 62}")
    if failures:
        print(f"FAILED: {len(failures)} of {checks} checks")
        for f in failures[:40]:
            print(f"  - {f}")
        if len(failures) > 40:
            print(f"  ... and {len(failures) - 40} more")
        return 1
    print(f"PASSED: all {checks} checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())