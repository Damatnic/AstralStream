#!/bin/bash

echo "========================================"
echo "Astral Player Automated Test Suite"
echo "========================================"
echo

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No Android device connected!"
    echo "Please connect a device or start an emulator."
    exit 1
fi

echo "Building and running tests..."
echo

# Clean build
echo "[1/4] Cleaning previous builds..."
./gradlew clean

# Build the app
echo
echo "[2/4] Building debug APK..."
./gradlew assembleDebug

# Build test APK
echo
echo "[3/4] Building test APK..."
./gradlew assembleDebugAndroidTest

# Run tests
echo
echo "[4/4] Running automated tests..."
./gradlew connectedDebugAndroidTest

# Generate report
echo
echo "========================================"
echo "Test Results:"
echo "========================================"

# Show summary
if [ -f "app/build/reports/androidTests/connected/index.html" ]; then
    grep -E "(passed|failed)" app/build/reports/androidTests/connected/index.html || echo "Could not parse test results"
fi

echo
echo "Full test report available at:"
echo "app/build/reports/androidTests/connected/index.html"
echo
echo "Test execution completed!"