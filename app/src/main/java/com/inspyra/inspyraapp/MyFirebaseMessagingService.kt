package com.inspyra.inspyraapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage



class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Se ejecuta cuando se recibe una notificación
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val channelId = "default_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 🔊 1. Sonido por defecto
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 📢 2. Canal para Android 8+ con vibración y sonido
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones INSPYRA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "INSPYRA"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "Mensaje recibido"
        val accion = remoteMessage.data["accion"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("accion", accion)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 🔔 3. Builder con sonido, vibración y estilo expandido
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.principal)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri) // 🔊 Sonido
            .setVibrate(longArrayOf(0, 400, 200, 400)) // 📳 Vibración
            .setLights(Color.GREEN, 500, 500) // 💡 LED (opcional)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }




    // Se ejecuta cuando se genera un nuevo token FCM (por primera vez o si se actualiza)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token FCM: $token")

        // Aquí podrías enviarlo a tu backend si quieres guardar los tokens para notificaciones personalizadas
        // Por ejemplo: enviarTokenAlServidor(token)
    }
}