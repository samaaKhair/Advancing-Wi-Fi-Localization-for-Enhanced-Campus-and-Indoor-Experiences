package com.example.wifimagnetometerapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log


data class Coordinate(val row: Int, val col: Int)
data class ShortestPathRequest(val start: List<Int>, val goal: List<Int>)
data class ShortestPathResponse(val path: List<List<Int>>)

interface FlaskService {
    @GET("coordinates")
    suspend fun getCoordinates(): List<Coordinate>

    @POST("process_phone_data")
    suspend fun sendPhoneData(@Body data: PhoneData): PredictedLocation

    @POST("shortest_path")
    suspend fun getShortestPath(@Body request: ShortestPathRequest): ShortestPathResponse
}

data class PhoneData(
    val rssi_HUAWEI_CUTE_BD57: Int,
    val rssi_YaSeR_Osama: Int,
    val rssi_MG2024: Int,
    val rssi_Samas_iPhone: Int,
    val mag_x: Float,
    val mag_y: Float,
    val mag_z: Float
)

data class PredictedLocation(
    val predicted_x: Int,
    val predicted_y: Int,
    val predicted_z: Int,
    val confidence: Float
)

class MainActivity : AppCompatActivity() {

    private lateinit var zoomableGridView: ZoomableGridView
    private lateinit var flaskService: FlaskService
    private val gridSize = 16
    private val visitedLocations = Array(gridSize) { BooleanArray(gridSize) { false } }
    private var userLocation = Coordinate(1, 7)

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            sendPhoneDataAndReceivePrediction()
            handler.postDelayed(this, 1000)  // Schedule next update in 1 second
        }
    }

    private fun sendPhoneDataAndReceivePrediction() {
        val phoneData = PhoneData(
            rssi_HUAWEI_CUTE_BD57 = 30,  // Replace with actual data
            rssi_YaSeR_Osama = 40,       // Replace with actual data
            rssi_MG2024 = 50,            // Replace with actual data
            rssi_Samas_iPhone = 60,      // Replace with actual data
            mag_x = 1.1f,                // Replace with actual data
            mag_y = 2.2f,                // Replace with actual data
            mag_z = 3.3f                 // Replace with actual data
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prediction = flaskService.sendPhoneData(phoneData)
                withContext(Dispatchers.Main) {
                    if (prediction.predicted_x in 1..gridSize && prediction.predicted_y in 1..gridSize) {
                        updateUserLocation(prediction.predicted_x, prediction.predicted_y)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        zoomableGridView = findViewById(R.id.zoomableGridView)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.20.141.37:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        flaskService = retrofit.create(FlaskService::class.java)

        // Update visited locations from the Flask server
        updateVisitedLocations()

        // Handle cell click events
        zoomableGridView.setOnCellClickListener { row, col ->
            showCellOptionsDialog(row, col)
        }

        // Start the periodic update
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
        super.onBackPressed() // Add this call
    }

    private fun showCellOptionsDialog(row: Int, col: Int) {
        val options = arrayOf("Take me there", "Add a new point")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an action")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> requestShortestPath(Coordinate(userLocation.row, userLocation.col), Coordinate(row, col))
                1 -> {
                    val intent = Intent(this, DataCollectionActivity::class.java)
                    intent.putExtra("x", row)
                    intent.putExtra("y", col)
                    startActivity(intent)
                }
            }
        }
        builder.show()
    }

    private fun requestShortestPath(start: Coordinate, goal: Coordinate) {
        val request = ShortestPathRequest(
            start = listOf(start.row, start.col),
            goal = listOf(goal.row, goal.col)
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = flaskService.getShortestPath(request)
                withContext(Dispatchers.Main) {
                    val path = response.path
                    updateGridWithPath(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "No possible paths:(", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateGridWithPath(path: List<List<Int>>) {
        zoomableGridView.setPath(path)
    }

    private fun updateVisitedLocations() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val coordinates = flaskService.getCoordinates()
                withContext(Dispatchers.Main) {
                    for (coordinate in coordinates) {
                        if (coordinate.row in 1..gridSize && coordinate.col in 1..gridSize) {
                            visitedLocations[coordinate.row - 1][coordinate.col - 1] = true
                        }
                    }
                    zoomableGridView.setVisitedLocations(visitedLocations)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUserLocation(x: Int, y: Int) {
        userLocation = Coordinate(x, y)
        Log.d("MainActivity", "Updating user location to ($x, $y)")
        zoomableGridView.setUserLocation(userLocation)
    }
}

//package com.example.wifimagnetometerapp
//
//import android.content.Intent
//import android.graphics.Color
//import android.os.Bundle
//import android.view.Gravity
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.GridLayout
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.Body
//import retrofit2.http.GET
//import retrofit2.http.POST
//import android.app.AlertDialog
//import android.os.Handler
//import android.os.Looper
//import android.widget.Toast
//
//data class Coordinate(val row: Int, val col: Int)
//data class ShortestPathRequest(val start: List<Int>, val goal: List<Int>)
//data class ShortestPathResponse(val path: List<List<Int>>)
//
//interface FlaskService {
//    @GET("coordinates")
//    suspend fun getCoordinates(): List<Coordinate>
//
//    @POST("process_phone_data")
//    suspend fun sendPhoneData(@Body data: PhoneData): PredictedLocation
//
//    @POST("shortest_path")
//    suspend fun getShortestPath(@Body request: ShortestPathRequest): ShortestPathResponse
//}
//
//data class PhoneData(
//    val rssi_HUAWEI_CUTE_BD57: Int,
//    val rssi_YaSeR_Osama: Int,
//    val rssi_MG2024: Int,
//    val rssi_Samas_iPhone: Int,
//    val mag_x: Float,
//    val mag_y: Float,
//    val mag_z: Float
//)
//
//data class PredictedLocation(
//    val predicted_x: Int,
//    val predicted_y: Int,
//    val predicted_z: Int,
//    val confidence: Float
//)
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var zoomableGridView: ZoomableGridView
//    private lateinit var flaskService: FlaskService
//    private val gridSize = 16
//    private val visitedLocations = Array(gridSize) { BooleanArray(gridSize) { false } }
//    private var userLocation = Coordinate(1, 7)
//
//    private val handler = Handler(Looper.getMainLooper())
//    private val updateRunnable = object : Runnable {
//        override fun run() {
//            sendPhoneDataAndReceivePrediction()
//            handler.postDelayed(this, 1000)  // Schedule next update in 1 second
//        }
//    }
//    private fun sendPhoneDataAndReceivePrediction() {
//                val phoneData = PhoneData(
//            rssi_HUAWEI_CUTE_BD57 = 30,  // Replace with actual data
//            rssi_YaSeR_Osama = 40,       // Replace with actual data
//            rssi_MG2024 = 50,            // Replace with actual data
//            rssi_Samas_iPhone = 60,      // Replace with actual data
//            mag_x = 1.1f,                // Replace with actual data
//            mag_y = 2.2f,                // Replace with actual data
//            mag_z = 3.3f                 // Replace with actual data
//        )
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val prediction = flaskService.sendPhoneData(phoneData)
//                withContext(Dispatchers.Main) {
//                    updateUserLocation(prediction.predicted_x, prediction.predicted_y)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    // Handle error, for example, show a Toast or a Snackbar
//                    // Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        zoomableGridView = findViewById(R.id.zoomableGridView)
//
//        val retrofit = Retrofit.Builder()
//            .baseUrl("http://10.20.141.37:5000/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        flaskService = retrofit.create(FlaskService::class.java)
//
//        // Update visited locations from the Flask server
//        updateVisitedLocations()
//
//        // Handle cell click events
//        zoomableGridView.setOnCellClickListener { row, col ->
//            showCellOptionsDialog(row, col)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        handler.removeCallbacks(updateRunnable)
//    }
//
//    override fun onBackPressed() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
//        finish()
//        super.onBackPressed() // Add this call
//    }
//
//    private fun showCellOptionsDialog(row: Int, col: Int) {
//        val options = arrayOf("Take me there", "Add a new point")
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Choose an action")
//        builder.setItems(options) { dialog, which ->
//            when (which) {
//                0 -> requestShortestPath(Coordinate(userLocation.row, userLocation.col), Coordinate(row, col))
//                1 -> {
//                    val intent = Intent(this, DataCollectionActivity::class.java)
//                    intent.putExtra("x", row)
//                    intent.putExtra("y", col)
//                    startActivity(intent)
//                }
//            }
//        }
//        builder.show()
//    }
//
//    private fun requestShortestPath(start: Coordinate, goal: Coordinate) {
//        val request = ShortestPathRequest(
//            start = listOf(start.row, start.col),
//            goal = listOf(goal.row, goal.col)
//        )
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = flaskService.getShortestPath(request)
//                withContext(Dispatchers.Main) {
//                    val path = response.path
//                    updateGridWithPath(path)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MainActivity, "No possible paths:(", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun updateGridWithPath(path: List<List<Int>>) {
//        zoomableGridView.setPath(path)
//    }
//
//    private fun updateVisitedLocations() {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val coordinates = flaskService.getCoordinates()
//                withContext(Dispatchers.Main) {
//                    for (coordinate in coordinates) {
//                        if (coordinate.row in 1..gridSize && coordinate.col in 1..gridSize) {
//                            visitedLocations[coordinate.row - 1][coordinate.col - 1] = true
//                        }
//                    }
//                    zoomableGridView.setVisitedLocations(visitedLocations)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun updateUserLocation(x: Int, y: Int) {
//        userLocation = Coordinate(x, y)
//        zoomableGridView.setUserLocation(userLocation)
//    }
//}
////data class Coordinate(val row: Int, val col: Int)
////data class ShortestPathRequest(val start: List<Int>, val goal: List<Int>)
////data class ShortestPathResponse(val path: List<List<Int>>)
////
////
////interface FlaskService {
////    @GET("coordinates")
////    suspend fun getCoordinates(): List<Coordinate>
////
////    @POST("process_phone_data")
////    suspend fun sendPhoneData(@Body data: PhoneData): PredictedLocation
////
////    @POST("shortest_path")
////    suspend fun getShortestPath(@Body request: ShortestPathRequest): ShortestPathResponse
////}
////
////data class PhoneData(
////    val rssi_HUAWEI_CUTE_BD57: Int,
////    val rssi_YaSeR_Osama: Int,
////    val rssi_MG2024: Int,
////    val rssi_Samas_iPhone: Int,
////    val mag_x: Float,
////    val mag_y: Float,
////    val mag_z: Float
////)
////
////data class PredictedLocation(
////    val predicted_x: Int,
////    val predicted_y: Int,
////    val predicted_z: Int,
////    val confidence: Float
////)
////
////class MainActivity : AppCompatActivity() {
////
////    private lateinit var zoomableGridView: ZoomableGridView
////    private lateinit var flaskService: FlaskService
////    private val gridSize = 16
////    private val gridState = Array(gridSize) { BooleanArray(gridSize) { false } }
////    private val visitedLocations = Array(gridSize) { BooleanArray(gridSize) { false } }
////    private var userLocation = Coordinate(1, 7)
////
////    private val handler = Handler(Looper.getMainLooper())
////    private val updateRunnable = object : Runnable {
////        override fun run() {
////            //sendPhoneDataAndReceivePrediction()
////            handler.postDelayed(this, 1000)  // Schedule next update in 1 second
////        }
////    }
////
////    override fun onCreate(savedInstanceState: Bundle?) {
////        super.onCreate(savedInstanceState)
////        setContentView(R.layout.activity_main)
////        zoomableGridView = findViewById(R.id.zoomableGridView)
////
////        val retrofit = Retrofit.Builder()
////            .baseUrl("http://10.20.141.37:5000/")
////            .addConverterFactory(GsonConverterFactory.create())
////            .build()
////
////        flaskService = retrofit.create(FlaskService::class.java)
////
////        // Update visited locations from the Flask server
////        updateVisitedLocations()
////
////        zoomableGridView.setOnCellClickListener { row, col ->
////            showCellOptionsDialog(row, col)
////        }
////    }
////
////
////    override fun onDestroy() {
////        super.onDestroy()
////        handler.removeCallbacks(updateRunnable)
////    }
////    override fun onBackPressed() {
////        val intent = Intent(this, MainActivity::class.java)
////        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
////        startActivity(intent)
////        finish()
////        super.onBackPressed() // Add this call
////    }
//////    private fun initGrid() {
//////        for (i in 1..gridSize) {
//////            for (j in 1..gridSize) {
//////                val cell = FrameLayout(this).apply {
//////                    setBackgroundColor(Color.GRAY)
//////                }
//////                val textView = TextView(this).apply {
//////                    text = "$i,$j"
//////                    setTextColor(Color.WHITE)
//////                    textAlignment = View.TEXT_ALIGNMENT_CENTER
//////                }
//////                cell.addView(textView)
//////                val params = GridLayout.LayoutParams().apply {
//////                    rowSpec = GridLayout.spec(i - 1, 1f)
//////                    columnSpec = GridLayout.spec(j - 1, 1f)
//////                    width = 0
//////                    height = 0
//////                    setMargins(1, 1, 1, 1)
//////                }
//////                gridLayout.addView(cell, params)
//////
//////                cell.setOnClickListener {
//////                    showCellOptionsDialog(i, j)
//////                }
//////            }
//////        }
//////    }
////    private fun showCellOptionsDialog(row: Int, col: Int) {
////        val options = arrayOf("Take me there", "Add a new point")
////        val builder = AlertDialog.Builder(this)
////        builder.setTitle("Choose an action")
////        builder.setItems(options) { dialog, which ->
////            when (which) {
////                0 -> requestShortestPath(Coordinate(userLocation.row, userLocation.col), Coordinate(row, col))
////                1 -> {
////                    val intent = Intent(this, DataCollectionActivity::class.java)
////                    intent.putExtra("x", row)
////                    intent.putExtra("y", col)
////                    startActivity(intent)
////                }
////            }
////        }
////        builder.show()
////    }
////
////    private fun requestShortestPath(start: Coordinate, goal: Coordinate) {
////        val request = ShortestPathRequest(
////            start = listOf(start.row, start.col),
////            goal = listOf(goal.row, goal.col)
////        )
////
////        CoroutineScope(Dispatchers.IO).launch {
////            try {
////                val response = flaskService.getShortestPath(request)
////                withContext(Dispatchers.Main) {
////                    val path = response.path
////                    updateGridWithPath(path)
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(this@MainActivity, "No possible paths:(", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
////    private fun updateGridWithPath(path: List<List<Int>>) {
////        zoomableGridView.setPath(path)
////    }
//////    private fun updateGridWithPath(path: List<List<Int>>) {
//////        for (coords in path) {
//////            val row = coords[0]
//////            val col = coords[1]
//////            val cellIndex = (row - 1) * gridSize + (col - 1)
//////            val cell = gridLayout.getChildAt(cellIndex) as FrameLayout
//////            cell.setBackgroundColor(Color.RED)  // Highlight the path in red
//////        }
//////    }
////
////    private fun sendPhoneDataAndReceivePrediction() {
////        val phoneData = PhoneData(
////            rssi_HUAWEI_CUTE_BD57 = 30,  // Replace with actual data
////            rssi_YaSeR_Osama = 40,       // Replace with actual data
////            rssi_MG2024 = 50,            // Replace with actual data
////            rssi_Samas_iPhone = 60,      // Replace with actual data
////            mag_x = 1.1f,                // Replace with actual data
////            mag_y = 2.2f,                // Replace with actual data
////            mag_z = 3.3f                 // Replace with actual data
////        )
////
////        CoroutineScope(Dispatchers.IO).launch {
////            try {
////                val prediction = flaskService.sendPhoneData(phoneData)
////                withContext(Dispatchers.Main) {
////                    updateUserLocation(prediction.predicted_x, prediction.predicted_y)
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                withContext(Dispatchers.Main) {
////                    // Handle error, for example, show a Toast or a Snackbar
////                    // Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
////    private fun updateVisitedLocations() {
////        CoroutineScope(Dispatchers.IO).launch {
////            try {
////                val coordinates = flaskService.getCoordinates()
////                withContext(Dispatchers.Main) {
////                    for (coordinate in coordinates) {
////                        if (coordinate.row in 1..gridSize && coordinate.col in 1..gridSize) {
////                            visitedLocations[coordinate.row - 1][coordinate.col - 1] = true
////                        }
////                    }
////                    zoomableGridView.setVisitedLocations(visitedLocations)
////                }
////            } catch (e: Exception) {
////                e.printStackTrace()
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(this@MainActivity, "Failed to load data", Toast.LENGTH_LONG).show()
////                }
////            }
////        }
////    }
////
////    private fun updateUserLocation(x: Int, y: Int) {
////        userLocation = Coordinate(x, y)
////        zoomableGridView.setUserLocation(userLocation)
////    }
////
//////    private fun updateGrid() {
//////        for (i in 1..gridSize) {
//////            for (j in 1..gridSize) {
//////                val cellIndex = (i - 1) * gridSize + (j - 1)
//////                val cell = gridLayout.getChildAt(cellIndex) as FrameLayout
//////
//////                if (i == userLocation.row && j == userLocation.col) {
//////                    cell.setBackgroundColor(Color.GRAY)
//////                    addBlueDot(cell)
//////                } else if (visitedLocations[i - 1][j - 1]) {
//////                    cell.setBackgroundColor(Color.GREEN)
//////                } else {
//////                    cell.setBackgroundColor(Color.GRAY)
//////                }
//////            }
//////        }
//////    }
////    private fun addBlueDot(cell: FrameLayout) {
////        val blueDot = View(this).apply {
////            setBackgroundColor(Color.BLUE)
////            val size = 20
////            layoutParams = FrameLayout.LayoutParams(size, size).apply {
////                gravity = Gravity.CENTER
////            }
////        }
////        cell.addView(blueDot)
////    }
////}
