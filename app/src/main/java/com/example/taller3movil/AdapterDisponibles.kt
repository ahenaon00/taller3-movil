package com.example.taller3movil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class AdapterDisponibles(var contexto: Context,var recurso: Int, var listadoUsuarios : List<Usuario>) : ArrayAdapter<Usuario>(contexto, recurso, listadoUsuarios) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(contexto)
        val view = layoutInflater.inflate(recurso,parent, false)
        val nombreUsuario = view.findViewById<TextView>(R.id.nombreUsuario)
        val imagenUsuario = view.findViewById<ImageView>(R.id.imageUsuario)
        if(listadoUsuarios[position].fotoUrl != "")
            Glide.with(contexto).load(listadoUsuarios[position].fotoUrl).into(imagenUsuario)
        nombreUsuario.text = listadoUsuarios[position].nombre + " " + listadoUsuarios[position].apellidos
        return view
    }
}