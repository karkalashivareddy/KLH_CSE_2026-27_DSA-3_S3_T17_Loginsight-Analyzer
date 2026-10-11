"""
Render the final project report to PDF.

LibreOffice is not available in this environment, so the PDF is produced directly
with reportlab from the same content as `LogInsight_Final_Project_Report.docx`
(`scripts/build_submission.py`). Both are generated from one set of constants and
the same captured screenshots, so they cannot drift apart.

Run:  python scripts/build_pdf.py
"""
from __future__ import annotations

import pathlib

from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)

ROOT = pathlib.Path(__file__).resolve().parent.parent
IMAGES = ROOT / "docs" / "images" / "signal-atlas"
OUT = ROOT / "final-submission" / "LogInsight_Final_Project_Report.pdf"

INK = "#16211f"
MUTED = "#55625c"
ACCENT = "#0a5560"
RULE = "#dbe6e6"

import json as _json
import sys as _sys
_sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import build_submission as _sub

_META = _json.loads((ROOT / "scripts" / "submission_meta.json").read_text(encoding="utf-8"))
COURSE = _META["course"]
STUDENTS = [(s["name"], s["roll"]) for s in _META["students"]]
GUIDE = _META["guide"]
DEPARTMENT = _META["department"]
INSTITUTION = _META["institution"]
TERM = _META["term"]

VERIFY = [("Check", "Result", "Command")] + [
    (v["label"], v["value"], v["note"]) for v in _META["verification"]
]

ALGO = [("Algorithm", "Complexity", "Role in the product")] + [
    (a["name"], a["complexity"], a["role"]) for a in _META["algorithms"]
]

_RAW = _sub.first_load_kb()[0]
BUNDLE = [
    ("First-load JS", "before: shipped inside the entry chunk", f"after: {_RAW}"),
    ("Three.js", "before: loaded with the application", "after: deferred to the 3D topology"),
]

LIMITATIONS = list(_META["limitations"])

CAPTIONS = [
    ("command-center-desktop.png", "Command center with backend-derived selected-window values."),
    ("incident-workbench-desktop.png", "Incident workbench: identity, measured context, blast radius, lifecycle."),
    ("log-explorer-desktop.png", "Log explorer with a server-side query and results."),
    ("analytics-desktop.png", "Analytics computed for the selected range."),
    ("topology-selection-desktop.png", "Service topology with a selected service."),
    ("algorithms-desktop.png", "Algorithm catalogue."),
    ("no-dataset-state.png", "No dataset: onboarding rather than fabricated metrics."),
    ("command-center-mobile.png", "Responsive mobile layout."),
]


def styles() -> dict[str, ParagraphStyle]:
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle("t", parent=base["Title"], fontName="Helvetica-Bold",
                                fontSize=24, textColor=INK, spaceAfter=2, alignment=TA_LEFT),
        "subtitle": ParagraphStyle("st", parent=base["Normal"], fontName="Helvetica",
                                   fontSize=12, textColor=ACCENT, spaceAfter=2),
        "meta": ParagraphStyle("m", parent=base["Normal"], fontName="Helvetica",
                               fontSize=9, textColor=MUTED, spaceAfter=2),
        "h1": ParagraphStyle("h1", parent=base["Heading1"], fontName="Helvetica-Bold",
                             fontSize=13, textColor=ACCENT, spaceBefore=12, spaceAfter=5),
        "body": ParagraphStyle("b", parent=base["Normal"], fontName="Helvetica",
                               fontSize=9.5, textColor=INK, leading=14, spaceAfter=5),
        "bullet": ParagraphStyle("bu", parent=base["Normal"], fontName="Helvetica",
                                 fontSize=9.5, textColor=INK, leading=13.5,
                                 leftIndent=10, bulletIndent=2, spaceAfter=3),
        "caption": ParagraphStyle("c", parent=base["Normal"], fontName="Helvetica-Oblique",
                                  fontSize=8, textColor=MUTED, spaceAfter=10),
        "cell": ParagraphStyle("cell", parent=base["Normal"], fontName="Helvetica",
                               fontSize=8.5, textColor=INK, leading=11),
    }


def grid(rows: list[tuple[str, str, str]], widths: list[float]) -> Table:
    table = Table(rows, colWidths=widths, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), RULE),
        ("TEXTCOLOR", (0, 0), (-1, 0), ACCENT),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.5),
        ("TEXTCOLOR", (0, 1), (-1, -1), INK),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.4, RULE),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), ["#ffffff", "#f7f9f8"]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def build() -> None:
    s = styles()
    width = A4[0] - 36 * mm
    story: list = []

    story.append(Paragraph("LogInsight Analyzer", s["title"]))
    story.append(Paragraph("Algorithm-Driven Log Analysis and Incident Investigation", s["subtitle"]))
    story.append(Paragraph(COURSE, s["meta"]))
    story.append(Paragraph(f"{INSTITUTION}  ·  {TERM}", s["meta"]))
    story.append(Spacer(1, 8))
    story.append(grid(
        [("Team member", "Roll number", "Guide")] +
        [(name, roll, GUIDE if i == 0 else "") for i, (name, roll) in enumerate(STUDENTS)],
        [width * 0.32, width * 0.2, width * 0.48],
    ))
    story.append(Spacer(1, 4))
    story.append(Paragraph(DEPARTMENT, s["meta"]))

    sections = [
        ("1. Problem statement", [
            "During an outage the evidence required to explain what happened is spread across services, "
            "arrives interleaved and out of order, and is queried far faster than a human can read it. "
            "Substring search over the corpus is O(nm) and rescans on every attempt, so the engineer "
            "never learns which algorithm actually ran or what it measured.",
            "LogInsight applies classical data structures and algorithms to that work — multi-pattern and "
            "exact-substring search, bounded edit distance, streaming window aggregation and dependency-graph "
            "traversal — and presents the returned events, aggregates and algorithm evidence together with "
            "their source and scope.",
        ], []),
        ("2. Architecture", [
            "The application is three layers with one rule: telemetry is server-owned. The Spring Boot backend "
            "generates deterministic scenarios, computes product aggregates, executes the algorithm engines and "
            "manages process-local incident state. The React frontend renders API responses with their scope and "
            "provenance, and claims algorithm execution evidence only where a real endpoint produced it.",
            "Transport is HTTP REST plus three Server-Sent Event streams that are deliberately kept distinct: the "
            "generated simulation, the bounded dataset replay, and recorded trace replay. There is no WebSocket "
            "client and no external log collector.",
        ], []),
        ("3. Data flow", [
            "A dataset is loaded from sample-data, uploaded, or generated by the backend.",
            "DatasetService installs it as one immutable in-memory current dataset; readers take a consistent snapshot.",
            "Analytics, incident detection, pattern extraction and search all derive from that dataset.",
            "The frontend requests derived values and never recomputes backend analysis.",
            "Mutating the dataset invalidates every cached view through a shared event.",
        ], []),
        ("4. Algorithms and their actual roles", [
            "The catalogue distinguishes algorithms exposed through an execution endpoint, algorithms that "
            "additionally emit a trace, and entries that exist as lab implementations. A wider repertoire — "
            "bitmask TSP, Needleman-Wunsch, Dinic flow, vertex-cover kernelization, parallel prefix scan and "
            "Miller-Rabin — is exposed as runnable lab entries rather than presented as product capability.",
        ], []),
        ("5. 2D and 3D visualisation", [
            "The service topology renders as an accessible 2D SVG by default, with a paired service and edge list "
            "so the same data is available without WebGL. An optional Three.js view is code-split and loaded on demand.",
            "The WebGL scene is rebuilt only when the graph genuinely changes, not on every render.",
            "Teardown disposes geometries, materials and controls, stops the animation loop, and explicitly releases "
            "the browser WebGL context, which renderer disposal alone does not do.",
            "Context loss surfaces a usable 2D fallback; context restoration rebuilds the scene automatically.",
            "Switching modes preserves the selected service and every active filter.",
            "Node size follows observed event counts and health follows error-rate bands; neither implies causation.",
        ], []),
        ("6. Error handling and data honesty", [
            "Every dataset-backed request returns a structured error envelope. Validation and malformed input map "
            "to 400, a missing dataset to 404, and unexpected failures to a sanitised 500 that never carries a "
            "stack trace. When no dataset is loaded the interface shows onboarding rather than zeroed metrics, and "
            "unmeasured values render as an em dash instead of being coerced to zero.",
        ], []),
    ]

    for heading, paragraphs, bullets in sections:
        story.append(Paragraph(heading, s["h1"]))
        for paragraph in paragraphs:
            story.append(Paragraph(paragraph, s["body"]))
        for bullet in bullets:
            story.append(Paragraph(bullet, s["bullet"], bulletText="•"))

    # algorithm table
    story.append(Paragraph("4.1 Algorithm catalogue", s["h1"]))
    story.append(grid([tuple(Paragraph(c, s["cell"]) for c in row) for row in ALGO],
                      [width * 0.26, width * 0.18, width * 0.56]))

    story.append(Paragraph("7. Testing methodology and results", s["h1"]))
    story.append(Paragraph(
        "The browser suite covers direct navigation to every documented route, route aliases, dataset loading and "
        "replacement, search and pagination, empty and failing states, repeated 2D/3D mode switching, the WebGL "
        "fallback, the guided walkthrough, reduced motion, console and failed network requests, and responsive "
        "overflow at six viewport sizes.", s["body"]))
    story.append(grid([tuple(Paragraph(c, s["cell"]) for c in row) for row in VERIFY],
                      [width * 0.3, width * 0.28, width * 0.42]))

    story.append(Paragraph("8. Performance", s["h1"]))
    story.append(grid(
        [("Measure", "Before", "After")] + [(a, b, c) for a, b, c in BUNDLE],
        [width * 0.4, width * 0.3, width * 0.3],
    ))
    story.append(Spacer(1, 6))

    story.append(PageBreak())
    story.append(Paragraph("9. Screenshots from the final implementation", s["h1"]))
    for filename, caption in CAPTIONS:
        path = IMAGES / filename
        if not path.exists():
            continue
        reader = ImageReader(str(path))
        iw, ih = reader.getSize()
        target_w = width
        target_h = target_w * ih / iw
        # Keep a full-page image from overflowing; cap at a readable height.
        if target_h > 165 * mm:
            target_h = 165 * mm
            target_w = target_h * iw / ih
        story.append(KeepTogether([Image(str(path), width=target_w, height=target_h),
                                   Paragraph(caption, s["caption"])]))

    story.append(PageBreak())
    story.append(Paragraph("10. Known limitations", s["h1"]))
    for item in LIMITATIONS:
        story.append(Paragraph(item, s["bullet"], bulletText="•"))

    story.append(Paragraph("11. Future work", s["h1"]))
    for item in [
        "Durable dataset storage so investigations survive a restart.",
        "Authentication and authorisation, which the current deployment model does not provide.",
        "Streaming ingestion from a real collector, replacing the generated simulation and bounded replay.",
        "Distributed algorithm execution for the largest catalogue entries.",
    ]:
        story.append(Paragraph(item, s["bullet"], bulletText="•"))

    def decorate(canvas, doc):
        canvas.saveState()
        canvas.setFont("Helvetica", 7)
        canvas.setFillColor("#66736d")
        canvas.drawString(18 * mm, 10 * mm,
                          "LogInsight Analyzer  ·  KLH_CSE_2026-27_DSA-3_S3_T17_Loginsight-Analyzer")
        canvas.drawRightString(A4[0] - 18 * mm, 10 * mm, f"Page {doc.page}")
        canvas.setStrokeColor(RULE)
        canvas.setLineWidth(0.4)
        canvas.line(18 * mm, 13 * mm, A4[0] - 18 * mm, 13 * mm)
        canvas.restoreState()

    doc = BaseDocTemplate(str(OUT), pagesize=A4,
                          leftMargin=18 * mm, rightMargin=18 * mm,
                          topMargin=16 * mm, bottomMargin=18 * mm,
                          title="LogInsight Analyzer — Final Project Report",
                          author=", ".join(name for name, _ in STUDENTS),
                          subject=COURSE)
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="main")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=decorate)])
    doc.build(story)
    print(f"pdf: {OUT}")


if __name__ == "__main__":
    build()