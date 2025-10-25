package com.example.myapplication

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions

// --- DATA KLASE ---
data class WorkoutPark(
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("opis") @set:PropertyName("opis") var opis: String = "",
    @get:PropertyName("latituda") @set:PropertyName("latituda") var latituda: Double = 0.0,
    @get:PropertyName("longituda") @set:PropertyName("longituda") var longituda: Double = 0.0,
    @get:PropertyName("rating") @set:PropertyName("rating") var rating: Float = 0.0f,
    @get:PropertyName("createdBy") @set:PropertyName("createdBy") var createdBy: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Timestamp? = null
) {
    constructor() : this("", "", 0.0, 0.0, 0.0f, "", null)
}

data class UserLocation(
    val userId: String = "",
    val username: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)

// --- SERVIS ---
object FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    fun getWorkoutParks(onResult: (List<WorkoutPark>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection("workout_park")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                val parks = snapshot?.toObjects(WorkoutPark::class.java) ?: emptyList()
                onResult(parks)
            }
    }

    fun updateUserLocation(userId: String, lat: Double, lng: Double) {
        val locationData = mapOf(
            "userId" to userId,
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to Timestamp.now()
        )
        db.collection("user_locations").document(userId).set(locationData, SetOptions.merge())
    }

    fun listenForOtherUsers(currentUserId: String, onResult: (List<UserLocation>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection("user_locations")
            .whereNotEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                val users = snapshot?.toObjects(UserLocation::class.java) ?: emptyList()
                onResult(users)
            }
    }

    fun addWorkoutPark(
        name: String,
        opis: String,
        lat: Double,
        lng: Double,
        creatorId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val newPark = WorkoutPark(
            name = name,
            opis = opis,
            latituda = lat,
            longituda = lng,
            rating = 0.0f,
            createdBy = creatorId,
            createdAt = Timestamp.now()
        )

        db.collection("workout_park")
            .add(newPark)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
