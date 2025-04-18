package com.kwos.dronepilotapp

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class InformazioniActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        setContentView(R.layout.activity_informazioni)

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

        // Crea una stringa HTML dinamica
        val htmlContent = """
<html>

<head>
    <title>Informazioni</title>
</head>

<body>
    <h2>App per il pilota remoto</h2>
    <p><strong>Stai utilizzando la versione:</strong>&nbsp;$versionName</p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">Questa app &egrave; dedicata ai piloti remoti di UAS, denominati anche &quot;piloti di droni&quot;.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong><span style="color: rgb(184, 49, 47);">Per far volare un drone &egrave; necessario conoscere delle informazioni basilari:</span></strong></span></p>
    <ul>
        <li style="font-family: Arial, Helvetica, sans-serif;">ogni pilota deve aver stipulato una assicurazione per danni contro terzi</li>
        <li style="font-family: Arial, Helvetica, sans-serif;">ogni pilota deve essere registrato come operatore presso il sito: <a href="http://www.d-flight.it">http://www.d-flight.it</a></li>
        <li style="font-family: Arial, Helvetica, sans-serif;">dopo la registrazione si potr&agrave; scaricare un QRCode da applicare sul drone</li>
        <li style="font-family: Arial, Helvetica, sans-serif;">se il drone pesa pi&ugrave; di 250g sar&agrave; necessario conseguire un attestato OPEN A1/A3</li>
        <li style="font-family: Arial, Helvetica, sans-serif;">non &egrave; possibile far volare il drone ovunque, per capire dove, bisogna utilizzare la mappa di D-Flight.</li>
    </ul>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>La presente app sar&agrave; solo di ausilio al pilota che ne vorr&agrave; far uso, indicando dove sta operando e facendo conoscere la propria posizione agli altri piloti.</strong></span></p>
    <p><br></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>FUNZIONALITA&apos; - ISTRUZIONI</strong></span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Posizione:&nbsp;</strong>il pilota remoto potr&agrave; inserire il nome del drone che sta facendo volare e dichiarare l&apos;Inizio del volo. In questo modo apparir&agrave; un <span style="color: rgb(226, 80, 65);"><em>marker di colore rosso</em></span> sulla posizione corrente. Sulla finestra principale se nel raggio di 1Km vi saranno altri piloti remoti, a<span style="font-family: Arial, Helvetica, sans-serif;">pparir&agrave; un avviso.</span></span><br><br><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Chat privata:&nbsp;</strong>tramite uno switch, il pilota remoto potr&agrave; dichiarare la volont&agrave; di ricevere messaggi privati da altri piloti. Il <span style="color: rgb(44, 130, 201);"><em>colore del marker sar&agrave; di colore blu</em></span>. Cliccando sul marker si aprir&agrave; un tooltip su cui &egrave; possibile cliccare per chattare con il pilota. All&apos;arrivo di un messaggio, apparir&agrave; un alert sullo smartphone e una notifica nella schermata principale. Cliccandovi sopra, si acceder&agrave; alla schermata della chat privata.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Chat di gruppo:</strong> sul men&ugrave; opzioni (in alto a destra) sar&agrave; possibile accedere alla Chat di gruppo a cui accedono tutti i piloti remoti che sono loggati sulla Web App Drone Pilot.</span><br><br><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Restrizioni al volo</strong>: prima di iniziare a far volare un drone &egrave; buona norma consultare d-flight.it per consultare la mappa delle aree in cui &egrave; consentito. In ogni caso, nella schermata principale, in basso, vi &egrave; una prima indicazione dell&apos;altitudine massima consentita nella posizione in cui vi trovate.</span><br><br><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Previsioni meteo e GPS</strong>: &egrave; possibile consultare lo stato attuale delle condizioni meteorologiche e le previsioni per le prossime ore. Inoltre, &egrave; possibile monitorare lo stato della ionosfera per informarsi sul buon funzionamento delle costellazioni GNSS (GPS).</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Spot di volo</strong>: &egrave; possibile dichiarare la presenza di una postazione di volo (spot) o osservare su una mappa, quelle comunicate dagli altri piloti. Se si dichiara una postazione, vicina ad un&apos;altra (500m di raggio) gi&agrave; presente nel database, si incrementer&agrave; solo un contatore e non verr&agrave; creata una nuova. E&apos; possibile cancellare uno spot solo se siete stati voi a crearlo e non &egrave; stato segnalato da qualcun altro.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>Radio PMR446</strong>: se con te hai una radio PMR446 (Personal Mobile Radio) accesa sul canale 4 (CH 4), dedicato ai Piloti di Droni, vai nel men&ugrave; Impostazioni e seleziona il flag. <span style="color: rgb(97, 189, 109);"><em>L&apos;icona del tuo marker diventer&agrave; di colore verde.&nbsp;</em></span><span style="color: rgb(0, 0, 0);">In questo modo i piloti che saranno nelle tue vicinanze, potranno contattarti anche via radio.</span></span></p>
    <p><br></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><strong>INFORMAZIONI</strong></span><br><span style="font-family: Arial, Helvetica, sans-serif;"><br><strong><em>Meteo</em></strong></span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><em>Le previsioni meteorologiche e lo stato attuale del tempo, vengono fornite da &nbsp;</em><a data-fr-linked="true" href="https://www.meteoblue.com/"><em>https://www.meteoblue.com/</em></a> e <a data-fr-linked="true" href="https://www.openweathermap.org/"><em>https://www.openweathermap.org/</em></a></span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">E&apos; sempre responsabilit&agrave; del pilota conoscere le condizioni meteo ottimali per far volare il proprio drone, sia in termini di visibilit&agrave; atmosferica, che di pioggia, neve o vento.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">Anche le giornate particolarmente calde o particolarmente fredde possono inficiare sulle prestazioni dell&apos;UAS.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><em><strong>Rete GNSS (GPS)</strong></em></span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;"><em>Le condizioni della ionosfera, ed in particolare il numero di elettroni presenti, &egrave; fornito da&nbsp;</em><a data-fr-linked="true" href="http://www.eswua.ingv.it/"><em>http://www.eswua.ingv.it/</em></a></span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">I droni stabilizzati, utilizzano un ricevitore GPS per rimanere stabili in volo e per avere un traiettoria corretta.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">Sebbene al<span style="font-family: Arial, Helvetica, sans-serif;">le nostre latitudini sia veramente difficile rilevare errori di posizionamento superiori al metro, &egrave; bene sapere che il</span> numero di elettroni totali (Total Electron Content) presenti nello strato della ionosfera terrestre, &egrave; indice degli errori di valutazione della posizione da parte dei ricevitori GPS.</span></p>
    <p><span style="font-family: Arial, Helvetica, sans-serif;">In condizioni di elevato numero di TEC si pu&ograve; arrivare ad errori compresi tra -3 e +7m, soprattutto alle latitudini elevate (verso i poli terrestri).</span></p>
    <p><strong><span style="font-family: Arial, Helvetica, sans-serif;"><em>Informazioni tecniche e privacy</em></span></strong></p>
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
