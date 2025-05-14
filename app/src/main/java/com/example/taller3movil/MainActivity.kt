package com.example.taller3movil

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3movil.databinding.ActivityMainBinding
import com.example.taller3movil.databinding.ActivityMapMenuBinding
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MapMenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registrate.setOnClickListener {
            startActivity(Intent(baseContext, RegistarActivity::class.java))
        }

        binding.iniciarSesion.setOnClickListener {
            startActivity(Intent(baseContext, LoginActivity::class.java))
        }
    }
}