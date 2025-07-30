# PowerShell script to fix R class references

$oldRImport = "import dev.anilbeesetti.nextplayer.core.ui.R"
$newRImport = "import com.astralplayer.astralstream.core.ui.R"

# Get all Kotlin files in core/ui
$files = Get-ChildItem -Path "core/ui/src" -Recurse -Include "*.kt"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match $oldRImport) {
        Write-Host "Updating R import in: $($file.FullName)"
        $content = $content -replace [regex]::Escape($oldRImport), $newRImport
        Set-Content $file.FullName $content -NoNewline
    }
}

Write-Host "R class reference update complete!"