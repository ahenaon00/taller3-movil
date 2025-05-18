package com.example.taller3movil

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(
   var nombre: String = "",
   var apellidos: String = "",
   var correo: String = "",
   var fotoUrl: String = ""
) : Parcelable
