package com.kwos.dronepilotapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class CleanupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        cleanOldLocations()
        return Result.success()
    }

    private fun cleanOldLocations() {
        val db = FirebaseFirestore.getInstance()
        val oneHourAgo = System.currentTimeMillis() - 3600000  // Un'ora fa

        db.collection("piloti")
            .whereLessThan("timestamp", oneHourAgo)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("piloti").document(document.id).delete()
                }
                Log.d("DronePilotApp", "Posizioni vecchie rimosse")
            }
            .addOnFailureListener { e ->
                Log.e("DronePilotApp", "Errore nella pulizia delle posizioni", e)
            }
    }
}