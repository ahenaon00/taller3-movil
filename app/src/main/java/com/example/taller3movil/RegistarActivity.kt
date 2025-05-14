package com.example.taller3movil

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3movil.databinding.ActivityRegistarBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import java.util.UUID
import java.util.regex.Pattern

class RegistarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistarBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private var tempNombre: String = ""
    private var tempApellidos: String = ""
    private var tempEmail: String = ""
    private var tempPassword: String = ""

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        storage = Firebase.storage("gs://taller3-fad0b.firebasestorage.app")
        setUpBinding()

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImageUri = uri
            procederConValidacionYRegistro()
        }

        binding.ingresarRegistro.setOnClickListener {
            tempNombre = binding.nombreRegistro.text.toString()
            tempApellidos = binding.apellidosRegistro.text.toString()
            tempEmail = binding.correoRegistro.text.toString()
            tempPassword = binding.contraseARegistro.text.toString()

            pickImageLauncher.launch("image/*")
        }

        binding.botonBackRegistro.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MapMenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun procederConValidacionYRegistro() {
        if (tempNombre.isEmpty() || tempApellidos.isEmpty() || tempEmail.isEmpty() || tempPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
        } else {
            registerUser(tempNombre, tempApellidos, tempEmail, tempPassword)
        }
    }

    private fun registerUser(nombre: String, apellidos: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    user?.let { firebaseUser ->
                        if (selectedImageUri != null) {
                            subirImagenyGuardarDatos(firebaseUser, nombre, apellidos, email, selectedImageUri!!)
                        } else {
                            guardarDatosUsuario(firebaseUser, nombre, apellidos, email, "")
                        }
                    }
                } else {
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun subirImagenyGuardarDatos(
        firebaseUser: FirebaseUser,
        nombre: String,
        apellidos: String,
        email: String,
        imageUri: Uri
    ) {
        val fileName = UUID.randomUUID().toString()
        val imageRef = storage.reference.child("profile_images/$fileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    guardarDatosUsuario(firebaseUser, nombre, apellidos, email, downloadUrl.toString())
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al obtener URL de imagen", Toast.LENGTH_SHORT).show()
                    guardarDatosUsuario(firebaseUser, nombre, apellidos, email, "")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al subir imagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                guardarDatosUsuario(firebaseUser, nombre, apellidos, email, "")
            }
    }


    private fun guardarDatosUsuario(firebaseUser: FirebaseUser, nombre: String, apellidos: String, email : String, fotoUrl: String) {
        val uid = firebaseUser.uid
        val datosUsuario = mutableMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "correo" to email
        )
        if (fotoUrl.isNotEmpty()) {
            datosUsuario["fotoUrl"] = fotoUrl
        }

        val database = Firebase.database.reference
        val userRef = database.child("usuarios").child(uid)
        userRef.setValue(datosUsuario)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
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