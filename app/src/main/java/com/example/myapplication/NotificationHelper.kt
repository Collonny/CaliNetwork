package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = "park_proximity_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Park Proximity"
            val descriptionText = "Notifications when you are near a workout park"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showParkNotification(context: Context, park: WorkoutPark) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Zameniti sa odgovarajućom ikonicom
            .setContentTitle("Park u blizini!")
            .setContentText("Nalazite se u blizini parka: ${park.name}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // 🔹 ISPRAVKA: Provera dozvole pre slanja notifikacije
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Ako dozvola nije data, ne radimo ništa. Alternativno, mogli bismo da logujemo poruku.
            return
        }
        NotificationManagerCompat.from(context).notify(park.id.hashCode(), builder.build())
    }

    fun showUserNotification(context: Context, user: UserLocation) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Zameniti sa odgovarajućom ikonicom
            .setContentTitle("Korisnik u blizini!")
            .setContentText("Korisnik ${user.username} je u vašoj blizini.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(user.userId.hashCode(), builder.build())
    }
}
