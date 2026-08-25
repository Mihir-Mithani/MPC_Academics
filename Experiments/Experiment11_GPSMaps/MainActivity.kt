package com.example.gpsmaps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val PERMISSION_REQUEST_CODE = 1001
    private val LOCATION_REQUEST_INTERVAL = 5000L // 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            GPSMapsTheme {
                GPSMapsScreen(
                    onLocationClick = { getCurrentLocation() },
                    onPermissionClick = { requestPermissions() },
                    onMapReady = { map -> googleMap = map }
                )
            }
        }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted || !coarseGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            // Initialize map fragment
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment?.getMapAsync(this)
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun getCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        updateMapLocation(location)
                        GPSMapsScreen.updateLocationInfo(location)
                        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                    }
                }
                ?.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            requestPermissions()
        }
    }

    private fun updateMapLocation(location: Location) {
        googleMap?.let { map ->
            val latLng = LatLng(location.latitude, location.longitude)
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Current Location"))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL)

        // Try to get current location immediately
        getCurrentLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val fineGranted = grantResults.any { it == PackageManager.PERMISSION_GRANTED && permissions[grantResults.indexOf(it)] == Manifest.permission.ACCESS_FINE_LOCATION }
            val coarseGranted = grantResults.any { it == PackageManager.PERMISSION_GRANTED && permissions[grantResults.indexOf(it)] == Manifest.permission.ACCESS_COARSE_LOCATION }

            if (fineGranted || coarseGranted) {
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
                mapFragment?.getMapAsync(this)
                getCurrentLocation()
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permissions denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private var locationInfo = "Waiting for location..."
            private set

        fun updateLocationInfo(location: Location) {
            locationInfo = "Lat: ${location.latitude}, Lng: ${location.longitude}\nAccuracy: ${location.accuracy}m\nAltitude: ${location.altitude}m\nSpeed: ${location.speed} m/s\nBearing: ${location.bearing}°\nTime: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(location.time))}"
        }

        fun getLocationInfo(): String = locationInfo
    }
}

@Composable
fun GPSMapsScreen(
    onLocationClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onMapReady: (com.google.android.gms.maps.GoogleMap) -> Unit
) {
    var locationInfo by remember { mutableStateOf(MainActivity.getLocationInfo()) }
    var hasPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            locationInfo = MainActivity.getLocationInfo()
            androidx.compose.runtime.delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Card
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS Location on Google Maps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (hasPermissions) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                        contentDescription = "GPS status",
                        tint = if (hasPermissions) Color.Green else Color.Red
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                Text(
                    text = locationInfo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Action Buttons
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            androidx.compose.material3.Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onLocationClick) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Get Location")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = "Get Current Location", fontSize = 16.sp)
                }
                IconButton(onClick = onPermissionClick) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Permissions")
                }
            }
        }

        // Map Fragment Container
        androidx.compose.ui.platform.AndroidView(
            factory = { context ->
                // Create a FrameLayout to hold the map fragment
                val frameLayout = android.widget.FrameLayout(context)
                frameLayout.id = R.id.map_fragment
                frameLayout.layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                frameLayout
            },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            update = { view ->
                // Map is handled by SupportMapFragment
            }
        ) {
            // We need to add the map fragment programmatically
        }

        // Since Compose doesn't easily host Fragments, we use AndroidView with MapView
        // Alternative: Use AndroidView with MapView directly
        androidx.compose.ui.platform.AndroidView(
            factory = { context ->
                com.google.android.gms.maps.MapView(context).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        onMapReady(map)
                        map.uiSettings.isZoomControlsEnabled = true
                        map.uiSettings.isMyLocationButtonEnabled = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(16.dp),
            update = { mapView ->
                // MapView lifecycle handled by Compose
            }
        )
    }
}