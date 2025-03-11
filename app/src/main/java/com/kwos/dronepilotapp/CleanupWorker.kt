package com.kwos.dronepilotapp

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class CleanupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        cleanOldLocations()
        return Result.success()
    }

    private fun cleanOldLocations() {
        val oneHourAgo = System.currentTimeMillis() - 3600000  // Un'ora fa

        // Recupera i piloti che hanno posizioni più vecchie di un'ora
        db.collection("piloti")
            .whereLessThan("timestamp", oneHourAgo)
            .get()
            .addOnSuccessListener { documents ->
                val userIdsToRemove = mutableListOf<String>()

                for (document in documents) {
                    val userId = document.id
                    // Verifica lo stato "inVolo" del pilota
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val inVolo = userDoc.getBoolean("inVolo") ?: false

                            if (!inVolo) {
                                // Rimuovi i piloti che non sono più in volo
                                userIdsToRemove.add(userId)
                                // Rimuovere anche i dati nella collezione "piloti"
                                db.collection("piloti").document(userId).delete()
                                Log.d("DronePilotApp", "Pilota $userId non più in volo, rimosso.")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("DronePilotApp", "Errore nel recuperare lo stato di volo di $userId", e)
                        }
                }

                // Invia un broadcast con gli ID dei piloti da rimuovere
                val intent = Intent("com.kwos.dronepilotapp.CLEANUP")
                intent.putStringArrayListExtra("userIdsToRemove", ArrayList(userIdsToRemove))
                applicationContext.sendBroadcast(intent) // Usa applicationContext invece di context
            }
            .addOnFailureListener { e ->
                Log.e("DronePilotApp", "Errore nella pulizia delle posizioni", e)
            }
    }
}
