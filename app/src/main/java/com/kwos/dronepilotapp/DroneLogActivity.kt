package com.kwos.dronepilotapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class DroneLogActivity : AppCompatActivity() {

    private lateinit var fileInfoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drone_log)

        // Padding automatico
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        supportActionBar?.hide()

        findViewById<Button>(R.id.close_weather_button).setOnClickListener { finish() }
        findViewById<TextView>(R.id.logTitleTextView).text = "\uD83D\uDCC1 Importa Log di Volo DJI"
        findViewById<TextView>(R.id.logDescriptionTextView).text =
            "Importa un file di log generato dalla DJI FLY copiato nella cartella Download del tuo dispositivo. La traiettoria verrà mostrata sulla mappa."

        val layout = findViewById<android.widget.LinearLayout>(R.id.droneLogLayout)

        // TextView dinamica per mostrare nome file
        fileInfoTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DroneLogActivity, android.R.color.black))
            setPadding(0, 16, 0, 0)
        }
        layout.addView(fileInfoTextView)

        // Bottone Importa
        val importButton = findViewById<Button>(R.id.importLogButton)
        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }
            startActivityForResult(intent, 1234)
        }

        // Bottone Istruzioni
        val helpButton = Button(this).apply {
            text = "\u2139\uFE0F Istruzioni"
            setOnClickListener { showImportInstructions() }
        }
        layout.addView(helpButton)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234 && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                var fileName = "file selezionato"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
                fileInfoTextView.text = "\uD83D\uDCC4 Hai selezionato: $fileName"
                uploadLogFile(uri)
            }
        }
    }

    private fun uploadLogFile(uri: Uri) {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val fileBytes = inputStream.readBytes()

        var fileName = "log.txt"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "logfile",
                fileName,
                RequestBody.create("text/plain".toMediaTypeOrNull(), fileBytes)
            )
            .build()

        val request = Request.Builder()
            .url("http://91.121.90.186:5555/upload")
            .addHeader("X-API-KEY", "RaDa0707")
            .post(requestBody)
            .build()

        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val response = client.newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        fileInfoTextView.text = "✅ Log inviato con successo!"
                    } else {
                        fileInfoTextView.text = "❌ Errore upload: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    fileInfoTextView.text = "❌ Errore rete: ${e.message}"
                }
            }
        }.start()
    }

    private fun showImportInstructions() {
        val message = """
            📥 Come importare un file di log DJI:

            1️⃣ Collega lo smartphone al PC con un cavo USB

            2️⃣ Sul PC, apri la cartella:
            Ad esempio:
            Questo PC > realme 11 Pro+ 5G > Memoria condivisa interna > Android > data > dji.go.v5 > files > FlightRecord

            3️⃣ Copia il file di log che ti interessa (es. DJIFlightRecord_2025-05-15_[12-05-38].txt)

            4️⃣ Incollalo in una cartella accessibile, come:
            Questo PC > realme 11 Pro+ 5G > Memoria condivisa interna > Download

            5️⃣ Torna nell'app Drone Pilot e premi "Importa log"

            6️⃣ Seleziona il file .txt dalla cartella Download
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Istruzioni per importare un log")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
