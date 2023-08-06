package org.tensorflow.lite.examples.objectdetection // Make sure the package name matches the one in AndroidManifest.xml

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import android.speech.tts.TextToSpeech
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var textToSpeech: TextToSpeech

    data class Station(val name: String, val latitude: Double, val longitude: Double)

    // Replace this list with all the MRT/LRT stations and their coordinates
    private val stations = listOf(
        Station("Woodlands North", 1.403618, 103.825883),
        Station("Woodlands", 1.404184, 103.826071),
        Station("Tampines West", 1.325249, 103.873865),
        Station("Tampines East", 1.326507, 103.875191),
        Station("Upper Changi", 1.329352, 103.876117),
        Station("HarbourFront", 1.255558, 103.860718),
        Station("Telok Blangah", 1.251901, 103.861963),
        Station("Labrador Park", 1.248244, 103.863208),
        Station("Pasir Panjang", 1.244587, 103.864453),
        Station("Haw Paw Villa", 1.240930, 103.865698),
        Station("Kent Ridge", 1.237273, 103.866943),
        Station("one-north", 1.233616, 103.868188),
        Station("Holland Village", 1.229959, 103.869433),
        Station("Farrer Road", 1.226302, 103.870678)
        // Add more stations here...
    )

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val LONG_PRESS_TIMEOUT = 3000L // 3 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Handle the case when the language is not supported or missing data
                }
            } else {
                // Handle the case when TextToSpeech initialization fails
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Request location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        mMap.setOnMapLongClickListener { latLng ->
            // When the map is long-pressed, find the nearest station and read it out
            val nearestStation = findNearestStation(latLng)
            if (nearestStation != null) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        mMap.addMarker(MarkerOptions().position(userLatLng).title("You are here"))

                        val nearestStationLatLng = LatLng(nearestStation.latitude, nearestStation.longitude)
                        mMap.addMarker(MarkerOptions().position(nearestStationLatLng).title(nearestStation.name))

                        val distanceToNearestStation = calculateDistance(userLatLng, nearestStationLatLng)
                        val message = "The nearest MRT Station to you is ${nearestStation.name}. " +
                                "It is approximately ${String.format("%.2f", distanceToNearestStation)} meters away."
                        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            } else {
                Toast.makeText(this, "No MRT station found nearby.", Toast.LENGTH_SHORT).show()
            }
        }

        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(userLatLng).title("You are here"))

                val nearestStation = findNearestStation(userLatLng)
                if (nearestStation != null) {
                    val nearestStationLatLng = LatLng(nearestStation.latitude, nearestStation.longitude)
                    mMap.addMarker(MarkerOptions().position(nearestStationLatLng).title(nearestStation.name))
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
        }
    }

    private fun findNearestStation(userLatLng: LatLng): Station? {
        var nearestStation: Station? = null
        var nearestDistance = Float.MAX_VALUE

        for (station in stations) {
            val stationLatLng = LatLng(station.latitude, station.longitude)
            val distance = calculateDistance(userLatLng, stationLatLng)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestStation = station
            }
        }

        return nearestStation
    }

    // You can use the Haversine formula or other distance calculation methods
    private fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startLatLng.latitude,
            startLatLng.longitude,
            endLatLng.latitude,
            endLatLng.longitude,
            results
        )
        return results[0]
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation()
            } else {
                // Handle the case when location permission is denied by the user
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to release the TextToSpeech resources when the activity is destroyed
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
