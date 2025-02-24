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
    private val gearCalculator: GearCalculator // Inject GearCalculator
) {
    private var readingJob: Job? = null
    private var currentRpm = 0
    private var currentSpeed: Double = 0.0
    private var currentFuelRate: Double = 0.0 // Store current fuel rate

    // --- Data Storage for Calculations ---
    private var totalDistance: Double = 0.0
    private var totalFuelUsed: Double = 0.0
    private var lastTimestamp: Long = 0
    private var lastInstantFuelRate: Double = 0.0 // in L/h
    private var tripStartTime: Long = 0 // Timestamp for calculating trip duration

    data class ObdData(
        val rpm: String = "- RPM",
        val speed: String = "- km/h",
        val temperature: String = "- °C",
        val instantFuelRate: String = "- L/h", // Liters per hour
        val averageFuelConsumption: String = "- L/100km",
        val instantFuelConsumption: String = "- L/100km", // Added instant consumption
        val averageSpeed: String = "- km/h",
        val distanceTraveled: String = "- km",
        val fuelUsed: String = "- L"
    )

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData

    suspend fun startContinuousReading() {
        Log.d("ObdDataReader", "startContinuousReading called")
        readingJob = scope.launch(Dispatchers.IO) {
            // Initialize trip start time.
            tripStartTime = System.currentTimeMillis()
            lastTimestamp = tripStartTime

            val commands = listOf("010C", "010D", "0105", "015E")  // Keep only necessary PIDs

            while (isActive) {
                try {
                    val instantFuelRateStr = sendAndParseCommand("015E")
                    //Calculate instant fuel consumption
                    val instantFuelConsumption = if (currentSpeed > 0.0) {
                        // Instant Consumption = (Instant Fuel Rate / Speed) * 100
                        val fuelRate = instantFuelRateStr.split(" ")[0].toDoubleOrNull() ?: 0.0
                        String.format("%.2f L/100km", (fuelRate / currentSpeed) * 100)
                    } else {
                        "∞ L/100km" // Handle zero speed
                    }


                    val newData = ObdData(
                        rpm = sendAndParseCommand("010C"),
                        speed = sendAndParseCommand("010D"),
                        temperature = sendAndParseCommand("0105"),
                        instantFuelRate = instantFuelRateStr,
                        averageFuelConsumption = if (totalDistance > 0) String.format("%.2f L/100km", (totalFuelUsed / totalDistance) * 100) else "- L/100km",
                        instantFuelConsumption = instantFuelConsumption, // Use calculated value
                        averageSpeed = if (System.currentTimeMillis() - tripStartTime > 0) String.format("%.2f km/h", totalDistance / ((System.currentTimeMillis() - tripStartTime).toDouble() / 3600000.0)) else "- km/h",
                        distanceTraveled = String.format("%.2f km", totalDistance),
                        fuelUsed = String.format("%.2f L", totalFuelUsed)
                    )

                    _obdData.emit(newData)
                    delay(100) // Adjust delay as needed

                } catch (e: CancellationException) {
                    Log.d("ObdDataReader", "Reading loop cancelled")
                    break
                } catch (e: Exception) {
                    Log.e("ObdDataReader", "Error in reading loop: ${e.message}", e)
                    _obdData.emit(ObdData( //Emit error data.
                        rpm = "- RPM (Error)",
                        speed = "- km/h (Error)",
                        temperature = "- °C (Error)",
                        instantFuelRate = "- L/h (Error)",
                        averageFuelConsumption = "- L/100km (Error)",
                        instantFuelConsumption = "- L/100km (Error)",
                        averageSpeed = "- km/h (Error)",
                        distanceTraveled = "- km (Error)",
                        fuelUsed = "- L (Error)"
                    ))
                    delay(500)
                }
            }
        }
    }

    private suspend fun sendAndParseCommand(command: String): String {
        val response = sendCommandAndReadResponse(command)
        return parseObdResponse(command, response)
    }

    private suspend fun sendCommandAndReadResponse(command: String): String {
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            try {
                Log.d("SendCommand", "Sending command: $command")
                val startTime = System.currentTimeMillis()
                val response = withTimeoutOrNull(2000) {
                    outputStream.write((command + "\r").toByteArray())
                    delay(50)
                    val bytesRead = withTimeoutOrNull(1000) { inputStream.read(buffer) } ?: 0
                    if (bytesRead > 0) {
                        String(buffer, 0, bytesRead)
                    } else {
                        ""
                    }
                } ?: ""

                val endTime = System.currentTimeMillis()
                Log.d("SendCommand", "Command $command took ${endTime - startTime} ms")
                response
            } catch (e: IOException) {
                Log.e("ObdDataReader", "Error sending/reading command $command: ${e.message}")
                ""
            }
        }
    }

    private fun parseObdResponse(command: String, response: String): String {
        try {
            val cleanResponse = response.replace(">", "").trim()
            Log.d("OBD_PARSER", "Raw response for $command: $cleanResponse")

            if (cleanResponse.isEmpty() || cleanResponse.uppercase() == "NO DATA") {
                return "- (No Data)"
            }
            if (cleanResponse.contains("ERROR") || cleanResponse.contains("CAN ERROR") || cleanResponse.startsWith("?")) {
                return "- (Error)"
            }
            if (cleanResponse.contains("SEARCHING")) {
                return "- (Searching...)"
            }
            if(cleanResponse.contains("BUS INIT")){
                return "- (BUS INIT Error)"
            }

            val pid = command.substring(2)
            val pidIndex = cleanResponse.indexOf(pid, ignoreCase = true)

            if (pidIndex == -1) {
                return "- (Unexpected Response)"
            }

            var dataBytes = cleanResponse.substring(pidIndex + pid.length).trim()
            dataBytes = dataBytes.filter { it.isDigit() || (it in 'A'..'F') || (it in 'a'..'f') }

            Log.d("OBD_PARSER", "Cleaned data bytes for $command: $dataBytes")

            return when (command) {
                "010C" -> { // RPM
                    if (dataBytes.isNotEmpty() && dataBytes.length >= 4) {
                        Log.d("OBD_PARSER", "Parsing RPM. dataBytes: $dataBytes")
                        val a = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        val b = Integer.parseInt(dataBytes.substring(2, 4), 16)
                        currentRpm = ((256 * a) + b) / 4
                        "$currentRpm RPM"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }
                "010D" -> { // Speed
                    if (dataBytes.length >= 2) {
                        val newSpeed = Integer.parseInt(dataBytes.substring(0, 2), 16).toDouble()
                        val currentGear = gearCalculator.calculateGear(currentRpm, newSpeed)

                        // Calculate distance (trapezoidal integration)
                        val currentTime = System.currentTimeMillis()
                        val deltaTimeSeconds = (currentTime - lastTimestamp) / 1000.0
                        val distanceDelta = (currentSpeed + newSpeed) / 2 * deltaTimeSeconds / 3600 // km
                        totalDistance += distanceDelta
                        lastTimestamp = currentTime
                        currentSpeed = newSpeed


                        "$currentSpeed km/h (Gear: $currentGear)"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }
                "0105" -> { // Coolant Temp
                    if (dataBytes.length >= 2) {
                        val temp = Integer.parseInt(dataBytes.substring(0, 2), 16) - 40
                        "$temp°C"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }
                "015E" -> {  // Engine fuel rate
                    if (dataBytes.length >= 4) {
                        val a = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        val b = Integer.parseInt(dataBytes.substring(2, 4), 16)
                        val fuelRate = ((256 * a) + b) * 0.05 // L/h

                        //Calculate fuel used since last
                        val currentTime = System.currentTimeMillis()
                        val deltaTimeSeconds = (currentTime - lastTimestamp) / 1000.0
                        val fuelUsedDelta = (lastInstantFuelRate + fuelRate) / 2 * deltaTimeSeconds / 3600 // in Liters

                        totalFuelUsed += fuelUsedDelta // Accumulate
                        lastInstantFuelRate = fuelRate
                        currentFuelRate = fuelRate // Store for instant consumption calculation

                        String.format("%.2f L/h", fuelRate)
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }

                else -> "Unknown command: $command"
            }
        } catch (e: NumberFormatException) {
            Log.e("OBD_PARSER", "Number format error: ${e.message} for response: $response")
            return "- (Parse Error: Invalid Number)"
        } catch (e: Exception) {
            Log.e("OBD_PARSER", "Parse error: ${e.message} for response: $response")
            return "- (Parse Error)"
        }
    }

    fun resetTripData() {
        totalDistance = 0.0
        totalFuelUsed = 0.0
        tripStartTime = System.currentTimeMillis()
        lastTimestamp = tripStartTime
    }

    fun stopReading() {
        readingJob?.cancel()
    }
}