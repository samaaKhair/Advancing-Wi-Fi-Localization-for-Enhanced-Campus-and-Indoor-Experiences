package com.example.wifimagnetometerapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifimagnetometerapp.databinding.ActivityDataCollectionBinding
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorEventListener
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.github.pwittchen.reactivewifi.ReactiveWifi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class DataCollectionActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityDataCollectionBinding
    private val disposables = CompositeDisposable()
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var wifiManager: WifiManager

    private var x: Int = 0
    private var y: Int = 0

    private var lastScanTime = 0L
    private val SCAN_INTERVAL = 30000L // 30 seconds

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        x = intent.getIntExtra("x", 0)
        y = intent.getIntExtra("y", 0)

        // Set the text of labelTextView to the x, y coordinates
        binding.labelTextView.text = "let us know where the point ($x, $y) is!"

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        binding.getWifiRSSIButton.setOnClickListener {
            if (checkPermissions()) {
                getWifiRssi()
            } else {
                requestPermissions()
            }
        }

        binding.getMagnetometerDataButton.setOnClickListener {
            getMagnetometerData()
        }

        binding.sendDataButton.setOnClickListener {
            sendDataToServer()
        }
    }

    private fun checkPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getWifiRssi()
        } else {
            Toast.makeText(this, "Permissions required to scan WiFi networks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWifiRssi() {
        if (checkPermissions()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScanTime < SCAN_INTERVAL) {
                Toast.makeText(this, "Scan request throttled. Please wait a few seconds.", Toast.LENGTH_SHORT).show()
                return
            }

            lastScanTime = currentTime

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions()
                return
            }

            disposable = ReactiveWifi.observeWifiAccessPoints(this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { scanResults ->
                        val results = scanResults.joinToString("\n") { "${it.SSID}: ${it.level}" }
                        binding.wifiRSSITextView.text = results
                        disposable?.dispose()
                    },
                    { error ->
                        Log.e("DataCollectionActivity", "Error observing Wi-Fi access points", error)
                    },
                    {
                        Log.d("DataCollectionActivity", "Wi-Fi scan completed, disposing subscription")
                    }
                )
            disposables.add(disposable!!)
        } else {
            requestPermissions()
        }
    }

    private fun getMagnetometerData() {
        magnetometer?.also { magnetometer ->
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            binding.magnetometerDataTextView.text = "Magnetometer Data:\nX: $x\nY: $y\nZ: $z"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something if sensor accuracy changes
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        sensorManager.unregisterListener(this)
    }

    private fun sendDataToServer() {
        try {
            val label = "($x,$y)"
            val wifiRssiText = binding.wifiRSSITextView.text.toString()

            if (wifiRssiText.isEmpty()) {
                Log.e("DataCollectionActivity", "WiFi RSSI data is empty")
                Toast.makeText(this, "WiFi RSSI data is empty", Toast.LENGTH_SHORT).show()
                return
            }

            val wifiRssi = wifiRssiText.split("\n")
            val rssiData = mutableMapOf<String, Int>()

            wifiRssi.forEach { line ->
                val parts = line.split(": ")
                if (parts.size == 2) {
                    rssiData[parts[0]] = parts[1].toInt()
                } else {
                    Log.e("DataCollectionActivity", "Invalid WiFi RSSI data: $line")
                }
            }

            val rssi_HUAWEI_CUTE_BD57 = rssiData["HUAWEI_CUTE_BD57"] ?: null
            val rssi_YaSeR_Osama = rssiData["YaSeR_Osama"] ?: null
            val rssi_MG2024 = rssiData["MG2024"] ?: null
            val rssi_Samas_iPhone = rssiData["Samas_iPhone"] ?: null

            val magText = binding.magnetometerDataTextView.text.toString()
            if (magText.isEmpty()) {
                Log.e("DataCollectionActivity", "Magnetometer data is empty")
                Toast.makeText(this, "Magnetometer data is empty", Toast.LENGTH_SHORT).show()
                return
            }

            val magValues = magText.split("\n").mapNotNull {
                it.split(": ").getOrNull(1)?.toFloatOrNull()
            }

            if (magValues.size != 3) {
                Log.e("DataCollectionActivity", "Invalid magnetometer data")
                Toast.makeText(this, "Invalid magnetometer data", Toast.LENGTH_SHORT).show()
                return
            }

            val json = JSONObject().apply {
                put("x", x)
                put("y", y)
                put("z", 0)  // Assuming z is 0 or another value as needed
                put("rssi_HUAWEI_CUTE_BD57", rssi_HUAWEI_CUTE_BD57)
                put("rssi_YaSeR_Osama", rssi_YaSeR_Osama)
                put("rssi_MG2024", rssi_MG2024)
                put("rssi_Samas_iPhone", rssi_Samas_iPhone)
                put("mag_x", magValues[0])
                put("mag_y", magValues[1])
                put("mag_z", magValues[2])
            }

            val client = OkHttpClient()
            val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
            val request = Request.Builder()
                .url("http://10.20.141.37:5000/phone_data")
                .post(requestBody)
                .build()

            Log.d("DataCollectionActivity", "Sending data to server: $json")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@DataCollectionActivity, "Failed to send data: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("DataCollectionActivity", "Failed to send data: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: "No response from server"
                    runOnUiThread {
                        Log.d("DataCollectionActivity", "Response from server: $responseBody")
                        val builder = AlertDialog.Builder(this@DataCollectionActivity)
                        builder.setMessage("The point is now known to us!")
                            .setPositiveButton("Okay") { dialog, _ ->
                                dialog.dismiss()
                                // Navigate back to MainActivity
                                val intent = Intent(this@DataCollectionActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }
                            .create()
                            .show()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("DataCollectionActivity", "Exception: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
