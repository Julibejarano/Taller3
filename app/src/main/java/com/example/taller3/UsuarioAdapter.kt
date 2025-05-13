package com.example.taller3

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsuarioAdapter(
    private val usuarios: List<Usuario>,
    private val onVerPosicionClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPerfil: ImageView = view.findViewById(R.id.imgPerfil)
        val txtNombre: TextView = view.findViewById(R.id.txtNombre)
        val btnVerPosicion: Button = view.findViewById(R.id.btnVerPosicion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.txtNombre.text = usuario.nombre

        // Si tienes imágenes, puedes cargarlas con Glide o Picasso
        // Glide.with(holder.imgPerfil.context).load(usuario.urlImagen).into(holder.imgPerfil)

        holder.btnVerPosicion.setOnClickListener {
            val intent = Intent(holder.itemView.context, MapsActivity::class.java)
            intent.putExtra("nombre", usuario.nombre)
            intent.putExtra("lat", usuario.lat)
            intent.putExtra("lon", usuario.lon)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = usuarios.size
}
