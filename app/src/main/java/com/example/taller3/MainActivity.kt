package com.example.taller3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, MapsActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            binding.tvErrorMessage.text = "Error al iniciar sesión: ${task.exception?.message}"
                        }
                    }
            } else {
                binding.tvErrorMessage.text = "Por favor ingresa tu email y contraseña"
            }
        }

        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
