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
                <h2>App per il pilota remoto</h2>
                <p><strong>Stai utilizzando la versione:</strong> $versionName</p>
                <p><span style='font-family: Arial, Helvetica, sans-serif;'>Questa app &egrave; dedicata ai piloti remoti di UAS, denominati anche &quot;piloti di droni&quot;.</span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><strong><span style='color: rgb(184, 49, 47);'>Per far volare un drone &egrave; necessario conoscere delle informazioni basilari:</span></strong></span></p>
<ul>
    <li style='font-family: Arial, Helvetica, sans-serif;'>ogni pilota deve aver stipulato una assicurazione per danni contro terzi</li>
    <li style='font-family: Arial, Helvetica, sans-serif;'>ogni pilota deve essere registrato come operatore presso il sito: <a href='http://www.d-flight.it'>http://www.d-flight.it</a></li>
    <li style='font-family: Arial, Helvetica, sans-serif;'>dopo la registrazione si potr&agrave; scaricare un QRCode da applicare sul drone</li>
    <li style='font-family: Arial, Helvetica, sans-serif;'>se il drone pesa pi&ugrave; di 250g sar&agrave; necessario conseguire un attestato OPEN A1/A3</li>
    <li style='font-family: Arial, Helvetica, sans-serif;'>non &egrave; possibile far volare il drone ovunque, per capire dove, bisogna utilizzare la mappa di D-Flight.</li>
</ul>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><strong>La presente app sar&agrave; solo di ausilio al pilota che ne vorr&agrave; far uso, indicando dove sta operando e facendo conoscere la propria posizione agli altri piloti.</strong></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><strong>Gli sar&agrave; possibile scambiare brevi messaggi di testo con altri piloti, se avranno dato la disponibilit&agrave; a chattare.</strong></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><strong>Prima di iniziare a far volare il drone, consultare le condizioni meteorologiche e lo stato delle costellazioni GPS.</strong></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><br><strong>METEO</strong><br></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><em>Le previsioni meteorologiche e lo stato attuale del tempo, vengono fornite da &nbsp;</em><a data-fr-linked='true' href='https://www.meteoblue.com/'><em>https://www.meteoblue.com/</em></a></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'>E&apos; sempre responsabilit&agrave; del pilota conoscere le condizioni meteo ottimali per far volare il proprio drone, sia in termini di visibilit&agrave; atmosferica, che di pioggia, neve o vento.</span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'>Anche le giornate particolarmente calde o particolarmente fredde possono inficiare sulle prestazioni dell&apos;UAS.</span></p>
<p><br><span style='font-family: Arial, Helvetica, sans-serif;'><strong>RETE GNSS (GPS)</strong></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'><em>Le condizioni della ionosfera, ed in particolare il numero di elettroni presenti, &egrave; fornito da&nbsp;</em><a data-fr-linked='true' href='http://www.eswua.ingv.it/'><em>http://www.eswua.ingv.it/</em></a></span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'>I droni stabilizzati, utilizzano un ricevitore GPS per rimanere stabili in volo e per avere un traiettoria corretta.</span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'>Sebbene al<span style='font-family: Arial, Helvetica, sans-serif;'>le nostre latitudini sia veramente difficile rilevare errori di posizionamento superiori al metro, &egrave; bene sapere che il</span>&nbsp;numero di elettroni totali (Total Electron Content) presenti nello strato della ionosfera terrestre, &egrave; indice degli errori di valutazione della posizione da parte dei ricevitori GPS.</span></p>
<p><span style='font-family: Arial, Helvetica, sans-serif;'>In condizioni di elevato numero di TEC si pu&ograve; arrivare ad errori compresi tra -3 e +7m, soprattutto alle latitudini elevate (verso i poli terrestri).</span></p>
<p><strong><span style="font-family: Arial, Helvetica, sans-serif;">Informazioni tecniche e privacy</span></strong></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">L&apos;applicazione &egrave; stata sviluppata in linguaggio Kotlin ed &egrave; rilasciata in licenza Open Source.&nbsp;</span><br><span style="font-family: Arial, Helvetica, sans-serif;">Si pu&ograve; scaricare il progetto da: <a data-fr-linked="true" href="https://github.com/iz0qwm/Drone_Pilot_App/">https://github.com/iz0qwm/Drone_Pilot_App/</a></span></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">Per il corretto funzionamento sono necessari un Firestore DataBase ed un Firestore Realtime Database.&nbsp;</span></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">Gli unici dati registrati e permanenti sono l&apos;email e il nome dell&apos;utente, per garantire i successivi Login.</span></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">I dati di posizione vengono cancellati a &quot;Fine Volo&quot; o al &quot;Logout&quot;.</span></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">Le chat tra due piloti vengono cancellate periodicamente se entrambi i piloti sono off-line o al termine volo di entrambi.</span></p>
<p><span style="font-family: Arial, Helvetica, sans-serif;">I messaggi contenuti nella Group Chat, vengono cancellati periodicamente quando non &egrave; pi&ugrave; nessuno presente nel gruppo.&nbsp;</span></p>
            </body>
            </html>
        """

        // Imposta il WebView
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true // Abilita JavaScript se necessario
        webView.loadData(htmlContent, "text/html", "UTF-8")
    }
}
