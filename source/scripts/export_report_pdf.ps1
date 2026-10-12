# Convert the built report to PDF using the installed Microsoft Word, so the PDF
# is Word's own rendering of the DOCX rather than a second layout engine. Word's
# export preserves the sample's pagination, figure sizing and page numbering
# exactly, which a reportlab re-layout could not guarantee.
param(
    [string]$Docx = (Join-Path $PSScriptRoot "..\LogInsight_Final_Project_Report.docx"),
    [string]$Pdf  = (Join-Path $PSScriptRoot "..\LogInsight_Final_Project_Report.pdf")
)

$ErrorActionPreference = "Stop"
$docxPath = (Resolve-Path -LiteralPath $Docx).Path
$pdfPath = [System.IO.Path]::GetFullPath($Pdf)

if (-not (Test-Path -LiteralPath (Split-Path -Parent $pdfPath) -PathType Container)) {
    throw "Output directory does not exist: $(Split-Path -Parent $pdfPath)"
}

$word = $null
$doc = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0

    $doc = $word.Documents.Open($docxPath, $false, $true)   # ConfirmConversions=false, ReadOnly=true

    # 17 = wdExportFormatPDF. Export the document as laid out, keeping the
    # markup off so the exported PDF matches what a reader sees on screen.
    $doc.ExportAsFixedFormat($pdfPath, 17, $false, 0, 0, 0, 0, 0, $true, $true, 0, $true, $true, $false)

    $pages = $doc.ComputeStatistics(2)   # 2 = wdStatisticPages
    Write-Output "Exported $pdfPath"
    Write-Output "  pages : $pages"
}
finally {
    if ($doc)  { $doc.Close($false) | Out-Null; [void][Runtime.InteropServices.Marshal]::ReleaseComObject($doc) }
    if ($word) { $word.Quit() | Out-Null;          [void][Runtime.InteropServices.Marshal]::ReleaseComObject($word) }
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
}

$size = (Get-Item -LiteralPath $pdfPath).Length
Write-Output "  bytes : $('{0:N0}' -f $size)"