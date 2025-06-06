package com.kwos.dronepilotapp

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.gms.maps.SupportMapFragment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddTakeoffSpotActivity : AppCompatActivity() {

    private lateinit var spotNameInput: EditText
    private lateinit var spotDescriptionInput: EditText
    private lateinit var selectPhotoButton: Button
    private lateinit var submitButton: Button
    private lateinit var photoPreview: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var closeButton: Button
    private lateinit var loadingDialog: ProgressDialog
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>


    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var spotMarker: com.google.android.gms.maps.model.Marker? = null

    companion object {
        private const val REQUEST_IMAGE_GALLERY = 1001
        private const val REQUEST_IMAGE_CAMERA = 1002
        private const val REQUEST_PERMISSION = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_takeoff_spot)

        // Launcher fotocamera
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Permesso Fotocamera Negato", Toast.LENGTH_SHORT).show()
            }
        }


        loadingDialog = ProgressDialog(this)
        loadingDialog.setMessage("Caricamento in corso...")
        loadingDialog.setCancelable(false)
        loadingDialog.setCanceledOnTouchOutside(false)

        // View binding
        spotNameInput = findViewById(R.id.spot_name_input)
        spotDescriptionInput = findViewById(R.id.spot_description_input)
        selectPhotoButton = findViewById(R.id.select_photo_button)
        submitButton = findViewById(R.id.submit_spot_button)
        photoPreview = findViewById(R.id.photo_preview)
        closeButton = findViewById(R.id.close_addspot_button)

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
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true // o false, dipende dal tema

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

        // Torna indietro
        closeButton.setOnClickListener {
            finish()
        }


        // Seleziona o scatta foto
        selectPhotoButton.setOnClickListener {
            showImagePickerDialog()
        }

        // Invia spot
        submitButton.setOnClickListener {
            val spotName = spotNameInput.text.toString().trim()
            val spotDescription = spotDescriptionInput.text.toString().trim()

            if (spotName.isEmpty() || spotDescription.isEmpty() || photoUri == null) {
                Toast.makeText(this, "Compila tutti i campi e seleziona una foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadPhotoAndCreateSpot(spotName, spotDescription)
        }


        // Poi, dentro onCreate:
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mini_map_fragment) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)

                        googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))

                        // Carica l'icona personalizzata
                        val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/icons8-drone-takeoff-96.png"
                        com.bumptech.glide.Glide.with(this)
                            .asBitmap()
                            .load(iconUrl)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // 🚀 Non usa cache su disco
                            .skipMemoryCache(true) // 🚀 Salta la cache in memoria
                            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>(96, 96) {
                                override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                                    val iconBitmap = com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(resource)
                                    spotMarker = googleMap.addMarker(
                                        com.google.android.gms.maps.model.MarkerOptions()
                                            .position(userLatLng)
                                            .icon(iconBitmap)
                                            .title("Posizione spot")
                                            .draggable(true)
                                    )
                                }

                                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            })

                        // Listener per trascinamento marker
                        googleMap.setOnMarkerDragListener(object : com.google.android.gms.maps.GoogleMap.OnMarkerDragListener {
                            override fun onMarkerDragStart(marker: com.google.android.gms.maps.model.Marker) {
                                // Puoi mettere log se vuoi
                            }

                            override fun onMarkerDrag(marker: com.google.android.gms.maps.model.Marker) {
                                // Durante il drag
                            }

                            override fun onMarkerDragEnd(marker: com.google.android.gms.maps.model.Marker) {
                                // Alla fine del drag aggiorniamo spotMarker
                                spotMarker = marker
                            }
                        })
                    } else {
                        Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION)
            }
        }

    }

    private fun showImagePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_picker, null)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val btnCamera = dialogView.findViewById<Button>(R.id.btnCamera)
        val btnGallery = dialogView.findViewById<Button>(R.id.btnGallery)

        btnCamera.setOnClickListener {
            openCamera()
            dialog.dismiss()
        }

        btnGallery.setOnClickListener {
            openGallery()
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA)
                }
            }
        } else {
            // Chiede il permesso usando il launcher nuovo
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }




    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GALLERY -> {
                    photoUri = data?.data
                    photoPreview.visibility = View.VISIBLE
                    photoPreview.setImageURI(photoUri)
                }
                REQUEST_IMAGE_CAMERA -> {
                    photoPreview.visibility = View.VISIBLE
                    photoPreview.setImageURI(photoUri)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permesso Fotocamera Negato", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun uploadPhotoAndCreateSpot(name: String, description: String) {
        loadingDialog.show()

        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val spotId = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("takeoff_spots")
            .document().id

        val photoRef = storageRef.child("takeoff_spot_photos/$spotId.jpg")

        photoUri?.let { uri ->
            val resizedFile = resizeImage(uri)

            if (resizedFile != null) {
                val uploadTask = photoRef.putFile(android.net.Uri.fromFile(resizedFile))

                uploadTask.addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveSpotToFirestore(spotId, name, description, downloadUri.toString())
                    }
                }.addOnFailureListener {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Errore nell'upload della foto", Toast.LENGTH_SHORT).show()
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, "Errore nella riduzione immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun saveSpotToFirestore(spotId: String, name: String, description: String, photoUrl: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Recupera la posizione dal frammento della mappa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mini_map_fragment) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            val center = googleMap.cameraPosition.target

            val spotData = hashMapOf(
                "name" to name,
                "description" to description,
                "photoUrl" to photoUrl,
                "lat" to center.latitude,
                "lng" to center.longitude,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "userId" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                "count" to 1
            )

            db.collection("takeoff_spots").document(spotId)
                .set(spotData)
                .addOnSuccessListener {
                    loadingDialog.dismiss() // nascondi
                    Toast.makeText(this, "Spot creato con successo!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    loadingDialog.dismiss() // nascondi
                    Toast.makeText(this, "Errore nel salvataggio dello spot", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun resizeImage(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Definisci la larghezza massima (800px)
            val maxWidth = 800
            val maxHeight = 800

            val ratioBitmap = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight

            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }

            val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)

            // Crea file temporaneo
            val tempFile = File.createTempFile("resized_image", ".jpg", cacheDir)

            // Verifica il tipo di connessione
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            var quality = 90 // Default alta qualità

            if (capabilities != null) {
                if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // Se su rete mobile, riduci la qualità
                    quality = 70
                }
            }

            val outputStream = tempFile.outputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.close()

            return tempFile

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}


