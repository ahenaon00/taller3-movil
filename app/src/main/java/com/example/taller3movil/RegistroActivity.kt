package com.example.taller3movil

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3movil.MainActivity
import com.example.taller3movil.MapMenuActivity
import com.example.taller3movil.LoginUsuarioActivity
import com.example.taller3movil.databinding.ActivityRegistroBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.database
import java.util.regex.Pattern

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setUpBinding()

        binding.ingresarRegistro.setOnClickListener {
            validarYRegistrarUsuario()
        }

        binding.botonBackRegistro.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MapMenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun validarYRegistrarUsuario() {
        val nombre = binding.nombreRegistro.text.toString()
        val apellidos = binding.apellidosRegistro.text.toString()
        val email = binding.correoRegistro.text.toString()
        val password = binding.contraseARegistro.text.toString()

        if (nombre.isEmpty() || apellidos.isEmpty()  || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
        } else {
            registerUser(nombre, apellidos, email, password)
        }
    }

    private fun registerUser(nombre: String, apellidos: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    guardarDatosUsuario(nombre, apellidos, email)
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginUsuarioActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun guardarDatosUsuario(nombre: String, apellidos: String, email : String) {
        val usuario = FirebaseAuth.getInstance().currentUser
        val uid = usuario?.uid
        if (uid != null) {
            val datosUsuario = mapOf(
                "nombre" to nombre,
                "apellidos" to apellidos,
                "correo" to email
            )

            val database = Firebase.database.reference
            val userRef = database.child("usuarios").child(uid)
            userRef.setValue(datosUsuario)
                .addOnSuccessListener {
                    //IMPLEMENTAR ALGUNA VAINA
                }
                .addOnFailureListener { e ->
                    println("Error al guardar los datos del usuario: ${e.message}")
                }
        } else {
            println("No hay un usuario autenticado.")
        }
    }

    private fun setUpBinding() {
        binding.correoRegistro.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val email = s.toString()
                val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
                val pattern = Pattern.compile(emailRegex)
                if (email.isNotEmpty() && !pattern.matcher(email).matches()) {
                    binding.correoRegistro.error = "Correo no válido"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.contraseARegistro.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val password = s.toString()
                if (password.isNotEmpty() && password.length < 6) {
                    binding.contraseARegistro.error = "Mínimo 6 caracteres"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}