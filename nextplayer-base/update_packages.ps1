# PowerShell script to update all package names from nextplayer to astralstream

$oldPackage = "dev.anilbeesetti.nextplayer"
$newPackage = "com.astralplayer.astralstream"

# Get all Kotlin and Gradle files
$files = Get-ChildItem -Recurse -Include "*.kt", "*.kts" | Where-Object { $_.FullName -notlike "*build*" -and $_.FullName -notlike "*\.gradle*" }

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match $oldPackage) {
        Write-Host "Updating: $($file.FullName)"
        $content = $content -replace [regex]::Escape($oldPackage), $newPackage
        Set-Content $file.FullName $content -NoNewline
    }
}

Write-Host "Package name update complete!"