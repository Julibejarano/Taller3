package com.example.taller3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflamos el layout para la pantalla de Login
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lógica de login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Validación básica (agregar validaciones adicionales según se requiera)
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Aquí iría la lógica de login (ej. Firebase Auth)
                // Si la autenticación es exitosa, navegaríamos a la siguiente pantalla
                // Por ahora solo mostramos un mensaje de éxito
                // Puedes agregar tu lógica de autenticación con Firebase aquí
                // startActivity(Intent(this, HomeActivity::class.java))
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                binding.tvErrorMessage.text = "Por favor ingresa tu email y contraseña"
            }
        }

        // Navegar a la pantalla de registro
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
