#!/bin/bash

echo "Testing MX Player-style gestures..."

# Clear logcat
adb logcat -c

# Test 1: Single tap in center (should toggle controls)
echo "Test 1: Single tap to toggle controls"
adb shell input tap 450 1200
sleep 2

# Test 2: Double tap left (should seek backward)
echo "Test 2: Double tap left to seek backward"
adb shell input tap 200 1200
adb shell input tap 200 1200
sleep 2

# Test 3: Double tap right (should seek forward)
echo "Test 3: Double tap right to seek forward"
adb shell input tap 700 1200
adb shell input tap 700 1200
sleep 2

# Test 4: Swipe left side up/down (brightness)
echo "Test 4: Swipe left for brightness"
adb shell input swipe 150 1500 150 800 300
sleep 2

# Test 5: Swipe right side up/down (volume)
echo "Test 5: Swipe right for volume"
adb shell input swipe 750 1500 750 800 300
sleep 2

# Test 6: Horizontal swipe in center (seek)
echo "Test 6: Horizontal swipe for seek"
adb shell input swipe 300 1200 600 1200 300
sleep 2

# Get logs
echo -e "\n=== Gesture Logs ==="
adb logcat -d | grep -E "(MxGesture|PlayerVM)" | tail -50