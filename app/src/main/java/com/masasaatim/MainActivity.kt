package com.masasaatim

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.masasaatim.core.theme.DesktopClockTheme
import com.masasaatim.presentation.MainScreen
import com.masasaatim.presentation.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var isLocationDataLoaded = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getDeviceLocation()
        } else {
            startWithGermanyFallback()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        viewModel = ViewModelProvider(this, MainViewModel.provideFactory(application))[MainViewModel::class.java]

        checkLocationPermissions()

        setContent {
            DesktopClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        val fineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun getDeviceLocation() {
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isLocationDataLoaded) {
                android.util.Log.w("MasaSaatim", "GPS-Timeout nach 3 Sekunden erreicht. Fallback wird aktiviert.")
                startWithGermanyFallback()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 3000)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                timeoutHandler.removeCallbacks(timeoutRunnable)

                if (location != null && !isLocationDataLoaded) {
                    isLocationDataLoaded = true
                    viewModel.loadPrayerDataWithLocation(location.latitude, location.longitude)
                } else if (!isLocationDataLoaded) {
                    startWithGermanyFallback()
                }
            }.addOnFailureListener {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                startWithGermanyFallback()
            }
        } catch (e: SecurityException) {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            e.printStackTrace()
            startWithGermanyFallback()
        }
    }

    private fun startWithGermanyFallback() {
        if (!isLocationDataLoaded) {
            isLocationDataLoaded = true
            viewModel.loadPrayerDataWithLocation(51.9311, 8.8681)
        }
    }
}
