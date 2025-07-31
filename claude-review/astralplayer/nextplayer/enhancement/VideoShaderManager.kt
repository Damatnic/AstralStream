package com.astralplayer.nextplayer.enhancement

import android.opengl.GLES30
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages OpenGL shaders for video enhancement
 */
@Singleton
class VideoShaderManager @Inject constructor() {
    
    private val shaderCache = mutableMapOf<ShaderCacheKey, Int>()
    
    /**
     * Load and compile a shader program
     */
    fun loadProgram(
        type: SmartVideoEnhancementEngine.ShaderType,
        vertexShaderSource: String,
        fragmentShaderSource: String
    ): Int {
        val cacheKey = ShaderCacheKey(type, vertexShaderSource.hashCode(), fragmentShaderSource.hashCode())
        
        // Check cache first
        shaderCache[cacheKey]?.let { return it }
        
        // Compile vertex shader
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        if (vertexShader == 0) {
            Log.e(TAG, "Failed to compile vertex shader for $type")
            return 0
        }
        
        // Compile fragment shader
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)
        if (fragmentShader == 0) {
            Log.e(TAG, "Failed to compile fragment shader for $type")
            GLES30.glDeleteShader(vertexShader)
            return 0
        }
        
        // Link program
        val program = GLES30.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Failed to create program for $type")
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }
        
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        
        // Bind attribute locations
        GLES30.glBindAttribLocation(program, 0, "aPosition")
        GLES30.glBindAttribLocation(program, 1, "aTexCoord")
        
        GLES30.glLinkProgram(program)
        
        // Check link status
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        
        if (linkStatus[0] == 0) {
            val infoLog = GLES30.glGetProgramInfoLog(program)
            Log.e(TAG, "Failed to link program for $type: $infoLog")
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }
        
        // Clean up shaders (they're linked into the program now)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        
        // Cache the program
        shaderCache[cacheKey] = program
        
        Log.i(TAG, "Successfully created shader program for $type")
        return program
    }
    
    /**
     * Compile a single shader
     */
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Failed to create shader of type $type")
            return 0
        }
        
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        
        // Check compile status
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        
        if (compileStatus[0] == 0) {
            val infoLog = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "Failed to compile shader: $infoLog")
            GLES30.glDeleteShader(shader)
            return 0
        }
        
        return shader
    }
    
    /**
     * Set uniform values
     */
    fun setUniform1f(program: Int, name: String, value: Float) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniform1f(location, value)
        }
    }
    
    fun setUniform2f(program: Int, name: String, x: Float, y: Float) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniform2f(location, x, y)
        }
    }
    
    fun setUniform3f(program: Int, name: String, x: Float, y: Float, z: Float) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniform3f(location, x, y, z)
        }
    }
    
    fun setUniform4f(program: Int, name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniform4f(location, x, y, z, w)
        }
    }
    
    fun setUniform1i(program: Int, name: String, value: Int) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniform1i(location, value)
        }
    }
    
    fun setUniformMatrix4fv(program: Int, name: String, matrix: FloatArray) {
        val location = GLES30.glGetUniformLocation(program, name)
        if (location >= 0) {
            GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0)
        }
    }
    
    /**
     * Release all cached shaders
     */
    fun release() {
        shaderCache.values.forEach { program ->
            GLES30.glDeleteProgram(program)
        }
        shaderCache.clear()
    }
    
    /**
     * Validate a shader program
     */
    fun validateProgram(program: Int): Boolean {
        GLES30.glValidateProgram(program)
        
        val validateStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_VALIDATE_STATUS, validateStatus, 0)
        
        if (validateStatus[0] == 0) {
            val infoLog = GLES30.glGetProgramInfoLog(program)
            Log.e(TAG, "Program validation failed: $infoLog")
            return false
        }
        
        return true
    }
    
    /**
     * Get active uniforms in a program
     */
    fun getActiveUniforms(program: Int): List<UniformInfo> {
        val uniforms = mutableListOf<UniformInfo>()
        
        val numUniforms = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_ACTIVE_UNIFORMS, numUniforms, 0)
        
        for (i in 0 until numUniforms[0]) {
            val nameBuffer = ByteArray(256)
            val length = IntArray(1)
            val size = IntArray(1)
            val type = IntArray(1)
            
            GLES30.glGetActiveUniform(
                program, i,
                nameBuffer.size, length, 0,
                size, 0,
                type, 0,
                nameBuffer, 0
            )
            
            val name = String(nameBuffer, 0, length[0])
            val location = GLES30.glGetUniformLocation(program, name)
            
            uniforms.add(
                UniformInfo(
                    name = name,
                    location = location,
                    type = type[0],
                    size = size[0]
                )
            )
        }
        
        return uniforms
    }
    
    companion object {
        private const val TAG = "VideoShaderManager"
    }
    
    /**
     * Cache key for shader programs
     */
    private data class ShaderCacheKey(
        val type: SmartVideoEnhancementEngine.ShaderType,
        val vertexHash: Int,
        val fragmentHash: Int
    )
    
    /**
     * Information about a uniform variable
     */
    data class UniformInfo(
        val name: String,
        val location: Int,
        val type: Int,
        val size: Int
    )
}