package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Variables para seguimiento
    private var usuarioSeguido: Usuario? = null
    private var marcadorUsuarioSeguido: Marker? = null
    private var miUbicacion: GeoPoint? = null
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var locationUpdateRunnable: Runnable
    private var lineaDistancia: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar configuración de OSM
        org.osmdroid.config.Configuration.getInstance().load(
            applicationContext,
            android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        mapView = MapView(this)
        binding.mapFrameLayout.addView(mapView)
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        loadInterestPoints()
        checkPermissions()

        // Verificar si se pasó un usuario para seguir
        intent.extras?.let {
            if (it.containsKey("usuario_id")) {
                val usuarioId = it.getString("usuario_id", "")
                escucharActualizacionEnTiempoReal(usuarioId)
            }
        }
    }

    private fun loadInterestPoints() {
        try {
            val json = loadJSONFromAsset("locations.json")
            val locationsObj = JSONObject(json).getJSONArray("locationsArray")

            for (i in 0 until locationsObj.length()) {
                val location = locationsObj.getJSONObject(i)
                val latitude = location.getDouble("latitude")
                val longitude = location.getDouble("longitude")
                val name = location.getString("name")
                val point = GeoPoint(latitude, longitude)
                val marker = Marker(mapView).apply {
                    position = point
                    title = name
                }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación", Toast.LENGTH_SHORT).show()
            }
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
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                miUbicacion = currentLocation

                val mapController: IMapController = mapView.controller
                mapController.setZoom(15)
                mapController.setCenter(currentLocation)

                val currentLocationMarker = Marker(mapView)
                currentLocationMarker.position = currentLocation
                currentLocationMarker.title = "Tu ubicación"
                mapView.overlays.add(currentLocationMarker)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun escucharActualizacionEnTiempoReal(usuarioId: String) {
        db.collection("usuarios").document(usuarioId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error escuchando cambios: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val usuario = documentSnapshot.toObject(Usuario::class.java)
                    usuario?.let {
                        usuarioSeguido = it
                        iniciarSeguimiento(it)
                    }
                }
            }
    }

    private fun iniciarSeguimiento(usuario: Usuario) {
        val posicionUsuario = GeoPoint(usuario.lat, usuario.lon)
        marcadorUsuarioSeguido = Marker(mapView).apply {
            position = posicionUsuario
            title = "${usuario.nombre} ${usuario.apellido}"
        }
        mapView.overlays.add(marcadorUsuarioSeguido)

        val mapController: IMapController = mapView.controller
        mapController.setZoom(15)
        mapController.setCenter(posicionUsuario)

        startDistanceUpdates(usuario.id)
    }

    private fun startDistanceUpdates(usuarioId: String) {
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                db.collection("usuarios").document(usuarioId).get()
                    .addOnSuccessListener { document ->
                        val usuario = document.toObject(Usuario::class.java)
                        usuario?.let {
                            usuarioSeguido = it
                            marcadorUsuarioSeguido?.position = GeoPoint(it.lat, it.lon)
                            calcularYMostrarDistancia()
                            mapView.invalidate()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@MapsActivity, "Error obteniendo ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                handler.postDelayed(this, 3000)
            }
        }

        handler.post(locationUpdateRunnable)
    }

    private fun calcularYMostrarDistancia() {
        miUbicacion?.let { miPos ->
            usuarioSeguido?.let { usuario ->
                val results = FloatArray(1)
                Location.distanceBetween(miPos.latitude, miPos.longitude, usuario.lat, usuario.lon, results)
                val distancia = results[0]
                val formateador = DecimalFormat("#,###.##")
                val distanciaTexto = when {
                    distancia < 1000 -> "${formateador.format(distancia)} metros"
                    else -> "${formateador.format(distancia / 1000)} kilómetros"
                }
                binding.tvDistancia.text = "Distancia a ${usuario.nombre}: $distanciaTexto"
                dibujarLineaDistancia(miPos, GeoPoint(usuario.lat, usuario.lon))
            }
        }
    }

    private fun dibujarLineaDistancia(puntoInicio: GeoPoint, puntoFin: GeoPoint) {
        lineaDistancia?.let {
            mapView.overlays.remove(it)
        }

        lineaDistancia = Polyline().apply {
            outlinePaint.color = android.graphics.Color.RED
            outlinePaint.strokeWidth = 5f
            addPoint(puntoInicio)
            addPoint(puntoFin)
        }

        mapView.overlays.add(lineaDistancia)
        mapView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // ✅ onCreateOptionsMenu corregido
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_menu, menu)
        return true
    }

    // ✅ onOptionsItemSelected corregido
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_toggle_status -> {
                val intent = Intent(this, Menu::class.java)
                startActivity(intent)
                return true
            }
            R.id.menu_list_users -> {
                val intent = Intent(this, ListaUsuariosActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.menu_logout -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                finish()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}