package com.kwos.dronepilotapp

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions

class CleanupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val TAG = "DronePilotApp"
    private val db = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword("admin@dronepilotapp.com", "_SuperSicura123!_")
            .addOnSuccessListener {
                val adminData = mapOf("admin" to true)
                db.collection("users").document("lCbQfFOWfXcbBcVOQ6MjzYSI87x1")
                    .set(adminData, SetOptions.merge()) // 🔥 Assicura che il campo venga aggiornato
                    .addOnSuccessListener {
                        logDebug(TAG, "CleanupWorker: Admin impostato correttamente!")
                        cleanOldLocations() // 🔥 Ora puoi eseguire la pulizia
                    }
                    .addOnFailureListener { e ->
                        logDebug(TAG, "CleanupWorker: Errore nell'impostare admin")
                    }
            }
            .addOnFailureListener { e ->
                logError(TAG, "CleanupWorker: Errore nell'autenticazione", e)
            }

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
                                logDebug(TAG, "CleanupWorker: Pilota $userId non più in volo, rimosso.")
                            }
                        }
                        .addOnFailureListener { e ->
                            logError(TAG, "CleanupWorker: Errore nel recuperare lo stato di volo di $userId", e)
                        }
                }

                // Invia un broadcast con gli ID dei piloti da rimuovere
                val intent = Intent("com.kwos.dronepilotapp.CLEANUP")
                intent.putStringArrayListExtra("userIdsToRemove", ArrayList(userIdsToRemove))
                applicationContext.sendBroadcast(intent) // Usa applicationContext invece di context
            }
            .addOnFailureListener { e ->
                logError(TAG, "CleanupWorker: Errore nella pulizia delle posizioni", e)
            }
    }
}
