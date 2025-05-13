package com.example.taller3

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_logout -> {
                    cerrarSesion()
                    true
                }
                R.id.action_toggle_status -> {
                    alternarEstado(item)
                    true
                }
                R.id.action_list_users -> {
                    abrirListaUsuarios()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

// Variables y funciones auxiliares
        private var disponible = true

        private fun alternarEstado(item: MenuItem) {
            disponible = !disponible
            item.title = if (disponible) "Disponible" else "Desconectado"
            Toast.makeText(this, "Estado: ${item.title}", Toast.LENGTH_SHORT).show()
        }

        private fun cerrarSesion() {
            // Aquí puedes limpiar datos, cerrar sesión, y/o navegar a LoginActivity
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            // finish() o startActivity(Intent(this, LoginActivity::class.java))
        }

        private fun abrirListaUsuarios() {
            // Lanza la pantalla de listado de usuarios
            val intent = Intent(this, ListaUsuariosActivity::class.java)
            startActivity(intent)
        }

    }
}