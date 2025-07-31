package com.astralplayer.nextplayer.enhancement

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AI models for video enhancement
 */
@Singleton
class AIModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val modelCache = mutableMapOf<String, Interpreter>()
    private val compatibilityList = CompatibilityList()
    
    /**
     * Load a TensorFlow Lite model
     */
    suspend fun loadModel(modelName: String, options: Interpreter.Options? = null): Interpreter {
        return withContext(Dispatchers.IO) {
            // Check cache first
            modelCache[modelName]?.let { 
                Log.i(TAG, "Returning cached model: $modelName")
                return@withContext it 
            }
            
            try {
                // Load model buffer
                val modelBuffer = loadModelFile(modelName)
                
                // Create interpreter with options
                val interpreterOptions = options ?: createDefaultOptions()
                val interpreter = Interpreter(modelBuffer, interpreterOptions)
                
                // Cache the interpreter
                modelCache[modelName] = interpreter
                
                Log.i(TAG, "Successfully loaded model: $modelName")
                interpreter
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model $modelName: ${e.message}")
                throw ModelLoadException("Failed to load model: $modelName", e)
            }
        }
    }
    
    /**
     * Create default interpreter options with GPU support if available
     */
    private fun createDefaultOptions(): Interpreter.Options {
        val options = Interpreter.Options()
        
        // Check GPU compatibility
        if (compatibilityList.isDelegateSupportedOnThisDevice) {
            try {
                val gpuDelegateOptions = compatibilityList.bestOptionsForThisDevice
                val gpuDelegate = GpuDelegate(gpuDelegateOptions)
                options.addDelegate(gpuDelegate)
                Log.i(TAG, "GPU acceleration enabled")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable GPU acceleration: ${e.message}")
            }
        } else {
            Log.i(TAG, "GPU not supported, using CPU")
        }
        
        // Set thread count for CPU execution
        options.setNumThreads(Runtime.getRuntime().availableProcessors())
        
        // Enable NNAPI if available
        options.setUseNNAPI(true)
        
        return options
    }
    
    /**
     * Load model file from assets
     */
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("models/$modelName")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Get model input shape
     */
    fun getInputShape(interpreter: Interpreter): IntArray {
        return interpreter.getInputTensor(0).shape()
    }
    
    /**
     * Get model output shape
     */
    fun getOutputShape(interpreter: Interpreter): IntArray {
        return interpreter.getOutputTensor(0).shape()
    }
    
    /**
     * Resize model input
     */
    fun resizeInput(interpreter: Interpreter, inputShape: IntArray) {
        interpreter.resizeInput(0, inputShape)
    }
    
    /**
     * Run inference with progress callback
     */
    suspend fun runInference(
        interpreter: Interpreter,
        input: Any,
        output: Any,
        onProgress: ((Float) -> Unit)? = null
    ) {
        withContext(Dispatchers.Default) {
            try {
                onProgress?.invoke(0f)
                interpreter.run(input, output)
                onProgress?.invoke(1f)
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed: ${e.message}")
                throw InferenceException("Inference failed", e)
            }
        }
    }
    
    /**
     * Batch inference for multiple inputs
     */
    suspend fun runBatchInference(
        interpreter: Interpreter,
        inputs: List<Any>,
        outputs: List<Any>,
        onProgress: ((Float) -> Unit)? = null
    ) {
        withContext(Dispatchers.Default) {
            inputs.forEachIndexed { index, input ->
                try {
                    interpreter.run(input, outputs[index])
                    onProgress?.invoke((index + 1).toFloat() / inputs.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Batch inference failed at index $index: ${e.message}")
                    throw InferenceException("Batch inference failed at index $index", e)
                }
            }
        }
    }
    
    /**
     * Warm up model for better performance
     */
    suspend fun warmUpModel(interpreter: Interpreter, dummyInput: Any, dummyOutput: Any) {
        withContext(Dispatchers.Default) {
            try {
                // Run inference a few times to warm up
                repeat(3) {
                    interpreter.run(dummyInput, dummyOutput)
                }
                Log.i(TAG, "Model warmed up successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Model warm-up failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get model metadata
     */
    fun getModelMetadata(interpreter: Interpreter): ModelMetadata {
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        
        return ModelMetadata(
            inputShape = inputTensor.shape(),
            outputShape = outputTensor.shape(),
            inputDataType = inputTensor.dataType().toString(),
            outputDataType = outputTensor.dataType().toString(),
            quantized = inputTensor.quantizationParams().scale != 0f
        )
    }
    
    /**
     * Release a specific model
     */
    fun releaseModel(modelName: String) {
        modelCache[modelName]?.let { interpreter ->
            interpreter.close()
            modelCache.remove(modelName)
            Log.i(TAG, "Released model: $modelName")
        }
    }
    
    /**
     * Release all models
     */
    fun releaseAll() {
        modelCache.forEach { (name, interpreter) ->
            interpreter.close()
            Log.i(TAG, "Released model: $name")
        }
        modelCache.clear()
    }
    
    /**
     * Check if a model is loaded
     */
    fun isModelLoaded(modelName: String): Boolean {
        return modelCache.containsKey(modelName)
    }
    
    /**
     * Get memory usage of loaded models
     */
    fun getMemoryUsage(): Long {
        var totalMemory = 0L
        modelCache.forEach { (_, interpreter) ->
            // Estimate memory usage based on tensor sizes
            val inputSize = interpreter.getInputTensor(0).numBytes()
            val outputSize = interpreter.getOutputTensor(0).numBytes()
            totalMemory += inputSize + outputSize
        }
        return totalMemory
    }
    
    companion object {
        private const val TAG = "AIModelManager"
    }
    
    /**
     * Model metadata
     */
    data class ModelMetadata(
        val inputShape: IntArray,
        val outputShape: IntArray,
        val inputDataType: String,
        val outputDataType: String,
        val quantized: Boolean
    )
    
    /**
     * Custom exceptions
     */
    class ModelLoadException(message: String, cause: Throwable) : Exception(message, cause)
    class InferenceException(message: String, cause: Throwable) : Exception(message, cause)
}