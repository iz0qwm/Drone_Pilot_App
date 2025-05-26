package com.kwos.dronepilotapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddDroneActivity : AppCompatActivity() {

    private lateinit var droneNameInput: EditText
    private lateinit var droneDescriptionInput: EditText
    private lateinit var selectPhotoButton: Button
    private lateinit var submitButton: Button
    private lateinit var deleteButton: Button
    private lateinit var photoPreview: ImageView
    private lateinit var closeButton: Button
    private lateinit var loadingDialog: ProgressDialog

    private var photoUri: Uri? = null
    private var droneId: String? = null
    private var originalPhotoUrl: String = ""

    companion object {
        private const val REQUEST_IMAGE_GALLERY = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_drone)

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

        droneNameInput = findViewById(R.id.drone_name_input)
        droneDescriptionInput = findViewById(R.id.drone_description_input)
        selectPhotoButton = findViewById(R.id.select_drone_photo_button)
        submitButton = findViewById(R.id.submit_drone_button)
        deleteButton = findViewById(R.id.delete_drone_button)
        photoPreview = findViewById(R.id.drone_photo_preview)
        closeButton = findViewById(R.id.close_adddrone_button)

        loadingDialog = ProgressDialog(this).apply {
            setMessage("Caricamento in corso...")
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        val intent = intent
        val isEditMode = intent.getBooleanExtra("editMode", false)
        droneId = intent.getStringExtra("droneId")

        if (isEditMode && droneId != null) {
            droneNameInput.setText(intent.getStringExtra("name") ?: "")
            droneDescriptionInput.setText(intent.getStringExtra("description") ?: "")
            originalPhotoUrl = intent.getStringExtra("photoUrl") ?: ""
            if (originalPhotoUrl.isNotEmpty()) {
                photoPreview.visibility = View.VISIBLE
                Glide.with(this).load(originalPhotoUrl).into(photoPreview)
            }
            deleteButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
        }

        closeButton.setOnClickListener { finish() }
        selectPhotoButton.setOnClickListener { openGallery() }

        submitButton.setOnClickListener {
            val name = droneNameInput.text.toString().trim()
            val description = droneDescriptionInput.text.toString().trim()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            if (name.isEmpty()) {
                Toast.makeText(this, "Inserisci il nome del drone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingDialog.show()

            if (photoUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                val fileRef = storageRef.child("drone_photos/$uid/${UUID.randomUUID()}.jpg")

                fileRef.putFile(photoUri!!).addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        salvaDrone(uid, name, description, downloadUrl.toString())
                    }
                }.addOnFailureListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Errore nel caricamento foto", Toast.LENGTH_SHORT).show()
                }
            } else {
                salvaDrone(uid, name, description, originalPhotoUrl)
            }
        }

        deleteButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val id = droneId ?: return@setOnClickListener

            loadingDialog.show()
            FirebaseFirestore.getInstance()
                .collection("pilotProfiles").document(uid)
                .collection("drones").document(id)
                .delete()
                .addOnSuccessListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Drone eliminato", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Errore nella cancellazione", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun salvaDrone(uid: String, name: String, description: String, photoUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val droneData = mapOf(
            "name" to name,
            "description" to description,
            "photoUrl" to photoUrl
        )

        val ref = if (droneId != null) {
            db.collection("pilotProfiles").document(uid)
                .collection("drones").document(droneId!!)
        } else {
            db.collection("pilotProfiles").document(uid)
                .collection("drones").document()
        }

        ref.set(droneData)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                Toast.makeText(this, "Drone salvato!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                loadingDialog.dismiss()
                Toast.makeText(this, "Errore nel salvataggio del drone", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_GALLERY) {
            photoUri = data?.data
            photoPreview.visibility = View.VISIBLE
            photoPreview.setImageURI(photoUri)
        }
    }
}
