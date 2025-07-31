// ================================
// Phase 3.3: AR Overlay Manager and Immersive UI Components
// ================================

// 9. Immersive UI Manager Implementation
class ImmersiveUIManager @Inject constructor(
    private val context: Context
) {
    
    private var currentUIMode: UIMode = UIMode.TRADITIONAL
    private var systemUIVisibility: Int = 0
    private val uiHandlers = mutableMapOf<UIMode, UIHandler>()
    
    fun initialize() {
        // Register UI handlers for each mode
        uiHandlers[UIMode.VR] = VRUIHandler()
        uiHandlers[UIMode.CARDBOARD] = CardboardUIHandler()
        uiHandlers[UIMode.SPHERICAL] = SphericalUIHandler()
        uiHandlers[UIMode.AR] = ARUIHandler()
        uiHandlers[UIMode.IMMERSIVE] = ImmersiveUIHandler()
        uiHandlers[UIMode.TRADITIONAL] = TraditionalUIHandler()
    }
    
    fun switchToVRMode() {
        switchUIMode(UIMode.VR)
    }
    
    fun switchToCardboardMode() {
        switchUIMode(UIMode.CARDBOARD)
    }
    
    fun switchToSphericalMode() {
        switchUIMode(UIMode.SPHERICAL)
    }
    
    fun switchToARMode() {
        switchUIMode(UIMode.AR)
    }
    
    fun switchToImmersiveMode() {
        switchUIMode(UIMode.IMMERSIVE)
    }
    
    fun switchToTraditionalMode() {
        switchUIMode(UIMode.TRADITIONAL)
    }
    
    private fun switchUIMode(newMode: UIMode) {
        // Cleanup current mode
        uiHandlers[currentUIMode]?.onExit()
        
        // Switch to new mode
        currentUIMode = newMode
        uiHandlers[newMode]?.onEnter()
    }
    
    // UI Mode Handlers
    private abstract class UIHandler {
        abstract fun onEnter()
        abstract fun onExit()
    }
    
    private inner class VRUIHandler : UIHandler() {
        override fun onEnter() {
            // Hide all traditional UI elements
            hideSystemUI()
            
            // Setup VR-specific UI elements
            // - Gaze-based selection
            // - Floating panels
            // - 3D controls
        }
        
        override fun onExit() {
            restoreSystemUI()
        }
    }
    
    private inner class CardboardUIHandler : UIHandler() {
        override fun onEnter() {
            hideSystemUI()
            
            // Setup Cardboard UI
            // - Split screen controls
            // - Simplified interface
            // - Large touch targets
        }
        
        override fun onExit() {
            restoreSystemUI()
        }
    }
    
    private inner class SphericalUIHandler : UIHandler() {
        override fun onEnter() {
            // Keep minimal UI
            hidePartialSystemUI()
            
            // Setup 360° controls
            // - Compass overlay
            // - Field of view indicator
            // - Navigation controls
        }
        
        override fun onExit() {
            restoreSystemUI()
        }
    }
    
    private inner class ARUIHandler : UIHandler() {
        override fun onEnter() {
            // Transparent UI mode
            makeUITransparent()
            
            // Setup AR controls
            // - Object placement tools
            // - Overlay management
            // - Camera controls
        }
        
        override fun onExit() {
            restoreUIOpacity()
        }
    }
    
    private inner class ImmersiveUIHandler : UIHandler() {
        override fun onEnter() {
            hideSystemUI()
            
            // Minimal, edge-activated UI
            setupEdgeActivatedControls()
        }
        
        override fun onExit() {
            restoreSystemUI()
        }
    }
    
    private inner class TraditionalUIHandler : UIHandler() {
        override fun onEnter() {
            restoreSystemUI()
        }
        
        override fun onExit() {
            // Nothing to cleanup
        }
    }
    
    private fun hideSystemUI() {
        if (context is Activity) {
            systemUIVisibility = context.window.decorView.systemUiVisibility
            
            context.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    private fun hidePartialSystemUI() {
        if (context is Activity) {
            context.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    private fun restoreSystemUI() {
        if (context is Activity) {
            context.window.decorView.systemUiVisibility = systemUIVisibility
        }
    }
    
    private fun makeUITransparent() {
        // Implementation for transparent UI in AR mode
    }
    
    private fun restoreUIOpacity() {
        // Restore normal UI opacity
    }
    
    private fun setupEdgeActivatedControls() {
        // Setup controls that appear when user touches screen edges
    }
    
    enum class UIMode {
        TRADITIONAL,
        VR,
        CARDBOARD,
        SPHERICAL,
        AR,
        IMMERSIVE
    }
}

// 10. Immersive Video Player UI Component
@Composable
fun ImmersiveVideoPlayerUI(
    viewModel: ImmersiveVideoViewModel,
    modifier: Modifier = Modifier
) {
    val immersiveState by viewModel.immersiveState.collectAsState()
    val viewingMode by viewModel.viewingMode.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        // Video surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.player
                    useController = false // We'll use custom controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay UI based on viewing mode
        when (viewingMode) {
            ViewingMode.VR_HEADSET -> VROverlayUI(viewModel)
            ViewingMode.VR_CARDBOARD -> CardboardOverlayUI(viewModel)
            ViewingMode.SPHERICAL_360 -> SphericalOverlayUI(viewModel)
            ViewingMode.AR_OVERLAY -> AROverlayUI(viewModel)
            ViewingMode.IMMERSIVE_FULLSCREEN -> ImmersiveOverlayUI(viewModel)
            ViewingMode.TRADITIONAL -> TraditionalControlsUI(viewModel)
        }
        
        // Mode switcher (always visible)
        ViewingModeSwitcher(
            currentMode = viewingMode,
            onModeSelected = { viewModel.switchViewingMode(it) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun VROverlayUI(viewModel: ImmersiveVideoViewModel) {
    // VR-specific UI elements
    Box(modifier = Modifier.fillMaxSize()) {
        // Gaze pointer in center
        GazePointer(
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Floating control panel
        if (viewModel.showControls.collectAsState().value) {
            FloatingControlPanel(
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seek(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}

@Composable
fun CardboardOverlayUI(viewModel: ImmersiveVideoViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left eye view
        Box(modifier = Modifier.weight(1f)) {
            CardboardEyeView(isLeftEye = true)
        }
        
        // Right eye view
        Box(modifier = Modifier.weight(1f)) {
            CardboardEyeView(isLeftEye = false)
        }
    }
}

@Composable
fun SphericalOverlayUI(viewModel: ImmersiveVideoViewModel) {
    val rotation by viewModel.sphericalRotation.collectAsState()
    val fov by viewModel.fieldOfView.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Compass indicator
        CompassOverlay(
            rotation = rotation,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )
        
        // FOV indicator
        FieldOfViewIndicator(
            fov = fov,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        
        // Navigation controls
        SphericalNavigationControls(
            onRotate = { viewModel.rotateSphere(it) },
            onZoom = { viewModel.adjustFieldOfView(it) },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun AROverlayUI(viewModel: ImmersiveVideoViewModel) {
    val arOverlays by viewModel.arOverlays.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // AR overlays
        arOverlays.forEach { overlay ->
            AROverlayItem(
                overlay = overlay,
                onUpdate = { viewModel.updateAROverlay(it) },
                onRemove = { viewModel.removeAROverlay(overlay.id) }
            )
        }
        
        // AR toolbar
        ARToolbar(
            onAddText = { viewModel.addTextOverlay() },
            onAddImage = { viewModel.addImageOverlay() },
            onToggleTracking = { viewModel.toggleARTracking() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun ImmersiveOverlayUI(viewModel: ImmersiveVideoViewModel) {
    val showControls by viewModel.showControls.collectAsState()
    
    // Edge-activated controls
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Show controls if tapped near edges
                    val edgeThreshold = 50.dp.toPx()
                    if (offset.x < edgeThreshold || 
                        offset.x > size.width - edgeThreshold ||
                        offset.y < edgeThreshold || 
                        offset.y > size.height - edgeThreshold) {
                        viewModel.toggleControlsVisibility()
                    }
                }
            }
    ) {
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            MinimalControls(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// Supporting UI Components
@Composable
fun ViewingModeSwitcher(
    currentMode: ViewingMode,
    onModeSelected: (ViewingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.background(
                Color.Black.copy(alpha = 0.5f),
                CircleShape
            )
        ) {
            Icon(
                painter = painterResource(currentMode.icon),
                contentDescription = "Switch viewing mode",
                tint = Color.White
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ViewingMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(mode.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.displayName)
                        }
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    leadingIcon = if (mode == currentMode) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun GazePointer(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(32.dp)
    ) {
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 16.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx()
        )
    }
}

@Composable
fun CompassOverlay(
    rotation: Quaternion,
    modifier: Modifier = Modifier
) {
    val yaw = atan2(
        2 * (rotation.w * rotation.z + rotation.x * rotation.y),
        1 - 2 * (rotation.y * rotation.y + rotation.z * rotation.z)
    ) * 180 / PI
    
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Navigation,
            contentDescription = "Compass",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = yaw.toFloat() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${yaw.toInt()}°",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

// Extension properties for ViewingMode
val ViewingMode.icon: Int
    get() = when (this) {
        ViewingMode.TRADITIONAL -> R.drawable.ic_traditional
        ViewingMode.VR_HEADSET -> R.drawable.ic_vr_headset
        ViewingMode.VR_CARDBOARD -> R.drawable.ic_cardboard
        ViewingMode.SPHERICAL_360 -> R.drawable.ic_360
        ViewingMode.AR_OVERLAY -> R.drawable.ic_ar
        ViewingMode.IMMERSIVE_FULLSCREEN -> R.drawable.ic_fullscreen
    }

val ViewingMode.displayName: String
    get() = when (this) {
        ViewingMode.TRADITIONAL -> "Traditional"
        ViewingMode.VR_HEADSET -> "VR Headset"
        ViewingMode.VR_CARDBOARD -> "Cardboard VR"
        ViewingMode.SPHERICAL_360 -> "360° Video"
        ViewingMode.AR_OVERLAY -> "AR Mode"
        ViewingMode.IMMERSIVE_FULLSCREEN -> "Immersive"
    }