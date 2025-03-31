package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.OkHttpClient
import okhttp3.Request
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Properties
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.method.ScrollingMovementMethod
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64
//import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.KeyStore
import javax.crypto.spec.GCMParameterSpec

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val TAG = "DronePilotApp"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Abilita l'invio dei crash su Firebase console
        //FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        //FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
        //FirebaseCrashlytics.getInstance().checkForUnsentReports()
        //FirebaseCrashlytics.getInstance().sendUnsentReports()

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        val rootView = findViewById<View>(android.R.id.content)

        // Applica il padding per evitare che gli elementi vengano coperti
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = systemBars.top, // Evita sovrapposizione con la status bar
                bottom = systemBars.bottom // Evita sovrapposizione con la navigation bar
            )

            WindowInsetsCompat.CONSUMED
        }
        // fine padding

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


        // SOLO PER VERDE MARCO ---- DA RIMUOVERE - STACKTRACE -
        // Trova il TextView dove verrà mostrato lo stacktrace
        //val stacktraceTextView = findViewById<TextView>(R.id.stacktraceTextView)

        // Se ci sono extra nell'intent, mostra lo stacktrace
        //val stackTrace = intent.getStringExtra("stacktrace")
        //if (stackTrace != null) {
        //    stacktraceTextView.text = stackTrace
        //    stacktraceTextView.visibility = View.VISIBLE
        //    stacktraceTextView.setMovementMethod(ScrollingMovementMethod()) // Per scorrere
        //}
        //



        // Controllo presenza della registrazione email nelle SharedPreferences
        val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
        val savedEmail = prefs.getString("user_email", null)

        //prefs.edit().remove("encrypted_password").apply()  // Cancella solo la password
        //prefs.edit().remove("user_email").apply()  // Cancella solo l'email

        loginButton.setOnClickListener {
            if (savedEmail != null) {
                logDebug(TAG, "setOnClickListener: Sono nel login button la mail è salvata")
                // Se la password è salvata, la utilizziamo
                val savedPassword = getPasswordFromKeystore()
                if (savedPassword != null) {
                    logDebug(TAG, "setOnClickListener: La password è già salvata $savedEmail - $savedPassword")
                    // Login con password dal Keystore
                    loginUser(savedEmail, savedPassword)
                } else {
                    logDebug(TAG, "setOnClickListener: La password non è salvata")
                    AlertDialog.Builder(this)
                        .setTitle("Attenzione")
                        .setMessage("Inserisci la password")
                        .setPositiveButton("OK", null)
                        .show()
                    // Se la password non è salvata la facciamo scrivere
                    emailField.setText(savedEmail)
                    val email = emailField.text.toString().trim()
                    val password = passwordField.text.toString().trim()
                    loginUser(email, password)
                    savePasswordToKeystore(password)  // Salva la password nel Keystore
                }
            } else {
                logDebug(TAG, "setOnClickListener: Sono nel login button la mail non è salvata")
                val email = emailField.text.toString().trim()
                val password = passwordField.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Attenzione")
                        .setMessage("Inserisci email e password")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    loginUser(email, password)
                    savePasswordToKeystore(password)  // Salva la password nel Keystore
                }
            }
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


    private fun savePasswordToKeystore(password: String) {
        try {
            // Genera una chiave segreta nel Keystore
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder("password_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keySpec)
            val secretKey: SecretKey = keyGenerator.generateKey()

            // Cifra la password con la chiave nel Keystore
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryption = cipher.doFinal(password.toByteArray())

            // Salva la password cifrata e l'IV nelle SharedPreferences (non direttamente, ma cifrato)
            val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("encrypted_password", Base64.encodeToString(encryption, Base64.DEFAULT))
            editor.putString("iv", Base64.encodeToString(iv, Base64.DEFAULT))
            editor.apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPasswordFromKeystore(): String? {
        try {
            val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
            val encryptedPassword = prefs.getString("encrypted_password", null)
            val ivString = prefs.getString("iv", null)

            if (encryptedPassword != null && ivString != null) {
                val iv = Base64.decode(ivString, Base64.DEFAULT)
                val encryptedPasswordBytes = Base64.decode(encryptedPassword, Base64.DEFAULT)

                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)

                val key = keyStore.getKey("password_key", null) as SecretKey

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmParameterSpec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)

                val decryptedPasswordBytes = cipher.doFinal(encryptedPasswordBytes)
                return String(decryptedPasswordBytes)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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

    private fun cancellaTokens(userId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("fcmTokens")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete() // Cancella ogni token nel database
                }
                logDebug(TAG, "cancellaTokens: Tutti i token dell'utente sono stati eliminati.")
            }
            .addOnFailureListener { e ->
                logError(TAG, "cancellaTokens: Errore nella rimozione dei token.", e)
            }

        // Cancella anche il token locale del dispositivo
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logDebug(TAG, "cancellaTokens: Token FCM locale rimosso con successo.")
                } else {
                    logError(TAG, "cancellaTokens: Errore nella rimozione del token FCM locale.", task.exception)
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        logDebug(TAG, "loginUser: Sto per inserire $email e $password")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        cancellaTokens(userId) // Cancella i token dell'utente senza crearne uno nuovo
                    }

                    //salvaLogin(email) // Salva il login nelle Shared Preferences
                    //savePasswordToKeystore(password)  // Salva la password nel Keystore
                    // Login riuscito, procedi con l'ottenimento del token FCM
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            logDebug(TAG, "loginUser: Nuovo Token FCM: $token")
                            saveTokenToServer(token) // Salva il token nel database
                            salvaLogin(email) // Salva il login nelle Shared Preferences
                            savePasswordToKeystore(password)  // Salva la password nel Keystore
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
    return try {
        val inputStream = context.assets.open("secrets.properties")
        properties.load(inputStream)
        inputStream.close() // Chiudiamo il file dopo averlo letto
        val key = properties.getProperty(keyName)

        if (key.isNullOrBlank()) {
            logDebug("DronePilotApp", "getApiKey: Chiave $keyName non trovata o vuota in secrets.properties")
            null
        } else {
            key
        }
    } catch (e: Exception) {
        logError("DronePilotApp", "getApiKey: Errore nel caricamento di secrets.properties: ${e.message}")
        null
    }
}

fun checkForUpdate(context: Context) {
    val TAG = "DronePilotApp"
    val url = "https://api.github.com/repos/iz0qwm/Drone_Pilot_App/releases"
    val token = getApiKey("GITHUB_TOKEN", context)
    if (token == null) {
        logError(TAG, "checkForUpdate: Token GitHub non trovato!")
    } else {
        logDebug(TAG, "Token GitHub caricato correttamente")
    }

    CoroutineScope(Dispatchers.IO).launch {
        logDebug(TAG, "UpdateCheck: Token: $token")
        logDebug(TAG, "UpdateCheck: Checking update from URL: $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            //commentato perchè il repository GitHub è Open
            //.addHeader("Authorization", "token $token")  // <-- Aggiunto l'header per l'autenticazione
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
