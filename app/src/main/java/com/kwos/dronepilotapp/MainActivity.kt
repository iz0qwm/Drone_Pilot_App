package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val TAG = "DronePilotApp"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        //Carica il logo
        val logoImage: ImageView = findViewById(R.id.logoImage)
        logoImage.setImageResource(R.drawable.logo)

        //Descrizione sulla pagina iniziale
        val descriptionTextView: TextView = findViewById(R.id.descriptionText)
        descriptionTextView.text = HtmlCompat.fromHtml(getString(R.string.description_text), HtmlCompat.FROM_HTML_MODE_LEGACY)
        descriptionTextView.movementMethod = LinkMovementMethod.getInstance() // Abilita link cliccabili se presenti

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // AppCheck iniziata
        FirebaseAppCheck.getInstance().setTokenAutoRefreshEnabled(true)
        // Inizializza Firebase
        try {
            Firebase.initialize(context = this)
            logDebug(TAG, "Firebase inizializzato con successo")
        } catch (e: Exception) {
            logError(TAG, "Errore nell'inizializzazione di Firebase", e)
        }
        // AppCheck su PlayStore (con SHA256)
        //Firebase.appCheck.installAppCheckProviderFactory(
        //    PlayIntegrityAppCheckProviderFactory.getInstance(),
        //)
        //AppCheck di debug con Token letto da Logcat: I FirebaseAppCheck: Debug token: YOUR_DEBUG_TOKEN_HERE
        // E inserito su Firebase AppCheck --> Gestisci i token di debug
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )

        //controllo gli aggiornamenti su Github
        checkForUpdate(this)

        // Controlla e richiedi i permessi di geolocalizzazione
        if (!checkLocationPermission()) {
            requestLocationPermission()
        }

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val fullNameField = findViewById<EditText>(R.id.fullNameField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)


        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(this)
        }


        // Verifica e richiedi permesso per notifiche su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val fullName = fullNameField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty()) {
                registerUser(email, password, fullName)
            } else {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }


    }

    /**
     * Controlla se i permessi di geolocalizzazione sono già stati concessi.
     */
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Richiede all'utente i permessi di geolocalizzazione se non sono stati concessi.
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Gestisce la risposta dell'utente alla richiesta di permessi.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logDebug(TAG, "Permesso di posizione concesso")
            } else {
                Toast.makeText(this, "Permesso posizione necessario per utilizzare l'app", Toast.LENGTH_LONG).show()
                finish() // Chiude l'app se il permesso non è concesso
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login riuscito, procedi con l'ottenimento del token FCM
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            logDebug(TAG, "loginUser: Nuovo Token FCM: $token")
                            saveTokenToServer(token) // Salva il token nel database
                            salvaLogin(email) // Salva il login nelle Shared Preferences
                        } else {
                            logError(TAG, "loginUser: Errore nel recuperare il token FCM")
                        }
                    }

                    Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun salvaLogin(email: String) {
        val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    private fun saveTokenToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)

            // Log per vedere se l'utente è effettivamente autenticato
            logDebug(TAG, "Utente autenticato: $userId")

            // Aggiungi il token alla lista esistente di token
            userDocRef.update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    logDebug(TAG, "Token FCM aggiunto nel database per l'utente $userId")
                }
                .addOnFailureListener { exception ->
                    logError(TAG, "Errore aggiornando il token FCM", exception)
                }
        } else {
            logError(TAG, "Errore: nessun utente autenticato")
        }
    }


    private fun registerUser(email: String, password: String, fullName: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val userMap = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "fullName" to fullName,
                    "availableForChat" to false // Chat disabilitata di default
                )

                userId?.let {
                    db.collection("users").document(it).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Register", "Errore durante il salvataggio", e)
                        }
                }
            } else {
                Log.e("Register", "Registrazione fallita", task.exception)
            }
        }
    }

}

fun getApiKey(keyName: String, context: Context): String? {
    val properties = Properties()
    try {
        val inputStream = context.assets.open("secrets.properties")
        properties.load(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return properties.getProperty(keyName)
}

fun checkForUpdate(context: Context) {
    val TAG = "DronePilotApp"
    val url = "https://api.github.com/repos/iz0qwm/Drone_Pilot_App/releases"
    val token = getApiKey("GITHUB_TOKEN", context)

    CoroutineScope(Dispatchers.IO).launch {
        logDebug(TAG, "UpdateCheck: Checking update from URL: $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "token $token")  // <-- Aggiunto l'header per l'autenticazione
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                logError(TAG, "UpdateCheck: Request failed: ${response.code} - ${response.message}")
                return@launch
            }

            val responseBody = response.body?.string()
            logDebug(TAG, "UpdateCheck: Response JSON: $responseBody")

            if (responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val latestRelease = jsonArray.getJSONObject(0) // Accedi al primo rilascio dell'array
                val latestVersion = latestRelease.optString("tag_name", "unknown")
                logDebug(TAG, "UpdateCheck: Latest version: $latestVersion")

                val currentVersion = BuildConfig.VERSION_NAME
                logDebug(TAG, "UpdateCheck: Current version: $currentVersion")

                if (latestVersion != "unknown" && latestVersion != currentVersion) {
                    logDebug(TAG, "UpdateCheck: New version available: $latestVersion")
                    val assets = latestRelease.getJSONArray("assets")
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    // Aggiungi qui la logica per notificare l'utente o avviare il download
                    (context as Activity).runOnUiThread {
                        AlertDialog.Builder(context)
                            .setTitle("Nuovo aggiornamento disponibile")
                            .setMessage("Scaricare la versione $latestVersion?")
                            .setPositiveButton("Scarica") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                            }
                            .setNegativeButton("Annulla", null)
                            .show()
                    }
                } else {
                    logDebug(TAG, "UpdateCheck: App is up to date.")
                }
            } else {
                logError(TAG, "UpdateCheck: Response body is null")
            }
        } catch (e: Exception) {
            logError(TAG, "UpdateCheck: Error checking update: ${e.message}", e)
        }
    }
}
