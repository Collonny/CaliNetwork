package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// 🔹 DODATO
import com.example.myapplication.UserLocation
import com.example.myapplication.WorkoutPark

object NotificationHelper {

    private const val CHANNEL_ID = "park_proximity_channel"
    private const val CHANNEL_NAME = "Proximity Alerts"
    private const val CHANNEL_DESCRIPTION = "Notifications when you are near a park or another user"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showParkNotification(context: Context, park: WorkoutPark) {
        val notificationId = park.name.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle("Park u blizini!")
            .setContentText("Nalazite se u blizini parka: ${park.name}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun showUserNotification(context: Context, otherUser: UserLocation) {
        val notificationId = otherUser.userId.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle("Korisnik u blizini!")
            .setContentText("Nalazite se u blizini korisnika: ${otherUser.username}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
