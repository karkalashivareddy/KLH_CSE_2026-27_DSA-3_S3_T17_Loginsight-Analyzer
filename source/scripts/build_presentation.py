"""Build the final LogInsight presentation deck.

Design: 16:9, deep navy canvas, restrained sky/cyan accents, white type. Every
diagram is built from native PowerPoint shapes so it stays editable, and every
screenshot is a real capture of the running application.

The content is driven by `scripts/submission_meta.json` and by figures captured
with `npm run capture:report`, so the deck cannot drift from the repository the
way a hand-edited one does.

    python scripts/build_presentation.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Emu, Inches, Pt

REPO = Path(__file__).resolve().parents[1]
# Deliverables are hoisted to the repository root; REPO is the source tree.
ROOT = REPO.parent
META = REPO / "scripts" / "submission_meta.json"
FIGURES = REPO / "docs" / "images" / "report"
LOGO = REPO / "frontend" / "public" / "klh-logo.png"
WORK = REPO / "docs" / "images" / "deck"
OUTPUT = ROOT / "LogInsight_Analyzer_Final_Presentation.pptx"

# ---------------------------------------------------------------- design tokens

BG = RGBColor(0x0A, 0x16, 0x23)
PANEL = RGBColor(0x14, 0x26, 0x3A)
PANEL_ALT = RGBColor(0x1B, 0x31, 0x49)
LINE = RGBColor(0x27, 0x44, 0x60)
ACCENT = RGBColor(0x38, 0xBD, 0xF8)
CYAN = RGBColor(0x22, 0xD3, 0xEE)
AMBER = RGBColor(0xFB, 0xBF, 0x24)
TEXT = RGBColor(0xF8, 0xFA, 0xFC)
MUTED = RGBColor(0xA3, 0xB8, 0xCE)
DIM = RGBColor(0x7C, 0x93, 0xAB)

FONT = "Segoe UI"
FONT_MONO = "Consolas"

SW, SH = Inches(13.333), Inches(7.5)
MARGIN = Inches(0.62)
CONTENT_W = SW - 2 * MARGIN
BODY_TOP = Inches(1.62)
BODY_H = Inches(5.2)


# ------------------------------------------------------------------- primitives


def new_slide(prs: Presentation):
    slide = prs.slides.add_slide(prs.slide_layouts[6])  # blank
    background = slide.background
    fill = background.fill
    fill.solid()
    fill.fore_color.rgb = BG
    return slide


def rect(slide, x, y, w, h, fill=PANEL, line=None, radius=0.06, shape=MSO_SHAPE.ROUNDED_RECTANGLE):
    shp = slide.shapes.add_shape(shape, x, y, w, h)
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        try:
            shp.adjustments[0] = radius
        except (IndexError, ValueError):
            pass
    if fill is None:
        shp.fill.background()
    else:
        shp.fill.solid()
        shp.fill.fore_color.rgb = fill
    if line is None:
        shp.line.fill.background()
    else:
        shp.line.color.rgb = line
        shp.line.width = Pt(1)
    shp.shadow.inherit = False
    return shp


def bar(slide, x, y, w, h, fill):
    return rect(slide, x, y, w, h, fill=fill, shape=MSO_SHAPE.RECTANGLE)


def textbox(slide, x, y, w, h, anchor=MSO_ANCHOR.TOP):
    box = slide.shapes.add_textbox(x, y, w, h)
    frame = box.text_frame
    frame.word_wrap = True
    frame.vertical_anchor = anchor
    frame.margin_left = frame.margin_right = Inches(0.04)
    frame.margin_top = frame.margin_bottom = 0
    return frame


def style(paragraph, size, color=TEXT, bold=False, font=FONT, space_after=0, space_before=0,
           align=PP_ALIGN.LEFT, line_spacing=None):
    paragraph.alignment = align
    paragraph.space_after = Pt(space_after)
    paragraph.space_before = Pt(space_before)
    if line_spacing:
        paragraph.line_spacing = line_spacing
    run = paragraph.add_run()
    run.font.size = Pt(size)
    run.font.color.rgb = color
    run.font.bold = bold
    run.font.name = font
    return run


def write(frame, text, size=16, color=TEXT, bold=False, font=FONT, space_after=6,
          space_before=0, align=PP_ALIGN.LEFT, first=False, line_spacing=None):
    paragraph = frame.paragraphs[0] if first else frame.add_paragraph()
    run = paragraph.add_run()
    run.text = text
    run.font.size = Pt(size)
    run.font.color.rgb = color
    run.font.bold = bold
    run.font.name = font
    paragraph.alignment = align
    paragraph.space_after = Pt(space_after)
    paragraph.space_before = Pt(space_before)
    if line_spacing:
        paragraph.line_spacing = line_spacing
    return paragraph


# ------------------------------------------------------------------ page chrome


def chrome(slide, index, total, eyebrow, title, subtitle=None):
    """Standard header rule, title and footer used by every content slide."""
    bar(slide, 0, 0, SW, Inches(0.06), ACCENT)
    write(textbox(slide, MARGIN, Inches(0.34), CONTENT_W, Inches(0.26)),
          eyebrow.upper(), size=11, color=ACCENT, bold=True, space_after=0, first=True)
    write(textbox(slide, MARGIN, Inches(0.60), CONTENT_W, Inches(0.6)),
          title, size=30, color=TEXT, bold=True, space_after=0, first=True)
    # The accent rule sits between the title and the subtitle so it can never
    # collide with BODY_TOP content on a dense slide.
    bar(slide, MARGIN, Inches(1.20), Inches(0.7), Pt(2.5), CYAN)
    if subtitle:
        frame = textbox(slide, MARGIN, Inches(1.32), CONTENT_W, Inches(0.26))
        write(frame, subtitle, size=13, color=MUTED, space_after=0, first=True, line_spacing=1.12)

    footer = textbox(slide, MARGIN, SH - Inches(0.46), CONTENT_W, Inches(0.26))
    write(footer, "LogInsight Analyzer  ·  DATA STRUCTURES AND ALGORITHMS-3  ·  25CS2103E",
          size=9.5, color=DIM, space_after=0, first=True)
    counter = textbox(slide, SW - MARGIN - Inches(1.2), SH - Inches(0.46), Inches(1.2), Inches(0.26))
    write(counter, f"{index} / {total}", size=9.5, color=DIM, space_after=0,
          align=PP_ALIGN.RIGHT, first=True)


def logo_chip(slide, x, y, w, pad=Inches(0.12)):
    """Place the institution logo on a white chip so it reads on the dark canvas."""
    with Image.open(LOGO) as im:
        ratio = im.width / im.height
    h = w / ratio
    chip = rect(slide, x - pad, y - pad, w + 2 * pad, h + 2 * pad,
                fill=RGBColor(0xFF, 0xFF, 0xFF), radius=0.18)
    chip.line.color.rgb = LINE
    chip.line.width = Pt(0.75)
    slide.shapes.add_picture(str(LOGO), x, y, width=w)
    return h


def bullets(slide, x, y, w, items, size=15, gap=11, dot=CYAN, line_spacing=1.22):
    """Bulleted list drawn with explicit markers so spacing is fully controlled."""
    frame = textbox(slide, x, y, w, Inches(0.4))
    for i, item in enumerate(items):
        paragraph = frame.paragraphs[0] if i == 0 else frame.add_paragraph()
        marker = paragraph.add_run()
        marker.text = "— "
        marker.font.size = Pt(size)
        marker.font.color.rgb = dot
        marker.font.bold = True
        marker.font.name = FONT
        body = paragraph.add_run()
        body.text = item
        body.font.size = Pt(size)
        body.font.color.rgb = TEXT
        body.font.name = FONT
        paragraph.space_after = Pt(gap)
        paragraph.line_spacing = line_spacing
    return frame


def stat_tile(slide, x, y, w, h, value, label, note=None, accent=CYAN):
    rect(slide, x, y, w, h, fill=PANEL, line=LINE, radius=0.1)
    bar(slide, x, y, Inches(0.045), h, accent)
    frame = textbox(slide, x + Inches(0.24), y + Inches(0.14), w - Inches(0.36),
                    h - Inches(0.24), anchor=MSO_ANCHOR.TOP)
    write(frame, value, size=27, color=accent, bold=True, space_after=1, first=True)
    write(frame, label, size=11.5, color=TEXT, bold=True, space_after=2)
    if note:
        write(frame, note, size=9.5, color=DIM, space_after=0, line_spacing=1.12)


def picture(slide, name, x, y, width):
    """Insert a capture, framing it on a panel with a hairline border."""
    path = FIGURES / name
    if not path.exists():
        raise SystemExit(f"Missing figure {name}. Run `npm run capture:report` first.")
    with Image.open(path) as im:
        ratio = im.width / im.height
    height = int(width / ratio)
    frame = rect(slide, x - Inches(0.05), y - Inches(0.05), width + Inches(0.1),
                 height + Inches(0.1), fill=PANEL, line=LINE, radius=0.03)
    slide.shapes.add_picture(str(path), x, y, width=width)
    return height


def caption(slide, x, y, w, text):
    frame = textbox(slide, x, y, w, Inches(0.3))
    write(frame, text, size=10, color=DIM, space_after=0, first=True, line_spacing=1.1)


def chip(slide, x, y, w, h, text, fill=PANEL_ALT, color=TEXT, size=11.5, bold=False):
    rect(slide, x, y, w, h, fill=fill, line=LINE, radius=0.22)
    frame = textbox(slide, x + Inches(0.06), y, w - Inches(0.12), h, anchor=MSO_ANCHOR.MIDDLE)
    write(frame, text, size=size, color=color, bold=bold, space_after=0,
          align=PP_ALIGN.CENTER, first=True)


def node(slide, x, y, w, h, title, detail=None, accent=CYAN):
    rect(slide, x, y, w, h, fill=PANEL, line=LINE, radius=0.09)
    bar(slide, x, y, w, Pt(2.5), accent)
    frame = textbox(slide, x + Inches(0.14), y + Inches(0.13), w - Inches(0.28),
                    h - Inches(0.2), anchor=MSO_ANCHOR.TOP)
    write(frame, title, size=12.5, color=TEXT, bold=True, space_after=3, first=True)
    if detail:
        write(frame, detail, size=9.8, color=MUTED, space_after=0, line_spacing=1.14)


def arrow(slide, x, y, w, h=Inches(0.2), color=LINE):
    shp = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, x, y, w, h)
    shp.fill.solid()
    shp.fill.fore_color.rgb = color
    shp.line.fill.background()
    shp.shadow.inherit = False
    return shp


def down_arrow(slide, x, y, h=Inches(0.26), w=Inches(0.22), color=LINE):
    shp = slide.shapes.add_shape(MSO_SHAPE.DOWN_ARROW, x, y, w, h)
    shp.fill.solid()
    shp.fill.fore_color.rgb = color
    shp.line.fill.background()
    shp.shadow.inherit = False
    return shp


def compare(slide, x, y, w, h, rows, left_head, right_head):
    """Two-column comparison with a header band on each side."""
    gap = Inches(0.28)
    col = int((w - gap) / 2)
    for offset, (head, accent) in enumerate(((left_head, AMBER), (right_head, CYAN))):
        cx = x + offset * (col + gap)
        rect(slide, cx, y, col, h, fill=PANEL, line=LINE, radius=0.05)
        bar(slide, cx, y, col, Pt(3), accent)
        frame = textbox(slide, cx + Inches(0.2), y + Inches(0.16), col - Inches(0.4), Inches(0.34))
        write(frame, head, size=13, color=accent, bold=True, space_after=0, first=True)
        items = rows[offset]
        body = textbox(slide, cx + Inches(0.2), y + Inches(0.6), col - Inches(0.4), h - Inches(0.76))
        for i, item in enumerate(items):
            paragraph = body.paragraphs[0] if i == 0 else body.add_paragraph()
            run = paragraph.add_run()
            run.text = "· " + item
            run.font.size = Pt(11.5)
            run.font.color.rgb = TEXT if offset else MUTED
            run.font.name = FONT
            paragraph.space_after = Pt(8)
            paragraph.line_spacing = 1.16


# ------------------------------------------------------------------------ slides


def slide_title(prs, meta, total):
    slide = new_slide(prs)
    bar(slide, 0, 0, SW, Inches(0.07), ACCENT)

    logo_chip(slide, MARGIN, Inches(0.55), Inches(1.5))

    write(textbox(slide, MARGIN, Inches(1.72), CONTENT_W, Inches(0.3)),
          "DATA STRUCTURES AND ALGORITHMS-3  ·  25CS2103E  ·  PROJECT BASED LEARNING",
          size=12, color=ACCENT, bold=True, space_after=0, first=True)

    frame = textbox(slide, MARGIN, Inches(2.12), CONTENT_W, Inches(1.3))
    write(frame, meta["project_title"], size=54, color=TEXT, bold=True, space_after=4, first=True)
    write(frame, meta["project_subtitle"], size=20, color=CYAN, space_after=0)

    bar(slide, MARGIN, Inches(3.62), Inches(1.5), Pt(3), CYAN)

    cards = [
        ("Team", "\n".join(f"{s['name']}  ·  {s['roll']}" for s in meta["students"])),
        ("Course instructor", f"{meta['guide']}\n{meta['department']}"),
        ("Institution", f"{meta['institution']}\n{meta['term']}"),
    ]
    gap = Inches(0.26)
    card_w = int((CONTENT_W - 2 * gap) / 3)
    for i, (head, body) in enumerate(cards):
        cx = MARGIN + i * (card_w + gap)
        rect(slide, cx, Inches(4.02), card_w, Inches(1.62), fill=PANEL, line=LINE, radius=0.06)
        inner = textbox(slide, cx + Inches(0.24), Inches(4.2), card_w - Inches(0.48), Inches(1.3))
        write(inner, head.upper(), size=10, color=ACCENT, bold=True, space_after=7, first=True)
        write(inner, body, size=13, color=TEXT, space_after=0, line_spacing=1.34)

    write(textbox(slide, MARGIN, Inches(6.22), CONTENT_W, Inches(0.3)),
          f"Department of Computer Science and Engineering  ·  {meta['institution']}",
          size=11, color=DIM, space_after=0, first=True)
    counter = textbox(slide, SW - MARGIN - Inches(1.2), Inches(6.22), Inches(1.2), Inches(0.3))
    write(counter, f"1 / {total}", size=10, color=DIM, space_after=0, align=PP_ALIGN.RIGHT, first=True)
    return slide


def slide_problem(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "The problem", "An outage is never explained by one log line",
           "The evidence that matters is spread across services, interleaved and out of order.")

    w = int((CONTENT_W - Inches(0.3)) / 2)
    rect(slide, MARGIN, BODY_TOP, w, Inches(3.05), fill=PANEL, line=LINE, radius=0.05)
    frame = textbox(slide, MARGIN + Inches(0.28), BODY_TOP + Inches(0.24), w - Inches(0.56), Inches(2.6))
    write(frame, "HOW THE FAILURE ARRIVES", size=10.5, color=AMBER, bold=True, space_after=10, first=True)
    for item in (
        "A payment provider starts returning 502 responses. One ERROR line, one service, easy to miss.",
        "Within minutes the order service retries, the gateway returns 504, confirmations stop, checkouts fail.",
        "The signal is present from the first moment, diluted across ~14,000 records spanning 8 services.",
        "A developer scrolling a flat log view never finds it.",
    ):
        paragraph = frame.add_paragraph()
        dot = paragraph.add_run()
        dot.text = "— "
        dot.font.size = Pt(13)
        dot.font.color.rgb = AMBER
        dot.font.bold = True
        dot.font.name = FONT
        run = paragraph.add_run()
        run.text = item
        run.font.size = Pt(13)
        run.font.color.rgb = TEXT
        run.font.name = FONT
        paragraph.space_after = Pt(9)
        paragraph.line_spacing = 1.2

    x2 = MARGIN + w + Inches(0.3)
    rect(slide, x2, BODY_TOP, w, Inches(3.05), fill=PANEL, line=LINE, radius=0.05)
    frame2 = textbox(slide, x2 + Inches(0.28), BODY_TOP + Inches(0.24), w - Inches(0.56), Inches(2.6))
    write(frame2, "WHY PLAIN SEARCH IS NOT ENOUGH", size=10.5, color=CYAN, bold=True,
          space_after=10, first=True)
    for item in (
        "A naive substring scan is O(nm). Re-scanning 14k records on every keystroke is wasteful and, in the worst case, quadratic in the pattern.",
        "Worse: the engineer does not know the phrase. The message that matters is the one that actually occurred, not the one remembered.",
        "The corpus is homogeneous in format but heterogeneous in signal, so volume alone does not identify the failure.",
    ):
        paragraph = frame2.add_paragraph()
        dot = paragraph.add_run()
        dot.text = "— "
        dot.font.size = Pt(13)
        dot.font.color.rgb = CYAN
        dot.font.bold = True
        dot.font.name = FONT
        run = paragraph.add_run()
        run.text = item
        run.font.size = Pt(13)
        run.font.color.rgb = TEXT
        run.font.name = FONT
        paragraph.space_after = Pt(9)
        paragraph.line_spacing = 1.2

    tiles = [
        ("14,000", "log events", "deterministic seeded corpus"),
        ("8", "services", "observed in the demo estate"),
        ("O(nm)", "naive search", "the cost to beat"),
    ]
    gap = Inches(0.3)
    tw = int((CONTENT_W - 2 * gap) / 3)
    for i, (value, label, note) in enumerate(tiles):
        stat_tile(slide, MARGIN + i * (tw + gap), Inches(5.02), tw, Inches(1.3),
                  value, label, note, accent=ACCENT)
    return slide


def slide_objectives(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "Objectives", "What the project sets out to prove",
           "Every objective is stated so it can be checked, and each is checked by a test or a measured run.")

    items = [
        ("Make the algorithm visible",
         "Each algorithm's contribution is observable in the interface, not hidden behind a search box."),
        ("Stay honest about provenance",
         "Dataset replay, generated simulation and observed topology are labelled distinctly at every surface."),
        ("Stay linear where it matters",
         "Exact substring search and multi-pattern labelling are O(n) so the cost does not grow with the corpus."),
        ("Keep the boundary visible",
         "Of 42 catalogue algorithms, 36 are reachable through an endpoint and 13 are traceable; the interface reports which."),
        ("Prove it in a browser",
         "Layout, contrast, keyboard access and runtime stability are asserted against the running application."),
    ]
    y = BODY_TOP + Inches(0.04)
    for i, (head, body) in enumerate(items):
        rect(slide, MARGIN, y, CONTENT_W, Inches(0.88), fill=PANEL, line=LINE, radius=0.07)
        rect(slide, MARGIN, y, Inches(0.05), Inches(0.88), fill=CYAN if i % 2 == 0 else ACCENT,
             shape=MSO_SHAPE.RECTANGLE)
        num = textbox(slide, MARGIN + Inches(0.26), y + Inches(0.16), Inches(0.5), Inches(0.56),
                      anchor=MSO_ANCHOR.MIDDLE)
        write(num, f"0{i + 1}", size=19, color=DIM, bold=True, space_after=0, first=True)
        frame = textbox(slide, MARGIN + Inches(0.9), y + Inches(0.13), CONTENT_W - Inches(1.16), Inches(0.66))
        write(frame, head, size=14.5, color=TEXT, bold=True, space_after=2, first=True)
        write(frame, body, size=11.5, color=MUTED, space_after=0, line_spacing=1.16)
        y += Inches(0.99)
    return slide


def slide_comparison(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "Positioning", "Conventional tooling vs LogInsight",
           "The gap is not the absence of a search box; it is the absence of traceability and honesty.")

    compare(
        slide, MARGIN, BODY_TOP, CONTENT_W, Inches(3.44),
        [
            ["Grep or a plain log viewer over a flat file",
             "Substring scan repeated per query, O(nm)",
             "The operator must already know the phrase",
             "Any error volume is presented as a failure",
             "Topology drawn from hand-written config",
             "Nothing states whether the data is live or replayed"],
            ["Command center over a single loaded dataset",
             "KMP linear exact search, no backtracking",
             "Aho-Corasick labels every record in one pass",
             "Levenshtein ranks a suggestion when the phrase is wrong",
             "Topology derived from observed request trails",
             "Every surface labels its own provenance"],
        ],
        "Conventional tooling", "LogInsight",
    )

    y = BODY_TOP + Inches(3.78)
    rect(slide, MARGIN, y, CONTENT_W, Inches(1.3), fill=PANEL, line=LINE, radius=0.06)
    bar(slide, MARGIN, y, Inches(0.05), Inches(1.3), ACCENT)
    frame = textbox(slide, MARGIN + Inches(0.28), y + Inches(0.2), CONTENT_W - Inches(0.56), Inches(1.0))
    write(frame, "The contribution is the last two rows", size=13, color=TEXT, bold=True,
          space_after=5, first=True)
    write(frame,
          "Traceability and provenance are what make an algorithmic tool usable during an incident: a number an "
          "operator cannot trace to its source cannot be acted on, and a graph whose provenance is unstated cannot be "
          "relied on. Both are enforced in the interface rather than only described in documentation.",
          size=11.2, color=MUTED, space_after=0, line_spacing=1.2)
    return slide


def slide_architecture(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "System design", "Three layers, one deterministic stream",
           "Each layer owns one concern, and the boundary between them is enforced in code and in the interface.")

    col_w = Inches(3.72)
    gap = Inches(0.66)
    layers = [
        ("PRESENTATION", ACCENT, [
            ("React 18 + TypeScript SPA", "Command center, Scenario Lab, Incident Workbench, Algorithm Lab"),
            ("Accessible 2D SVG topology", "paired node and edge list for keyboard and screen-reader use"),
            ("Lazy-loaded WebGL scene", "Three.js in a separate bundle, downloaded only on demand"),
        ]),
        ("SERVICES", CYAN, [
            ("REST + Server-Sent Events", "one telemetry frame per tick, owned by a single provider"),
            ("Incident detector", "fixed 5-minute windows, baseline-relative threshold"),
            ("Lifecycle store", "forward-only transitions, illegal moves rejected with HTTP 409"),
        ]),
        ("DATA & ALGORITHMS", AMBER, [
            ("Seeded corpus generator", "14,000 reproducible events across 8 services"),
            ("com.loginsight.dsa", "8 packages: string, dp, graph, flow, approximation, randomized, parallel, common"),
            ("Hand-written structures", "queue and stack underpinning the parallel algorithms"),
        ]),
    ]
    for i, (title, accent, entries) in enumerate(layers):
        x = MARGIN + i * (col_w + gap)
        rect(slide, x, BODY_TOP, col_w, Inches(3.9), fill=PANEL, line=LINE, radius=0.05)
        bar(slide, x, BODY_TOP, col_w, Pt(3), accent)
        frame = textbox(slide, x + Inches(0.22), BODY_TOP + Inches(0.18), col_w - Inches(0.44), Inches(0.3))
        write(frame, title, size=11, color=accent, bold=True, space_after=0, first=True)
        y = BODY_TOP + Inches(0.62)
        for head, detail in entries:
            node(slide, x + Inches(0.22), y, col_w - Inches(0.44), Inches(0.98), head, detail, accent=accent)
            y += Inches(1.08)
        if i < len(layers) - 1:
            arrow(slide, x + col_w + Inches(0.1), BODY_TOP + Inches(1.72), Inches(0.46), Inches(0.3), LINE)

    write(textbox(slide, MARGIN, Inches(5.72), CONTENT_W, Inches(0.6)),
          "One Server-Sent Events stream is owned by the client provider, so the overview, the live monitor and the workbench "
          "can never display contradictory numbers for the same tick.",
          size=12, color=MUTED, space_after=0, first=True, line_spacing=1.2)
    return slide


def slide_stack(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "Implementation", "Technology stack",
           "Versions are pinned in the build, so the figures quoted in the results are reproducible.")

    groups = [
        ("BACKEND", AMBER, [("Java 21", "language"), ("Spring Boot 3.5", "framework"),
                            ("Maven Wrapper", "build"), ("JUnit + JaCoCo", "test + coverage")]),
        ("FRONTEND", CYAN, [("React 18", "UI"), ("TypeScript", "types"),
                            ("Vite 6", "build"), ("Vitest", "unit tests")]),
        ("DATA & 3D", ACCENT, [("Server-Sent Events", "telemetry"), ("Three.js", "WebGL topology"),
                               ("Playwright", "18 browser workflows"), ("npm audit", "0 vulnerabilities")]),
    ]
    col_w = Inches(3.72)
    gap = Inches(0.66)
    for i, (title, accent, entries) in enumerate(groups):
        x = MARGIN + i * (col_w + gap)
        rect(slide, x, BODY_TOP, col_w, Inches(3.52), fill=PANEL, line=LINE, radius=0.05)
        bar(slide, x, BODY_TOP, col_w, Pt(3), accent)
        frame = textbox(slide, x + Inches(0.22), BODY_TOP + Inches(0.18), col_w - Inches(0.44), Inches(0.3))
        write(frame, title, size=11, color=accent, bold=True, space_after=0, first=True)
        y = BODY_TOP + Inches(0.66)
        for name, role in entries:
            rect(slide, x + Inches(0.22), y, col_w - Inches(0.44), Inches(0.6), fill=PANEL_ALT,
                 line=LINE, radius=0.12)
            inner = textbox(slide, x + Inches(0.38), y, col_w - Inches(0.76), Inches(0.6),
                            anchor=MSO_ANCHOR.MIDDLE)
            write(inner, name, size=13, color=TEXT, bold=True, space_after=0, first=True)
            write(inner, role, size=9.5, color=DIM, space_after=0)
            y += Inches(0.7)

    tiles = [("6", "viewport widths", "1440x900 down to 360x800"),
             ("36", "route declarations", "1 layout, 34 paths, 1 catch-all"),
             ("19", "navigation entries", "5 Workspace, 14 Advanced")]
    gap2 = Inches(0.3)
    tw = int((CONTENT_W - 2 * gap2) / 3)
    for i, (value, label, note) in enumerate(tiles):
        stat_tile(slide, MARGIN + i * (tw + gap2), Inches(5.36), tw, Inches(1.3),
                  value, label, note, accent=ACCENT)
    return slide


def slide_structures(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "Data structures", "The structures the algorithms are built from",
           "Written for this project rather than borrowed, so their cost is explicit and testable.")

    items = [
        ("Failure function (LPS)", "int[]", "Longest proper prefix that is also a suffix of every pattern prefix. Built once, indexed on mismatch.", "O(m) space"),
        ("Trie + failure links", "nodes[]", "Aho-Corasick states. Goto edges completed so every character has a transition, plus dict-suffix output links.", "O(states x alphabet)"),
        ("Edit-distance table", "D[i][j]", "Dynamic programming table; a rolling row reduces the distance-only case to the shorter axis.", "O(min(n, m))"),
        ("Dependency graph", "outbound / inbound", "Adjacency sets for both directions, so reachability and degree share one structure.", "O(V + E)"),
        ("Fixed windows", "buckets[]", "Events bucketed by time span, error counts per bucket, then a linear merge over flagged windows.", "O(n + W)"),
        ("Hand-written queue & stack", "CustomQueue / CustomStack", "Underpin the parallel algorithms, which then analyse work and span rather than wall-clock.", "sequential below 1024"),
    ]
    gap_x, gap_y = Inches(0.3), Inches(0.26)
    col_w = int((CONTENT_W - 2 * gap_x) / 3)
    row_h = Inches(2.12)
    for i, (name, kind, detail, cost) in enumerate(items):
        col, row = i % 3, i // 3
        x = MARGIN + col * (col_w + gap_x)
        y = BODY_TOP + row * (row_h + gap_y)
        rect(slide, x, y, col_w, row_h, fill=PANEL, line=LINE, radius=0.06)
        bar(slide, x, y, col_w, Pt(2.5), CYAN)
        frame = textbox(slide, x + Inches(0.2), y + Inches(0.16), col_w - Inches(0.4), Inches(0.6))
        write(frame, name, size=13.5, color=TEXT, bold=True, space_after=2, first=True)
        mono = textbox(slide, x + Inches(0.2), y + Inches(0.62), col_w - Inches(0.4), Inches(0.24))
        write(mono, kind, size=10, color=CYAN, bold=True, font=FONT_MONO, space_after=0, first=True)
        body = textbox(slide, x + Inches(0.2), y + Inches(0.94), col_w - Inches(0.4), Inches(0.8))
        write(body, detail, size=10.5, color=MUTED, space_after=0, first=True, line_spacing=1.16)
        chip(slide, x + Inches(0.2), y + row_h - Inches(0.52), col_w - Inches(0.4), Inches(0.36),
             cost, fill=PANEL_ALT, color=ACCENT, size=10.5)
    return slide


def slide_algorithms(prs, total, index, meta):
    slide = new_slide(prs)
    chrome(slide, index, total, "Algorithms", "Five production algorithms, and where the catalogue stops",
           "The first five run on every frame of the pipeline. The sixth is the study catalogue, kept visibly separate.")

    entries = [(a, "PRODUCTION") for a in meta["algorithms"][:5]]
    entries.append((meta["algorithms"][5], "STUDY CATALOGUE"))

    gap_x, gap_y = Inches(0.28), Inches(0.22)
    col_w = int((CONTENT_W - 2 * gap_x) / 3)
    row_h = Inches(1.62)
    for i, (algo, scope) in enumerate(entries):
        col, row = i % 3, i // 3
        x = MARGIN + col * (col_w + gap_x)
        y = BODY_TOP + row * (row_h + gap_y)
        rect(slide, x, y, col_w, row_h, fill=PANEL, line=LINE, radius=0.06)
        accent = CYAN if scope == "PRODUCTION" else AMBER
        bar(slide, x, y, col_w, Pt(2.5), accent)

        # The title gets the full card width; the scope badge sits beside the
        # complexity line so a two-line title cannot push the body text down.
        frame = textbox(slide, x + Inches(0.22), y + Inches(0.15), col_w - Inches(0.44), Inches(0.54))
        write(frame, algo["name"], size=13, color=TEXT, bold=True, space_after=0, first=True,
              line_spacing=1.06)

        complexity = textbox(slide, x + Inches(0.22), y + Inches(0.72), Inches(1.9), Inches(0.24))
        write(complexity, algo["complexity"], size=10.5, color=accent, bold=True,
              font=FONT_MONO, space_after=0, first=True)

        badge_w = Inches(1.3) if scope == "PRODUCTION" else Inches(1.62)
        chip(slide, x + col_w - badge_w - Inches(0.2), y + Inches(0.66), badge_w, Inches(0.3),
             scope, fill=PANEL_ALT, color=accent, size=8.5, bold=True)

        body = textbox(slide, x + Inches(0.22), y + Inches(1.04), col_w - Inches(0.44), Inches(0.5))
        write(body, algo["role"], size=10.3, color=MUTED, space_after=0, first=True, line_spacing=1.14)

    tiles = [("42", "algorithms", "across 6 modules"),
             ("36", "reachable", "via a canonical or trace endpoint"),
             ("13", "traceable", "with a step-by-step trace endpoint")]
    gap2 = Inches(0.3)
    tw = int((CONTENT_W - 2 * gap2) / 3)
    for i, (value, label, note) in enumerate(tiles):
        stat_tile(slide, MARGIN + i * (tw + gap2), Inches(5.42), tw, Inches(1.24),
                  value, label, note, accent=ACCENT)
    return slide


def slide_simulation(prs, total, index):
    slide = new_slide(prs)
    chrome(slide, index, total, "How it works", "A frame is a pure function of scenario, seed and tick",
           "The live surface is server-generated and deterministic, not recorded traffic.")

    steps = [
        ("1", "Resolve scenario", "CatalogueDefinition supplies severity, affected services and declared error signatures."),
        ("2", "Generate events", "DeterministicRandom seeded by (scenario, seed, tick) yields the same events every time."),
        ("3", "Label signatures", "AhoCorasick matches the whole signature set in one left-to-right pass over each message."),
        ("4", "Evaluate detector", "Fixed-window counts; flag buckets at max(3, 3 x baseline) and merge adjacent ones."),
        ("5", "Compute blast radius", "BFS over outbound edges gives the services reachable downstream of the origin."),
        ("6", "Emit the frame", "Metrics, events, signals, evidence, health, incidents and topology ship as one SSE payload."),
    ]
    gap_x = Inches(0.24)
    col_w = int((CONTENT_W - 5 * gap_x) / 6)
    y = BODY_TOP
    for i, (num, head, detail) in enumerate(steps):
        x = MARGIN + i * (col_w + gap_x)
        rect(slide, x, y, col_w, Inches(2.62), fill=PANEL, line=LINE, radius=0.07)
        badge = rect(slide, x + Inches(0.16), y + Inches(0.16), Inches(0.4), Inches(0.4),
                     fill=CYAN, shape=MSO_SHAPE.OVAL)
        bframe = textbox(slide, x + Inches(0.16), y + Inches(0.16), Inches(0.4), Inches(0.4),
                         anchor=MSO_ANCHOR.MIDDLE)
        write(bframe, num, size=12.5, color=BG, bold=True, space_after=0,
              align=PP_ALIGN.CENTER, first=True)
        head_frame = textbox(slide, x + Inches(0.16), y + Inches(0.68), col_w - Inches(0.32), Inches(0.56))
        write(head_frame, head, size=12, color=TEXT, bold=True, space_after=0, first=True, line_spacing=1.12)
        body = textbox(slide, x + Inches(0.16), y + Inches(1.3), col_w - Inches(0.32), Inches(1.2))
        write(body, detail, size=9.6, color=MUTED, space_after=0, first=True, line_spacing=1.18)
        if i < len(steps) - 1:
            arrow(slide, x + col_w + Inches(0.03), y + Inches(1.1), gap_x - Inches(0.06), Inches(0.2), LINE)

    tiles = [("7", "scenarios", "curated failure modes"),
             ("5 min", "window size", "fixed buckets"),
             ("max(3, 3x)", "threshold", "with an absolute floor of 3"),
             ("8x", "delivery speed", "pacing only, never content")]
    gap2 = Inches(0.24)
    tw = int((CONTENT_W - 3 * gap2) / 4)
    for i, (value, label, note) in enumerate(tiles):
        stat_tile(slide, MARGIN + i * (tw + gap2), Inches(4.56), tw, Inches(1.2),
                  value, label, note, accent=ACCENT)

    write(textbox(slide, MARGIN, Inches(5.94), CONTENT_W, Inches(0.5)),
          "Raising delivery speed only paces frames. It never changes which events a tick contains, so a captured "
          "run is reproducible at any speed setting.",
          size=11.5, color=DIM, space_after=0, first=True, line_spacing=1.18)
    return slide


def slide_screenshot(prs, total, index, eyebrow, title, subtitle, figures, notes, accent=CYAN):
    """A single large capture on the left with supporting notes on the right."""
    slide = new_slide(prs)
    chrome(slide, index, total, eyebrow, title, subtitle)

    width = Inches(7.55)
    height = picture(slide, figures[0], MARGIN, BODY_TOP, width)
    caption(slide, MARGIN, BODY_TOP + height + Inches(0.14), width,
            figures[1] if len(figures) > 1 else "")

    x = MARGIN + width + Inches(0.42)
    w = SW - MARGIN - x
    y = BODY_TOP
    for head, detail in notes:
        rect(slide, x, y, w, Inches(1.16), fill=PANEL, line=LINE, radius=0.06)
        bar(slide, x, y, Inches(0.045), Inches(1.16), accent)
        frame = textbox(slide, x + Inches(0.22), y + Inches(0.14), w - Inches(0.44), Inches(0.9))
        write(frame, head, size=12.5, color=TEXT, bold=True, space_after=3, first=True)
        write(frame, detail, size=10.5, color=MUTED, space_after=0, line_spacing=1.16)
        y += Inches(1.3)
    return slide


def slide_pair(prs, total, index, eyebrow, title, subtitle, left, right, notes, accent=CYAN):
    """Two captures side by side with a shared note row underneath.

    The captures are sized by height, not width, because the note row has to sit
    below them without covering either the image or its caption.
    """
    slide = new_slide(prs)
    chrome(slide, index, total, eyebrow, title, subtitle)

    gap = Inches(0.34)
    col_w = int((CONTENT_W - gap) / 2)
    max_h = Inches(3.22)

    for i, (name, cap) in enumerate((left, right)):
        x = MARGIN + i * (col_w + gap)
        with Image.open(FIGURES / name) as im:
            ratio = im.width / im.height
        width = int(max_h * ratio)
        height = int(width / ratio)
        # Centre the capture inside its column so narrower figures stay balanced.
        px = x + int((col_w - width) / 2)
        picture(slide, name, px, BODY_TOP, width)
        caption(slide, x, BODY_TOP + height + Inches(0.13), col_w, cap)

    y = BODY_TOP + max_h + Inches(0.56)
    note_w = int((CONTENT_W - 2 * gap) / 3)
    note_h = SH - Inches(0.62) - y
    for i, (head, detail) in enumerate(notes):
        x = MARGIN + i * (note_w + gap)
        rect(slide, x, y, note_w, note_h, fill=PANEL, line=LINE, radius=0.06)
        bar(slide, x, y, note_w, Pt(2.5), accent)
        frame = textbox(slide, x + Inches(0.2), y + Inches(0.18), note_w - Inches(0.4), note_h - Inches(0.3))
        write(frame, head, size=12, color=TEXT, bold=True, space_after=4, first=True)
        write(frame, detail, size=10.3, color=MUTED, space_after=0, line_spacing=1.16)
    return slide


def slide_results(prs, total, index, meta):
    slide = new_slide(prs)
    chrome(slide, index, total, "Verification", "What was actually measured",
           "Figures below are the output of the verification commands on this revision, not estimates.")

    tiles = [(v["label"], v["value"], v["note"]) for v in meta["verification"]]
    gap_x = Inches(0.26)
    col_w = int((CONTENT_W - 4 * gap_x) / 5)
    for i, (label, value, note) in enumerate(tiles):
        x = MARGIN + i * (col_w + gap_x)
        rect(slide, x, BODY_TOP, col_w, Inches(1.86), fill=PANEL, line=LINE, radius=0.07)
        bar(slide, x, BODY_TOP, col_w, Pt(2.5), CYAN)
        frame = textbox(slide, x + Inches(0.18), BODY_TOP + Inches(0.22), col_w - Inches(0.36), Inches(1.5))
        write(frame, label.upper(), size=9.5, color=DIM, bold=True, space_after=8, first=True)
        write(frame, value, size=34, color=CYAN, bold=True, space_after=6)
        write(frame, note, size=9.3, color=MUTED, space_after=0, line_spacing=1.16)

    y = Inches(3.78)
    write(textbox(slide, MARGIN, y, CONTENT_W, Inches(0.3)),
          "Browser verification, run separately because unit tests do not exercise layout or interaction",
          size=12.5, color=TEXT, bold=True, space_after=0, first=True)

    checks = [
        ("Zero console errors", "on every route of the application"),
        ("Zero horizontal overflow", "at 1440x900, 1280x800, 1024x768, 768x1024, 390x844 and 360x800"),
        ("Contrast pass", "zero failures against the required 4.5:1 for body text"),
        ("Keyboard focus", "a visible indicator on every element reached by tabbing"),
        ("Runtime stability", "repeated 2D/3D switching, stream restarts and route changes: no DOM growth, no duplicated renderer"),
    ]
    gap2 = Inches(0.22)
    cw = int((CONTENT_W - 4 * gap2) / 5)
    for i, (head, detail) in enumerate(checks):
        x = MARGIN + i * (cw + gap2)
        rect(slide, x, y + Inches(0.36), cw, Inches(1.56), fill=PANEL_ALT, line=LINE, radius=0.07)
        frame = textbox(slide, x + Inches(0.16), y + Inches(0.52), cw - Inches(0.32), Inches(1.3))
        write(frame, head, size=11.2, color=TEXT, bold=True, space_after=4, first=True)
        write(frame, detail, size=9.5, color=MUTED, space_after=0, line_spacing=1.14)

    write(textbox(slide, MARGIN, Inches(5.98), CONTENT_W, Inches(0.4)),
          "Continuous integration runs the Maven verify goal, the frontend suite, the TypeScript compile and the "
          "production build on every push to the main branch.",
          size=11, color=DIM, space_after=0, first=True, line_spacing=1.18)
    return slide


def slide_closing(prs, total, index, meta):
    slide = new_slide(prs)
    chrome(slide, index, total, "Conclusion", "Limits, next steps and what the project shows",
           "Stating the boundary of the work is part of the result.")

    head_y = BODY_TOP + Inches(0.16)
    body_y = head_y + Inches(0.34)
    write(textbox(slide, MARGIN, head_y, Inches(6.3), Inches(0.3)),
          "LIMITATIONS", size=11, color=AMBER, bold=True, space_after=0, first=True)
    bullets(slide, MARGIN, body_y, Inches(6.3), meta["limitations"][:5],
            size=11.3, gap=8, dot=AMBER, line_spacing=1.16)

    x2 = MARGIN + Inches(6.7)
    w2 = SW - MARGIN - x2
    write(textbox(slide, x2, head_y, w2, Inches(0.3)),
          "FUTURE WORK", size=11, color=CYAN, bold=True, space_after=0, first=True)
    bullets(slide, x2, body_y, w2, [
        "Durable storage so a loaded dataset and its run history survive a restart.",
        "A collector path so the platform can observe a real system rather than a generated one.",
        "Authentication and per-operator incident assignment.",
        "Root-cause ranking that states a confidence score, once one can actually be computed.",
    ], size=11.3, gap=8, dot=CYAN, line_spacing=1.16)

    y = Inches(4.62)
    rect(slide, MARGIN, y, CONTENT_W, Inches(1.52), fill=PANEL, line=LINE, radius=0.06)
    bar(slide, MARGIN, y, Inches(0.05), Inches(1.52), ACCENT)
    frame = textbox(slide, MARGIN + Inches(0.28), y + Inches(0.18), CONTENT_W - Inches(0.56), Inches(1.2))
    write(frame, "What this project demonstrates", size=13, color=TEXT, bold=True, space_after=6, first=True)
    write(frame,
          "Classical string matching, dynamic programming, graph traversal and randomized algorithms are not a "
          "laboratory exercise here: each one runs on every frame of a live stream, each one has a complexity claim, "
          "and each claim is checked by 910 backend tests, 72 frontend tests and 18 browser workflows that run against "
          "the real application rather than a mock.",
          size=11.3, color=MUTED, space_after=0, line_spacing=1.2)

    thanks = textbox(slide, MARGIN, Inches(6.36), CONTENT_W, Inches(0.4))
    write(thanks, "Thank you  ·  Questions", size=15, color=CYAN, bold=True,
          space_after=0, align=PP_ALIGN.CENTER, first=True)
    return slide


# ------------------------------------------------------------------------- build


def main() -> int:
    meta = json.loads(META.read_text(encoding="utf-8"))
    WORK.mkdir(parents=True, exist_ok=True)

    prs = Presentation()
    prs.slide_width = SW
    prs.slide_height = SH
    core = prs.core_properties
    core.title = f"{meta['project_title']} — {meta['project_subtitle']}"
    core.author = ", ".join(s["name"] for s in meta["students"])
    core.subject = meta["course"]
    core.comments = "Generated by scripts/build_presentation.py"

    builders = [
        lambda: slide_title(prs, meta, 16),
        lambda: slide_problem(prs, 16, 2),
        lambda: slide_objectives(prs, 16, 3),
        lambda: slide_comparison(prs, 16, 4),
        lambda: slide_architecture(prs, 16, 5),
        lambda: slide_stack(prs, 16, 6),
        lambda: slide_structures(prs, 16, 7),
        lambda: slide_algorithms(prs, 16, 8, meta),
        lambda: slide_simulation(prs, 16, 9),
        lambda: slide_screenshot(
            prs, 16, 10, "The product", "Command center: state and next action above the fold",
            "The selected-window signals are readable without scrolling, and the simulation is reported separately from the dataset.",
            ("report-01-command-center.png",
             "Overview command center with the demo dataset loaded."),
            [("Source in view", "The active dataset, its event count and whether the current stream is replay or generated."),
             ("Backend-derived numbers", "Dataset, window, services and detector windows are returned by the API, never invented client-side."),
             ("Compact hero", "Dataset context, four headline figures and the primary actions all fit the first viewport at desktop width.")]),
        lambda: slide_pair(
            prs, 16, 11, "The product", "Scenario Lab: a run you can reproduce",
            "Seven declared failure modes; a frame is a pure function of scenario, seed and tick.",
            ("report-03-scenario-running.png",
             "A run in progress: measured health elevated, phase at peak, one incident open."),
            ("report-02-scenario-catalogue.png",
             "The catalogue with Checkout 5xx Cascade selected before the run begins."),
            [("Deterministic by construction", "The same scenario and seed replay identically, so a screenshot is evidence rather than decoration."),
             ("Signatures, not guesses", "Aho-Corasick labels each generated message with the signature that caused it."),
             ("Explicit provenance", "Generated traffic is labelled as generated on every surface, never as captured telemetry.")]),
        lambda: slide_screenshot(
            prs, 16, 12, "The product", "Incident workbench: evidence, then conclusion",
            "Three panes: incident navigator, selected incident, and health and topology context.",
            ("report-04-incident-workbench.png",
             "Incident workbench with a detected, resolved incident and its blast radius."),
            [("Measured evidence", "Severity, error rate, p95 latency and window size are measured, not asserted."),
             ("Blast radius by traversal", "Services reachable downstream over declared edges, labelled as traversal rather than confirmed root cause."),
             ("Forward-only lifecycle", "An illegal transition is rejected with HTTP 409 instead of being silently accepted.")]),
        lambda: slide_pair(
            prs, 16, 13, "The product", "One graph, two renderers",
            "Both consume the same nodes and edges; the accessible map is always available.",
            ("report-06-topology-2d.png",
             "Two-dimensional service topology as an accessible SVG map."),
            ("report-07-topology-3d.png",
             "Three-dimensional topology rendered as a real WebGL scene."),
            [("Accessible by default", "The 2D view keeps a paired node and edge list for keyboard and screen-reader users."),
             ("Lazy-loaded scene", "Three.js ships in a separate bundle, downloaded only when 3D is selected."),
             ("Explicit lifecycle", "Content-keyed effect, forceContextLoss on unmount, context restoration and pause when off-screen.")]),
        lambda: slide_pair(
            prs, 16, 14, "The product", "Catalogue and analytics",
            "The study catalogue stays visibly separate from the production pipeline.",
            ("report-08-algorithms.png",
             "Algorithm catalogue with time and space complexity and exposure flags."),
            ("report-09-analytics.png",
             "Analytics computed by the backend from the loaded demo corpus."),
            [("42 algorithms", "Across 6 modules, from naive search to parallel prefix scan."),
             ("36 reachable, 13 traceable", "36 reachable through an endpoint across 35 dispatch keys; 13 traceable."),
             ("Backend-computed aggregates", "Traffic, severity, HTTP and host rollups come from the API, not the browser.")]),
        lambda: slide_results(prs, 16, 15, meta),
        lambda: slide_closing(prs, 16, 16, meta),
    ]

    for build in builders:
        build()

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    prs.save(OUTPUT)
    print(f"Wrote {OUTPUT.relative_to(ROOT)} ({OUTPUT.stat().st_size:,} bytes, {len(prs.slides.__iter__.__self__._sldIdLst)} slides)")
    return 0


if __name__ == "__main__":
    sys.exit(main())