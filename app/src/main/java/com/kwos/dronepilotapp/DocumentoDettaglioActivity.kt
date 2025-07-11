// DocumentoDettaglioActivity.kt
package com.kwos.dronepilotapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.kwos.dronepilotapp.models.Documento
import java.text.SimpleDateFormat
import java.util.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import android.view.View


class DocumentoDettaglioActivity : AppCompatActivity() {

    private lateinit var editTitle: EditText
    private lateinit var editType: EditText
    private lateinit var editExpiryDate: EditText
    private lateinit var editRenewalUrl: EditText
    private lateinit var btnCaricaFile: Button
    private lateinit var btnSalvaDocumento: Button
    private lateinit var previewFile: ImageView
    private lateinit var closeButton: Button
    private lateinit var editPolicyNumber: EditText
    private lateinit var editTesseraNumber: EditText
    private lateinit var btnDeleteDocumento: Button

    private var fileUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val PICK_FILE_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documento_dettaglio)

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


        editTitle = findViewById(R.id.editTitle)
        editType = findViewById(R.id.editType)
        editExpiryDate = findViewById(R.id.editExpiryDate)
        editRenewalUrl = findViewById(R.id.editRenewalUrl)
        btnCaricaFile = findViewById(R.id.btnCaricaFile)
        btnSalvaDocumento = findViewById(R.id.btnSalvaDocumento)
        previewFile = findViewById(R.id.previewFile)
        closeButton = findViewById(R.id.btnChiudiDocumento)
        editPolicyNumber = findViewById(R.id.editPolicyNumber)
        editTesseraNumber = findViewById(R.id.editTesseraNumber)


        btnCaricaFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }

        btnSalvaDocumento.setOnClickListener {
            salvaDocumento()
        }

        btnDeleteDocumento = findViewById(R.id.btnDeleteDocumento)

        // Visibile solo se stai modificando un documento già esistente
        val documentId = intent.getStringExtra("documentId")

        if (!documentId.isNullOrEmpty()) {

            //rendi visibili i vari tasti
            btnDeleteDocumento.visibility = View.VISIBLE

            val uid = auth.currentUser?.uid ?: return

            db.collection("pilotProfiles").document(uid)
                .collection("documents").document(documentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        editTitle.setText(document.getString("title") ?: "")
                        editType.setText(document.getString("type") ?: "")
                        editExpiryDate.setText(document.getString("expiryDate") ?: "")
                        editRenewalUrl.setText(document.getString("renewalUrl") ?: "")
                        editPolicyNumber.setText(document.getString("policyNumber") ?: "")
                        editTesseraNumber.setText(document.getString("tesseraNumber") ?: "")

                        // Visualizza immagine del documento
                        // Visualizza link del documento se PDF
                        val fileUrl = document.getString("fileUrl")
                        val downloadLink = findViewById<TextView>(R.id.pdfDownloadLink)
                        val mimeType = document.getString("fileMimeType") ?: ""

                        if (!fileUrl.isNullOrEmpty()) {
                            val uri = Uri.parse(fileUrl)
                            if (mimeType == "application/pdf") {
                                downloadLink.text = "📄 Scarica il documento originale"
                                downloadLink.setOnClickListener {
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    startActivity(intent)
                                }
                                downloadLink.visibility = View.VISIBLE
                            } else {
                                previewFile.setImageURI(uri)
                                previewFile.visibility = View.VISIBLE
                            }
                        }

                        // Fai visualizzare il link di rinnovo o meno
                        val renewalUrl = document.getString("renewalUrl") ?: ""

                        val textRenewalUrl = findViewById<TextView>(R.id.textRenewalUrl)
                        val editRenewalUrl = findViewById<EditText>(R.id.editRenewalUrl)

                        if (renewalUrl.isNotEmpty()) {
                            textRenewalUrl.text = "🔗 Rinnovo: $renewalUrl"
                            textRenewalUrl.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(renewalUrl))
                                startActivity(intent)
                            }
                            textRenewalUrl.visibility = View.VISIBLE
                        } else {
                            textRenewalUrl.visibility = View.GONE
                        }

                        // Nascondi campo di input in sola lettura
                        editRenewalUrl.visibility = View.GONE



                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nel caricamento del documento", Toast.LENGTH_SHORT).show()
                }
        }



        btnDeleteDocumento.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val docRef = db.collection("pilotProfiles").document(uid)
                .collection("documents").document(documentId!!)

            docRef.get().addOnSuccessListener { snapshot ->
                val fileUrl = snapshot.getString("fileUrl")

                // Elimina documento Firestore
                docRef.delete().addOnSuccessListener {
                    Toast.makeText(this, "Documento eliminato", Toast.LENGTH_SHORT).show()

                    // Elimina file da Firebase Storage
                    if (!fileUrl.isNullOrEmpty()) {
                        FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                            .delete()
                            .addOnSuccessListener {
                                // File eliminato
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Documento eliminato ma non il file", Toast.LENGTH_SHORT).show()
                            }
                    }

                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Errore eliminazione", Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Errore lettura documento", Toast.LENGTH_SHORT).show()
            }
        }


        closeButton.setOnClickListener { finish() }

        editRenewalUrl.setOnClickListener {
            val url = editRenewalUrl.text.toString()
            if (url.startsWith("http")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        editExpiryDate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = editExpiryDate.text.toString().trim()

                // Match date in formati comuni: 12/07/2025, 12.07.2025, ecc.
                val dateRegex = Regex("""(\d{2})[./\\-](\d{2})[./\\-](\d{4})""")
                val match = dateRegex.matchEntire(input)

                if (match != null) {
                    val (day, month, year) = match.destructured

                    try {
                        // Verifica la validità della data
                        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        date.isLenient = false
                        date.parse("$day-$month-$year") // lancia eccezione se non valida

                        editExpiryDate.setText("$day-$month-$year") // Riformatta in modo coerente
                    } catch (e: Exception) {
                        Toast.makeText(this, "Data non valida", Toast.LENGTH_SHORT).show()
                        editExpiryDate.setText("") // Reset se non valida
                    }
                } else if (input.isNotBlank()) {
                    Toast.makeText(this, "Formato data errato. Usa gg-MM-aaaa", Toast.LENGTH_SHORT).show()
                    editExpiryDate.setText("") // Reset se malformata
                }
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            fileUri = data.data

            PDFBoxResourceLoader.init(applicationContext)

            val inputStream = contentResolver.openInputStream(fileUri!!)
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()

            // Mostra anteprima (se immagine)
            previewFile.setImageURI(fileUri)

            // Parser unificato
            when {
                text.contains("Polizza Collettiva") -> {
                    val polizza = Regex("Polizza Collettiva n\\.?(\\d+)").find(text)?.groupValues?.get(1)
                    val tessera = Regex("N\\.\\s*TESSERA\\s*([A-Z0-9]+)").find(text)?.groupValues?.get(1)
                    val scadenza = Regex("Scadenza.*?(\\d{2}-\\d{2}-\\d{4})").find(text)?.groupValues?.get(1)

                    editTitle.setText("Assicurazione DronEzine")
                    editType.setText("assicurazione")
                    editExpiryDate.setText(scadenza ?: "")
                    editPolicyNumber.setText(polizza ?: "")
                    editTesseraNumber.setText(tessera ?: "")
                    editRenewalUrl.setText("https://www.dronezine.it/istruzioni-per-il-rinnovo-delle-opzioni-plus/")
                }

                text.contains("Coverdrone", ignoreCase = true) || text.contains("PARTE 1 N. polizza") -> {
                    val polizza = Regex("N\\. polizza\\s+([A-Z0-9]+)").find(text)?.groupValues?.get(1)
                    val rangeRegex = Regex("da\\s+(\\d{2}/\\d{2}/\\d{4})\\s*(?:\\r?\\n)?\\s*a\\s+(\\d{2}/\\d{2}/\\d{4})", RegexOption.IGNORE_CASE)
                    val scadenza = rangeRegex.find(text)?.groupValues?.get(2)?.replace("/", "-")

                    editTitle.setText("Assicurazione Coverdrone")
                    editType.setText("assicurazione")
                    editExpiryDate.setText(scadenza ?: "")
                    editPolicyNumber.setText(polizza ?: "")
                    editTesseraNumber.setText("")
                    editRenewalUrl.setText("https://www.coverdrone.com/it/my-account/")
                }

                text.contains("REMOTE PILOT", ignoreCase = true)
                        || text.contains("REMOTE PILOT CERTIFICATE", ignoreCase = true)
                        || text.contains("PROOF OF COMPLETION", ignoreCase = true)
                        || text.contains("Numero di registrazione", ignoreCase = true)
                        || text.contains("Identification number", ignoreCase = true) -> {

                    val idRegex = Regex("[A-Z]{2,}-[A-Z]{2,}-\\w+")
                    val identificativo = idRegex.find(text)?.value ?: ""

                    val dataRegex = Regex("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})")
                    val dataMatch = dataRegex.findAll(text).map { it.value }.toList()
                    val dataScadenza = (dataMatch.getOrNull(1) ?: dataMatch.getOrNull(0))
                        ?.replace(".", "-")?.replace("/", "-")

                    editTitle.setText("Attestato UAS")
                    editType.setText("patentino")
                    editPolicyNumber.setText(identificativo)
                    editExpiryDate.setText(dataScadenza ?: "")
                    editTesseraNumber.setText("")
                    editRenewalUrl.setText("")
                }

                else -> {
                    Toast.makeText(this, "⚠️ Documento non riconosciuto", Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun salvaDocumento() {
        val uid = auth.currentUser?.uid ?: return
        val title = editTitle.text.toString().trim()
        val type = editType.text.toString().trim()
        val expiry = editExpiryDate.text.toString().trim()
        val renewalUrl = editRenewalUrl.text.toString().trim()
        val policyNumber = editPolicyNumber.text.toString().trim()
        val tesseraNumber = editTesseraNumber.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "Compila il titolo", Toast.LENGTH_SHORT).show()
            return
        }

        val documentId = intent.getStringExtra("documentId")
        val docRef = db.collection("pilotProfiles").document(uid).collection("documents")

        fun updateFirestore(uri: String?, mimeType: String?) {
            val docData = hashMapOf(
                "title" to title,
                "type" to type,
                "expiryDate" to expiry,
                "renewalUrl" to renewalUrl,
                "policyNumber" to policyNumber,
                "tesseraNumber" to tesseraNumber,
                "uploadDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )

            // Se è stato caricato un file, salviamo anche fileUrl e mimeType
            if (uri != null && mimeType != null) {
                docData["fileUrl"] = uri
                docData["fileMimeType"] = mimeType
            }

            if (!documentId.isNullOrEmpty()) {
                docRef.document(documentId).update(docData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Documento aggiornato", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore aggiornamento documento", Toast.LENGTH_SHORT).show()
                    }
            } else {
                if (fileUri == null) {
                    Toast.makeText(this, "Carica un file per il nuovo documento", Toast.LENGTH_SHORT).show()
                    return
                }

                docRef.add(docData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Documento salvato", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore salvataggio Firestore", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        if (fileUri != null) {
            // L'utente ha caricato un nuovo file
            val filename = "documents/$uid/${UUID.randomUUID()}"
            val fileRef = storage.reference.child(filename)

            fileRef.putFile(fileUri!!).addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    val mimeType = contentResolver.getType(fileUri!!) ?: "application/octet-stream"
                    updateFirestore(uri.toString(), mimeType)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Errore upload file", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Nessun nuovo file → aggiorna solo metadati
            updateFirestore(null, null)
        }
    }

}
