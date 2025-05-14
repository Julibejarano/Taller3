package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Menu : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private var disponible = true
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Alternar estado localmente
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
        val nuevoEstado = if (disponible) "disponible" else "desconectado"

        // Actualizar en Firebase
        auth.currentUser?.let { user ->
            db.collection("usuarios").document(user.uid)
                .update("estado", nuevoEstado)
                .addOnSuccessListener {
                    Toast.makeText(this, "Estado actualizado: $nuevoEstado", Toast.LENGTH_SHORT).show()
                    binding.btnToggleStatus.text = nuevoEstado.capitalize()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        finish()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun abrirListaUsuarios() {
        val intent = Intent(this, ListaUsuariosActivity::class.java)
        startActivity(intent)
    }
}