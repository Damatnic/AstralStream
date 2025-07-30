# PowerShell script to move all files to new package structure

$modules = @(
    "core\common",
    "core\data", 
    "core\database",
    "core\datastore",
    "core\domain",
    "core\media",
    "core\model",
    "feature\player",
    "feature\settings", 
    "feature\videopicker"
)

foreach ($module in $modules) {
    $oldPath = "$module\src\main\java\dev\anilbeesetti\nextplayer"
    $newPath = "$module\src\main\java\com\astralplayer\astralstream"
    
    if (Test-Path $oldPath) {
        Write-Host "Moving $module..."
        
        # Create new directory structure
        New-Item -ItemType Directory -Path $newPath -Force | Out-Null
        
        # Move files
        robocopy $oldPath $newPath /E /MOVE | Out-Null
        
        Write-Host "Moved $module successfully"
    }
}

Write-Host "All packages moved to new structure!"