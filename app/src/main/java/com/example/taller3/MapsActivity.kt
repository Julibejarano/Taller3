package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller3.databinding.ActivityMapsBinding
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar configuración de OSM
        org.osmdroid.config.Configuration.getInstance().load(applicationContext, android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        // Inicializar el MapView
        mapView = MapView(this)
        binding.mapFrameLayout.addView(mapView)  // Asegúrate de que el mapFrameLayout esté en tu layout XML

        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Cargar puntos de interés
        loadInterestPoints()

        // Verificar permisos para la ubicación
        checkPermissions()
    }

    private fun loadInterestPoints() {
        try {
            // Cargar el JSON de los assets
            val json = loadJSONFromAsset("locations.json")
            val locationsObj = JSONObject(json).getJSONArray("locationsArray")

            // Iterar sobre los puntos de interés y agregarlos al mapa
            for (i in 0 until locationsObj.length()) {
                val location = locationsObj.getJSONObject(i)
                val latitude = location.getDouble("latitude")
                val longitude = location.getDouble("longitude")
                val name = location.getString("name")

                val point = GeoPoint(latitude, longitude)
                val marker = Marker(mapView)
                marker.position = point
                marker.title = name

                // Agregar el marcador al mapa
                mapView.overlays.add(marker)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error cargando los puntos de interés", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadJSONFromAsset(fileName: String): String {
        val json: String
        try {
            val inputStream = assets.open(fileName)
            json = inputStream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return json
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                // Obtén la ubicación del dispositivo
                val currentLocation = GeoPoint(location.latitude, location.longitude)

                // Establece el centro del mapa y el zoom
                val mapController: IMapController = mapView.controller
                mapController.setZoom(15)
                mapController.setCenter(currentLocation)

                // Agrega un marcador para la ubicación actual
                val currentLocationMarker = Marker(mapView)
                currentLocationMarker.position = currentLocation
                currentLocationMarker.title = "Tu ubicación"
                mapView.overlays.add(currentLocationMarker)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
