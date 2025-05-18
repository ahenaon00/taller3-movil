package com.example.taller3movil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.taller3movil.databinding.FragmentDisponiblesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener



class DisponiblesFragment : BottomSheetDialogFragment() {
    private var listaUsuarios: MutableList<Usuario> = mutableListOf()
    private lateinit var binding : FragmentDisponiblesBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var userActual : Usuario? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisponiblesBinding.inflate(inflater, container, false)
        cargarUsuario()
        return binding.root
    }
    private fun cargarDatos() {

        val ref = FirebaseDatabase.getInstance().getReference("disponibles")
        ref.limitToFirst(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaCheckeo = mutableListOf<Usuario>()
                for (child in snapshot.children) {
                    val usuario = child.getValue(Usuario::class.java)
                    if (usuario != null && usuario.correo != userActual?.correo) {
                        listaCheckeo.add(usuario)
                    }
                }
                listaUsuarios = listaCheckeo
                if(isAdded && view!=null ) {
                    val adapter = AdapterDisponibles(requireContext(), R.layout.customrowusers, listaUsuarios)
                    binding.listUsuarios.adapter = adapter
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun cargarUsuario() {
        firebaseAuth = FirebaseAuth.getInstance()
        val refUsuarios = FirebaseDatabase.getInstance().getReference("usuarios")
        val currentUser = firebaseAuth.currentUser
        val query = refUsuarios.orderByChild("correo").equalTo(currentUser?.email)
        query.get().addOnSuccessListener { snapshot ->
            if(snapshot.exists()) {
                for (userSnapshot in snapshot.children) {
                    val correo = userSnapshot.child("correo").value.toString()
                    userActual = Usuario("","",correo,"")
                }
            }
            cargarDatos()
        }.addOnFailureListener {
            //
        }
    }

}