## 🕶️ Visualizzazione AR dei POI

La funzione **AR Spot** ti consente di visualizzare in **realtà aumentata** i punti di interesse turistico 🎯 attorno alla tua posizione, direttamente sulla fotocamera del dispositivo.

### 🚀 Come si accede

Per aprire la modalità AR:
1. Tocca l'icona 👁️‍🗨️ (dell'occhio) **AR Spot** nella schermata principale (accanto a *Spot di volo*).
2. L'app aprirà una nuova finestra in **modalità landscape** con la fotocamera attiva.

> ℹ️ Assicurati di avere attivato i **servizi di localizzazione** 📍 e di essere in una zona con buona visibilità GPS.

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/AR_spot_01.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
</div>

---

### 🔍 Cosa vedo nell’HUD AR

Nella modalità AR vedrai:
- La tua posizione attuale in alto sullo schermo 🧭
- I nomi dei **POI** (punti di interesse) 📍 visualizzati come etichette virtuali nello spazio
- La distanza dal punto di interesse in metri 📏 accanto a ogni nome
- La distanza dalla zona con restrizione al volo, in metri 📏 
- La distanza da un eventuale pilota di drone che ha lo stato In Volo sulla Drone Pilot App 

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/AR_spot_02.jpeg" alt="Dashboard principale" width="660">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
</div>

---

### ⚙️ Calibrazione

Per funzionare correttamente la **direzione dei POI va calibrata con un Punto di interesse noto**:

Posizionarsi davanti ad un Punto di interesse noto che appare sullo schermo AR (Es. il paese Nerola)
Spostare **l'offset di azimuth** tramite una barra di scorrimento affinchè l'etichetta con il nome del POI (Es. Nerola) sia in corrispondenza del paese noto.

> ✅ L’offset viene salvato automaticamente e mantenuto anche dopo la chiusura dell’app.
> ✅ Solitamente non è necessario fare ulteriori affinamenti all'offset

**NOTA**: _Se il nome del paese non dovesse apparire, potrebbe essere più lontano di quanto si pensi. Spostare **il raggio di ricerca** con la barra dedicata 📐 che regola il raggio di azione dell'AR (fino a 10 km).

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/AR_spot_03.jpeg" alt="Dashboard principale" width="660">
    <div style="margin-top: 8px;">Fase di calibrazione offset di azimuth</div>
  </div>
</div>

---

### ⚙️ Personalizzazione

Puoi regolare:
- **Il raggio di ricerca** con una barra dedicata 📐 che regola il numero di POI mostrati (fino a 10 km).

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/AR_spot_04_3500.jpeg" alt="Raggio di azione 3500" width="660">
    <div style="margin-top: 8px;">Raggio di azione 3500</div>
  </div>
</div>

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/AR_spot_04_5000.jpeg" alt="Raggio di azione 5000" width="660">
    <div style="margin-top: 8px;">Raggio di azione 5000</div>
  </div>
</div>

---

### 🧪 Funzionalità in sviluppo

Stiamo lavorando per arricchire la modalità AR con:
- Droni rilevati in tempo reale ✈️ tramite OpenDroneID
- Visualizzazione delle altezze rilevate 📶
- Possibilità di toccare un’etichetta per ottenere informazioni aggiuntive ℹ️

---


