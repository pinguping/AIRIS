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
        Station("Farrer Road", 1.226302, 103.870678),
        Station("Marymount", 1.303407, 103.860657),
        Station("Lorong Chuan", 1.301233, 103.861823),
        Station("Serangoon", 1.305657, 103.863068),
        Station("Bartley", 1.307872, 103.864388),
        Station("Tai Seng", 1.309991, 103.865698),
        Station("Dakota", 1.312109, 103.867008),
        Station("Mountbatten", 1.314227, 103.868318),
        Station("Stadium", 1.299794, 103.870343),
        Station("Nicoll Highway", 1.297600, 103.871548),
        Station("Farrer Park", 1.295406, 103.872753),
        Station("Boon Keng", 1.293212, 103.873958),
        Station("Potong Pasir", 1.291018, 103.875163),
        Station("Woodleigh", 1.288824, 103.876368),
        Station("Kovan", 1.286630, 103.877573),
        Station("Hougang", 1.284436, 103.878778),
        Station("Buangkok", 1.282242, 103.879983),
        Station("Sengkang", 1.280048, 103.881188),
        Station("Punggol", 1.277854, 103.882393),
        Station("Sentosa", 1.239843, 103.844360),
        Station("Renjong", 1.280463, 103.886368),
        Station("Tongkang", 1.278269, 103.887573),
        Station("Layar", 1.276075, 103.888778),
        Station("Fernvale", 1.273881, 103.889983),
        Station("Thanggam", 1.271687, 103.891188),
        Station("Kupang", 1.269493, 103.892393),
        Station("Farmway", 1.267299, 103.893598),
        Station("Cheng Lim", 1.265105, 103.894798),
        Station("Compassvale", 1.262911, 103.895998),
        Station("Rumbia", 1.260717, 103.897198),
        Station("Bakau", 1.258523, 103.898398),
        Station("Kangkar", 1.256329, 103.899598),
        Station("Ranggung", 1.256329, 103.899598),
        Station("Damai", 1.254135, 103.900798),
        Station("Oasis", 1.251941, 103.901998),
        Station("Kadaloor", 1.249747, 103.903198),
        Station("Riviera", 1.247553, 103.904398),
        Station("Coral Edge", 1.245359, 103.905598),
        Station("Meridian", 1.243165, 103.906798),
        Station("Cove", 1.240971, 103.907998),
        Station("Soo Teck", 1.238777, 103.909198),
        Station("Sumang", 1.236583, 103.910398),
        Station("Nibong", 1.234389, 103.911598),
        Station("Samudera", 1.232195, 103.912798),
        Station("Punggol Point", 1.229911, 103.913998),
        Station("Sam Kee", 1.227727, 103.915198),
        Station("South View", 1.225543, 103.916398),
        Station("Keat Hong", 1.223359, 103.917598),
        Station("Tech Whye", 1.221175, 103.918798),
        Station("Phoenix", 1.218991, 103.919998),
        Station("Senja", 1.216807, 103.921198),
        Station("Jelapang", 1.214623, 103.922398),
        Station("Segar", 1.212439, 103.923598),
        Station("Fajar", 1.210255, 103.924798),
        Station("Bangkit", 1.208071, 103.925998),
        Station("Pending", 1.205887, 103.927198),
        Station("Petir", 1.203703, 103.928398),
        Station("Woodlands North", 1.403618, 103.825883),
        Station("Woodlands", 1.404184, 103.826071),
        Station("Woodlands South", 1.404750, 103.826259),
        Station("Springleaf", 1.359237, 103.827343),
        Station("Lentor", 1.359803, 103.827531),
        Station("Mayflower", 1.360369, 103.827719),
        Station("Bright Hill", 1.360935, 103.827907),
        Station("Upper Thomson", 1.361501, 103.828095),
        Station("Caldecott", 1.362067, 103.828283),
        Station("Mount Pleasant", 1.354430, 103.843746),
        Station("Stevens", 1.354996, 103.843934),
        Station("Napier", 1.355562, 103.844122),
        Station("Orchard Boulevard", 1.356128, 103.844310),
        Station("Orchard", 1.356694, 103.844498),
        Station("Great World", 1.357260, 103.844686),
        Station("Havelock", 1.357826, 103.844874),
        Station("Outram Park", 1.358392, 103.845062),
        Station("Maxwell", 1.358958, 103.845249),
        Station("Shenton Way", 1.359524, 103.845437),
        Station("Marina Bay", 1.360090, 103.845625),
        Station("Marina South", 1.360656, 103.845813),
        Station("Gardens by the Bay", 1.361222, 103.845999),
        Station("Founders' Memorial", 1.361788, 103.846187),
        Station("Tanjong Rhu", 1.354184, 103.855010),
        Station("Katong Park", 1.354750, 103.855202),
        Station("Tanjong Katong", 1.355316, 103.855394),
        Station("Marine Parade", 1.355882, 103.855586),
        Station("Marine Terrace", 1.356448, 103.855778),
        Station("Siglap", 1.356978, 103.855970),
        Station("Bayshore", 1.357486, 103.856162),
        Station("Bedok South", 1.358108, 103.856354),
        Station("Sungei Bedok", 1.326507, 103.849691),
        Station("Tuas Link", 1.300720, 103.875195),
        Station("Tuas West Road", 1.300183, 103.875643),
        Station("Tuas Crescent", 1.299646, 103.876091),
        Station("Gul Circle", 1.299109, 103.876539),
        Station("Joo Koon", 1.293916, 103.879692),
        Station("Pioneer", 1.289991, 103.882038),
        Station("Boon Lay", 1.285879, 103.884598),
        Station("Lakeside", 1.281325, 103.887567),
        Station("Chinese Gardens", 1.279113, 103.888954),
        Station("Jurong East", 1.277306, 103.890195),
        Station("Clementi", 1.274683, 103.892786),
        Station("Dover", 1.273019, 103.894130),
        Station("Buona Vista", 1.271473, 103.895414),
        Station("Commonwealth", 1.269754, 103.896850),
        Station("Queenstown", 1.268290, 103.898109),
        Station("Redhill", 1.266801, 103.899346),
        Station("Tiong Bahru", 1.265075, 103.900758),
        Station("Tanjong Pagar", 1.263501, 103.901952),
        Station("Raffles Place", 1.262082, 103.903104),
        Station("City Hall", 1.260679, 103.904204),
        Station("Bugis", 1.259097, 103.905422),
        Station("Lavendar", 1.257638, 103.906499),
        Station("Kallang", 1.256089, 103.907703),
        Station("Aljunied", 1.254540, 103.908948),
        Station("Paya Lebar", 1.252991, 103.910162),
        Station("Kovan", 1.251442, 103.911364),
        Station("Kembangan", 1.249893, 103.912565),
        Station("Bedok", 1.248344, 103.913766),
        Station("Tanah Merah", 1.246811, 103.914919),
        Station("Expo", 1.245295, 103.915981),
        Station("Changi Airport", 1.357477, 103.982239),
        Station("Simei", 1.354996, 103.843934),
        Station("Tampines", 1.336360, 103.860573),
        Station("Pasir Ris", 1.328529, 103.872323),
        Station("Bukit Batok", 1.309886, 103.889638),
        Station("Bukit Gombak", 1.305769, 103.892456),
        Station("Choa Chu Kang", 1.301652, 103.895274),
        Station("Yew Tee", 1.297535, 103.898092),
        Station("Kranji", 1.293418, 103.900909),
        Station("Marsiling", 1.289301, 103.903726),
        Station("Admiralty", 1.316182, 103.882401),
        Station("Sembawang", 1.312065, 103.885219),
        Station("Canberra", 1.307948, 103.888036),
        Station("Yishun", 1.303831, 103.890853),
        Station("Khatib", 1.299714, 103.893670),
        Station("Yio Chu Kang", 1.295597, 103.896487),
        Station("Ang Mo Kio", 1.291480, 103.899304),
        Station("Bishan", 1.287363, 103.902121),
        Station("Braddell", 1.283246, 103.904938),
        Station("Toa Payoh", 1.279129, 103.907755),
        Station("Novena", 1.275012, 103.910572),
        Station("Newton", 1.270895, 103.913389),
        Station("Somerset", 1.266778, 103.916206),
        Station("Dhoby Ghout", 1.262661, 103.919023),
        Station("Marina South Pier", 1.258544, 103.921840),
        Station("Bukit Panjang Cashew", 1.322289, 103.897510),
        Station("Hillview", 1.318172, 103.899943),
        Station("Beauty World", 1.314055, 103.902370),
        Station("King Albert Park", 1.309938, 103.904807),
        Station("Sixth Avenue", 1.305821, 103.907244),
        Station("Tan Kah Kee", 1.301704, 103.909681),
        Station("Botanic Gardens", 1.287432, 103.828342),
        Station("Little India", 1.311845, 103.852158),
        Station("Rochor", 1.316903, 103.853989),
        Station("Promenade", 1.258624, 103.849948),
        Station("Bayfront", 1.258473, 103.852075),
        Station("Downtown", 1.260996, 103.853119),
        Station("Telok Ayer", 1.263501, 103.854104),
        Station("Chinatown", 1.265795, 103.855225),
        Station("Fort Canning", 1.270712, 103.856384),
        Station("Bencoolen", 1.273019, 103.857630),
        Station("Jalan Besar", 1.275326, 103.858876),
        Station("Bendemeer", 1.277633, 103.860122),
        Station("Geylang Bahru", 1.279939, 103.861368),
        Station("Mattar", 1.282246, 103.862614),
        Station("MacPherson", 1.284553, 103.863860),
        Station("Ubi", 1.286860, 103.865106),
        Station("Kaki Bukit", 1.289167, 103.866352),
        Station("Bedok North", 1.291210, 103.867798),
        Station("Bedok Reservoir", 1.293555, 103.869013)

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
                                "It is approximately ${String.format("%.0f", distanceToNearestStation)} meters away."
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