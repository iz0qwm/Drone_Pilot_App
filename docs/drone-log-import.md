# 📁 Importazione Log di Volo DJI

La DronePilotApp ti permette di importare i file di log generati dall'app DJI FLY e visualizzarne la traiettoria di volo.

---

## 🧭 A cosa serve?

Questa funzione ti consente di:
- 📍 Visualizzare la rotta di un volo effettuato
- ✈️ Verificare il comportamento del drone (modello, tempo, altitudine)
- 🛰️ Salvare la traiettoria su mappa e integrarla con altri dati

---

## 🛠️ Come si usa

### 1️⃣ Estrai il file dal telefono

1. 🔌 Collega lo smartphone al PC con un cavo USB
2. 💻 Sul PC, naviga nella cartella:

   ```
   Ad esempio:
   Questo PC > [nome del tuo telefono] > Memoria condivisa interna > Android > data > dji.go.v5 > files > FlightRecord
   ```

3. 📄 Copia il file `.txt` relativo al volo che vuoi analizzare (es. DJIFlightRecord_2025-05-15_[12-05-38].txt)
4. 📂 Incollalo in una cartella pubblica del telefono, come:

   ```
   Memoria condivisa interna > Download
   ```

---

### 2️⃣ Importa il file in DronePilotApp

1. 📱 Apri la DronePilotApp
2. Vai su **📁 Drone LogFiles**
3. Premi il pulsante **📂 Importa log**
4. Seleziona il file `.txt` copiato in **Download**
5. ⏳ Attendi l'invio al server e la risposta con il risultato

---

## ✅ Cosa succede dopo

- Il file viene inviato al server che esegue la decodifica
- Se l’elaborazione ha successo, verrà mostrato un messaggio di conferma
- I dati (modello, rotta, durata) potranno essere visualizzati sulla mappa

---

## ⚠️ Note importanti

- Il file deve essere **quello originale** generato da DJI FLY
- I log devono essere copiati **manualmente** fuori dalla cartella `Android/data` (protetta da Android)
- Serve una connessione Internet attiva per inviare il file al server

---

## ✉️ In caso di problemi

Se vedi messaggi come:
- `❌ Il file non è accessibile`
- `❌ Errore rete: timeout`
- `❌ Errore upload: 400`

Verifica di aver:
- Copiato il file nella cartella **Download**
- Selezionato correttamente il file `.txt`
- Una buona connessione attiva

---

🛫 **Buon volo... e buona analisi!**
