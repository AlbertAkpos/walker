package me.alberto.walker

import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val RC_LOCATION_PERMISSION = 100
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var map: GoogleMap
    private var permisionsGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        getLocationPermission()
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permisionsGranted = true
            getDeviceLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                RC_LOCATION_PERMISSION
            )
        }

    }

    private fun updateDeviceUI() {
        map ?: return

        try {
            if (permisionsGranted) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                Toast.makeText(this, "Allow app to access device location", Toast.LENGTH_LONG)
                    .show()

            }
        } catch (error: SecurityException) {
            Log.d(TAG, error.message.toString())
        }
    }

    @RequiresPermission(value = android.Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getDeviceLocation() {
        try {
            if (permisionsGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val lastLocation = task.result
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastLocation!!.latitude, lastLocation.longitude),
                                14.0F
                            )
                        )
                        map.animateCamera(CameraUpdateFactory.zoomTo(18F))

                        val androidOverlay = GroundOverlayOptions()
                            .image(BitmapDescriptorFactory.fromResource(R.drawable.optimus))
                            .position(LatLng(lastLocation.latitude, lastLocation.longitude), 100f)
                    }

                }
            }
        } catch (error: Exception) {
            Log.d(TAG, error.message.toString())
        }
    }

    private fun init() {
        Places.initialize(applicationContext, getString(R.string.app_key))
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            map = it
            setMapStyle(map)
            setMapLongClick(map)
            updateDeviceUI()
            setPOIClick(map)
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (exp: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", exp)
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(), "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.app_name))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )

        }
    }

    private fun setPOIClick(map: GoogleMap) {
        map.setOnPoiClickListener { pointOfInterest ->
            val poiMarker = map.addMarker(
                MarkerOptions().position(pointOfInterest.latLng)
                    .title(pointOfInterest.name)
            )
            poiMarker.showInfoWindow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.normal_map -> {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.hybrid_map -> {
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            R.id.terrain_map -> {
                map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            R.id.satellite_map -> {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

