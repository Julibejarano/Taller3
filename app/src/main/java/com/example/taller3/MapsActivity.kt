package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller3.databinding.ActivityMapsBinding
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variables para el seguimiento
    private var usuarioSeguido: Usuario? = null
    private var marcadorUsuarioSeguido: Marker? = null
    private var miUbicacion: GeoPoint? = null
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var actualizacionRunnable: Runnable
    private var lineaDistancia: Polyline? = null

    // Función para cargar puntos de interés
    private fun loadInterestPoints() {
        try {
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

                mapView.overlays.add(marker)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error cargando los puntos de interés", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar configuración de OSM
        org.osmdroid.config.Configuration.getInstance().load(applicationContext, android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        // Inicializar el MapView
        mapView = MapView(this)
        binding.mapFrameLayout.addView(mapView)  // Asegúrate de que el mapFrameLayout esté en tu layout XML

        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Verificar si se abrió para seguimiento
        intent.extras?.let {
            if (it.containsKey("nombre")) {
                val nombre = it.getString("nombre", "")
                val lat = it.getDouble("lat", 0.0)
                val lon = it.getDouble("lon", 0.0)

                // Iniciar seguimiento
                usuarioSeguido = Usuario(nombre, "", lat, lon)
                iniciarSeguimiento()
            } else {
                // Cargar puntos de interés normales
                loadInterestPoints()
            }
        } ?: loadInterestPoints()

        checkPermissions()
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
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                miUbicacion = currentLocation  // Guardar mi ubicación

                // Establece el centro del mapa y el zoom
                val mapController: IMapController = mapView.controller
                mapController.setZoom(15)
                mapController.setCenter(currentLocation)

                // Agrega un marcador para la ubicación actual
                val currentLocationMarker = Marker(mapView)
                currentLocationMarker.position = currentLocation
                currentLocationMarker.title = "Tu ubicación"
                mapView.overlays.add(currentLocationMarker)

                // Si estamos en modo seguimiento, calcular distancia
                if (usuarioSeguido != null) {
                    calcularYMostrarDistancia()
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // FUNCIONES PARA EL SEGUIMIENTO
    private fun iniciarSeguimiento() {
        usuarioSeguido?.let { usuario ->
            // Mostrar el panel de distancia
            binding.tvDistancia.visibility = View.VISIBLE

            // Agregar marcador del usuario seguido
            val posicionUsuario = GeoPoint(usuario.lat, usuario.lon)
            marcadorUsuarioSeguido = Marker(mapView).apply {
                position = posicionUsuario
                title = usuario.nombre
            }
            mapView.overlays.add(marcadorUsuarioSeguido)

            // Centrar el mapa en el usuario seguido
            val mapController: IMapController = mapView.controller
            mapController.setZoom(15)
            mapController.setCenter(posicionUsuario)

            // Configurar actualización periódica (simulación)
            configurarActualizacionTiempoReal()
        }
    }

    private fun configurarActualizacionTiempoReal() {
        actualizacionRunnable = Runnable {
            usuarioSeguido?.let { usuario ->
                // Crear un nuevo objeto Usuario con las coordenadas actualizadas
                val nuevaLat = usuario.lat + (Math.random() - 0.5) * 0.001
                val nuevaLon = usuario.lon + (Math.random() - 0.5) * 0.001
                usuarioSeguido = Usuario(usuario.nombre, usuario.urlImagen, nuevaLat, nuevaLon)

                // Actualizar posición del marcador
                val nuevaPosicion = GeoPoint(nuevaLat, nuevaLon)
                marcadorUsuarioSeguido?.position = nuevaPosicion

                // Recalcular distancia
                calcularYMostrarDistancia()

                // Redibujar mapa
                mapView.invalidate()

                // Programar próxima actualización
                handler.postDelayed(actualizacionRunnable, 3000)
            }
        }

        // Iniciar actualizaciones
        handler.postDelayed(actualizacionRunnable, 3000)
    }

    private fun calcularYMostrarDistancia() {
        miUbicacion?.let { miPos ->
            usuarioSeguido?.let { usuario ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    miPos.latitude, miPos.longitude,
                    usuario.lat, usuario.lon,
                    results
                )
                val distancia = results[0]

                val formateador = DecimalFormat("#,###.##")
                val distanciaTexto = when {
                    distancia < 1000 -> "${formateador.format(distancia)} metros"
                    else -> "${formateador.format(distancia / 1000)} kilómetros"
                }

                binding.tvDistancia.text = "Distancia a ${usuario.nombre}: $distanciaTexto"

                // Dibujar línea entre los dos puntos
                dibujarLineaDistancia(miPos, GeoPoint(usuario.lat, usuario.lon))
            }
        }
    }

    private fun dibujarLineaDistancia(puntoInicio: GeoPoint, puntoFin: GeoPoint) {
        // Eliminar línea anterior si existe
        lineaDistancia?.let {
            mapView.overlays.remove(it)
        }

        // Crear nueva línea
        lineaDistancia = Polyline().apply {
            outlinePaint.color = Color.RED
            outlinePaint.strokeWidth = 5f

            // Agregar los puntos
            addPoint(puntoInicio)
            addPoint(puntoFin)
        }

        // Agregar la línea al mapa
        mapView.overlays.add(lineaDistancia)

        // Actualizar el mapa
        mapView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener actualizaciones cuando se destruye la actividad
        if (::actualizacionRunnable.isInitialized) {
            handler.removeCallbacks(actualizacionRunnable)
        }
    }
}
