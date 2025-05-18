package com.example.taller3movil

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class AdapterDisponibles(
    var contexto: Context,
    var recurso: Int,
    var listadoUsuarios: List<Usuario>
) : ArrayAdapter<Usuario>(contexto, recurso, listadoUsuarios) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater: LayoutInflater = LayoutInflater.from(contexto)
        val view = layoutInflater.inflate(recurso, parent, false)

        val nombreUsuario = view.findViewById<TextView>(R.id.nombreUsuario)
        val imagenUsuario = view.findViewById<ImageView>(R.id.imageUsuario)
        val botonVerPosicion = view.findViewById<Button>(R.id.verPosicionButton)

        val usuario = listadoUsuarios[position]
        nombreUsuario.text = "${usuario.nombre} ${usuario.apellidos}"

        if (usuario.fotoUrl.isNotEmpty()) {
            Glide.with(contexto).load(usuario.fotoUrl).into(imagenUsuario)
        }

        botonVerPosicion.setOnClickListener {
            val correo = usuario.correo
            val ref = FirebaseDatabase.getInstance().getReference("disponibles")
            ref.orderByChild("correo").equalTo(correo)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (usuarioSnap in snapshot.children) {
                            val pushId = usuarioSnap.key
                            val nombre = "${usuario.nombre} ${usuario.apellidos}"

                            val intent = Intent(contexto, MapMenuActivity::class.java)
                            intent.putExtra("disponible_id", pushId)
                            intent.putExtra("usuario_nombre", nombre)
                            intent.putExtra("tipo", "seguimiento")
                            contexto.startActivity(intent)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(contexto, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        return view
    }
}
