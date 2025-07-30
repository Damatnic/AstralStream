#!/bin/bash

echo "Testing improved MX Player-style gestures..."

# Clear logcat
adb logcat -c

# Wait for video to load
sleep 3

# Test 1: Long press on right side (fast forward)
echo "Test 1: Long press right for fast forward"
adb shell input swipe 700 1200 700 1200 1000
sleep 2

# Test 2: Long press on left side (rewind)
echo "Test 2: Long press left for rewind"
adb shell input swipe 200 1200 200 1200 1000
sleep 2

# Test 3: Horizontal swipe to seek
echo "Test 3: Horizontal swipe to seek forward"
adb shell input swipe 200 1200 600 1200 300
sleep 2

echo "Test 4: Horizontal swipe to seek backward"
adb shell input swipe 600 1200 200 1200 300
sleep 2

# Test 5: Volume control (not too close to edge)
echo "Test 5: Volume up (right side with padding)"
adb shell input swipe 700 1500 700 900 300
sleep 2

echo "Test 6: Volume down"
adb shell input swipe 700 900 700 1500 300
sleep 2

# Test 7: Brightness control (not too close to edge)
echo "Test 7: Brightness up (left side with padding)"
adb shell input swipe 200 1500 200 900 300
sleep 2

echo "Test 8: Brightness down"
adb shell input swipe 200 900 200 1500 300
sleep 2

# Test 9: Double tap seek
echo "Test 9: Double tap right"
adb shell input tap 700 1200
adb shell input tap 700 1200
sleep 2

# Test 10: Single tap for controls
echo "Test 10: Single tap to toggle controls"
adb shell input tap 450 1200
sleep 2

# Get logs
echo -e "\n=== Gesture Logs ==="
adb logcat -d | grep -E "(MxGesture|PlayerVM)" | tail -100