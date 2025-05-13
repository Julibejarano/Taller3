package com.example.taller3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListaUsuariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewUsuarios)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Simulación de usuarios disponibles
        val usuarios = listOf(
            Usuario("Juan Pérez", "", 4.6, -74.1),
            Usuario("Ana Gómez", "", 4.62, -74.08),
            Usuario("Carlos Ruiz", "", 4.65, -74.12)
        )

        recyclerView.adapter = UsuarioAdapter(usuarios) { usuario ->
            // Al pulsar el botón "Ver posición"
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("nombre", usuario.nombre)
            intent.putExtra("lat", usuario.lat)
            intent.putExtra("lon", usuario.lon)
            startActivity(intent)
        }
    }
}
