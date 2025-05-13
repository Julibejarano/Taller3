package com.example.taller3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var selectedImageUri: Uri? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data!!.data
                binding.imageProfile.setImageURI(selectedImageUri)
            } else {
                Toast.makeText(this, "No se seleccionó imagen", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestLocationPermission()
        obtenerUbicacion()

        binding.btnSelectImage.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        binding.btnRegistrar.setOnClickListener {
            validarCamposYCrearUsuario()
        }
    }

    private fun validarCamposYCrearUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val apellido = binding.etApellido.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val id = binding.etId.text.toString().trim()
        val latitud = binding.etLatitud.text.toString().toDoubleOrNull()
        val longitud = binding.etLongitud.text.toString().toDoubleOrNull()

        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() ||
            password.isEmpty() || id.isEmpty() || selectedImageUri == null ||
            latitud == null || longitud == null) {
            Toast.makeText(this, "No es posible crear el usuario, hay campos incompletos", Toast.LENGTH_LONG).show()
            return
        }

        // Registrar usuario en Firebase Auth
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Subir imagen de perfil
                    val storageRef = Firebase.storage.reference.child("profile_images/${id}.jpg")
                    selectedImageUri?.let { uri ->
                        storageRef.putFile(uri)
                            .addOnSuccessListener { taskSnapshot ->
                                // Obtener URL de descarga
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    // Guardar datos adicionales en Firestore
                                    val user = hashMapOf(
                                        "nombre" to nombre,
                                        "apellido" to apellido,
                                        "email" to email,
                                        "id" to id,
                                        "latitud" to latitud,
                                        "longitud" to longitud,
                                        "imagenPerfil" to downloadUri.toString(),
                                        "estado" to "disponible" // o "desconectado"
                                    )

                                    FirebaseFirestore.getInstance().collection("usuarios")
                                        .document(id)
                                        .set(user)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error guardando datos en Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error subiendo imagen: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error registrando usuario: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkStoragePermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
    }

    private fun obtenerUbicacion() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location?.let {
                latitude = it.latitude
                longitude = it.longitude
                binding.etLatitud.setText(latitude.toString())
                binding.etLongitud.setText(longitude.toString())
            }
        }
    }
}
