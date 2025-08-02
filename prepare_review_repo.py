#!/usr/bin/env python3
"""
Prepare review repository with optimized file structure for Claude web
"""

import os
import shutil
import json
from pathlib import Path

def copy_source_files():
    """Copy relevant source files to review repository"""
    
    review_repo = Path("review-repo")
    review_repo.mkdir(exist_ok=True)
    
    # Create directory structure
    src_dir = review_repo / "src"
    src_dir.mkdir(exist_ok=True)
    
    # Define source paths to copy
    source_paths = [
        ("../../Aplay/android/app/src/main/java", "android/java"),
        ("../../Aplay/android/app/src/main/res", "android/res"),
        ("../../Aplay/android/app/build.gradle", "android/build.gradle"),
        ("../../Aplay/android/app/src/main/AndroidManifest.xml", "android/AndroidManifest.xml"),
    ]
    
    # Copy files
    for src, dest in source_paths:
        src_path = Path(src)
        dest_path = src_dir / dest
        
        if src_path.exists():
            if src_path.is_file():
                dest_path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src_path, dest_path)
                print(f"Copied: {src} -> {dest}")
            elif src_path.is_dir():
                if dest_path.exists():
                    shutil.rmtree(dest_path)
                shutil.copytree(src_path, dest_path)
                print(f"Copied directory: {src} -> {dest}")
    
    # Create README for review repo
    readme_content = """# AstralStream Code Review Repository

This repository contains the source code for AstralStream, optimized for review in Claude web.

## Structure

- `src/android/` - Android application source code
  - `java/` - Kotlin/Java source files
  - `res/` - Android resources
  - `build.gradle` - Build configuration
  - `AndroidManifest.xml` - App manifest

## Purpose

This repository is specifically organized to make code review easier with AI tools that have file size limitations.

## Main Repository

The full project repository is available at: https://github.com/Damatnic/AstralStream
"""
    
    (review_repo / "README.md").write_text(readme_content)
    print("Created README.md")
    
    # Create .gitignore
    gitignore_content = """.DS_Store
Thumbs.db
*.log
"""
    (review_repo / ".gitignore").write_text(gitignore_content)
    print("Created .gitignore")

if __name__ == "__main__":
    copy_source_files()
    print("\nReview repository prepared successfully!")