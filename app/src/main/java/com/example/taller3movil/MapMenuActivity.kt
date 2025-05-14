package com.example.taller3movil

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3movil.databinding.ActivityMapMenuBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MapMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapMenuBinding
    lateinit var map : MapView
    val BOGOTA = GeoPoint(4.62, -74.07)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMapa()
    }

    private fun setupMapa() {
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.
            MAPNIK)
        map.setMultiTouchControls(true)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.
        controller.setZoom(18.0)
        map.
        controller.animateTo(BOGOTA)
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}