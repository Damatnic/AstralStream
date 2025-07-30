#!/bin/bash

echo "Testing MX Player-style gestures with proper zones..."
echo "Screen zones: Left 40% (Brightness), Center 20% (Seek), Right 40% (Volume)"

# Clear logcat
adb logcat -c

# Wait for video to load
sleep 3

# Get screen dimensions
SCREEN_WIDTH=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | cut -d'x' -f1)
SCREEN_HEIGHT=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | cut -d'x' -f2)

# Calculate zone positions
LEFT_ZONE_X=$((SCREEN_WIDTH * 20 / 100))     # 20% (middle of left 40%)
CENTER_ZONE_X=$((SCREEN_WIDTH * 50 / 100))   # 50% (middle of center)
RIGHT_ZONE_X=$((SCREEN_WIDTH * 80 / 100))    # 80% (middle of right 40%)
MIDDLE_Y=$((SCREEN_HEIGHT * 50 / 100))       # 50% height

echo "Screen: ${SCREEN_WIDTH}x${SCREEN_HEIGHT}"
echo "Left zone X: $LEFT_ZONE_X, Center X: $CENTER_ZONE_X, Right zone X: $RIGHT_ZONE_X"

# Test 1: Single tap to toggle controls
echo -e "\nTest 1: Single tap to toggle controls"
adb shell input tap $CENTER_ZONE_X $MIDDLE_Y
sleep 2

# Test 2: Double tap left half (rewind)
echo -e "\nTest 2: Double tap left half (rewind 10s)"
adb shell input tap $LEFT_ZONE_X $MIDDLE_Y
adb shell input tap $LEFT_ZONE_X $MIDDLE_Y
sleep 2

# Test 3: Double tap right half (forward)
echo -e "\nTest 3: Double tap right half (forward 10s)"
adb shell input tap $RIGHT_ZONE_X $MIDDLE_Y
adb shell input tap $RIGHT_ZONE_X $MIDDLE_Y
sleep 2

# Test 4: Long press left half (fast rewind)
echo -e "\nTest 4: Long press left half (fast rewind)"
adb shell input swipe $LEFT_ZONE_X $MIDDLE_Y $LEFT_ZONE_X $MIDDLE_Y 1000
sleep 2

# Test 5: Long press right half (fast forward)
echo -e "\nTest 5: Long press right half (fast forward)"
adb shell input swipe $RIGHT_ZONE_X $MIDDLE_Y $RIGHT_ZONE_X $MIDDLE_Y 1000
sleep 2

# Test 6: Brightness control (left zone vertical swipe)
echo -e "\nTest 6: Brightness up (left zone)"
adb shell input swipe $LEFT_ZONE_X $((MIDDLE_Y + 200)) $LEFT_ZONE_X $((MIDDLE_Y - 200)) 300
sleep 1

echo "Test 7: Brightness down (left zone)"
adb shell input swipe $LEFT_ZONE_X $((MIDDLE_Y - 200)) $LEFT_ZONE_X $((MIDDLE_Y + 200)) 300
sleep 2

# Test 8: Volume control (right zone vertical swipe)
echo -e "\nTest 8: Volume up (right zone)"
adb shell input swipe $RIGHT_ZONE_X $((MIDDLE_Y + 200)) $RIGHT_ZONE_X $((MIDDLE_Y - 200)) 300
sleep 1

echo "Test 9: Volume down (right zone)"
adb shell input swipe $RIGHT_ZONE_X $((MIDDLE_Y - 200)) $RIGHT_ZONE_X $((MIDDLE_Y + 200)) 300
sleep 2

# Test 10: Seek control (center zone horizontal swipe)
echo -e "\nTest 10: Seek forward (center zone)"
adb shell input swipe $((CENTER_ZONE_X - 100)) $MIDDLE_Y $((CENTER_ZONE_X + 100)) $MIDDLE_Y 300
sleep 2

echo "Test 11: Seek backward (center zone)"
adb shell input swipe $((CENTER_ZONE_X + 100)) $MIDDLE_Y $((CENTER_ZONE_X - 100)) $MIDDLE_Y 300
sleep 2

# Test 12: Horizontal swipe anywhere (strong swipe)
echo -e "\nTest 12: Strong horizontal swipe (seek)"
adb shell input swipe 100 $MIDDLE_Y $((SCREEN_WIDTH - 100)) $MIDDLE_Y 200
sleep 2

# Get logs
echo -e "\n=== Gesture Logs ==="
adb logcat -d | grep -E "(MxGesture|PlayerVM)" | tail -50