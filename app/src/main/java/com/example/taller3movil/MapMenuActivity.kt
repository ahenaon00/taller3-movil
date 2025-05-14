package com.example.taller3movil

import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3movil.databinding.ActivityMapMenuBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapMenuBinding
    lateinit var map : MapView
    val BOGOTA = GeoPoint(4.62, -74.07)
    lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationActual: GeoPoint? = null
    private var markerLocationActual: Marker? = null
    val RADIUS_EARTH_METERS = 6378137
    private var permisoSolicitado = false
    private var gpsDialogShown = false

    val locationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if (it.resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "El GPS está apagado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            permisoSolicitado = true
            if (it) {
                locationSettings()
            } else {
                Toast.makeText(this, "No hay permiso para acceder al GPS", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMapa()
        inicializarListenersBotones()
        inicializarSuscrLocalizacion()
    }

    private fun inicializarSuscrLocalizacion() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
        suscribirLocalizacion()
    }

    private fun suscribirLocalizacion() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido, continuar con la lógica de localización
            locationSettings()
        } else {
            // Permiso no concedido
            if (!permisoSolicitado && shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar explicación si el usuario ya lo denegó antes
                Toast.makeText(this, "El permiso es necesario para acceder a las funciones de localización.", Toast.LENGTH_SHORT).show()
            }
            // Solicitar el permiso (ya sea la primera vez o después de la explicación)
            if (!permisoSolicitado) {
                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun setupMapa() {
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
    }

    private fun inicializarListenersBotones() {
        binding.cerrarSesion.setOnClickListener {
            // TO DO
        }
        binding.disponible.setOnClickListener {
            // TO DO
        }
        binding.listarDisponibles.setOnClickListener {
            // TO DO
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.controller.setZoom(18.0)

        // Resetear el estado del diálogo cuando la actividad vuelve a primer plano
        gpsDialogShown = false

        locationActual?.let {
            map.controller.animateTo(it)
        } ?: run {
            map.controller.animateTo(BOGOTA)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSettings()
        } else if (!permisoSolicitado) {
            locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        stopLocationUpdates()
    }

    fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // GPS está activado, comenzar actualizaciones
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Mostrar diálogo solo si no lo hemos mostrado antes
                    if (!gpsDialogShown) {
                        val isr: IntentSenderRequest =
                            IntentSenderRequest.Builder(exception.resolution).build()
                        locationSettings.launch(isr)
                        gpsDialogShown = true
                    } else {
                        // Ya mostramos el diálogo, no volver a pedir
                        Toast.makeText(this, "Por favor activa el GPS en ajustes", Toast.LENGTH_LONG).show()
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Error al acceder al GPS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(5000)
            .build()
        return request
    }

    private fun createLocationCallback(): LocationCallback {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                if (loc != null) {
                    updateUI(loc)
                }
            }
        }
        return callback
    }

    fun updateUI(location: Location) {
        val newLocation = GeoPoint(location.latitude, location.longitude)

        var moverCamara = false

        if (locationActual == null) {
            moverCamara = true
        } else {
            val distancia = distance(
                locationActual!!.latitude,
                locationActual!!.longitude,
                newLocation.latitude,
                newLocation.longitude
            )

            if (distancia > 30.0) {
                moverCamara = true
            }
        }

        if (moverCamara) {
            locationActual = newLocation
            map.controller.animateTo(newLocation)
            Log.i("LOCATION", "Moviendo cámara a nueva ubicación: $newLocation")
        }

        markerLocationActual?.let {
            map.overlays.remove(it)
        }

        val marker = Marker(map)
        marker.position = newLocation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Mi ubicación"
        marker.snippet = "Estás aquí"
        marker.icon = ContextCompat.getDrawable(this, R.drawable.baseline_location_24)

        map.overlays.add(marker)
        markerLocationActual = marker
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_EARTH_METERS * c
        return Math.round(result * 100.0) / 100.0
    }
}