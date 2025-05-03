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
import android.text.InputType
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64
import java.security.KeyStore
import javax.crypto.spec.GCMParameterSpec
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.animation.ObjectAnimator
import android.app.ProgressDialog
import android.view.ViewTreeObserver
import android.widget.ProgressBar
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.firestore.SetOptions


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

        // Password dimenticata
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        forgotPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }



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

        val droneImage = findViewById<ImageView>(R.id.droneImage)

        // Aspetta che il layout sia pronto per calcolare la larghezza
        droneImage.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                droneImage.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val droneWidth = droneImage.width.toFloat()

                val animator = ObjectAnimator.ofFloat(
                    droneImage,
                    "translationX",
                    -droneWidth,
                    screenWidth
                )

                animator.duration = 4000 // durata in millisecondi
                animator.repeatCount = ObjectAnimator.INFINITE
                animator.start()
            }
        })


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

        val progressDialog = showProgressDialog("Accesso in corso...")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null && !user.isEmailVerified) {
                        AlertDialog.Builder(this)
                            .setTitle("Email non verificata")
                            .setMessage("Devi verificare il tuo indirizzo email prima di poter accedere.")
                            .setPositiveButton("Invia nuova email") { _, _ ->
                                user.sendEmailVerification()
                                    .addOnCompleteListener { resendTask ->
                                        if (resendTask.isSuccessful) {
                                            Toast.makeText(this, "Nuova email inviata! Controlla la tua casella di posta.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this, "Errore nell'invio della nuova email.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                            .setNegativeButton("Annulla", null)
                            .show()
                    } else {
                        // Email verificata: procediamo al login normale
                        val userId = auth.currentUser?.uid

                        if (userId != null) {
                            cancellaTokens(userId)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                ottieniTokenECreaSessione(email, password)
                            } else {
                                Toast.makeText(this, "Permesso notifiche negato. Il servizio chat potrebbe non funzionare correttamente.", Toast.LENGTH_LONG).show()
                                salvaLogin(email)
                                savePasswordToKeystore(password)
                                saveOnlineStatus(true)
                                Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            }
                        } else {
                            ottieniTokenECreaSessione(email, password)
                        }
                    }

                } else {
                    val exception = task.exception
                    logError(TAG, "Errore di login", exception)

                    when (exception?.message) {
                        "The email address is badly formatted." -> {
                            Toast.makeText(this, "Indirizzo email non valido.", Toast.LENGTH_SHORT).show()
                        }
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> {
                            Toast.makeText(this, "Nessun account trovato con questa email.", Toast.LENGTH_SHORT).show()
                        }
                        "The password is invalid or the user does not have a password." -> {
                            Toast.makeText(this, "Password errata. Riprova.", Toast.LENGTH_SHORT).show()
                        }
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> {
                            Toast.makeText(this, "Problema di rete. Controlla la connessione.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Errore di accesso: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun showProgressDialog(message: String): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.progress_dialog, null)

        val messageTextView = dialogLayout.findViewById<TextView>(R.id.progressMessage)
        messageTextView.text = message

        builder.setView(dialogLayout)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        return dialog
    }




    private fun ottieniTokenECreaSessione(email: String, password: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val token = tokenTask.result
                logDebug(TAG, "loginUser: Nuovo Token FCM: $token")
                saveTokenToServer(token)
                salvaLogin(email)
                savePasswordToKeystore(password)
                saveOnlineStatus(true)
                Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                logError(TAG, "loginUser: Errore nel recuperare il token FCM")
                // Anche se fallisce, portiamo comunque l'utente nella Dashboard
                salvaLogin(email)
                savePasswordToKeystore(password)
                saveOnlineStatus(true)
                Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }


    fun salvaLogin(email: String) {
        val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    // Salva lo stato di online per contare il numero di utenti connessi al sistema
    fun saveOnlineStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.set(mapOf("online" to isOnline), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "MainActivity - saveOnlineStatus: Stato online aggiornato a $isOnline")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "MainActivity - saveOnlineStatus: Errore aggiornamento stato online", e)
            }
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
        if (password.length < 6) {
            Toast.makeText(this, "La password deve contenere almeno 6 caratteri", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registrazione in corso...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val userMap = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "fullName" to fullName,
                    "availableForChat" to false
                )

                userId?.let {
                    db.collection("users").document(it).set(userMap)
                        .addOnSuccessListener {
                            // Prepara ActionCodeSettings con URL della tua pagina di verifica
                            val actionCodeSettings = ActionCodeSettings.newBuilder()
                                .setUrl("https://www.tuosito.it/verify.html") // <<< Cambia con il tuo dominio reale
                                .setHandleCodeInApp(false)
                                .build()

                            // Invia email di verifica
                            auth.currentUser?.sendEmailVerification(actionCodeSettings)
                                ?.addOnCompleteListener { emailTask ->
                                    progressDialog.dismiss() // Chiudiamo il dialog

                                    if (emailTask.isSuccessful) {
                                        Toast.makeText(this, "Registrazione completata! Controlla la tua email per confermare l'account.", Toast.LENGTH_LONG).show()
                                        startActivity(Intent(this, DashboardActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Errore nell'invio dell'email di conferma.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss() // Chiudiamo il dialog
                            Log.e("Register", "Errore durante il salvataggio", e)
                            Toast.makeText(this, "Errore durante la registrazione. Riprova.", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                progressDialog.dismiss() // Chiudiamo il dialog
                Log.e("Register", "Registrazione fallita", task.exception)
                Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Email per il reset inviata. Controlla la tua casella di posta.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reimposta password")

        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "Inserisci la tua email"
        input.setPadding(50, 40, 50, 40)

        builder.setView(input)

        builder.setPositiveButton("Invia") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Inserisci un indirizzo email", Toast.LENGTH_SHORT).show()
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email inviata! Controlla la tua casella di posta.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Annulla") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
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
            //.addHeader("Authorization", "token $token") // usalo se la repo diventa privata
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
                val latestRelease = jsonArray.getJSONObject(0)
                val latestVersion = latestRelease.optString("tag_name", "unknown")
                val releaseNotes = latestRelease.optString("body", "Nessuna descrizione disponibile.")
                logDebug(TAG, "UpdateCheck: Latest version: $latestVersion")

                val currentVersion = BuildConfig.VERSION_NAME
                logDebug(TAG, "UpdateCheck: Current version: $currentVersion")

                if (latestVersion != "unknown" && latestVersion != currentVersion) {
                    logDebug(TAG, "UpdateCheck: New version available: $latestVersion")
                    val assets = latestRelease.getJSONArray("assets")
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

                    (context as Activity).runOnUiThread {
                        // Crea una SpannableString con il testo in nero
                        val spannableMessage = SpannableString("Novità nella versione $latestVersion:\n\n$releaseNotes")
                        spannableMessage.setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), // Colore nero
                            0,
                            spannableMessage.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        val dialog = AlertDialog.Builder(context)
                            .setTitle("Aggiornamento disponibile")
                            .setMessage(spannableMessage)
                            .setPositiveButton("Scarica") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                context.startActivity(intent)
                            }
                            .setNegativeButton("Annulla", null)
                            .create()

                        dialog.show()

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            ?.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            ?.setTextColor(ContextCompat.getColor(context, R.color.gray))
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



