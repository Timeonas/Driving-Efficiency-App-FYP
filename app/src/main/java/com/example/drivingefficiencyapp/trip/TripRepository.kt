package com.example.drivingefficiencyapp.trip

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserTripsCollection() = auth.currentUser?.uid?.let { userId ->
        firestore.collection("users").document(userId).collection("trips")
    }

    suspend fun saveTrip(date: String, duration: String): Result<Trip> = withContext(Dispatchers.IO) {
        try {
            val tripsCollection = getUserTripsCollection()
                ?: throw IllegalStateException("User not logged in")

            val trip = Trip(
                date = date,
                duration = duration
            )

            val docRef = tripsCollection.add(trip).await()
            Result.success(trip.copy(id = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

        fun getTripsFlow(): Flow<List<Trip>> = callbackFlow {
        val tripsCollection = getUserTripsCollection()
            ?: throw IllegalStateException("User not logged in")

        val registration = tripsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(trips)
            }

        awaitClose { registration.remove() }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tripsCollection = getUserTripsCollection()
                ?: throw IllegalStateException("User not logged in")

            tripsCollection.document(tripId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}