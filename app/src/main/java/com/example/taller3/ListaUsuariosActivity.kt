package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ListaUsuariosActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val TAG = "ListaUsuariosActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewUsuarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = FirebaseFirestore.getInstance()

        // Cargar usuarios desde Firestore
        db.collection("usuarios")
            .whereEqualTo("estado", "disponible")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Error obteniendo usuarios", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val usuarios = mutableListOf<Usuario>()

                    for (document in snapshot) {
                        try {
                            // Extraer datos manualmente
                            val nombre = document.getString("nombre") ?: ""
                            val apellido = document.getString("apellido") ?: ""
                            val email = document.getString("email") ?: ""
                            val id = document.getString("id") ?: ""
                            val imagenPerfil = document.getString("imagenPerfil") ?: ""
                            // Obtener latitud y longitud con los nombres correctos
                            val latitud = document.getDouble("latitud") ?: 0.0
                            val longitud = document.getDouble("longitud") ?: 0.0
                            val estado = document.getString("estado") ?: "desconectado"

                            // Crear objeto Usuario con los datos extraídos
                            // Nota: asignamos latitud a lat y longitud a lon
                            val usuario = Usuario(
                                nombre = nombre,
                                apellido = apellido,
                                email = email,
                                id = id,
                                urlImagen = imagenPerfil,
                                lat = latitud,  // Asignamos latitud a lat
                                lon = longitud, // Asignamos longitud a lon
                                estado = estado
                            )

                            usuarios.add(usuario)
                            Log.d(TAG, "Usuario agregado: $nombre, latitud: $latitud, longitud: $longitud")
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error procesando documento: ${document.id}", ex)
                        }
                    }

                    Log.d(TAG, "Total usuarios: ${usuarios.size}")

                    // Actualizar el RecyclerView con los usuarios obtenidos
                    recyclerView.adapter = UsuarioAdapter(usuarios) { usuario ->
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.putExtra("nombre", usuario.nombre)
                        intent.putExtra("lat", usuario.lat)
                        intent.putExtra("lon", usuario.lon)
                        startActivity(intent)
                    }
                } else {
                    Log.d(TAG, "No hay usuarios disponibles")
                    // Mostrar lista vacía
                    recyclerView.adapter = UsuarioAdapter(emptyList()) { _ -> }
                }
            }
    }
}
