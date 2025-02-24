package com.example.drivingefficiencyapp.obd

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ObdDataReader(
    private val scope: CoroutineScope,
    private val outputStream: OutputStream,
    private val inputStream: InputStream,
    private val gearCalculator: GearCalculator
) {

    private var readingJob: Job? = null
    private var currentRpm: Int = 0
    private var currentSpeed: Double = 0.0
    private var currentFuelRate: Double = 0.0
    private var currentGear: Int = 0 // Store gear as Int

    // --- Dynamic AFR Estimation ---
    // Adjusted values for better idle accuracy.  TUNING REQUIRED.
    private val minNormalizedMaf: Double = 0.05  // Lowered: Expect lower MAF/RPM at high load
    private val maxNormalizedMaf: Double = 0.80 // Lowered:  Match observed idle MAF/RPM
    private val minAfr: Double = 50.0        // Slightly lowered:  Rich limit.
    private val maxAfr: Double = 90.0       // Significantly increased: Lean limit for idle.
    private val fuelDensity: Double = 832.0

    // --- Data Storage and Initialization ---
    private var totalDistance: Double = 0.0
    private var totalFuelUsed: Double = 0.0
    private var lastTimestamp: Long = 0
    private var tripStartTime: Long = 0
    private var firstIteration = true

    data class ObdData(
        val rpm: String = "- RPM",
        val speed: String = "- km/h",
        val gear: String = "- Gear", // Gear as String for display
        val temperature: String = "- °C",
        val instantFuelRate: String = "- L/h",
        val instantFuelConsumption: String = "- L/100km",
        val averageFuelConsumption: String = "- L/100km",
        val averageSpeed: String = "- km/h",
        val distanceTraveled: String = "- km",
        val fuelUsed: String = "- L",
        val maf: String = "- g/s",
        val afr: String = "- AFR"
    )

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData

    suspend fun startContinuousReading() {
        readingJob = scope.launch(Dispatchers.IO) {
            tripStartTime = System.currentTimeMillis()
            lastTimestamp = tripStartTime

            val commands = listOf("010C", "010D", "0105", "0110")

            while (isActive) {
                try {
                    val rpmStr = sendAndParseCommand("010C")
                    val speedStr = sendAndParseCommand("010D")
                    val tempStr = sendAndParseCommand("0105")
                    val mafStr = sendAndParseCommand("0110")

                    // --- Parse and Update Current Values ---
                    val rpmValue = parseNumericValue(rpmStr).toInt()
                    val speedValue = parseNumericValue(speedStr)
                    val mafValue = parseNumericValue(mafStr)

                    currentRpm = rpmValue          // Update currentRpm
                    currentSpeed = speedValue      // Update currentSpeed
                    // currentGear is updated in parseObdResponse

                    if (firstIteration) {
                        firstIteration = false
                        delay(200)
                        lastTimestamp = System.currentTimeMillis()
                        continue
                    }
                    val currentTime = System.currentTimeMillis()
                    val deltaTimeSeconds = (currentTime - lastTimestamp) / 1000.0

                    if (deltaTimeSeconds <= 0.001) {
                        Log.w("ObdDataReader", "Skipping: small/negative deltaTime: $deltaTimeSeconds")
                        continue
                    }

                    // --- Calculations ---
                    val instantDistance = currentSpeed * deltaTimeSeconds / 3600.0
                    val instantFuelLiters = calculateInstantFuel(mafValue, deltaTimeSeconds)
                    val instantFuelRate = calculateInstantFuelRate(mafValue)
                    currentFuelRate = instantFuelRate

                    totalDistance += instantDistance
                    totalFuelUsed += instantFuelLiters

                    val instantFuelConsumption = if (currentSpeed > 0) {
                        (instantFuelLiters / instantDistance) * 100.0
                    } else {
                        Double.POSITIVE_INFINITY
                    }

                    val averageFuelConsumption = if (totalDistance > 0) {
                        (totalFuelUsed / totalDistance) * 100.0
                    } else {
                        0.0
                    }

                    val tripDurationSeconds = (currentTime - tripStartTime) / 1000.0
                    val averageSpeed = if (tripDurationSeconds > 0) {
                        totalDistance / (tripDurationSeconds / 3600.0)
                    } else {
                        0.0
                    }
                    val estimatedAfr = if (currentRpm > 0) {
                        linearInterpolate(mafValue / currentRpm, minNormalizedMaf, maxNormalizedMaf, minAfr, maxAfr)
                    } else {
                        0.0
                    }

                    Log.d("AFR_Tuning", "RPM: $currentRpm, MAF: $mafValue, NormalizedMAF: ${mafValue / currentRpm}, EstimatedAFR: $estimatedAfr, InstantFuelRate: $instantFuelRate")


                    lastTimestamp = currentTime  // Update lastTimestamp

                    // --- Emit Data ---
                    _obdData.emit(
                        ObdData(
                            rpm = "$currentRpm RPM",
                            speed = String.format("%.1f km/h", currentSpeed),
                            gear = "Gear: $currentGear",  // Format gear here
                            temperature = tempStr,
                            instantFuelRate = String.format("%.2f L/h", instantFuelRate),
                            instantFuelConsumption = if (instantFuelConsumption.isInfinite()) "∞ L/100km" else String.format("%.2f L/100km", instantFuelConsumption),
                            averageFuelConsumption = String.format("%.2f L/100km", averageFuelConsumption),
                            averageSpeed = String.format("%.2f km/h", averageSpeed),
                            distanceTraveled = String.format("%.3f km", totalDistance),
                            fuelUsed = String.format("%.4f L", totalFuelUsed),
                            maf = String.format("%.2f g/s", mafValue),
                            afr = String.format("%.2f", estimatedAfr)
                        )
                    )
                    delay(100)

                } catch (e: CancellationException) {
                    Log.d("ObdDataReader", "Reading loop cancelled")
                    break
                } catch (e: Exception) {
                    Log.e("ObdDataReader", "Error: ${e.message}", e)
                    _obdData.emit(
                        ObdData( //Consistent error handling
                            rpm = "- RPM (Error)",
                            speed = "- km/h (Error)",
                            gear = "- Gear (Error)",
                            temperature = "- °C (Error)",
                            instantFuelRate = "- L/h (Error)",
                            instantFuelConsumption = "- L/100km (Error)",
                            averageFuelConsumption = "- L/100km (Error)",
                            averageSpeed = "- km/h (Error)",
                            distanceTraveled = "- km (Error)",
                            fuelUsed = "- L (Error)",
                            maf = "- g/s (Error)",
                            afr = "- (Error)"
                        )
                    )
                    delay(500)
                }
            }
        }
    }

    private fun linearInterpolate(x: Double, x0: Double, x1: Double, y0: Double, y1: Double): Double {
        val boundedX = x.coerceIn(x0, x1)
        return y0 + (boundedX - x0) * (y1 - y0) / (x1 - x0)
    }

    private fun calculateInstantFuel(mafGramsPerSecond: Double, deltaTimeSeconds: Double): Double {
        if (currentRpm <= 0 || mafGramsPerSecond <= 0) { return 0.0 }
        val normalizedMaf = mafGramsPerSecond / currentRpm
        val estimatedAfr = linearInterpolate(normalizedMaf, minNormalizedMaf, maxNormalizedMaf, minAfr, maxAfr)
        val fuelMassFlowRate = mafGramsPerSecond / estimatedAfr
        val fuelVolumeFlowRate = fuelMassFlowRate / fuelDensity
        return fuelVolumeFlowRate * deltaTimeSeconds
    }

    private fun calculateInstantFuelRate(mafGramsPerSecond: Double): Double {
        if (currentRpm <= 0 || mafGramsPerSecond <= 0) { return 0.0 }
        val normalizedMaf = mafGramsPerSecond / currentRpm
        val estimatedAfr = linearInterpolate(normalizedMaf, minNormalizedMaf, maxNormalizedMaf, minAfr, maxAfr)
        val fuelMassFlowRate = mafGramsPerSecond / estimatedAfr
        val fuelVolumeFlowRate = fuelMassFlowRate / fuelDensity
        return fuelVolumeFlowRate * 3600.0
    }

    private fun parseNumericValue(valueStr: String): Double {
        val parts = valueStr.split(" ")
        return parts[0].toDoubleOrNull() ?: 0.0
    }

    private suspend fun sendAndParseCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            try {
                Log.d("SendCommand", "Sending: $command")
                val response = withTimeoutOrNull(2000) {
                    outputStream.write((command + "\r").toByteArray())
                    delay(50)
                    val bytesRead = withTimeoutOrNull(1000) { inputStream.read(buffer) } ?: 0
                    if (bytesRead > 0) String(buffer, 0, bytesRead) else ""
                } ?: ""
                parseObdResponse(command, response)
            } catch (e: IOException) {
                Log.e("ObdDataReader", "IO Error: ${e.message}")
                ""
            }
        }
    }
    private fun parseObdResponse(command: String, response: String): String {
        try {
            val cleanResponse = response.replace(">", "").trim()
            Log.d("OBD_PARSER", "Raw: $command: $cleanResponse")

            if (cleanResponse.isEmpty() || cleanResponse.uppercase() == "NO DATA") {
                return "- (No Data)"
            }
            if (cleanResponse.contains("ERROR") || cleanResponse.contains("CAN ERROR") || cleanResponse.startsWith("?")) {
                return "- (Error)"
            }
            if (cleanResponse.contains("SEARCHING")) { return "- (Searching...)" }
            if (cleanResponse.contains("BUS INIT")) { return "- (BUS INIT Error)" }

            val pid = command.substring(2)
            val pidIndex = cleanResponse.indexOf(pid, ignoreCase = true)
            if (pidIndex == -1) { return "- (Unexpected Response)" }

            var dataBytes = cleanResponse.substring(pidIndex + pid.length).trim()
            dataBytes = dataBytes.filter { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
            Log.d("OBD_PARSER", "Cleaned: $command: $dataBytes")

            return when (command) {
                "010C" -> { // RPM
                    if (dataBytes.length >= 4) {
                        val a = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        val b = Integer.parseInt(dataBytes.substring(2, 4), 16)
                        val rpm = ((256 * a) + b) / 4
                        "$rpm"  // Return ONLY the RPM value as a string
                    } else { "- (Invalid Data - Short)" }
                }
                "010D" -> { // Speed
                    if (dataBytes.length >= 2) {
                        val speed = Integer.parseInt(dataBytes.substring(0, 2), 16).toDouble()
                        val currentGear = gearCalculator.calculateGear(currentRpm, speed) // Update currentGear!
                        "$currentSpeed km/h (Gear: $currentGear)"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }
                "0105" -> { // Coolant Temp
                    if (dataBytes.length >= 2) {
                        val temp = Integer.parseInt(dataBytes.substring(0, 2), 16) - 40
                        "$temp°C"  // Return only the numeric temperature
                    } else { "- (Invalid Data - Short)" }
                }
                "0110" -> { // MAF
                    if (dataBytes.length >= 4) {
                        val a = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        val b = Integer.parseInt(dataBytes.substring(2, 4), 16)
                        val maf = ((256 * a) + b) / 100.0
                        "$maf"  // Return only the numeric MAF value
                    } else { "- (Invalid Data - Short)" }
                }
                else -> "Unknown command: $command"
            }
        } catch (e: Exception) {
            Log.e("OBD_PARSER", "Parse error: ${e.message}, response: $response")
            return "- (Parse Error)"
        }
    }

    fun resetTripData() {
        totalDistance = 0.0
        totalFuelUsed = 0.0
        lastTimestamp = System.currentTimeMillis()
        tripStartTime = System.currentTimeMillis()
    }

    fun stopReading() {
        readingJob?.cancel()
    }
}