package com.astralplayer.nextplayer.immersive

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Handles spherical video processing and 360° video projection
 */
@Singleton
class SphericalVideoProcessor @Inject constructor(
    private val context: Context
) {
    
    private var isInitialized = false
    private var sphericalRenderer: SphericalRenderer? = null
    private var currentProjection: SphericalProjection? = null
    private var isProjectionEnabled = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            sphericalRenderer = SphericalRenderer(context)
            sphericalRenderer?.initialize()
            isInitialized = true
            Log.i("SphericalProcessor", "Spherical video processor initialized")
            true
        } catch (e: Exception) {
            Log.e("SphericalProcessor", "Failed to initialize spherical processor", e)
            false
        }
    }
    
    suspend fun enableSphericalProjection(projection: SphericalProjection) = withContext(Dispatchers.Main) {
        if (!isInitialized) {
            Log.w("SphericalProcessor", "Processor not initialized")
            return@withContext
        }
        
        currentProjection = projection
        sphericalRenderer?.setupProjection(projection)
        isProjectionEnabled = true
        
        Log.i("SphericalProcessor", "Enabled spherical projection: $projection")
    }
    
    suspend fun disableProjection() = withContext(Dispatchers.Main) {
        sphericalRenderer?.disableProjection()
        isProjectionEnabled = false
        currentProjection = null
        Log.i("SphericalProcessor", "Disabled spherical projection")
    }
    
    suspend fun updateViewDirection(rotation: Quaternion) = withContext(Dispatchers.Main) {
        if (isProjectionEnabled) {
            sphericalRenderer?.updateViewDirection(rotation)
        }
    }
    
    suspend fun setFieldOfView(fov: Float) = withContext(Dispatchers.Main) {
        if (isProjectionEnabled) {
            sphericalRenderer?.setFieldOfView(fov)
        }
    }
    
    suspend fun captureCurrentView(): ByteArray = withContext(Dispatchers.IO) {
        sphericalRenderer?.captureCurrentView() ?: ByteArray(0)
    }
    
    fun getCurrentFOV(): Float = sphericalRenderer?.getCurrentFOV() ?: 90f
    
    fun getCurrentViewDirection(): Quaternion = sphericalRenderer?.getCurrentViewDirection() ?: Quaternion.identity()
    
    fun isSphericalSupported(): Boolean = isInitialized
    
    fun isProjectionEnabled(): Boolean = isProjectionEnabled
    
    fun getCurrentProjection(): SphericalProjection? = currentProjection
    
    fun cleanup() {
        try {
            sphericalRenderer?.cleanup()
            sphericalRenderer = null
            isInitialized = false
            isProjectionEnabled = false
            currentProjection = null
            Log.i("SphericalProcessor", "Spherical video processor cleaned up")
        } catch (e: Exception) {
            Log.e("SphericalProcessor", "Error during cleanup", e)
        }
    }
}

/**
 * Enhanced Spherical Renderer Implementation
 */
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