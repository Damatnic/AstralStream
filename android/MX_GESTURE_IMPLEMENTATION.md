# MX Player-Style Gesture Implementation

## Zone Layout (Standard MX Player Pattern)

The screen is divided into three vertical zones:
- **Left 40%**: Brightness control (vertical swipe)
- **Center 20%**: Seek control (horizontal swipe) 
- **Right 40%**: Volume control (vertical swipe)

## Gesture Controls

### 1. Single Tap
- **Action**: Toggle player controls visibility
- **Zone**: Anywhere on screen

### 2. Double Tap
- **Left Half**: Rewind 10 seconds
- **Right Half**: Fast forward 10 seconds
- **Implementation**: Based on screen half, not zones

### 3. Long Press
- **Left Half**: Fast rewind (2x speed initially)
- **Right Half**: Fast forward (2x speed initially)
- **Duration**: 500ms to trigger

### 4. Vertical Swipes
- **Left Zone (0-40%)**: Adjust brightness
  - Swipe up: Increase brightness
  - Swipe down: Decrease brightness
- **Right Zone (60-100%)**: Adjust volume
  - Swipe up: Increase volume
  - Swipe down: Decrease volume

### 5. Horizontal Swipes
- **Center Zone (40-60%)**: Seek through video
  - Swipe right: Seek forward
  - Swipe left: Seek backward
- **Anywhere (strong swipe)**: If horizontal movement is 2x vertical, treat as seek

## Dead Zones
- **Top 8%**: Reserved for status bar
- **Bottom 8%**: Reserved for navigation bar

## Implementation Details

```kotlin
data class MxGestureZones(
    val deadZoneTop: Float = 0.08f,
    val deadZoneBottom: Float = 0.08f,
    val leftZoneWidth: Float = 0.4f,    // 40%
    val rightZoneWidth: Float = 0.4f,   // 40%
    val centerZoneWidth: Float = 0.2f,  // 20%
    val minimumSwipeDistance: Float = 20f,
    val doubleTapTimeout: Long = 300L,
    val longPressTimeout: Long = 500L
)
```

## Testing Commands

Use the `test-mx-gestures.sh` script to test all gestures:
```bash
chmod +x test-mx-gestures.sh
./test-mx-gestures.sh
```

## Known Issues to Fix

1. **Video Duration**: Currently showing 0, needs proper ExoPlayer state handling
2. **Visual Feedback**: Need to implement overlay indicators for volume/brightness changes
3. **Gesture Sensitivity**: May need tuning based on device screen size

## References

Based on research of MX Player and similar video player implementations:
- VLC for Android uses similar zones
- Files by Google adopted MX Player-style gestures
- YouTube Vanced also implements similar gesture patterns