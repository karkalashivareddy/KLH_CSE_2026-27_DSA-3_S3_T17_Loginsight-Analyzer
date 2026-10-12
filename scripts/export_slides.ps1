# Export every slide of a deck to PNG using the installed Microsoft PowerPoint,
# so the deck can be inspected as PowerPoint actually renders it rather than as
# python-pptx describes it. This is what catches text overflow, shapes pushed
# off-canvas and overlapping elements.
param(
    [Parameter(Mandatory = $true)][string]$Pptx,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [int]$Width = 1600
)

$ErrorActionPreference = "Stop"
$pptxPath = (Resolve-Path -LiteralPath $Pptx).Path

if (Test-Path -LiteralPath $OutDir) { Remove-Item -LiteralPath $OutDir -Recurse -Force }
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
$outFull = (Resolve-Path -LiteralPath $OutDir).Path

$powerPoint = $null
$deck = $null
try {
    $powerPoint = New-Object -ComObject PowerPoint.Application
    # msoTrue = -1 opens with the window visible, which some builds require for
    # Slide.Export to produce real raster output rather than a blank frame.
    $deck = $powerPoint.Presentations.Open($pptxPath, -1, 0, 0)

    $deckWidth = $deck.PageSetup.SlideWidth
    $deckHeight = $deck.PageSetup.SlideHeight
    $height = [int][Math]::Round($Width * $deckHeight / $deckWidth)

    Write-Output "Deck: $($deck.Slides.Count) slides, $([Math]::Round($deckWidth/72,2))in x $([Math]::Round($deckHeight/72,2))in, exporting at ${Width}x${height}"

    for ($i = 1; $i -le $deck.Slides.Count; $i++) {
        $target = Join-Path $outFull ("slide-{0:D2}.png" -f $i)
        $deck.Slides.Item($i).Export($target, "PNG", $Width, $height)
        $size = (Get-Item -LiteralPath $target).Length
        Write-Output ("  slide {0,2}  {1,9:N0} bytes  {2}" -f $i, $size, (Split-Path -Leaf $target))
    }
}
finally {
    if ($deck) { $deck.Close() | Out-Null; [void][Runtime.InteropServices.Marshal]::ReleaseComObject($deck) }
    if ($powerPoint) { $powerPoint.Quit() | Out-Null; [void][Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint) }
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
}