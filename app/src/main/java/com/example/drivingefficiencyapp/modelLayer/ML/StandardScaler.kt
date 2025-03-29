package com.example.drivingefficiencyapp.modelLayer.ML

import android.content.Context
import android.util.Log
import org.json.JSONObject

class StandardScaler {
    private var means: FloatArray? = null
    private var stds: FloatArray? = null

    companion object {
        private const val TAG = "ML"
    }

    fun loadFromAssets(context: Context, fileName: String): Boolean {
        Log.d(TAG, "Loading StandardScaler from assets file: $fileName")
        try {
            context.assets.open(fileName).use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(json)

                val meansArray = jsonObject.getJSONArray("means")
                val stdsArray = jsonObject.getJSONArray("stds")

                Log.d(TAG, "Parsed scaler JSON with ${meansArray.length()} features")

                means = FloatArray(meansArray.length())
                stds = FloatArray(stdsArray.length())

                for (i in 0 until meansArray.length()) {
                    means!![i] = meansArray.getDouble(i).toFloat()
                    stds!![i] = stdsArray.getDouble(i).toFloat()
                }

                Log.d(TAG, "Successfully loaded means and standard deviations")
                Log.d(TAG, "Means: ${means?.joinToString()}")
                Log.d(TAG, "Stds: ${stds?.joinToString()}")

                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading scaler from $fileName: ${e.message}", e)
            return false
        }
    }

    fun transform(features: FloatArray): FloatArray {
        if (means == null || stds == null) {
            Log.w(TAG, "Cannot scale features: means or stds not initialized")
            return features
        }

        if (features.size != means?.size) {
            Log.e(TAG, "Feature size mismatch: expected ${means?.size}, got ${features.size}")
            return features
        }

        Log.d(TAG, "Scaling features: ${features.joinToString()}")

        val scaled = FloatArray(features.size)
        for (i in features.indices) {
            scaled[i] = (features[i] - means!![i]) / stds!![i]
        }

        Log.d(TAG, "Scaled result: ${scaled.joinToString()}")
        return scaled
    }
}