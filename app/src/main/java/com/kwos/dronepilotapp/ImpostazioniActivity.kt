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
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.google.firebase.firestore.SetOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kwos.dronepilotapp.DroneAdapter
import com.kwos.dronepilotapp.models.Drone
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


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

    // Per l'Avatar
    private val PICK_IMAGE_REQUEST = 71
    private var imageUri: Uri? = null
    private lateinit var imageAvatar: ImageView
    private val storage = FirebaseStorage.getInstance()
    // Per la bio
    private lateinit var editBio: EditText
    // per la lista dei droni
    private lateinit var recyclerDroni: RecyclerView
    private lateinit var btnAggiungiDrone: Button
    private val droneList = mutableListOf<Drone>()
    private lateinit var droneAdapter: DroneAdapter
    private lateinit var droneResultLauncher: ActivityResultLauncher<Intent>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)

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
            setResult(Activity.RESULT_OK)
            finish()
        }

        btnSalvaNome.setOnClickListener {
            val nuovoNome = editFullName.text.toString().trim()
            val haRadioPMR = checkboxPMR.isChecked
            val nuovaBio = editBio.text.toString().trim()

            val updates = mapOf(
                "fullName" to nuovoNome,
                "radioPMR" to haRadioPMR
            )

            val uid = auth.currentUser?.uid

            if (nuovoNome.isNotEmpty() && uid != null) {
                // Aggiorna i dati base nella raccolta "users"
                db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dati utente aggiornati!", Toast.LENGTH_SHORT).show()
                    }

                // Aggiorna bio nella raccolta "pilotProfiles"
                val pilotProfileUpdate = mapOf("bio" to nuovaBio)
                db.collection("pilotProfiles").document(uid)
                    .set(pilotProfileUpdate, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profilo pilota aggiornato!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore nel salvataggio della bio", Toast.LENGTH_SHORT).show()
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

        // Per Avatar del Pilota
        imageAvatar = findViewById(R.id.imageAvatar)
        // per la bio
        editBio = findViewById(R.id.editBio)
        // Per la lista dei droni
        recyclerDroni = findViewById(R.id.recyclerDroni)
        btnAggiungiDrone = findViewById(R.id.btnAggiungiDrone)

        droneAdapter = DroneAdapter(droneList) { drone ->
            val intent = Intent(this, AddDroneActivity::class.java).apply {
                putExtra("editMode", true)
                putExtra("droneId", drone.id)
                putExtra("name", drone.name)
                putExtra("description", drone.description)
                putExtra("photoUrl", drone.photoUrl)
            }
            droneResultLauncher.launch(intent)
        }



        recyclerDroni.layoutManager = LinearLayoutManager(this)
        recyclerDroni.adapter = droneAdapter
        droneResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                aggiornaListaDroni()
            }
        }




        // Tap per selezionare una nuova immagine
        imageAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Carica avatar esistente da Firestore
        val uid = auth.currentUser?.uid
        if (uid == null) return

        db.collection("pilotProfiles").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val avatarUrl = document.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_person_placeholder)
                            .into(imageAvatar)
                    }
                }
                val bio = document.getString("bio")
                editBio.setText(bio ?: "")
            }
        aggiornaListaDroni()


        // Bottone aggiungi drone
        btnAggiungiDrone.setOnClickListener {
            val intent = Intent(this, AddDroneActivity::class.java)
            droneResultLauncher.launch(intent)
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

    // Per la foto dell'avatar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            imageUri?.let { uri ->
                // Preview immediata
                imageAvatar.setImageURI(uri)

                // Upload su Firebase Storage
                val uid = auth.currentUser?.uid ?: return
                val ref = storage.reference.child("avatars/$uid.jpg")
                ref.putFile(uri)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                            db.collection("pilotProfiles").document(uid)
                                .update("avatarUrl", downloadUri.toString())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Avatar aggiornato!", Toast.LENGTH_SHORT).show()
                                }
                            val profileData = mapOf(
                                "avatarUrl" to downloadUri.toString()
                            )

                            db.collection("pilotProfiles").document(uid)
                                .set(profileData, SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Avatar aggiornato nel profilo!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Errore nel salvataggio del profilo", Toast.LENGTH_SHORT).show()
                                }

                        }

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore nel caricamento immagine", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun aggiornaListaDroni() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("pilotProfiles").document(uid)
            .collection("drones")
            .get()
            .addOnSuccessListener { result ->
                droneList.clear()
                for (document in result) {
                    val drone = Drone(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        photoUrl = document.getString("photoUrl") ?: ""
                    )
                    droneList.add(drone)
                }
                droneAdapter.notifyDataSetChanged()
            }
    }



}
