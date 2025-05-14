package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore

    // Variables para el seguimiento
    private var usuarioSeguido: Usuario? = null
    private var marcadorUsuarioSeguido: Marker? = null
    private var miUbicacion: GeoPoint? = null
    private var lineaDistancia: Polyline? = null
    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance()

        // Inicializar configuración de OSM
        org.osmdroid.config.Configuration.getInstance().load(applicationContext, android.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        // Inicializar el MapView
        mapView = MapView(this)
        binding.mapFrameLayout.addView(mapView)

        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(true)
        mapView.setMultiTouchControls(true)

        // Cargar SIEMPRE los puntos de interés
        loadInterestPoints()

        // Verificar si se abrió para seguimiento
        intent.extras?.let {
            if (it.containsKey("usuario_id")) {
                val usuarioId = it.getString("usuario_id", "")
                Log.d(TAG, "Siguiendo usuario por ID: $usuarioId")
                escucharActualizacionEnTiempoReal(usuarioId)
            } else if (it.containsKey("nombre") && it.containsKey("lat") && it.containsKey("lon")) {
                val nombre = it.getString("nombre", "")
                val lat = it.getDouble("lat", 0.0)
                val lon = it.getDouble("lon", 0.0)

                Log.d(TAG, "Siguiendo usuario por coordenadas: $nombre en $lat, $lon")

                // Crear un usuario con estos datos
                val usuario = Usuario(nombre = nombre, lat = lat, lon = lon)
                usuarioSeguido = usuario
                iniciarSeguimiento(usuario)
            }
        }

        checkPermissions()
    }

    private fun loadInterestPoints() {
        try {
            val json = loadJSONFromAsset("locations.json")
            val locationsObj = JSONObject(json).getJSONArray("locationsArray")

            // Obtener un ícono personalizado para los puntos de interés
            val iconoAzul = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass)
            iconoAzul?.setTint(Color.BLUE) // Cambiar el color a azul

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
                marker.icon = iconoAzul // Asignar el ícono azul
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Centrar el ícono

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
                miUbicacion = currentLocation  // Guardar mi ubicación

                Log.d(TAG, "Ubicación actual: ${location.latitude}, ${location.longitude}")

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
    private fun escucharActualizacionEnTiempoReal(usuarioId: String) {
        Log.d(TAG, "Iniciando escucha para usuario ID: $usuarioId")
        db.collection("usuarios").document(usuarioId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error escuchando cambios: ${e.message}")
                    Toast.makeText(this, "Error escuchando cambios: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    try {
                        // Extraer datos manualmente
                        val nombre = documentSnapshot.getString("nombre") ?: ""
                        val urlImagen = documentSnapshot.getString("imagenPerfil") ?: ""
                        val latitud = documentSnapshot.getDouble("latitud") ?: 0.0
                        val longitud = documentSnapshot.getDouble("longitud") ?: 0.0

                        Log.d(TAG, "Datos recibidos: $nombre en $latitud, $longitud")

                        // Crear o actualizar el usuario seguido
                        if (usuarioSeguido == null) {
                            usuarioSeguido = Usuario(nombre = nombre, urlImagen = urlImagen, lat = latitud, lon = longitud)
                            iniciarSeguimiento(usuarioSeguido!!)
                        } else {
                            usuarioSeguido = usuarioSeguido?.copy(lat = latitud, lon = longitud)
                            // Actualizar marcador
                            marcadorUsuarioSeguido?.position = GeoPoint(latitud, longitud)
                            // Calcular distancia
                            calcularYMostrarDistancia()
                            mapView.invalidate()
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error procesando documento: ${ex.message}")
                    }
                } else {
                    Log.d(TAG, "El documento no existe")
                }
            }
    }

    private fun iniciarSeguimiento(usuario: Usuario) {
        Log.d(TAG, "Iniciando seguimiento para: ${usuario.nombre}")

        // Mostrar el panel de distancia
        binding.tvDistancia.visibility = View.VISIBLE

        // Agregar marcador del usuario seguido
        val posicionUsuario = GeoPoint(usuario.lat, usuario.lon)
        marcadorUsuarioSeguido = Marker(mapView).apply {
            position = posicionUsuario
            title = usuario.nombre
            // Usar un ícono distintivo para el usuario seguido
            val iconoUsuario = ContextCompat.getDrawable(this@MapsActivity, android.R.drawable.ic_menu_myplaces)
            iconoUsuario?.setTint(Color.RED)
            icon = iconoUsuario
        }
        mapView.overlays.add(marcadorUsuarioSeguido)

        // Centrar el mapa en el usuario seguido
        val mapController: IMapController = mapView.controller
        mapController.setZoom(15)
        mapController.setCenter(posicionUsuario)

        // Calcular distancia inicial si ya tenemos nuestra ubicación
        miUbicacion?.let {
            calcularYMostrarDistancia()
        }
    }

    private fun calcularYMostrarDistancia() {
        miUbicacion?.let { miPos ->
            usuarioSeguido?.let { usuario ->
                Log.d(TAG, "Calculando distancia entre ${miPos.latitude},${miPos.longitude} y ${usuario.lat},${usuario.lon}")

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
        // No es necesario limpiar nada más, ya que estamos usando Firestore listeners
        // que se limpian automáticamente cuando la actividad se destruye
    }
}
