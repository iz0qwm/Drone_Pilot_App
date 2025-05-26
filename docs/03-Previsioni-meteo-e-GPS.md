## 🌤️ Situazione meteorologica

Cliccando sul tasto **Meteo e GPS** si aprirà una schermata come quella che vedi qui sotto.

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/meteo_attuale_new.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/meteo_attuale_new_wa.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">🌐 Web App</div>
  </div>
</div>

_Non è possibile selezionare un altro luogo per le previsioni. Saranno disponibili solo quelle del luogo in cui ti trovi._

Le informazioni meteorologiche vengono prelevate dal sito [MeteoBlue](https://www.meteoblue.com/) e da [OpenWeather](https://openweathermap.org/)<br>
Le informazioni di Alba e tramonto sono prelevate dal sito [Sunrise Sunset](https://sunrise-sunset.org/)

Analizzando le condizioni meteorologiche del momento è possibile fornire degli **alert** per vento forte, pioggia imminente, tempo instabile, ecc.

## 📊 Layer della situazione meteo

Ad oggi la **Web App** ha anche i layer per il **vento**, la **nuvolosità** e il **radar meteorologico**

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/layer_radar_wa.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">Radar meteo - 🌐 Web App</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/layer_vento_wa.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">Vento - 🌐 Web App</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/layer_nuvolosita_wa.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">Nuvolosità - 🌐 Web App</div>
  </div>
</div>

## 📅 Previsioni meteorologiche
Cliccando sul tasto **Previsioni** verranno mostrate le previsioni meteorologiche del luogo in cui ti trovi, ora per ora.
Molto importante è il _profilo verticale del vento_ e la sua previsione nelle prossime ore.

Alla fine della pagina vi sono dei meteogrammi per la località.

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/previsioni_meteo_01_new.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/previsioni_meteo_02_new.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/previsioni_meteo_01_new_wa.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">🌐 Web App</div>
  </div>
</div>

## 📅 Windy - Servizio esterno

**Solo su Android** : potrai cliccare sul tasto Windy per aprire il servizio esterno [Windy](https://www.windy.com/)

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/meteo_windy.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">🌐 Web App</div>
  </div>
</div>

## 📡 Ricezione GPS
I nostri UAS stabilizzati, utilizzano un ricevitore GPS per rimanere stabili in aria e per percorrere la giusta traiettoria.<br>
Per questo motivo, è importante sapere se la ricezione GPS nel luogo in cui vi trovate, è ottimale per farlo volare.<br><br>

L'applicazione utilizza due modalità per capire se vi è ricezione GPS ottimale per effettuare il volo:<br>
* ricezione GPS dello smartphone su cui viene eseguita
* controllo del numero di elettroni nella ionosfera

<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/gps_new.jpeg" alt="Dashboard principale" width="360">
    <div style="margin-top: 8px;">📱 Android</div>
  </div>
  <div style="text-align: center;">
    <img src="https://www.kwos.org/appoggio/droni/dronepilotapp/wiki/previsioni_meteo_01_new_wa.jpeg" alt="Web App" width="360">
    <div style="margin-top: 8px;">🌐 Web App</div>
  </div>
</div>

Ovviamente lo smartphone sarà sempre con voi durante il volo, quindi se lui non riuscirà a fare il fix, si presuppone che anche il vostro drone possa avere dei problemi<br><br>

Il **Total Electron Count** invece, è un metodo, molto più avanzato e preciso rispetto al generico **Indice Kp**, noto a tutti i dronisti, per determinare gli errori di precisione nel rilevamento della posizione a causa di forti tempeste solari e geomagnetiche.<br>
Per maggiori informazioni potete consultare il sito di [IONORING](http://ionos.ingv.it/ionoring/ionoring.htm) o [INGV](https://www.ingv.it/ricerca/seminari/archivio-seminari/3352-osservazione-e-studio-delle-scintillazioni-ionosferiche)

