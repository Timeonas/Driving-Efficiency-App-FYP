package com.example.drivingefficiencyapp.modelLayer.ML

import android.content.Context
import android.util.Log
import com.example.drivingefficiencyapp.modelLayer.trip.Trip
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class DriverClassifierML(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var scaler: StandardScaler? = null

    companion object {
        private const val TAG = "ML"
    }

    init {
        try {
            Log.d(TAG, "Initializing DriverClassifierML model")
            // Load TFLite model
            val model = loadModelFile("driver_classifier.tflite")
            interpreter = Interpreter(model)
            Log.d(TAG, "TensorFlow Lite interpreter initialized successfully")

            // Load scaler
            scaler = StandardScaler()
            scaler!!.loadFromAssets(context, "scaler.json")
            Log.d(TAG, "StandardScaler loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ML classifier: ${e.message}", e)
        }
    }

    fun classify(trips: List<Trip>): DriverCategory {
        Log.d(TAG, "Starting classification on ${trips.size} trips")

        if (interpreter == null) {
            Log.w(TAG, "TFLite interpreter not initialized, falling back to rule-based classifier")
            return DriverClassifierRule().classifyDriver(trips)
        }

        // Extract and scale features
        val features = extractAndScaleFeatures(trips)
        Log.d(TAG, "Features extracted and scaled: ${features.joinToString()}")

        // Run inference
        val outputBuffer = Array(1) { FloatArray(4) } // 4 categories
        try {
            interpreter?.run(arrayOf(features), outputBuffer)
            Log.d(TAG, "Inference complete. Raw outputs: ${outputBuffer[0].joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during model inference: ${e.message}", e)
            return DriverClassifierRule().classifyDriver(trips)
        }

        // Get prediction
        val maxIndex = outputBuffer[0].indices.maxByOrNull { outputBuffer[0][it] } ?: 0
        val maxConfidence = outputBuffer[0][maxIndex]
        Log.d(TAG, "Predicted class index: $maxIndex with confidence: $maxConfidence")

        // Convert to category
        val result = when(maxIndex) {
            0 -> DriverCategory.ECO_FRIENDLY
            1 -> DriverCategory.BALANCED
            2 -> DriverCategory.MODERATE
            3 -> DriverCategory.AGGRESSIVE
            else -> DriverCategory.MODERATE
        }

        Log.d(TAG, "Final classification result: ${result.label}")
        return result
    }

    private fun extractAndScaleFeatures(trips: List<Trip>): FloatArray {
        Log.d(TAG, "Extracting features from ${trips.size} trips")

        val rawFeatures = floatArrayOf(
            trips.map { it.averageFuelConsumption }.average().toFloat(),
            trips.map { it.avgRPM }.average().toFloat(),
            trips.map { it.maxRPM }.average().toFloat(),
            trips.map { it.averageSpeed }.average().toFloat(),
            trips.map { it.efficiencyScore }.average().toFloat()
        )

        Log.d(
            TAG, "Raw features: avg fuel=${rawFeatures[0]}, avg RPM=${rawFeatures[1]}, " +
                "max RPM=${rawFeatures[2]}, avg speed=${rawFeatures[3]}, score=${rawFeatures[4]}")

        val scaledFeatures = scaler?.transform(rawFeatures) ?: rawFeatures
        Log.d(TAG, "Scaled features: ${scaledFeatures.joinToString()}")

        return scaledFeatures
    }

    private fun loadModelFile(assetName: String): ByteBuffer {
        Log.d(TAG, "Loading model file: $assetName")
        val fileDescriptor = context.assets.openFd(assetName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        Log.d(TAG, "Model loaded: size=${declaredLength} bytes")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}