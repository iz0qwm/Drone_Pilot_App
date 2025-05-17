package com.kwos.dronepilotapp

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


class InformazioniActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        setContentView(R.layout.activity_informazioni)

        val closeButton: Button = findViewById(R.id.close_informazioni_button)

        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }

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

        // Recupera la versione dell'app
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        // Imposta il WebView
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true // Abilita JavaScript se necessario


        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val zoneUrl = "https://www.kwos.org/appoggio/droni/dflight_geozones.json"

        val request = JsonObjectRequest(
            Request.Method.GET, zoneUrl, null,
            { response: JSONObject ->
                val zoneVersion = response.optString("title", "Versione zone non disponibile")

        val htmlContent = """
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <title>Informazioni</title>
  <style>
    body {
      font-family: 'Segoe UI', 'Roboto', 'Arial', sans-serif;
      color: #333;
      padding: 20px;
      line-height: 1.6;
      background-color: #fdfdfd;
    }
    h2 {
      color: #007bff;
    }
    ul {
      padding-left: 20px;
    }
    li {
      margin-bottom: 8px;
    }
    a {
      color: #007bff;
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
    .manual-box {
      background-color: #f0f8ff;
      border-left: 5px solid #007bff;
      padding: 12px;
      margin-top: 24px;
      font-size: 16px;
    }
    .footer {
      margin-top: 32px;
      font-style: italic;
      color: #666;
    }
  </style>
</head>
<body>

  <h2>📱 Drone Pilot App</h2>
  <p><strong>Versione installata:</strong> $versionName</p>
  <p><strong>Versione dati zone:</strong> $zoneVersion</p>

  <p>Questa app è dedicata ai piloti remoti di UAS, noti anche come "piloti di droni".</p>

  <p style="color: #c0392b;"><strong>Per far volare un drone è necessario conoscere alcune regole fondamentali:</strong></p>

  <ul>
    <li>Devi avere un'assicurazione contro terzi</li>
    <li>Devi registrarti come operatore su <a href="https://www.d-flight.it" target="_blank">d-flight.it</a></li>
    <li>Dopo la registrazione, scarica il QR Code da applicare sul drone</li>
    <li>Se il drone pesa più di 250g, devi ottenere l’attestato OPEN A1/A3</li>
    <li>Non puoi volare ovunque: consulta sempre la mappa di D-Flight</li>
  </ul>

  <p><strong>Questa app ti aiuta a condividere la tua posizione in tempo reale con altri piloti.</strong></p>

  <div class="manual-box">
    📘 <strong>Manuale Utente</strong><br/>
    Leggi la guida completa e aggiornata:<br/>
    <a href="https://iz0qwm.github.io/Drone_Pilot_App/" target="_blank">
      https://iz0qwm.github.io/Drone_Pilot_App/
    </a>
  </div>

  <h3 style="margin-top: 32px;">🔒 Privacy e dati salvati</h3>
  <p>I dati salvati in modo permanente sono solo <strong>email</strong> e <strong>nome</strong> dell'utente.</p>
  <p>I dati di posizione vengono eliminati al termine del volo o al logout.</p>
  <p>Le chat tra due piloti vengono eliminate quando entrambi sono offline o a fine volo.</p>
  <p>I messaggi della Group Chat vengono cancellati automaticamente quando il gruppo è vuoto.</p>

  <div class="footer">© 2025 Raffaello Di Martino - KWOS - IZ0QWM</div>

</body>
</html>
"""
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            },
            { error ->
                val htmlFallback = """
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <title>Informazioni</title>
  <style>
    body {
      font-family: 'Segoe UI', 'Roboto', 'Arial', sans-serif;
      color: #333;
      padding: 20px;
      line-height: 1.6;
      background-color: #fdfdfd;
    }
    h2 {
      color: #007bff;
    }
    ul {
      padding-left: 20px;
    }
    li {
      margin-bottom: 8px;
    }
    a {
      color: #007bff;
      text-decoration: none;
    }
    a:hover {
      text-decoration: underline;
    }
    .manual-box {
      background-color: #f0f8ff;
      border-left: 5px solid #007bff;
      padding: 12px;
      margin-top: 24px;
      font-size: 16px;
    }
    .footer {
      margin-top: 32px;
      font-style: italic;
      color: #666;
    }
  </style>
</head>
<body>

  <h2>📱 Drone Pilot App</h2>
  <p><strong>Versione installata:</strong> $versionName</p>
  <p><strong>Versione dati zone:</strong> ERRORE</p>

  <p>Questa app è dedicata ai piloti remoti di UAS, noti anche come "piloti di droni".</p>

  <p style="color: #c0392b;"><strong>Per far volare un drone è necessario conoscere alcune regole fondamentali:</strong></p>

  <ul>
    <li>Devi avere un'assicurazione contro terzi</li>
    <li>Devi registrarti come operatore su <a href="https://www.d-flight.it" target="_blank">d-flight.it</a></li>
    <li>Dopo la registrazione, scarica il QR Code da applicare sul drone</li>
    <li>Se il drone pesa più di 250g, devi ottenere l’attestato OPEN A1/A3</li>
    <li>Non puoi volare ovunque: consulta sempre la mappa di D-Flight</li>
  </ul>

  <p><strong>Questa app ti aiuta a condividere la tua posizione in tempo reale con altri piloti.</strong></p>

  <div class="manual-box">
    📘 <strong>Manuale Utente</strong><br/>
    Leggi la guida completa e aggiornata:<br/>
    <a href="https://iz0qwm.github.io/Drone_Pilot_App/" target="_blank">
      https://iz0qwm.github.io/Drone_Pilot_App/
    </a>
  </div>

  <h3 style="margin-top: 32px;">🔒 Privacy e dati salvati</h3>
  <p>I dati salvati in modo permanente sono solo <strong>email</strong> e <strong>nome</strong> dell'utente.</p>
  <p>I dati di posizione vengono eliminati al termine del volo o al logout.</p>
  <p>Le chat tra due piloti vengono eliminate quando entrambi sono offline o a fine volo.</p>
  <p>I messaggi della Group Chat vengono cancellati automaticamente quando il gruppo è vuoto.</p>

  <div class="footer">© 2025 Raffaello Di Martino - KWOS - IZ0QWM</div>

</body>
</html>
        """
                webView.loadDataWithBaseURL(null, htmlFallback, "text/html", "UTF-8", null)
            }
        )

        webView.loadData(
            "<html><body><p><em>📡 Un attimo, sto verificando la data di aggiornamento delle zone di d-flight...</em></p></body></html>",
            "text/html",
            "UTF-8"
        )

        requestQueue.add(request)


    }
}
