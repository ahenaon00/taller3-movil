package com.example.taller3movil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.taller3movil.databinding.FragmentDisponiblesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class DisponiblesFragment : BottomSheetDialogFragment() {
    private var listaUsuarios: MutableList<Usuario> = mutableListOf()
    private lateinit var binding : FragmentDisponiblesBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDisponiblesBinding.inflate(inflater, container, false)
        cargarDatos()
        return binding.root
    }
    private fun cargarDatos() {
        val ref = FirebaseDatabase.getInstance().getReference("disponibles")
        ref.limitToFirst(10).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listaCheckeo = mutableListOf<Usuario>()
                for (child in snapshot.children) {
                    val usuario = child.getValue(Usuario::class.java)
                    if (usuario != null) {
                        listaCheckeo.add(usuario)
                    }
                }
                listaUsuarios = listaCheckeo
                val adapter = AdapterDisponibles(requireContext(), R.layout.customrowusers, listaUsuarios)
                binding.listUsuarios.adapter = adapter
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

}