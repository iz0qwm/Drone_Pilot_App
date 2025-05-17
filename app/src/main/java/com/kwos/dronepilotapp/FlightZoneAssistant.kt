package com.kwos.dronepilotapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import org.jsoup.Jsoup


class FlightZoneAssistant(
    private val context: Context,
    private val onSpeechStarted: () -> Unit = {} // callback opzionale
) {

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val italianVoices = tts.voices.filter {
                    it.locale.language == "it" && !it.isNetworkConnectionRequired
                }

                val maleVoice = italianVoices.find { voice ->
                    voice.name.contains("male", ignoreCase = true) || voice.name.contains("1", ignoreCase = true)
                }

                if (maleVoice != null) {
                    tts.voice = maleVoice
                    Log.d("DronePilotApp", "TTS: Voce italiana maschile selezionata: ${maleVoice.name}")
                } else {
                    tts.language = Locale.ITALIAN
                    Log.w("DronePilotApp", "TTS: Voce maschile non trovata, uso lingua italiana predefinita")
                }
            } else {
                Log.e("DronePilotApp", "TTS: Errore nell'inizializzazione di TextToSpeech")
            }
        }
    }


    fun askPermissionToFly(latitude: Double, longitude: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = sendPostRequest(latitude, longitude)
            val reply = parseFlightZoneInfo(response)
            withContext(Dispatchers.Main) {
                onSpeechStarted() // notifica che inizia a parlare
                //Log.d("DronePilotApp", "🐛 DEBUG 📣 TESTO PRIMA DI SPEAK: $reply (${reply::class.simpleName})")
                (context as? DashboardActivity)?.showAssistantOverlay(reply)
                speak(reply)
            }
        }
    }

    private fun sendPostRequest(lat: Double, lon: Double): String {
        val jsonData = JSONObject().apply {
            put("latitude", lat)
            put("longitude", lon)
            put("userId", "VoiceCommand")
        }

        val url = URL("https://us-central1-tutto-sui-droni-community.cloudfunctions.net/getFlightVoiceInfo")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.write(jsonData.toString().toByteArray(Charsets.UTF_8))
            connection.outputStream.flush()

            val reader = connection.inputStream.bufferedReader()
            val result = reader.readText()
            connection.disconnect()
            result
        } catch (e: Exception) {
            Log.e("DronePilotApp", "FlightZoneAssistant: Errore HTTP: ${e.message}")
            ""
        }
    }

    private suspend fun parseFlightZoneInfo(jsonString: String): String {
        if (jsonString.isBlank()) return "Non riesco a connettermi al server delle zone di volo."

        return try {
            val json = JSONObject(jsonString)
            val zones = json.optJSONArray("zones") ?: return "Nessuna zona trovata."

            if (zones.length() == 0) {
                return "Nessuna restrizione trovata. Il drone può volare fino a 120 metri."
            }

            // Prima zona = più restrittiva
            val activeZone = zones.getJSONObject(0)

            // Trova la zona permanente se presente
            var defaultZone: JSONObject? = null
            Log.d("DronePilotApp", "ZONE_COUNT: ${zones.length()}")
            for (i in 0 until zones.length()) {
                val zone = zones.getJSONObject(i)
                Log.d("DronePilotApp", "ZONE #$i --> name=${zone.optString("name")} - lowerLimit=${zone.optString("lowerLimit")} - restriction=${zone.optString("restriction")} - isNotam=${zone.optBoolean("isNotam")} - permanent=${zone.optJSONObject("applicability")?.optString("permanent")}")
                val applicability = zone.optJSONObject("applicability")
                if (applicability?.optString("permanent") == "YES") {
                    defaultZone = zone
                    break
                }
            }

            val defaultLimit = defaultZone?.optString("lowerLimit", "120") ?: "120"
            val activeLimit = activeZone.optString("lowerLimit", "120")
            val isNotam = activeZone.optBoolean("isNotam", false)
            val name = activeZone.optString("name", "")
            val zonaDescrizione = name
                .replace(Regex("^\\w{2,5}[-_\\s]\\d+\\s*"), "") // rimuove codici tipo SEC-003 o LIR314
                .replace(Regex("[‘’']"), "'") // normalizza apostrofi strani
                .trim()


            val aipNote = if (!isNotam) {
                val aipMatch = Regex("LI[-_\\s]?[RDTP]{1,3}\\d+[A-Z/]*", RegexOption.IGNORE_CASE).find(name)
                    ?: Regex("LIR\\d+[A-Z/]*", RegexOption.IGNORE_CASE).find(name)

                Log.d("DronePilotApp", "FlightZoneAssistant: aipMatch = ${aipMatch?.value}")
                Log.d("DronePilotApp", "FlightZoneAssistant: Analizzo nome zona: $name")

                if (aipMatch == null) {
                    Log.w("DronePilotApp", "FlightZoneAssistant: Nessun codice AIP trovato nel nome")
                }

                val aipCode = aipMatch?.value?.let { code ->
                    val suffixMatch = Regex("LIR(\\d+)([A-Z])", RegexOption.IGNORE_CASE).find(code)
                    if (suffixMatch != null) {
                        val (digits, letter) = suffixMatch.destructured
                        "LI R$digits/$letter"
                    } else {
                        code.replace(Regex("LIR", RegexOption.IGNORE_CASE), "LI R")
                            .replace("_", " ")
                            .trim()
                    }
                }

                if (aipCode != null) {
                    try {
                        // ✅ QUI finalmente restituiamo il valore a aipNote
                        fetchAipDetails(aipCode)
                    } catch (e: Exception) {
                        Log.w("DronePilotApp", "Errore nel recupero dettagli AIP: ${e.message}")
                        "Attenzione: AIP $aipCode attiva. Consultare le pubblicazioni aeronautiche."
                    }
                } else {
                    ""
                }
            } else {
                ""
            }



            val restriction = activeZone.optString("restriction", "")
            val message = activeZone.optString("message", null)
            val otherReason = activeZone.optString("otherReasonInfo", "")
            val reasonsArray = activeZone.optJSONArray("reasons")
            val applicability = activeZone.optJSONObject("applicability")
            val permanent = applicability?.optString("permanent")
            val start = applicability?.optString("startDateTime")
            val end = applicability?.optString("endDateTime")

            val reasons = mutableListOf<String>()
            for (i in 0 until (reasonsArray?.length() ?: 0)) {
                reasons.add(reasonsArray?.getString(i) ?: "")
            }

            Log.d("DronePilotApp", "ACTIVE ZONE: name=$name - lowerLimit=$activeLimit - isNotam=$isNotam")

            val intro = if (activeLimit == "0") {
                "Volo non consentito in questa zona. "
            } else if (defaultZone != null && defaultLimit != activeLimit) {
                "Solitamente il drone può volare fino a $defaultLimit metri, ma "
            } else {
                "Il drone può volare fino a $activeLimit metri. "
            }


            val detail = if (isNotam) {
                Log.d("DronePilotApp", "FlightZoneAssistant: Provo a estrarre orari schedulati dal nome NOTAM: $name")
                val notamOrari = fetchNotamSchedule(name)
                val notamSintetico = buildString {
                    append(interpretNotam(message, permanent, start, end))
                    if (notamOrari != null) {
                        append(" ${notamOrari.descrizione}. ")
                        append(if (notamOrari.attivaOra) "La restrizione è attiva ora. " else "La restrizione non è attiva in questo momento. ")
                    }
                }

                val restrictionNote = when (restriction.uppercase()) {
                    "PROHIBITED" -> "Proibito far volare UAS negli orari e nei tempi in cui è attivo. "
                    "RESTRICTED" -> "Zona a volo ristretto per UAS. "
                    "DANGER" -> "Zona pericolosa per UAS. "
                    else -> ""
                }

                //"È attivo un NOTAM ($name) con restrizione $restriction. $restrictionNote$notamSintetico"
                "$restrictionNote$notamSintetico"
            } else {
                val reasonText = when {
                    reasons.contains("AIR_TRAFFIC") -> "Attenzione: sei in prossimità di un aeroporto."
                    reasons.contains("SENSITIVE") -> "Questa è una zona sensibile, vola con cautela."
                    reasons.contains("EMERGENCY") -> "Attenzione a conflitti di area con aeromobili."
                    reasons.contains("PROHIBITED") -> "Vietato volare."
                    reasons.contains("NATURE") && reasons.contains("NOISE") ->
                        "Questa è una zona naturale, non si deve recare disturbo."
                    reasons.contains("NATURE") -> "Questa è una zona naturale."
                    reasons.contains("NOISE") -> "Non si deve recare disturbo."
                    reasons.isNotEmpty() -> "Ci sono restrizioni attive: ${reasons.joinToString(", ")}."
                    else -> ""
                }

                val otherInfo = when (otherReason.uppercase(Locale.ROOT)) {
                    "NFZ" -> "Questa è una zona in cui il volo è interdetto. "
                    "ATM09", "ATM09A" -> "Zona soggetta a restrizioni aeronautiche ATM 09. "
                    else -> if (otherReason.isNotBlank()) "Circolare $otherReason. " else ""
                }

                "$reasonText$otherInfo"

            }

            val periodoAttivazione = if (permanent == "NO" && !start.isNullOrBlank() && !end.isNullOrBlank()) {
                try {
                    val formatter = DateTimeFormatter.ISO_DATE_TIME
                    val zonedStart = ZonedDateTime.parse(start, formatter).withZoneSameInstant(ZoneId.systemDefault())
                    val zonedEnd = ZonedDateTime.parse(end, formatter).withZoneSameInstant(ZoneId.systemDefault())

                    val formato = DateTimeFormatter.ofPattern("d MMMM yyyy 'alle' HH:mm", Locale.ITALIAN)
                    val startFormatted = formato.format(zonedStart)
                    val endFormatted = formato.format(zonedEnd)

                    "D-Flight dice che questa restrizione è valida dal $startFormatted fino al $endFormatted. "
                } catch (e: Exception) {
                    Log.w("DronePilotApp", "Errore parsing date zona non NOTAM: ${e.message}")
                    ""
                }
            } else ""

            val zonaDescrizionePulita = zonaDescrizione
                // 1. Trasforma 13/31 → pista 13 31
                .replace(Regex("(?<![\\w\\d])(\\d{2})/(\\d{2})(?!\\d)"), "pista $1 $2")
                // 2. Sostituisci altri / e _
                .replace("/", " ")
                .replace("_", " ")
                // 3. Se inizia con lettere seguite da cifre → separa le lettere
                .replace(Regex("^(?i)([A-Z]{3})(\\d)"), { match ->
                    match.groupValues[1].toCharArray().joinToString(" ") + " " + match.groupValues[2]
                })

            val tipoZonaExtra = if (zonaDescrizione.contains("SUP")) {
                "Zona di volo con restrizioni specifiche. "
            } else {
                ""
            }

            val zonaTesto = if (zonaDescrizionePulita.isNotBlank()) {
                "Zona attiva: $zonaDescrizionePulita. $tipoZonaExtra"
            } else {
                ""
            }


            val finalMessage = intro + zonaTesto + aipNote + detail + periodoAttivazione
            //Log.d("DronePilotApp", "🐛 DEBUG: ✉️ Messaggio finale: $finalMessage (${finalMessage::class.simpleName})")
            return finalMessage

        } catch (e: Exception) {
            Log.e("DronePilotApp", "Errore nel parsing delle zone: ${e.message}")
            return "Errore nel leggere i dati della zona di volo."
        }
    }


    private fun interpretNotam(
        message: String?,
        permanent: String?,
        start: String?,
        end: String?
    ): String {
        val simplified = StringBuilder()
        val lowercase = message?.lowercase() ?: ""

        // Attività
        when {
            lowercase.contains("parachute") -> simplified.append("Attività di paracadutisti in corso. ")
            lowercase.contains("military") -> simplified.append("Attività militare in corso. ")
            lowercase.contains("mil unmanned acft") -> simplified.append("Attività militare con droni in corso. ")
            lowercase.contains("civ unmanned acft") -> simplified.append("Attività civile con droni in corso. ")
            lowercase.contains("unmanned acft") -> simplified.append("Attività con droni in corso. ")
            lowercase.contains("uav") -> simplified.append("Operazioni con droni UAV attive. ")
            lowercase.contains("drone") -> simplified.append("Attività con droni rilevata. ")
            lowercase.contains("kite act") -> simplified.append("Attività con kite rilevata. ")
        }

        // Aree coinvolte
        Regex("AREA '?(\\d+)'?").findAll(message ?: "").forEach {
            simplified.append("Zona coinvolta: area ${it.groupValues[1]}. ")
        }

        // Regione
        Regex("REGION / (.+?) /").find(message ?: "")?.groupValues?.get(1)?.let {
            simplified.append("Regione interessata: ${it.trim()}. ")
        }

        // Autorità
        when {
            lowercase.contains("brindisi twr") -> simplified.append("Coordinamento affidato alla torre di Brindisi. ")
            lowercase.contains("napoli twr") -> simplified.append("Coordinamento affidato alla torre di Napoli. ")
            lowercase.contains("roma acc") -> simplified.append("Controllo aereo gestito da Roma ACC. ")
            lowercase.contains("cagliari acc") -> simplified.append("Controllo aereo gestito da Cagliari ACC. ")
        }

        // RMK note
        message?.lines()?.find { it.trim().startsWith("RMK") }?.let {
            simplified.append("Nota: ${it.removePrefix("RMK:").trim()}. ")
        }

        // Validità temporale
        if (permanent == "YES") {
            simplified.append("La restrizione è permanente. ")
        } else if (!end.isNullOrBlank()) {
            try {
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val zonedEnd = ZonedDateTime.parse(end, formatter).withZoneSameInstant(ZoneId.systemDefault())
                val formattedDate = DateTimeFormatter.ofPattern("d MMMM yyyy 'alle' HH:mm", Locale.ITALIAN).format(zonedEnd)
                //simplified.append("Il NOTAM è attivo fino al $formattedDate. ")
            } catch (e: Exception) {
                Log.w("DronePilotApp", "FlightZoneAssistant: Errore nel parsing della data: ${e.message}")
            }
        }

        return if (simplified.isNotBlank()) simplified.toString()
        //else "È presente un NOTAM attivo in questa zona."
        else " "
    }

    suspend fun fetchAipDetails(aipCode: String): String {
        val type = when {
            aipCode.contains(" P") -> "5.1.1"
            aipCode.contains(" R") -> "5.1.2"
            aipCode.contains(" D") -> "5.1.3"
            aipCode.contains("TRA") || aipCode.contains("TSA") -> "5.1.4"
            else -> return ""
        }

        val url = "https://www.kwos.org/appoggio/droni/dronepilotapp/ENR/ENR_$type.html"
        Log.d("DronePilotApp", "FlightZoneAssistant: AIP Scarico il file da $url per codice $aipCode")

        val doc = Jsoup.connect(url).get()
        val rows = doc.select("tr")

        val normalizedAipCode = aipCode.uppercase()
            .replace("LI", "")
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        Log.d("DronePilotApp", "FlightZoneAssistant: AIP Cerco codice normalizzato: $normalizedAipCode")

        for (row in rows) {
            val columns = row.select("td")
            if (columns.size >= 4) {
                val rawCode = columns[0].text().uppercase().trim()
                val codeOnly = Regex("(R\\d{1,4}[A-Z/]*)").find(rawCode)?.value
                //Log.d("DronePilotApp", "FlightZoneAssistant: AIP confronto $codeOnly con $normalizedAipCode")
                if (codeOnly == normalizedAipCode) {
                    Log.d("DronePilotApp", "FlightZoneAssistant: AIP Colonne trovate: ${columns.size}")
                    columns.forEachIndexed { index, col ->
                        Log.d("DronePilotApp", "FlightZoneAssistant: Colonna $index = ${col.text().trim()}")
                    }

                    val descrizione = columns.getOrNull(11)?.text()?.trim().orEmpty()
                    val orari = columns.getOrNull(14)?.text()?.trim().orEmpty()

                    Log.d("DronePilotApp", "FlightZoneAssistant: ✔️ Descrizione AIP: $descrizione")
                    Log.d("DronePilotApp", "FlightZoneAssistant: 🕐 Orari AIP: $orari")

                    val aipOrariInfo = parseOrariAIP(orari)

                    val statoAttivita = if (aipOrariInfo.attivaOra) {
                        "Attualmente attiva."
                    } else {
                        "Non attiva in questo momento."
                    }


                    val messaggio = StringBuilder()
                    messaggio.append("Attenzione: in questa zona è attiva una AIP per $aipCode. ")
                    messaggio.append("$descrizione. ")
                    messaggio.append("${aipOrariInfo.descrizione}. ")
                    messaggio.append("$statoAttivita ")
                    messaggio.append("Consultare le pubblicazioni aeronautiche.")

                    //Log.d("DronePilotApp", "DEBUG messaggioFinale: $messaggio")
                    return messaggio.toString()

                    //return "Attenzione: in questa zona è attiva una AIP per $aipCode. $descrizione. ${aipOrariInfo.descrizione}. $statoAttivita Consultare le pubblicazioni aeronautiche."

                }
            }
        }


        Log.d("DronePilotApp", "FlightZoneAssistant: AIP $aipCode non trovata tra le righe HTML")
        return "AIP $aipCode trovata ma non interpretabile automaticamente."
    }


    fun parseOrariAIP(rawText: String): AipOrariInfo {
        //val punto1 = Regex("1\\)(.*?)(?=\\d\\)|$)").find(rawText)?.groupValues?.get(1)?.trim() ?: rawText
        val punto1 = Regex("1\\)(.*)").find(rawText)?.groupValues?.get(1)?.trim() ?: rawText


        val termsToRemove = listOf(
            "/or",
            "/whichever is earlier",
            "/prior notice by NOTAM",
            "/and",
            "/excluded",
            "/activated by NOTAM",
            "/The zone will be activated on tactical basis.",
            "/Active upon notice by NOTAM",
            "/active upon notice by NOTAM",
            "/From",
            "/to",
            "/Moreover"
        )

        var pulito = punto1
        termsToRemove.forEach { term ->
            pulito = pulito.replace(term, "")
        }

        pulito = pulito
            .replace(Regex("giorni\\s*/\\s*orari", RegexOption.IGNORE_CASE), "giorni o orari")
            .replace("o/or", "o")
            .replace(Regex("(?i)^HR:\\s*"), "")
            .replace(Regex("(?i)MON-FRI"), "dal lunedì al venerdì")
            .replace(Regex("(?i)TUE"), "martedì")
            .replace(Regex("(?i)WED"), "mercoledì")
            .replace(Regex("(?i)THU"), "giovedì")
            .replace(Regex("(?i)FRI"), "venerdì")
            .replace(Regex("(?i)MON"), "lunedì")
            .replace(Regex("(?i)SAT"), "sabato")
            .replace(Regex("(?i)SUN"), "domenica")
            .replace(Regex("(?i)HOL(\\s*esclusi|\\s*excluded)?"), "festivi esclusi")
            .replace(Regex("(?i)H24"), "attiva H24")
            .replace(Regex("(?i)SR\\s*\\-\\s*SS"), "dall'alba al tramonto")
            .replace(Regex("(?i)HJ[\\-\\+]?30"), "da mezz'ora prima dell'alba a mezz'ora dopo il tramonto")
            .replace(Regex("(?i)SR[\\-\\+]?\\d{1,2}"), "all'alba più/minus tot") // opzionale, da raffinare
            .replace(Regex("(?i)(\\d{4})-(SS\\+?\\d{1,2})")) {
                val start = it.groupValues[1].chunked(2).joinToString(":")
                "dalle $start a mezz'ora dopo il tramonto"
            }
            // ⚠️ questa DEVE venire DOPO
            .replace(Regex("(\\d{4})-(\\d{4})(?!\\+|SS|SR)")) {
                val start = it.groupValues[1].chunked(2).joinToString(":")
                val end = it.groupValues[2].chunked(2).joinToString(":")
                "dalle $start alle $end"
            }
            .replace(Regex("/?whichever is earlier\\.?"), "")
            .replace(Regex("prior notice by NOTAM\\.?"), "") // nuova rimozione
            .replace(Regex("(?i)NOTAM/Other days/times upon notice by NOTAM\\.?"), "NOTAM.")
            .replace("/", "") // mettilo proprio alla fine
            .replace("  ", " ")
            .trim()

        // Taglia tutto dopo "2)" incluso
        pulito = pulito.replace(Regex("2\\).*", RegexOption.DOT_MATCHES_ALL), "").trim()


        val now = ZonedDateTime.now()
        val day = now.dayOfWeek
        val time = now.toLocalTime()
        var isActive = true

        if (rawText.contains("H24", ignoreCase = true)) {
            isActive = true
        } else if (rawText.contains("Except SAT", ignoreCase = true) && day == DayOfWeek.SATURDAY) {
            isActive = false
        } else if (rawText.contains("Except SUN", ignoreCase = true) && day == DayOfWeek.SUNDAY) {
            isActive = false
        } else if (rawText.contains("Except HOL", ignoreCase = true)) {
            isActive = true
        } else {
            val pattern = Regex("${day.name.substring(0, 3)}.*?(\\d{4})-(\\d{4})")
            val match = pattern.find(rawText)
            if (match != null) {
                val start = LocalTime.of(match.groupValues[1].substring(0, 2).toInt(), match.groupValues[1].substring(2).toInt())
                val end = LocalTime.of(match.groupValues[2].substring(0, 2).toInt(), match.groupValues[2].substring(2).toInt())
                isActive = time.isAfter(start) && time.isBefore(end)
            }
        }

        return AipOrariInfo(pulito, isActive)
    }

    private suspend fun fetchNotamSchedule(name: String): AipOrariInfo? {
        Log.d("DronePilotApp", "fetchNotamSchedule: Ricevuto nome: $name")

        val match = Regex("([A-Z])\\s?(\\d{4})/(\\d{2})").find(name)
        if (match != null) {
            val (serie, numeroRaw, anno) = match.destructured
            val numero = numeroRaw.trimStart('0') // <-- rimuove zeri iniziali
            Log.d("DronePilotApp", "fetchNotamSchedule: Serie=$serie Numero=$numero Anno=$anno")

            val url = "https://www.deskaeronautico.it/cerca-numero-notam/?serie=$serie&numero=$numero&anno=$anno"
            Log.d("DronePilotApp", "fetchNotamSchedule: URL costruito: $url")

            return try {
                val doc = Jsoup.connect(url).get()
                val allElements = doc.select("body").first()?.allElements ?: return null

                var periodoDescrizione = ""
                var schedulatoDescrizione = ""
                var attivaOra = true

                for (element in allElements) {
                    val testo = element.ownText()

                    // 🔹 Leggi il campo "Periodo (UTC):"
                    if (testo.contains("Periodo (UTC):")) {
                        val periodo = testo.substringAfter("Periodo (UTC):").substringBefore("(").trim()
                        periodoDescrizione = "Il NOTAM riporta il seguente periodo di validità: dal $periodo"

                        val match = Regex("(\\d{2})/(\\d{2})/(\\d{2}) (\\d{2}:\\d{2}) - (\\d{2})/(\\d{2})/(\\d{2}) (\\d{2}:\\d{2})").find(periodo)
                        if (match != null) {
                            val groups = match.groupValues
                            val dayStart = groups[1]
                            val monthStart = groups[2]
                            val yearStart = groups[3]
                            val timeStart = groups[4]
                            val dayEnd = groups[5]
                            val monthEnd = groups[6]
                            val yearEnd = groups[7]
                            val timeEnd = groups[8]

                            val startDateTime = ZonedDateTime.of(
                                "20$yearStart".toInt(), monthStart.toInt(), dayStart.toInt(),
                                timeStart.substring(0, 2).toInt(), timeStart.substring(3, 5).toInt(),
                                0, 0, ZoneId.of("UTC")
                            )
                            val endDateTime = ZonedDateTime.of(
                                "20$yearEnd".toInt(), monthEnd.toInt(), dayEnd.toInt(),
                                timeEnd.substring(0, 2).toInt(), timeEnd.substring(3, 5).toInt(),
                                0, 0, ZoneId.of("UTC")
                            )

                            val nowUtc = ZonedDateTime.now(ZoneId.of("UTC"))
                            attivaOra = nowUtc.isAfter(startDateTime) && nowUtc.isBefore(endDateTime)
                        }
                    }

                    // 🔸 Leggi il campo "Schedulato (UTC):"
                    if (testo.contains("Schedulato (UTC):")) {
                        val cleaned = testo.substringAfter("Schedulato (UTC):").substringBefore("<").trim()
                        schedulatoDescrizione = "Orari previsti: $cleaned"
                    }
                }

                if (periodoDescrizione.isNotBlank() || schedulatoDescrizione.isNotBlank()) {
                    val descrizione = listOf(periodoDescrizione, schedulatoDescrizione)
                        .filter { it.isNotBlank() }
                        .joinToString(". ") + "."

                    Log.d("DronePilotApp", "fetchNotamSchedule: ✔️ Descrizione combinata: $descrizione")
                    return AipOrariInfo(descrizione = descrizione, attivaOra = attivaOra)
                }

                Log.w("DronePilotApp", "fetchNotamSchedule: Nessun campo utile trovato.")
                return null

            } catch (e: Exception) {
                Log.w("DronePilotApp", "fetchNotamSchedule: Errore nel parsing HTML: ${e.message}")
                return null
            }

        } else {
            Log.w("DronePilotApp", "fetchNotamSchedule: Regex NOTAM non ha trovato nulla in $name")
            return null
        }
    }




    data class AipOrariInfo(
        val descrizione: String,
        val attivaOra: Boolean
    )


    private fun speak(text: String) {
        Log.d("DronePilotApp", "SpeechOutput: 🔊 Messaggio da dire: $text")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flightZone")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
