"""Verify the built report against the sample and against the repository facts.

Checks that the sample's layout survived the edit, that no superseded figure or
claim is still present, and that the screenshots really are the new captures.
"""
from __future__ import annotations

import os
import re
import zipfile
from pathlib import Path

from docx import Document
from docx.shared import Emu
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
# Deliverables are hoisted to the repository root; REPO is the source tree.
ROOT = REPO.parent

# The institutional sample is not committed here; set LOGINSIGHT_SAMPLE_REPORT
# to point at it. The fallback covers the author's machine.
SAMPLE = Path(
    os.environ.get(
        "LOGINSIGHT_SAMPLE_REPORT",
        r"C:\Users\karka\Downloads\LogInsight_Final_Project_Report (1) (1).docx",
    )
)
REPORT = ROOT / "LogInsight_Final_Project_Report.docx"
FIGURES = REPO / "docs" / "images" / "report"

NS = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "wp": "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}

# Claims from the older sample that must no longer appear.
FORBIDDEN = [
    (r"\b877\b", "old backend test count"),
    (r"\b55 (?:automated )?tests\b", "old frontend test count"),
    (r"17 files", "old frontend file count"),
    (r"seven services", "old service count"),
    (r"five viewport widths", "old viewport count"),
    (r"centroid", "algorithm replaced by rerooting DP"),
    (r"public Set<String> reachableFrom", "stale reachableFrom signature"),
    (r"public Map<String, Object> lifecycleConflict", "stale handler signature"),
    (r'\+ " to " \+ target \+\)', "malformed string concatenation"),
    (r"\b55\b(?= frontend)", "old frontend count in running text"),
]

# Claims that must be present, so an edit cannot silently drop verified content.
REQUIRED = [
    (r"\b910\b", "current backend test count"),
    (r"72 tests across 20 files", "current frontend test count"),
    (r"\beight services\b", "current service count"),
    (r"42 algorithms", "catalogue size"),
    (r"36\s+are reachable", "reachable count"),
    (r"13 are traceable", "traceable count"),
    (r"six viewport widths", "current viewport count"),
    (r"360x800", "narrowest tested viewport"),
    (r"eighteen-workflow Playwright", "browser suite size"),
    (r"forceContextLoss", "WebGL context release"),
    (r"webglcontextrestored", "WebGL restoration"),
    (r"IntersectionObserver", "WebGL pause"),
    (r"public List<String> reachableFrom", "corrected reachableFrom signature"),
    (r"IllegalLifecycleTransitionException\(id, incident\.getStatus\(\), target\)", "corrected exception construction"),
    (r"Spring Boot 3\.5", "pinned backend version"),
    (r"eight packages", "DSA package count"),
]

failures: list[str] = []
checks = 0


def check(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        failures.append(message)


def main() -> int:
    if not REPORT.exists():
        raise SystemExit(f"Report not built: {REPORT}")

    sample = Document(SAMPLE)
    report = Document(REPORT)
    sample_paras, report_paras = sample.paragraphs, report.paragraphs

    print("=== layout preserved ===")
    # Paragraph count is expected to move: longer code excerpts add lines, while
    # the pagination cleanup removes stranded spacers. What must not happen is
    # losing content, so that is what is compared.
    def content(d):
        return [p.text.strip() for p in d.paragraphs if p.text.strip()]

    sample_content, report_content = content(sample), content(report)
    check(len(report_content) >= len(sample_content),
          f"content paragraphs dropped: sample {len(sample_content)} -> report {len(report_content)}")
    print(f"  content paras  sample {len(sample_content)}  report {len(report_content)}")
    print(f"  total paras    sample {len(sample_paras)}  report {len(report_paras)}  "
          f"(code excerpts lengthened, spacers removed)")

    # Every heading the sample had must survive with the same wording.
    sample_headings = [
        p.text.strip() for p in sample_paras
        if p.text.strip() and round(max((r.font.size.pt for r in p.runs if r.font.size), default=0)) >= 13
    ]
    report_headings = [
        p.text.strip() for p in report_paras
        if p.text.strip() and round(max((r.font.size.pt for r in p.runs if r.font.size), default=0)) >= 13
    ]
    missing_headings = [h for h in sample_headings if h not in report_headings]
    check(not missing_headings, f"headings lost from the sample: {missing_headings}")
    print(f"  headings       {len(report_headings)} kept, 0 lost")

    for name in ("page_width", "page_height", "top_margin", "bottom_margin", "left_margin", "right_margin"):
        s = getattr(sample.sections[0], name)
        r = getattr(report.sections[0], name)
        check(s == r, f"{name} changed: {s.inches:.2f}in -> {r.inches:.2f}in")
        print(f"  {name:<13} {s.inches:.2f}in -> {r.inches:.2f}in  {'ok' if s == r else 'CHANGED'}")

    check(len(report.tables) == 0, "unexpected tables introduced")
    print(f"  tables        {len(report.tables)} (sample layout has none)")

    # The KLH letterhead on the cover must be untouched.
    print("\n=== cover artwork ===")
    letterhead = next(
        report.part.related_parts[blip.get(f"{{{NS['r']}}}embed")]
        for p in report_paras
        if (blip := p._p.find(".//a:blip", NS)) is not None
        and str(report.part.related_parts[blip.get(f"{{{NS['r']}}}embed")].partname) == "/word/media/image1.png"
    )
    sample_logo = next(
        sample.part.related_parts[blip.get(f"{{{NS['r']}}}embed")]
        for p in sample_paras
        if (blip := p._p.find(".//a:blip", NS)) is not None
        and str(sample.part.related_parts[blip.get(f"{{{NS['r']}}}embed")].partname) == "/word/media/image1.png"
    )
    check(letterhead.blob == sample_logo.blob, "cover letterhead was altered")
    print(f"  image1.png    unchanged ({len(letterhead.blob):,} bytes)")

    print("\n=== screenshots are the fresh captures ===")
    by_part = {name: (FIGURES / name).read_bytes() for name in {p.name for p in FIGURES.glob("*.png")}}
    swapped = 0
    for p in report_paras:
        blip = p._p.find(".//a:blip", NS)
        if blip is None:
            continue
        part = report.part.related_parts[blip.get(f"{{{NS['r']}}}embed")]
        if str(part.partname) == "/word/media/image1.png":
            continue
        match = next((n for n, b in by_part.items() if b == part.blob), None)
        check(match is not None, f"{part.partname} is not any current capture")
        swapped += 1
        print(f"  {str(part.partname):<24} = {match}")
    check(swapped == 11, f"expected 11 content figures, found {swapped}")
    print(f"  total         {swapped} content figures + 1 letterhead")

    print("\n=== image extents unchanged ===")
    def extents(d):
        out = []
        for p in d.paragraphs:
            ext = p._p.find(".//wp:extent", NS)
            if ext is not None:
                out.append((int(ext.get("cx")), int(ext.get("cy"))))
        return out
    check(extents(sample) == extents(report), "a figure extent changed, which would reflow the page")
    print(f"  {len(extents(report))} extents, all identical to the sample")

    print("\n=== stale claims removed ===")
    full_text = "\n".join(p.text for p in report_paras)
    for pattern, label in FORBIDDEN:
        hits = re.findall(pattern, full_text, re.I)
        check(not hits, f"stale {label} still present: {hits[:3]}")
        print(f"  {'clear' if not hits else 'STALE '}  {label}")

    print("\n=== verified claims present ===")
    for pattern, label in REQUIRED:
        hits = re.findall(pattern, full_text)
        check(bool(hits), f"missing required content: {label}")
        print(f"  {'ok   ' if hits else 'MISS '}  {label}")

    print("\n=== fonts and hierarchy ===")
    def profile(d, predicate):
        sizes = set()
        for p in d.paragraphs:
            if not p.text.strip() or not predicate(p):
                continue
            for r in p.runs:
                if r.font.size:
                    sizes.add(round(r.font.size.pt, 1))
        return sizes

    # Classes are defined by what the sample itself uses, so the invariant is
    # "the sample's hierarchy survived" rather than a hardcoded guess about it.
    headings = lambda p: p.text.strip() and round(max(
        (r.font.size.pt for r in p.runs if r.font.size), default=0)) >= 13
    body = lambda p: p.text.strip() and p.alignment is not None and p.alignment == 3
    code = lambda p: p.text.strip() and any(r.font.name == "Consolas" for r in p.runs)

    for name, predicate in (
        ("headings (>=13pt)", headings),
        ("body (justified)", body),
        ("code blocks", code),
    ):
        before, after = profile(sample, predicate), profile(report, predicate)
        check(before == after, f"{name} size profile changed: sample {before} -> report {after}")
        print(f"  {name:<18} {sorted(before)}  ==  report {sorted(after)}  "
              f"{'ok' if before == after else 'CHANGED'}")

    # Line spacing must also survive, since it drives where each figure lands.
    def spacing(d):
        return sorted({round(p.paragraph_format.line_spacing, 3)
                       for p in d.paragraphs
                       if p.paragraph_format.line_spacing is not None})
    check(spacing(sample) == spacing(report),
          f"line spacing changed: sample {spacing(sample)} -> report {spacing(report)}")
    print(f"  {'line spacing':<18} {spacing(report)}  ==  sample {spacing(sample)}")

    fonts = set()
    for p in report_paras:
        for r in p.runs:
            if r.font.name:
                fonts.add(r.font.name)
    print(f"  fonts           {sorted(fonts)}")
    check(fonts <= {"Cambria", "Consolas"}, f"unexpected fonts introduced: {fonts}")

    # Structural headings must survive in the same order.
    sample_chapters = [p.text.strip() for p in sample_paras if p.text.strip() in {
        "ALGORITHM / PSEUDO CODE", "CODE", "RESULTS"}]
    report_chapters = [p.text.strip() for p in report_paras if p.text.strip() in {
        "ALGORITHM / PSEUDO CODE", "CODE", "RESULTS"}]
    check(sample_chapters == report_chapters, "chapter headings changed")
    print(f"  chapters        {report_chapters}")

    print("\n=== package integrity ===")
    with zipfile.ZipFile(REPORT) as z:
        bad = z.testzip()
        check(bad is None, f"corrupt zip entry: {bad}")
        print(f"  zip entries     {len(z.namelist())}, integrity ok")
    print(f"  file size       {REPORT.stat().st_size:,} bytes")

    print(f"\n{'=' * 62}")
    if failures:
        print(f"FAILED: {len(failures)} of {checks} checks")
        for f in failures:
            print(f"  - {f}")
        return 1
    print(f"PASSED: all {checks} checks")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())