package com.kwos.dronepilotapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.View

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "DronePilotApp"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logDebug(TAG, "Messaggio ricevuto: $remoteMessage")

        var senderId: String = "Sconosciuto"
        var receiverId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "Sconosciuto" // 🔹 Usa l'UID dell'utente attuale
        var message: String = "Hai un nuovo messaggio"
        var notificationTitle = "Nuovo Messaggio"
        var notificationBody = "Hai un nuovo messaggio"

        // Controlla se il messaggio contiene una notifica
        remoteMessage.notification?.let { notification ->
            notificationTitle = notification.title ?: notificationTitle
            notificationBody = notification.body ?: notificationBody
        }

        // Controlla se il messaggio contiene dati extra
        if (remoteMessage.data.isNotEmpty()) {
            senderId = remoteMessage.data["senderId"] ?: senderId
            message = remoteMessage.data["message"] ?: notificationBody
        }

        // Se c'è un senderId valido, cerchiamo il nome su Firestore
        if (senderId != "Sconosciuto") {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(senderId).get()
                .addOnSuccessListener { document ->
                    val fullName = document.getString("fullName") ?: "Sconosciuto"
                    val finalTitle = "Messaggio da $fullName"
                    val finalMessage = message

                    // Mostra la notifica con il nome completo
                    showNotification(finalTitle, finalMessage, senderId, receiverId)

                    // Invia un broadcast
                    sendNewMessageBroadcast(senderId, finalTitle, finalMessage)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nel recupero del nome utente", e)
                    // In caso di errore, usa il senderId invece del nome
                    showNotification(notificationTitle, notificationBody, senderId, receiverId)
                    sendNewMessageBroadcast(senderId, notificationTitle, notificationBody)
                }
        } else {
            // Se non c'è un senderId valido, mostra la notifica con i dati ricevuti
            showNotification(notificationTitle, notificationBody, senderId, receiverId)
            sendNewMessageBroadcast(senderId, notificationTitle, notificationBody)
        }
    }

    /**
     * Invia un broadcast con i dettagli del nuovo messaggio
     */
    private fun sendNewMessageBroadcast(senderId: String, title: String, message: String) {
        val intent = Intent("com.kwos.dronepilotapp.NEW_MESSAGE").apply {
            putExtra("message", message)
            putExtra("title", title)
            putExtra("senderId", senderId)
        }
        sendBroadcast(intent)
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuovo Token FCM: $token")
        saveTokenToServer(token) // Invia il token al server
    }

    private fun showNotification(title: String, message: String, senderId: String, receiverId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("senderId", receiverId)  // Passa il senderId alla ChatActivity
            putExtra("receiverId", senderId)  // 📌 Aggiunto receiverId = senderId
        }

        val nullIntent = Intent() // Intent vuoto

        logDebug(TAG, "MyFirebaseMessagingService showNotification: senderId: $senderId, receiverId: $receiverId")

        val pendingIntent = PendingIntent.getActivity(this, 0, nullIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Abilitare se vuoi utilizzare l'Intent che apre la chat ( OCCHIO va in Destroy() mai risolto)
        //val pendingIntent = PendingIntent.getActivity(
        //    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        //)

        val channelId = "chat_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_mail)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Qui associamo il pendingIntent

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notifiche Chat", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }


    private fun saveTokenToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)

            // Log per vedere se l'utente è effettivamente autenticato
            logDebug(TAG, "MyFirebaseMessagingService: Utente autenticato: $userId")

            // Aggiungi il token alla lista esistente di token
            userDocRef.update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    logDebug(TAG, "MyFirebaseMessagingService: Token FCM aggiunto nel database per l'utente $userId")
                }
                .addOnFailureListener { exception ->
                    logError(TAG, "MyFirebaseMessagingService: Errore aggiornando il token FCM", exception)
                }
        } else {
            logError(TAG, "MyFirebaseMessagingService: Errore: nessun utente autenticato")
        }
    }

    // Quando un utente si disconnette, rimuovi tutti i suoi token
    fun removeTokensOnLogout(userId: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        logDebug(TAG, "MyFirebaseMessagingService: removeToken: Sto rimuovendo i token FCM per l'utente $userId")

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val tokens = document.get("fcmTokens") as? List<String>
                if (!tokens.isNullOrEmpty()) {
                    userDocRef.update("fcmTokens", FieldValue.delete())
                        .addOnSuccessListener {
                            logDebug(TAG, "MyFirebaseMessagingService: removeToken: Token FCM rimossi con successo per $userId")
                            onComplete() // Chiamata di callback per continuare il logout
                        }
                        .addOnFailureListener { exception ->
                            logError(TAG, "❌ MyFirebaseMessagingService: Errore nella rimozione dei token FCM", exception)
                            onComplete() // Anche in caso di errore, continua il logout
                        }
                } else {
                    logDebug(TAG, "MyFirebaseMessagingService: removeToken: Nessun token FCM trovato per $userId")
                    onComplete()
                }
            } else {
                logDebug(TAG, "MyFirebaseMessagingService:removeToken: Documento utente non esistente")
                onComplete()
            }
        }.addOnFailureListener { exception ->
            logError(TAG, "❌ MyFirebaseMessagingService: removeToken: Errore nel recupero del documento utente", exception)
            onComplete()
        }
    }


}
