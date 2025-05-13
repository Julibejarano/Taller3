package com.example.taller3

data class Usuario(
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val id: String = "",
    val urlImagen: String = "",
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    val estado: String = ""
)
