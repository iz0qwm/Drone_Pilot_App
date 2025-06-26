# 🕶️ Assistenza alla scelta del filtro ND

## ❓ ND: cos'è e a cosa serve?

I **filtri ND (Neutral Density)** sono dei filtri ottici che riducono **la quantità di luce** che entra nella fotocamera, **senza alterare i colori**.

🎥 Sono indispensabili per chi riprende con il drone, perché:
- permettono di mantenere una **velocità dell’otturatore più bassa** anche in piena luce, evitando l’effetto "scattoso" nei video;
- aiutano a rispettare la **regola del doppio dell’FPS** (es. 1/60 di shutter per 30fps) per ottenere un movimento fluido e cinematografico.

Più è alto il numero del filtro (ND4, ND8, ND16…), **più luce viene bloccata**. Scegliere il filtro giusto è quindi fondamentale per avere riprese di qualità.

---

## 📸 Perché l’immagine sembra (o è) sovraesposta?
Durante l'utilizzo dell'assistente alla scelta del filtro ND, l'immagine inquadrata può apparire molto luminosa o addirittura sovraesposta. 
Questo comportamento non è un errore, ma una scelta intenzionale e utile per individuare il filtro corretto.

🎯 Infatti, l'app utilizza parametri di esposizione fissi e controllati, simulando la situazione reale in cui si desidera mantenere un certo valore di shutter speed per ottenere un video fluido (tipicamente con la regola dei 180°, ovvero tempo di esposizione = 1 / (2 × FPS)).

🕶️ Il filtro ND ha proprio il compito di ridurre la quantità di luce che entra nell'obiettivo, senza modificare gli altri parametri di scatto (ISO e tempo di esposizione). Se non si usa alcun filtro ND, l’immagine risulterà inevitabilmente sovraesposta quando la scena è molto luminosa — come nelle riprese in pieno sole.

💡 Ecco perché l’assistente mostra volutamente l'immagine così com’è, senza “correggere” automaticamente l’esposizione: in questo modo puoi vedere con i tuoi occhi che è necessario un filtro per riportare la scena alla corretta luminosità.

🔧 Nota pratica: Quando posizioni il filtro ND suggerito davanti alla fotocamera dello smartphone, l’immagine dovrebbe tornare visivamente bilanciata. Se resta ancora troppo chiara, potrebbe essere necessario un filtro di intensità maggiore. Se invece diventa troppo scura, valuta un ND più leggero.

---

## 📸 Come funziona l'assistente

All'interno della schermata **Scelta filtro ND** della Drone Pilot App:
- la fotocamera dello smartphone viene attivata;
- viene analizzata la **luminanza** media della scena (componente Y);
- l’app suggerisce il filtro ND più adatto: ND4, ND8, ND16, ND32 o ND64;
- un **commento testuale e icona** spiegano il suggerimento (es. "🌞 ND32 necessario").

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ND_filter_01.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 Finestra principale</div>
  </div>
</div>

---

## ⚠️ Attenzione: l'effetto *ghost* della luminanza

Se ti sposti rapidamente da ambienti scuri a molto luminosi (es. dall’ombra alla luce diretta), la camera del telefono può **non aggiornare correttamente l’esposizione**, e la luminanza percepita continua a **calare anche in piena luce**.

Questo effetto, chiamato *ghost*, dipende da **parametri interni della camera** che possono "impallarsi" nel tempo.

### 🔄 Come risolvere

Se noti che la luminanza rilevata diventa anomala:
1. Chiudi la Drone Pilot App.
2. Apri l’**app nativa della fotocamera** del tuo telefono per alcuni secondi.
3. Questo resetta i parametri interni.
4. Riapri Drone Pilot App e torna alla funzione filtro ND.

✅ La lettura tornerà normale e precisa.

---

## ✅ Procedura operativa

**Evita movimenti troppo rapidi tra aree molto buie e molto luminose**.
**Durante la misura tieni bene ferma la mano** per evitare oscillazioni.

- Punta la camera del telefono tra i 45° e i 90° rispetto alla posizione del Sole.
- Scegli gli FPS con cui effettuerai la ripresa: _24, 25, 30, 50, 60_
- Scegli le condizioni del cielo:_☀️ Sole pieno_,_⛅ Qualche nuvola_ o _☁️ Cielo coperto_
- Leggi il valore del filtro che ti consiglia
- Esci dalla funzione **Scelta del filtro ND**
- Prendi il filtro suggerito
- Rientra nella finestra **Scelta del filtro ND** 
- Riposiziona il telefono tra i 45° e i 90° rispetto alla posizione del Sole.
- Posiziona il filtro davanti alla camera e dovresti leggere: _💡 Nessun filtro necessario o quello attuale va bene_

**Ricordati**, oltre ad installare il filtro sul drone, **di impostare la ripresa in Manuale**, g**li FPS come quelli selezionati durante la misura** e lo **shutter speed come quello suggerito in giallo**.

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ND_filter_02.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">⛅ Condizioni del cielo</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ND_filter_03.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 FPS di registrazione</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/ND_filter_04.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">🕶️ Con filtro davanti alla camera</div>
  </div>
</div>

---

🛠️ _Questa funzione è in evoluzione: nelle prossime versioni considererà altri parametri tecnici per offrire un’assistenza ND ancora più professionale._

---

## 📋 Tabella riassuntiva dei filtri ND

| ☀️ Condizione di luce           | 🔢 Filtro ND consigliato | 🎯 Situazione tipica                       |
|-------------------------------|--------------------------|-------------------------------------------|
| Molto buio / Interni          | ND2 o nessun filtro      | Riprese al tramonto, interni o in ombra   |
| Cielo coperto / alba / tramonto | ND4                     | Mattina presto o luce diffusa             |
| Parzialmente soleggiato       | ND8                      | Luce naturale non diretta                 |
| Sole pieno, luce intensa      | ND16                     | Mezzogiorno con cielo limpido             |
| Sole forte + superfici riflettenti | ND32                | Mare, neve, paesaggi molto aperti         |
| Estremamente luminoso (es. deserto, spiaggia) | ND64        | Massima luce estiva, a mezzogiorno        |

📌 _Nota: La scelta dipende anche da ISO e shutter speed. Questa è solo una guida indicativa._

---

## 🎬 Esempi pratici

- **Scenario 1**: voli al tramonto con luce calda e diffusa  
  → 👉 ND4 o ND8

- **Scenario 2**: riprese in una giornata nuvolosa ma luminosa  
  → 👉 ND8

- **Scenario 3**: video in spiaggia alle 13:00 con sole a picco  
  → 👉 ND32 o ND64

- **Scenario 4**: decolli da una zona d’ombra per poi salire verso il sole  
  → 👉 ND16 in media, ma attenzione all’effetto *ghost*

- **Scenario 5**: voli invernali su paesaggi innevati con cielo terso  
  → 👉 ND32 o ND64 per evitare sovraesposizioni

---

🎯 _Ricorda: l’obiettivo è mantenere lo shutter vicino al doppio del frame rate (es. 1/60 per 30fps), per un effetto cinema fluido._

