package com.example.myapplication

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

// --- DATA KLASE ---

data class User(
    @get:PropertyName("Ime") @set:PropertyName("Ime") var name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val points: Long = 0
) { constructor() : this("", "", "", "", 0) }

data class Challenge(
    @get:PropertyName("bestUser") @set:PropertyName("bestUser") var bestUser: String = "",
    @get:PropertyName("bestScore") @set:PropertyName("bestScore") var bestScore: Long = 0L
) { constructor() : this("", 0L) }

data class Rating(
    @get:PropertyName("average") @set:PropertyName("average") var average: Double = 0.0,
    @get:PropertyName("brRatings") @set:PropertyName("brRatings") var brRatings: Long = 0L,
    @get:PropertyName("userRatings") @set:PropertyName("userRatings") var userRatings: Map<String, Any> = emptyMap()
) { constructor() : this(0.0, 0L, emptyMap()) }

data class WorkoutPark(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("opis") @set:PropertyName("opis") var opis: String = "",
    @get:PropertyName("latituda") @set:PropertyName("latituda") var latituda: Double = 0.0,
    @get:PropertyName("longituda") @set:PropertyName("longituda") var longituda: Double = 0.0,
    @get:PropertyName("rating") @set:PropertyName("rating") var rating: Rating = Rating(),
    @get:PropertyName("createdBy") @set:PropertyName("createdBy") var createdBy: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Timestamp? = null,
    @get:PropertyName("challenges") @set:PropertyName("challenges") var challenges: Map<String, Challenge> = emptyMap()
) { constructor() : this("", "", "", 0.0, 0.0, Rating(), "", null, emptyMap()) }

data class UserRecord(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("parkId") @set:PropertyName("parkId") var parkId: String = "",
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "",
    @get:PropertyName("score") @set:PropertyName("score") var score: Long = 0L,
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Timestamp = Timestamp.now()
) { 
    constructor() : this("", "", "", 0L, Timestamp.now()) 
}

data class UserLocation(
    val userId: String = "",
    val username: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)

data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val bestScores: Map<String, Long>,
    val totalScore: Long
)

data class UserProfileData(
    val user: User,
    val createdParks: List<WorkoutPark>,
    val userRecords: List<UserRecord>
)

// --- SERVIS ---
object FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    fun getWorkoutParks(onResult: (List<WorkoutPark>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onError(Exception("Korisnik nije ulogovan."))
            return ListenerRegistration { }
        }

        return db.collection("workout_park").addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            val parks = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WorkoutPark::class.java)?.apply { id = doc.id }
            } ?: emptyList()
            onResult(parks)
        }
    }

    fun updateUserLocation(userId: String, lat: Double, lng: Double) {
        val locationData = mapOf("userId" to userId, "latitude" to lat, "longitude" to lng, "lastUpdated" to Timestamp.now())
        db.collection("user_locations").document(userId).set(locationData, SetOptions.merge())
    }

    fun addRecord(record: UserRecord, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection("records").add(record).addOnSuccessListener { 
            onSuccess()
            checkAndUpdateBestScore(record)
        }.addOnFailureListener { onError(it) }
    }

    private fun checkAndUpdateBestScore(record: UserRecord) {
        val parkRef = db.collection("workout_park").document(record.parkId)
        db.runTransaction { transaction ->
            val park = transaction.get(parkRef).toObject(WorkoutPark::class.java) ?: return@runTransaction
            val currentChallenge = park.challenges[record.type]
            if (currentChallenge == null || record.score > currentChallenge.bestScore) {
                val newBest = Challenge(bestUser = FirebaseAuth.getInstance().currentUser?.displayName ?: "Nepoznat", bestScore = record.score)
                val updatedChallenges = park.challenges.toMutableMap()
                updatedChallenges[record.type] = newBest
                transaction.update(parkRef, "challenges", updatedChallenges)
            }
        }
    }

    fun addWorkoutPark(name: String, opis: String, lat: Double, lng: Double, creatorId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val defaultChallenges = mapOf("zgibovi" to Challenge(), "sklekovi" to Challenge(), "propadanja" to Challenge())
        val newPark = WorkoutPark(name = name, opis = opis, latituda = lat, longituda = lng, createdBy = creatorId, createdAt = Timestamp.now(), challenges = defaultChallenges, rating = Rating())
        db.collection("workout_park").add(newPark).addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it) }
    }

    fun ratePark(parkId: String, userId: String, newRating: Int, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val parkRef = db.collection("workout_park").document(parkId)
        db.runTransaction { transaction ->
            val park = transaction.get(parkRef).toObject(WorkoutPark::class.java) ?: throw Exception("Park nije pronađen")
            val currentRating = park.rating
            val oldUserRating = (currentRating.userRatings[userId] as? Number)?.toInt()
            val updatedUserRatings = currentRating.userRatings.toMutableMap()
            updatedUserRatings[userId] = newRating.toLong()
            val newTotalScore = (currentRating.average * currentRating.brRatings) - (oldUserRating ?: 0) + newRating
            val newBrRatings = if (oldUserRating == null) currentRating.brRatings + 1 else currentRating.brRatings
            val newAverage = if (newBrRatings > 0) newTotalScore / newBrRatings else 0.0
            val newRatingData = Rating(average = newAverage, brRatings = newBrRatings, userRatings = updatedUserRatings)
            transaction.update(parkRef, "rating", newRatingData)
            null
        }.addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it) }
    }

    suspend fun getLeaderboardData(): List<LeaderboardEntry> {
        try {
            val recordsSnapshot = db.collection("records").get().await()
            val usersSnapshot = db.collection("users").get().await()

            val userNames = usersSnapshot.documents.associate { it.id to (it.getString("Ime") ?: "Nepoznat") }
            val records = recordsSnapshot.toObjects(UserRecord::class.java)

            val userScores = records.groupBy { it.userId }

            val leaderboardEntries = userScores.map { (userId, userRecords) ->
                val bestScores = mutableMapOf<String, Long>()
                bestScores["zgibovi"] = userRecords.filter { it.type == "zgibovi" }.maxOfOrNull { it.score } ?: 0L
                bestScores["sklekovi"] = userRecords.filter { it.type == "sklekovi" }.maxOfOrNull { it.score } ?: 0L
                bestScores["propadanja"] = userRecords.filter { it.type == "propadanja" }.maxOfOrNull { it.score } ?: 0L
                
                val totalScore = bestScores.values.sum()

                LeaderboardEntry(
                    userId = userId,
                    userName = userNames[userId] ?: "Nepoznat",
                    bestScores = bestScores,
                    totalScore = totalScore
                )
            }

            return leaderboardEntries.sortedByDescending { it.totalScore }
        } catch (e: Exception) {
            println("Greška pri preuzimanju podataka za rang listu: ${e.message}")
            return emptyList()
        }
    }

    suspend fun getUserProfileData(userId: String): UserProfileData? = coroutineScope {
        try {
            val userDeferred = async { db.collection("users").document(userId).get().await() }
            val parksDeferred = async { db.collection("workout_park").whereEqualTo("createdBy", userId).get().await() }
            val recordsDeferred = async { db.collection("records").whereEqualTo("userId", userId).get().await() }

            val userDoc = userDeferred.await()
            val user = userDoc.toObject(User::class.java) ?: return@coroutineScope null

            val createdParks = parksDeferred.await().toObjects(WorkoutPark::class.java)
            val userRecords = recordsDeferred.await().toObjects(UserRecord::class.java)

            UserProfileData(user, createdParks, userRecords.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            null
        }
    }
}
