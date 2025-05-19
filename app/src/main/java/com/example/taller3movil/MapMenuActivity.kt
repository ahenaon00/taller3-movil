package com.example.taller3movil

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class MapMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapMenuBinding
    lateinit var map: MapView
    val BOGOTA = GeoPoint(4.62, -74.07)
    lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationActual: GeoPoint? = null
    private var markerLocationActual: Marker? = null
    val RADIUS_EARTH_METERS = 6378137
    private var permisoSolicitado = false
    private var gpsDialogShown = false
    private var refPush: DatabaseReference? = null
    private var disponible: Boolean = false
    private var usuarioActual: Usuario? = null
    private var usuarioMarker: Marker? = null
    private var seguimientoRef: DatabaseReference? = null
    private var seguimientoListener: ValueEventListener? = null
    private var ultimaUbicacionSeguido: GeoPoint? = null


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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones permitidas en MapMenu", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Las notificaciones están deshabilitadas en MapMenu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gpsDialogShown = false
        binding = ActivityMapMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMapa()
        cargarUbicacionesDeArchivo()
        inicializarListenersBotones()
        inicializarSuscrLocalizacion()
        askNotificationPermission()
        val tipo = intent.getStringExtra("tipo")
        if (tipo == "seguimiento") {
            binding.botonDetenerSeguimiento.visibility = View.VISIBLE
        }
    }

    private fun inicializarSeguimiento() {
        if (intent.getStringExtra("tipo") == "seguimiento") {
            val pushId = intent.getStringExtra("disponible_id")
            val correo = intent.getStringExtra("correo")
            val nombre = intent.getStringExtra("usuario_nombre") ?: "Usuario disponible"

            if (pushId != null) {
                seguimientoRef = FirebaseDatabase.getInstance().getReference("disponibles").child(pushId)
                iniciarListenerSeguimiento(seguimientoRef!!, nombre)
            } else if (correo != null) {
                // Buscar en "disponibles" al usuario por correo
                val disponiblesRef = FirebaseDatabase.getInstance().getReference("disponibles")
                disponiblesRef.orderByChild("correo").equalTo(correo)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                seguimientoRef = child.ref
                                val nombreEncontrado = child.child("nombre").value?.toString() ?: nombre
                                iniciarListenerSeguimiento(seguimientoRef!!, nombreEncontrado)
                                return
                            }
                            Toast.makeText(this@MapMenuActivity, "Usuario no encontrado en disponibles", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("SEGUIMIENTO", "Error al buscar usuario por correo: ${error.message}")
                        }
                    })
            }
        }
    }

    private fun iniciarListenerSeguimiento(ref: DatabaseReference, nombre: String) {
        seguimientoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latStr = snapshot.child("latitude").value?.toString()
                val lonStr = snapshot.child("longitude").value?.toString()
                val lat = latStr?.toDoubleOrNull()
                val lon = lonStr?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val point = GeoPoint(lat, lon)

                    if (usuarioMarker == null) {
                        usuarioMarker = Marker(map).apply {
                            title = nombre
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(this@MapMenuActivity, R.drawable.ic_usuario_disponible)
                            map.overlays.add(this)
                        }
                    }

                    usuarioMarker?.position = point
                    map.invalidate()

                    // 🔍 Solo mover la cámara si se ha desplazado más de 30 metros
                    val moverCamara = ultimaUbicacionSeguido?.let { anterior ->
                        distance(anterior.latitude, anterior.longitude, point.latitude, point.longitude) > 30
                    } ?: true // Si no hay anterior, mover siempre

                    if (moverCamara) {
                        map.controller.animateTo(point)
                        ultimaUbicacionSeguido = point
                    }

                    if (locationActual != null) {
                        val actual = locationActual!!
                        val distancia = distance(point.latitude, point.longitude, actual.latitude, actual.longitude)
                        Toast.makeText(baseContext, "Distancia: $distancia metros", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(baseContext, "No se puede calcular la distancia", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SEGUIMIENTO", "Error al suscribirse: ${error.message}")
            }
        }

        ref.addValueEventListener(seguimientoListener!!)
    }


    private fun inicializarSuscrLocalizacion() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()
        suscribirLocalizacion()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MapMenuActivity", "Notification permission already granted.")
                // Ya tienes el permiso
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // Opcional: Muestra una UI explicando por qué necesitas el permiso.
                // Por ahora, solo lo solicitamos.
                Log.d("MapMenuActivity", "Showing rationale or requesting permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Solicita el permiso
                Log.d("MapMenuActivity", "Requesting notification permission.")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun sendCloud(playerName: String, correo: String) {
        val url =
            "https://us-central1-taller3-fad0b.cloudfunctions.net/notifyAvailablePlayerIndividual"
        val payload = Gson().toJson(mapOf("name" to playerName, "correo" to correo))
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, payload)
        val request = Request.Builder().url(url).post(body).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM_FUNC", "Error calling CF", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) Log.e("FCM_FUNC", "CF error: ${response.code}")
                else Log.i("FCM_FUNC", "CF success: \${response.body?.string()}")
                response.close()
            }
        })
    }

    private fun suscribirLocalizacion() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permiso concedido, continuar con la lógica de localización
            locationSettings()
        } else {
            // Permiso no concedido
            if (!permisoSolicitado && shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar explicación si el usuario ya lo denegó antes
                Toast.makeText(
                    this,
                    "El permiso es necesario para acceder a las funciones de localización.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Solicitar el permiso (ya sea la primera vez o después de la explicación)
            if (!permisoSolicitado) {
                locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun setupMapa() {
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.MAPNIK
        )
        map.setMultiTouchControls(true)
    }

    private fun inicializarListenersBotones() {
        binding.cerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        binding.listarDisponibles.setOnClickListener {
            val bottomSheet = DisponiblesFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        binding.disponible.setOnClickListener {
            if (!disponible) {
                // ACTIVAR DISPONIBILIDAD
                disponible = true
                binding.disponible.text = "No estoy disponible"

                val user = FirebaseAuth.getInstance().currentUser
                Log.i("USER", user?.email.toString())

                val ref = FirebaseDatabase.getInstance().getReference()
                val refDisponibles = ref.child("disponibles")
                val uniqueId = refDisponibles.push()
                refPush = uniqueId

                val refUsuarios = ref.child("usuarios")
                val query = refUsuarios.orderByChild("correo").equalTo(user?.email)
                query.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val nombre = userSnapshot.child("nombre").value.toString()
                            val apellidos = userSnapshot.child("apellidos").value.toString()
                            val fotoUrl = userSnapshot.child("fotoUrl").value.toString()
                            val correo = userSnapshot.child("correo").value.toString()
                            val usuario = Usuario(nombre, apellidos, correo, fotoUrl)
                            uniqueId.setValue(usuario)
                            val locActual = mapOf(
                                "latitude" to locationActual?.latitude.toString(),
                                "longitude" to locationActual?.longitude.toString()
                            )
                            uniqueId.updateChildren(locActual)
                            usuarioActual = usuario

                            // Enviar notificación
                            usuarioActual?.let { u -> sendCloud(u.nombre, u.correo) }
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e(
                        "DatabaseError",
                        "Error al obtener los datos de la base de datos: ${e.message}"
                    )
                }
            } else {
                // DESACTIVAR DISPONIBILIDAD
                disponible = false
                binding.disponible.text = "Estoy disponible"

                // Borrar nodo de Firebase
                refPush?.removeValue()
                refPush = null

                Toast.makeText(this, "Ya no estás disponible", Toast.LENGTH_SHORT).show()
            }
        }

        binding.botonDetenerSeguimiento.setOnClickListener {
            seguimientoListener?.let { listener ->
                seguimientoRef?.removeEventListener(listener)
            }
            seguimientoListener = null
            seguimientoRef = null

            usuarioMarker?.let {
                map.overlays.remove(it)
                map.invalidate()
            }
            usuarioMarker = null

            binding.botonDetenerSeguimiento.visibility = View.GONE
            locationActual?.let {
                map.controller.animateTo(it)
                Log.i("SEGUIMIENTO", "Cámara movida a la ubicación del usuario actual: $it")
            } ?: run {
                Log.w("SEGUIMIENTO", "No se pudo mover la cámara: ubicación actual nula")
            }
        }


    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.controller.setZoom(18.0)


        val tipo = intent.getStringExtra("tipo")
        if (tipo != "seguimiento") {
            locationActual?.let {
                map.controller.animateTo(it)
            } ?: run {
                map.controller.animateTo(BOGOTA)
            }
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
        seguimientoListener?.let { listener ->
            seguimientoRef?.removeEventListener(listener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refPush?.removeValue()
        disponible = false
    }

    fun locationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS está activado, comenzar actualizaciones
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Mostrar diálogo solo si no se ha mostrado antes en esta sesión
                    if (!gpsDialogShown) {
                        val isr: IntentSenderRequest =
                            IntentSenderRequest.Builder(exception.resolution).build()
                        locationSettings.launch(isr)
                        gpsDialogShown = true // Marcar que ya se mostró el diálogo
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Error al intentar activar el GPS", Toast.LENGTH_SHORT).show()
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
        if (disponible) {
            val locActual = mapOf(
                "latitude" to location.latitude.toString(),
                "longitude" to location.longitude.toString()
            )
            refPush?.updateChildren(locActual)
        }
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

            val tipo = intent.getStringExtra("tipo")
            if (tipo != "seguimiento") {
                map.controller.animateTo(newLocation)
                Log.i("LOCATION", "Moviendo cámara a nueva ubicación: $newLocation")
            }
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

        // ✅ Iniciar seguimiento solo cuando haya ubicación actual
        if (intent.getStringExtra("tipo") == "seguimiento" && seguimientoListener == null) {
            inicializarSeguimiento()
        }
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

    private fun cargarUbicacionesDeArchivo() {
        try {
            val inputStream = assets.open("locations.json")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = bufferedReader.use { it.readText() }

            val jsonObject = JSONObject(jsonString)
            val ubicacionesArray: JSONArray = jsonObject.getJSONArray("locationsArray")

            for (i in 0 until ubicacionesArray.length()) {
                val ubicacion = ubicacionesArray.getJSONObject(i)

                val lat = ubicacion.getDouble("latitude")
                val lon = ubicacion.getDouble("longitude")
                val nombre = ubicacion.getString("name")

                val punto = GeoPoint(lat, lon)
                val marker = Marker(map).apply {
                    position = punto
                    title = nombre
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(this@MapMenuActivity, R.drawable.baseline_interest_location_24)
                }
                map.overlays.add(marker)
            }
            map.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error leyendo ubicaciones JSON: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}