package com.kwos.dronepilotapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CouponActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coupon)

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        //val rootView = findViewById<View>(android.R.id.content)
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)


        // INIZIO PADDING
        // EDGE-TO-EDGE
        // Modalità edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false) // Abilita modalità edge-to-edge

        // Imposta se il contenuto della status bar deve essere scuro (true) o chiaro (false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // o false, dipende dal tema

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // FINE EDGE-TO-EDGE

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // GESTIONE INSETS
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // SOLO paddingBottom per evitare che l'ultima parte vada sotto la navigation bar
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        // fine padding
        // Nasconde la Action Bar
        supportActionBar?.hide()
        // FINE PADDING

        val titleText = findViewById<TextView>(R.id.title)
        val descText = findViewById<TextView>(R.id.description)
        val codeText = findViewById<TextView>(R.id.couponCode)
        val checkbox = findViewById<CheckBox>(R.id.checkboxNoMore)
        val buttonOk = findViewById<Button>(R.id.buttonOk)

        val db = Firebase.firestore

        db.collection("coupons").document("dronezine").get()
            .addOnSuccessListener { document ->
                if (document != null && document.getBoolean("active") == true) {
                    titleText.text = document.getString("title") ?: ""
                    descText.text = document.getString("description") ?: ""
                    codeText.text = document.getString("code") ?: ""

                    codeText.setOnClickListener {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Coupon Code", codeText.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Codice copiato negli appunti!", Toast.LENGTH_SHORT).show()
                    }

                }
            }
            .addOnFailureListener {
                descText.text = "Errore nel caricamento del coupon."
            }

        buttonOk.setOnClickListener {
            if (checkbox.isChecked) {
                val promoPrefs = getSharedPreferences("promo_prefs", Context.MODE_PRIVATE)
                promoPrefs.edit().putBoolean("showDronezineCoupon", false).apply()

            }
            finish()
        }
    }
}
