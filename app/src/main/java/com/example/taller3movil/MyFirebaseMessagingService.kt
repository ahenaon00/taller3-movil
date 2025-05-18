package com.example.taller3movil

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth // Asumiendo que usas Firebase Auth
import com.google.firebase.database.FirebaseDatabase // Para guardar el token
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Prioriza el manejo de la carga de datos, que es lo que enviaría la Cloud Function
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Jugador Disponible"
            val body = remoteMessage.data["body"] ?: "¡Alguien está esperando para jugar!"
            sendNotification(title, body)
        }

        else if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification!!.body}")
            sendNotification(
                remoteMessage.notification!!.title ?: "Título predeterminado",
                remoteMessage.notification!!.body ?: "Mensaje"
            )
        }
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }


    private fun sendRegistrationToServer(token: String?) {
        token?.let {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                // Guarda el token en Realtime Database bajo el ID del usuario
                // Ejemplo: /usuarios/{userId}/fcmToken
                val database = FirebaseDatabase.getInstance().getReference("usuarios")
                database.child(userId).child("fcmToken").setValue(token)
                    .addOnSuccessListener { Log.d(TAG, "FCM token saved to Realtime Database") }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to save FCM token", e) }
            } else {
                Log.w(TAG, "User not logged in, cannot save FCM token.")
            }
        }
    }

    /**
     * Crea y muestra una notificación simple.
     */
    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java) // O la actividad que quieras abrir
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            pendingIntentFlag
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android Oreo (API 26), los Canales de Notificación son obligatorios.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jugador Disponible", // Nombre visible del canal en la configuración del teléfono
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que aparezca como heads-up notification (en la parte superior de la pantalla)
            )
            channel.description = "Notificaciones cuando hay jugadores disponibles"
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* Un ID único para esta notificación */, notificationBuilder.build())
    }
}