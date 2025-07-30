# MX Player-Style Gesture Implementation - Final Version

## Gesture Zones Layout

The screen is divided into three vertical zones based on MX Player's standard implementation:
- **Left 40%**: Brightness control (vertical swipe)
- **Center 20%**: Seek control (horizontal swipe) 
- **Right 40%**: Volume control (vertical swipe)

## Implemented Gestures

### 1. Single Tap
- **Action**: Toggle player controls visibility
- **Zone**: Anywhere on screen

### 2. Double Tap
- **Left Half of Screen**: Rewind 10 seconds
- **Right Half of Screen**: Fast forward 10 seconds
- **Note**: Based on screen half, not the three zones

### 3. Long Press (MX Player Style)
- **Action**: Play at 2x speed while pressed
- **Release**: Returns to normal 1x speed
- **Zone**: Anywhere on screen
- **Use Case**: Quickly skip through boring parts
- **Duration**: 500ms to trigger

### 4. Vertical Swipes
- **Left Zone (0-40%)**: Brightness control
  - Swipe up: Increase brightness
  - Swipe down: Decrease brightness
- **Right Zone (60-100%)**: Volume control
  - Swipe up: Increase volume
  - Swipe down: Decrease volume

### 5. Horizontal Swipes
- **Center Zone (40-60%)**: Seek through video
  - Swipe right: Seek forward
  - Swipe left: Seek backward
  - Sensitivity: 30 seconds for full screen width
- **Strong Horizontal Swipe**: If horizontal movement is 2x vertical movement, treat as seek regardless of zone

### 6. Dead Zones
- **Top 8%**: Reserved for status bar
- **Bottom 8%**: Reserved for navigation bar

## Key Implementation Details

```kotlin
// Zone configuration
data class MxGestureZones(
    val deadZoneTop: Float = 0.08f,      // 8% top dead zone
    val deadZoneBottom: Float = 0.08f,   // 8% bottom dead zone
    val leftZoneWidth: Float = 0.4f,    // 40% for brightness
    val rightZoneWidth: Float = 0.4f,   // 40% for volume
    val centerZoneWidth: Float = 0.2f,  // 20% for seek
    val minimumSwipeDistance: Float = 20f,
    val doubleTapTimeout: Long = 300L,
    val longPressTimeout: Long = 500L
)
```

## Testing

Use the provided test script:
```bash
cd android
chmod +x test-mx-gestures.sh
./test-mx-gestures.sh
```

## Differences from Initial Implementation

1. **Long Press**: Changed from fast forward/rewind to 2x speed playback (true MX Player behavior)
2. **Zone Distribution**: Fixed to proper 40-20-40 distribution
3. **Dead Zones**: Increased to 8% for better system UI avoidance

## Known Issues

1. **Video Duration**: Currently showing 0, needs proper ExoPlayer state handling
2. **Visual Feedback**: Volume/brightness overlays need implementation
3. **Settings Integration**: Gesture sensitivity settings not yet connected

## References

- MX Player official features
- GitHub issue #568 on moneytoo/Player discussing long press 2x speed
- Android video player gesture implementations (VLC, Files by Google)