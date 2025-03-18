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
        Log.d(TAG, "Messaggio ricevuto: $remoteMessage")

        // Controlla se il messaggio contiene una notifica
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Nuovo Messaggio"
            val body = notification.body ?: "Hai un nuovo messaggio"
            // Mostra la notifica quando arriva una notifica
            showNotification(title, body)

            // Crea un Intent per inviare un broadcast
            //val intent = Intent("com.kwos.dronepilotapp.NEW_MESSAGE")
            val intent = Intent(this, MyFirebaseMessagingService::class.java)
            intent.action = "com.kwos.dronepilotapp.NEW_MESSAGE"
            sendBroadcast(intent)

            intent.putExtra("message", title)
            intent.putExtra("title", body)

            // Aggiungi il senderId al broadcast
            val senderId = remoteMessage.data["senderId"] ?: "Sconosciuto"
            intent.putExtra("senderId", senderId)

            // Invia il broadcast
            sendBroadcast(intent)
        }

        // Controlla se il messaggio contiene dati extra
        if (remoteMessage.data.isNotEmpty()) {
            val senderId = remoteMessage.data["senderId"] ?: "Sconosciuto"
            val message = remoteMessage.data["message"] ?: "Hai un nuovo messaggio"

            // Recupera il nome completo da Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(senderId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "Sconosciuto"

                        // Mostra la notifica con il nome
                        showNotification("Messaggio da $fullName", message)

                        // Invia un broadcast con il nome al posto del senderId
                        //val intent = Intent("com.kwos.dronepilotapp.NEW_MESSAGE")
                        val intent = Intent(this, MyFirebaseMessagingService::class.java)
                        intent.action = "com.kwos.dronepilotapp.NEW_MESSAGE"
                        sendBroadcast(intent)
                        intent.putExtra("message", message)
                        intent.putExtra("title", "Messaggio da $fullName")
                        intent.putExtra("senderId", senderId)
                        sendBroadcast(intent)
                    } else {
                        Log.w(TAG, "Utente non trovato")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore nel recupero del nome utente", e)
                }
        }



    }



    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuovo Token FCM: $token")
        saveTokenToServer(token) // Invia il token al server
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "chat_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_mail) // Usa un'icona della bustina
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notifiche Chat", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Canale di notifiche creato")
        }


        notificationManager.notify(0, notificationBuilder.build())

    }

    private fun saveTokenToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)

            // Log per vedere se l'utente è effettivamente autenticato
            Log.d(TAG, "Utente autenticato: $userId")

            // Aggiungi il token alla lista esistente di token
            userDocRef.update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM aggiunto nel database per l'utente $userId")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Errore aggiornando il token FCM", exception)
                }
        } else {
            Log.e(TAG, "Errore: nessun utente autenticato")
        }
    }

    // Quando un utente si disconnette, rimuovi tutti i suoi token
    fun removeTokensOnLogout(userId: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        Log.d(TAG, "removeToken: Sto rimuovendo i token FCM per l'utente $userId")

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val tokens = document.get("fcmTokens") as? List<String>
                if (!tokens.isNullOrEmpty()) {
                    userDocRef.update("fcmTokens", FieldValue.delete())
                        .addOnSuccessListener {
                            Log.d(TAG, "removeToken: Token FCM rimossi con successo per $userId")
                            onComplete() // Chiamata di callback per continuare il logout
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "❌ Errore nella rimozione dei token FCM", exception)
                            onComplete() // Anche in caso di errore, continua il logout
                        }
                } else {
                    Log.d(TAG, "removeToken: Nessun token FCM trovato per $userId")
                    onComplete()
                }
            } else {
                Log.d(TAG, "removeToken: Documento utente non esistente")
                onComplete()
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "❌ removeToke: Errore nel recupero del documento utente", exception)
            onComplete()
        }
    }


}
