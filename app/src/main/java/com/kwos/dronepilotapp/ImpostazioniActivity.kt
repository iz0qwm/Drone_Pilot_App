package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.View
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.aware.WifiAwareManager
import android.widget.Switch
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import android.util.Log
import com.kwos.dronepilotapp.data.AircraftObject
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager


class ImpostazioniActivity : AppCompatActivity() {

    private lateinit var textEmail: TextView
    private lateinit var editFullName: EditText
    private lateinit var btnSalvaNome: Button
    private lateinit var closeButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var checkboxPMR: CheckBox

    private lateinit var switchDroneId: Switch
    private lateinit var textBluetoothStatus: TextView
    private lateinit var textWifiAwareStatus: TextView
    private lateinit var textWifiBeaconStatus: TextView

    private val PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        supportActionBar?.hide()

        textEmail = findViewById(R.id.textEmail)
        editFullName = findViewById(R.id.editFullName)
        btnSalvaNome = findViewById(R.id.btnSalvaNome)
        checkboxPMR = findViewById(R.id.checkboxPMR)
        closeButton = findViewById(R.id.close_impostazioni_button)

        switchDroneId = findViewById(R.id.switchDroneId)
        textBluetoothStatus = findViewById(R.id.textBluetoothStatus)
        textWifiAwareStatus = findViewById(R.id.textWifiAwareStatus)
        textWifiBeaconStatus = findViewById(R.id.textWifiBeaconStatus)

        val user = auth.currentUser
        if (user != null) {
            textEmail.text = "Email: ${user.email ?: "Non disponibile"}"
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: ""
                        editFullName.setText(fullName)
                        val radioPMR = document.getBoolean("radioPMR") ?: false
                        checkboxPMR.isChecked = radioPMR
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nel recupero dei dati", Toast.LENGTH_SHORT).show()
                }
        }

        closeButton.setOnClickListener {
            finish()
        }

        btnSalvaNome.setOnClickListener {
            val nuovoNome = editFullName.text.toString().trim()
            val haRadioPMR = checkboxPMR.isChecked

            val updates = mapOf(
                "fullName" to nuovoNome,
                "radioPMR" to haRadioPMR
            )

            if (nuovoNome.isNotEmpty() && user != null) {
                db.collection("users").document(user.uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dati aggiornati!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore nell'aggiornamento", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Inserisci un nome valido", Toast.LENGTH_SHORT).show()
            }
        }

        checkAndRequestPermissions()

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val hasBLE = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val wifiAwareManager = getSystemService(WifiAwareManager::class.java)
        val hasWifiAware = wifiAwareManager?.isAvailable == true
        val hasWifiBeacon = true

        textBluetoothStatus.text = "Bluetooth LE: " + if (hasBLE) "supportato ✅" else "non supportato ❌"
        textWifiAwareStatus.text = "WiFi Aware: " + if (hasWifiAware) "supportato ✅" else "non supportato ❌"
        textWifiBeaconStatus.text = "WiFi Beacon: " + if (hasWifiBeacon) "supportato ✅" else "non supportato ❌"

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val isDroneIdEnabled = prefs.getBoolean("droneIdEnabled", false)
        switchDroneId.isChecked = isDroneIdEnabled

        switchDroneId.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("droneIdEnabled", isChecked).apply()
            val msg = if (isChecked) "Rilevamento droni attivato" else "Rilevamento droni disattivato"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CHANGE_WIFI_STATE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permessi Bluetooth e WiFi concessi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Alcuni permessi non concessi. Funzionalità limitata.", Toast.LENGTH_LONG).show()
                switchDroneId.isChecked = false
            }
        }
    }
}
