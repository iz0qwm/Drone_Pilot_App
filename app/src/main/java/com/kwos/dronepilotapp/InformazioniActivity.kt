package com.kwos.dronepilotapp

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class InformazioniActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        setContentView(R.layout.activity_informazioni)

        // Recupera la versione dell'app
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        // Crea una stringa HTML dinamica
        val htmlContent = """
            <html>
            <head><title>Informazioni</title></head>
            <body>
                <h1>Informazioni sull'App</h1>
                <p>Benvenuto nell'app di esempio!</p>
                <p><strong>Versione dell'app:</strong> $versionName</p>
                <!-- Puoi aggiungere altre informazioni HTML qui -->
            </body>
            </html>
        """

        // Imposta il WebView
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true // Abilita JavaScript se necessario
        webView.loadData(htmlContent, "text/html", "UTF-8")
    }
}
