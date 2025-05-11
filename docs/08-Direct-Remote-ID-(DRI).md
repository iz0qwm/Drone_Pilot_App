# 🛸 Ricevitore droni (DRI)

## 🕵️ Cosa è il DRI?

**Remote ID**, o **identificazione remota diretta** (DRI), è un _sistema che permette ai droni di trasmettere informazioni di identificazione e posizione_, come il numero dell'operatore e la posizione del drone, a persone in una zona limitata tramite segnali radio. Questa "targa digitale" per i droni è diventata obbligatoria per garantire la sicurezza e la responsabilità nell'uso dei droni, soprattutto in aree regolamentate o dove c'è un rischio per la sicurezza.

Per farti capire meglio, ti do qualche informazione in più:

📌 **Obbligatorio o quasi**:

> Il Remote ID è obbligatorio per quasi tutti i droni che volano nella categoria Open o sotto dichiarazione della Categoria Specifica, secondo le nuove normative europee. In particolare in Italia i droni con marcatura **C1,C2,C3,C5 e C6** sono **già dotati del DRI**. Per i **C0**, i **C4 e i droni senza marcatura di classe e gli autocostruiti** il **DRI non è obbligatorio**.

📡 **Informazioni trasmesse**:

> Il drone trasmette il numero dell'operatore, la posizione geografica (latitudine e longitudine), la velocità e la rotta, e altre informazioni importanti. 

🧰 **Come si implementa**: 

> Il Remote ID può essere implementato tramite un modulo integrato nel drone o come dispositivo esterno (Add-On). 

💻 **Tecnologie utilizzate**:

> I sistemi Remote ID possono utilizzare varie tecnologie di comunicazione, come Bluetooth Legacy, Bluetooth 5 Long Range, Wi-Fi NaN (Wi-Fi Aware) o Wi-Fi Beacon. 

> Solitamente **i droni della DJI trasmettono in Wi-Fi Beacon e/o Wi-Fi NaN**, mentre **i beacon esterni** che si applicano sui droni come i [Dronetag](https://dronetag.com/) trasmettono **in Bluetooh**.

## ✅ Attivare la ricezione 

Per attivare la ricezione del DRI devi andare su **Impostazioni** e **spostare lo switch**.

In questo modo i ricevitori che sono abilitati nel tuo telefono, inizieranno a catturare il DRI dei droni intorno a te.

<p align="center">
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/impostazioni_new.jpeg" alt="Prima pagina"  >
</p>

## 🚁 Cosa si vede sulla mappa

Sulla mappa si vedranno tutti i droni ricevuti e si traccerà la traiettoria finché la ricezione del tuo telefono ascolterà il DRI.

Se ci sarà **un volo dello stesso drone dopo un'ora dal precedente**, **verrà considerato un secondo volo** e sull'ultimo punto del primo volo, apparirà l'icona del drone atterrato.

<p align="center">
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/dri_01.jpeg" alt="Prima pagina" >
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/dri_02.jpeg" alt="Prima pagina"  >
</p>

## ✨ Se i ricevitori non funzionano?

Può succedere che per le limitazioni del sistema operativo, il tuo smartphone sebbene sia compatibile con il Wi-Fi Beacon, in realtà non riceva nulla.

Devi quindi eseguire questi passi:

- 🛠️ **Attivare le opzioni sviluppatore**: vai su Impostazioni ➡️ Informazioni sul telefono ➡️ cerca la voce Numero Build ➡️ tappa 7 volte sopra
<p align="center">
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/sviluppatori_01.jpeg" alt="Prima pagina" >
</p>
- ⚙️ **NO Ricerca limitata del Wi-Fi** : disabilitare la ricerca limitata in Wi-Fi.
<p align="center">
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/sviluppatori_02.jpeg" alt="Prima pagina" >
</p>

## 🧩 Migliorare la ricezione

Se vuoi migliorare la ricezione del tuo telefono, puoi comprare un **[ds100 DroneScout Bridge retail with external RP-SMA antenna](https://dronescout.co/bridge/)** che ti permetterà di fare da relay ai messaggi ricevuti dai droni.

Ti basterà collegarlo ad un powerbank e posizionarlo in un luogo alto.

<p align="center">
  <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ds100retail_ant_connectors.jpeg" alt="ds100 01" >
<img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ds100retail_package.jpeg" alt="ds100 02" >
</p>

[![Guarda il video su YouTube](https://img.youtube.com/vi/dSLGsIccyHY/0.jpg)](https://www.youtube.com/watch?v=dSLGsIccyHY)
