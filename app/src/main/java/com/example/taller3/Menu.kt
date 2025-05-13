package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityMenuBinding

class  Menu : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding
    private var disponible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar listeners para los botones
        binding.btnToggleStatus.setOnClickListener {
            alternarEstado()
        }

        binding.btnLogout.setOnClickListener {
            cerrarSesion()
        }

        binding.btnListUsers.setOnClickListener {
            abrirListaUsuarios()
        }
    }

    private fun alternarEstado() {
        disponible = !disponible
        binding.btnToggleStatus.text = if (disponible) "Disponible" else "Desconectado"
        Toast.makeText(this, "Estado: ${binding.btnToggleStatus.text}", Toast.LENGTH_SHORT).show()
    }

    private fun cerrarSesion() {
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        // Lógica para cerrar sesión
    }

    private fun abrirListaUsuarios() {
        val intent = Intent(this, ListaUsuariosActivity::class.java)
        startActivity(intent)
    }
}
