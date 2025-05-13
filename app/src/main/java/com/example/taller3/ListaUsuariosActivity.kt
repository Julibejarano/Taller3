package com.example.taller3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ListaUsuariosActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

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
                    // Manejar error
                    return@addSnapshotListener
                }

                val usuarios = mutableListOf<Usuario>()

                for (document in snapshot!!) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuarios.add(usuario)
                }

                recyclerView.adapter = UsuarioAdapter(usuarios) { usuario ->
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("nombre", usuario.nombre)
                    intent.putExtra("apellido", usuario.apellido)
                    intent.putExtra("lat", usuario.lat)
                    intent.putExtra("lon", usuario.lon)
                    startActivity(intent)
                }
            }
    }
}
