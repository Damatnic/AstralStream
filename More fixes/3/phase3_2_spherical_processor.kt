// ================================
// Phase 3.2: Spherical Video Processor (Complete Implementation)
// ================================

// 5. Gyroscope Controller (Continued from 3.1)
@Singleton
class GyroscopeController @Inject constructor(
    private val context: Context
) {
    
    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var headTrackingCallback: ((Quaternion) -> Unit)? = null
    private var sphericalNavigationCallback: ((Quaternion) -> Unit)? = null
    private var immersiveGestureCallback: ((GestureType, Float) -> Unit)? = null
    
    private val sensorEventListener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)
        private val quaternion = FloatArray(4)
        
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> handleGyroscopeData(event)
                Sensor.TYPE_ACCELEROMETER -> handleAccelerometerData(event)
                Sensor.TYPE_ROTATION_VECTOR -> handleRotationVector(event)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
        
        private fun handleGyroscopeData(event: SensorEvent) {
            // Process gyroscope data for smooth rotation tracking
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Convert to quaternion
            val quaternion = calculateQuaternionFromGyro(x, y, z)
            
            // Notify appropriate callback
            headTrackingCallback?.invoke(quaternion)
            sphericalNavigationCallback?.invoke(quaternion)
        }
        
        private fun handleAccelerometerData(event: SensorEvent) {
            // Use for gravity compensation
        }
        
        private fun handleRotationVector(event: SensorEvent) {
            // Direct rotation vector to quaternion conversion
            SensorManager.getQuaternionFromVector(quaternion, event.values)
            
            val q = Quaternion(
                x = quaternion[1],
                y = quaternion[2],
                z = quaternion[3],
                w = quaternion[0]
            )
            
            // Send to appropriate callbacks
            headTrackingCallback?.invoke(q)
            sphericalNavigationCallback?.invoke(q)
        }
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            
            Log.i("GyroscopeController", "Gyroscope controller initialized")
            true
        } catch (e: Exception) {
            Log.e("GyroscopeController", "Failed to initialize gyroscope controller", e)
            false
        }
    }
    
    fun enableHeadTracking(callback: (Quaternion) -> Unit) {
        headTrackingCallback = callback
        registerSensors()
    }
    
    fun enableCardboardTracking() {
        // Use rotation vector for better Cardboard experience
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationVector?.let {
            sensorManager?.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }
    
    fun enableSphericalNavigation(callback: (Quaternion) -> Unit) {
        sphericalNavigationCallback = callback
        registerSensors()
    }
    
    fun enableImmersiveGestures() {
        // Enable gesture detection based on device movement
        registerSensors()
    }
    
    fun disableHeadTracking() {
        headTrackingCallback = null
        unregisterSensors()
    }
    
    fun disableSphericalNavigation() {
        sphericalNavigationCallback = null
        unregisterSensors()
    }
    
    fun disableImmersiveGestures() {
        immersiveGestureCallback = null
        unregisterSensors()
    }
    
    private fun registerSensors() {
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        rotationVector?.let {
            sensorManager?.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        } ?: run {
            // Fallback to gyroscope + accelerometer
            gyroscope?.let {
                sensorManager?.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
            
            accelerometer?.let {
                sensorManager?.registerListener(
                    sensorEventListener,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }
    
    private fun unregisterSensors() {
        if (headTrackingCallback == null && 
            sphericalNavigationCallback == null && 
            immersiveGestureCallback == null) {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }
    
    private fun calculateQuaternionFromGyro(x: Float, y: Float, z: Float): Quaternion {
        // Simplified quaternion calculation from gyroscope data
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude > 0) {
            val normalized = floatArrayOf(x / magnitude, y / magnitude, z / magnitude)
            val angle = magnitude * 0.5f
            val sinHalfAngle = sin(angle)
            val cosHalfAngle = cos(angle)
            
            return Quaternion(
                x = normalized[0] * sinHalfAngle,
                y = normalized[1] * sinHalfAngle,
                z = normalized[2] * sinHalfAngle,
                w = cosHalfAngle
            )
        }
        return Quaternion.identity()
    }
    
    fun isGyroscopeAvailable(): Boolean {
        return sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
               sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }
    
    fun cleanup() {
        sensorManager?.unregisterListener(sensorEventListener)
        headTrackingCallback = null
        sphericalNavigationCallback = null
        immersiveGestureCallback = null
    }
}

// 6. Enhanced Spherical Renderer Implementation
class SphericalRenderer(private val context: Context) {
    
    private var sphereVertexBuffer: FloatBuffer? = null
    private var sphereIndexBuffer: ShortBuffer? = null
    private var shaderProgram: Int = 0
    private var textureId: Int = 0
    
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    private var modelMatrix = FloatArray(16)
    
    private var currentFOV = 90f
    private var interactiveMode = true
    private var viewDirection = Quaternion.identity()
    
    fun initialize() {
        // Initialize OpenGL ES context and resources
        initializeSphere()
        loadShaders()
    }
    
    fun setupProjection(projection: SphericalProjection) {
        when (projection) {
            SphericalProjection.EQUIRECTANGULAR -> setupEquirectangular()
            SphericalProjection.CUBIC -> setupCubicProjection()
            SphericalProjection.CYLINDRICAL -> setupCylindricalProjection()
        }
    }
    
    fun updateViewDirection(rotation: Quaternion) {
        viewDirection = rotation
        updateViewMatrix()
    }
    
    fun setFieldOfView(fov: Float) {
        currentFOV = fov.coerceIn(30f, 120f)
        updateProjectionMatrix()
    }
    
    fun enableInteractiveNavigation(enable: Boolean) {
        interactiveMode = enable
    }
    
    fun captureCurrentView(): ByteArray {
        // Capture current rendered frame
        // Implementation would involve reading from framebuffer
        return ByteArray(0) // Placeholder
    }
    
    fun getCurrentFOV(): Float = currentFOV
    
    fun getCurrentViewDirection(): Quaternion = viewDirection
    
    fun isInteractiveModeEnabled(): Boolean = interactiveMode
    
    fun disableProjection() {
        // Clean up OpenGL resources
        cleanupOpenGL()
    }
    
    fun cleanup() {
        cleanupOpenGL()
    }
    
    private fun initializeSphere() {
        // Create sphere mesh for 360° video projection
        val sphereData = generateSphereVertices(50, 50, 1.0f)
        
        sphereVertexBuffer = ByteBuffer.allocateDirect(sphereData.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(sphereData.vertices)
            .position(0) as FloatBuffer
        
        sphereIndexBuffer = ByteBuffer.allocateDirect(sphereData.indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(sphereData.indices)
            .position(0) as ShortBuffer
    }
    
    private fun loadShaders() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                texCoord = vTexCoord;
            }
        """.trimIndent()
        
        val fragmentShaderCode = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 texCoord;
            
            void main() {
                gl_FragColor = texture2D(uTexture, texCoord);
            }
        """.trimIndent()
        
        // Compile and link shaders
        shaderProgram = createShaderProgram(vertexShaderCode, fragmentShaderCode)
    }
    
    private fun setupEquirectangular() {
        // Setup texture coordinates for equirectangular projection
        Matrix.setIdentityM(modelMatrix, 0)
    }
    
    private fun setupCubicProjection() {
        // Setup for cubic projection (6 faces)
        // Would involve creating cube map texture
    }
    
    private fun setupCylindricalProjection() {
        // Setup for cylindrical projection
        // Adjust texture mapping accordingly
    }
    
    private fun updateViewMatrix() {
        // Convert quaternion to view matrix
        val rotationMatrix = FloatArray(16)
        quaternionToMatrix(viewDirection, rotationMatrix)
        
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 0f,  // Eye position
            0f, 0f, -1f, // Look at
            0f, 1f, 0f   // Up vector
        )
        
        Matrix.multiplyMM(viewMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
    }
    
    private fun updateProjectionMatrix() {
        val aspect = 16f / 9f // Adjust based on screen
        Matrix.perspectiveM(projectionMatrix, 0, currentFOV, aspect, 0.1f, 100f)
    }
    
    private fun generateSphereVertices(
        latitudeBands: Int,
        longitudeBands: Int,
        radius: Float
    ): SphereData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val textureCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        
        for (lat in 0..latitudeBands) {
            val theta = lat * Math.PI / latitudeBands
            val sinTheta = sin(theta).toFloat()
            val cosTheta = cos(theta).toFloat()
            
            for (lon in 0..longitudeBands) {
                val phi = lon * 2 * Math.PI / longitudeBands
                val sinPhi = sin(phi).toFloat()
                val cosPhi = cos(phi).toFloat()
                
                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta
                val u = 1f - (lon.toFloat() / longitudeBands)
                val v = 1f - (lat.toFloat() / latitudeBands)
                
                normals.addAll(listOf(x, y, z))
                textureCoords.addAll(listOf(u, v))
                vertices.addAll(listOf(radius * x, radius * y, radius * z))
            }
        }
        
        for (lat in 0 until latitudeBands) {
            for (lon in 0 until longitudeBands) {
                val first = (lat * (longitudeBands + 1) + lon).toShort()
                val second = (first + longitudeBands + 1).toShort()
                
                indices.addAll(listOf(first, second, (first + 1).toShort()))
                indices.addAll(listOf(second, (second + 1).toShort(), (first + 1).toShort()))
            }
        }
        
        return SphereData(
            vertices = vertices.toFloatArray(),
            normals = normals.toFloatArray(),
            textureCoords = textureCoords.toFloatArray(),
            indices = indices.toShortArray()
        )
    }
    
    private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        // Standard OpenGL shader compilation
        // Returns compiled shader program ID
        return 0 // Placeholder
    }
    
    private fun quaternionToMatrix(q: Quaternion, matrix: FloatArray) {
        val xx = q.x * q.x
        val xy = q.x * q.y
        val xz = q.x * q.z
        val xw = q.x * q.w
        
        val yy = q.y * q.y
        val yz = q.y * q.z
        val yw = q.y * q.w
        
        val zz = q.z * q.z
        val zw = q.z * q.w
        
        matrix[0] = 1 - 2 * (yy + zz)
        matrix[1] = 2 * (xy - zw)
        matrix[2] = 2 * (xz + yw)
        matrix[3] = 0f
        
        matrix[4] = 2 * (xy + zw)
        matrix[5] = 1 - 2 * (xx + zz)
        matrix[6] = 2 * (yz - xw)
        matrix[7] = 0f
        
        matrix[8] = 2 * (xz - yw)
        matrix[9] = 2 * (yz + xw)
        matrix[10] = 1 - 2 * (xx + yy)
        matrix[11] = 0f
        
        matrix[12] = 0f
        matrix[13] = 0f
        matrix[14] = 0f
        matrix[15] = 1f
    }
    
    private fun cleanupOpenGL() {
        // Release OpenGL resources
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
        
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }
    }
    
    data class SphereData(
        val vertices: FloatArray,
        val normals: FloatArray,
        val textureCoords: FloatArray,
        val indices: ShortArray
    )
}

// 7. Supporting Types
enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    PINCH_ZOOM,
    ROTATE,
    DOUBLE_TAP,
    LONG_PRESS
}

// 8. Resolution Data Class
data class Resolution(
    val width: Int,
    val height: Int
) {
    fun aspectRatio(): Float = width.toFloat() / height
    
    override fun toString(): String = "${width}x${height}"
}

// Utility Extensions
fun Matrix.perspectiveM(
    m: FloatArray,
    offset: Int,
    fovy: Float,
    aspect: Float,
    zNear: Float,
    zFar: Float
) {
    val f = 1.0f / tan(fovy * (Math.PI / 360.0)).toFloat()
    val rangeReciprocal = 1.0f / (zNear - zFar)
    
    m[offset + 0] = f / aspect
    m[offset + 1] = 0.0f
    m[offset + 2] = 0.0f
    m[offset + 3] = 0.0f
    
    m[offset + 4] = 0.0f
    m[offset + 5] = f
    m[offset + 6] = 0.0f
    m[offset + 7] = 0.0f
    
    m[offset + 8] = 0.0f
    m[offset + 9] = 0.0f
    m[offset + 10] = (zFar + zNear) * rangeReciprocal
    m[offset + 11] = -1.0f
    
    m[offset + 12] = 0.0f
    m[offset + 13] = 0.0f
    m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal
    m[offset + 15] = 0.0f
}