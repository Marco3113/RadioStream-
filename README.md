# 📻 RadioStream — Guida al Build

## Requisiti
- **Android Studio** Hedgehog (2023.1) o più recente → [download](https://developer.android.com/studio)
- **JDK 11** (incluso in Android Studio)
- Connessione Internet (per scaricare le dipendenze Gradle)

---

## Come buildare l'APK in 4 passi

1. **Apri il progetto**
   - Avvia Android Studio
   - `File → Open` → seleziona la cartella `RadioStream`
   - Attendi che Gradle finisca la sincronizzazione

2. **Connetti il tuo telefono** (o usa l'emulatore)
   - Abilita "Opzioni sviluppatore" → "Debug USB" sul telefono
   - Il dispositivo apparirà nella toolbar in alto

3. **Build APK debug** (per installazione diretta)
   - Menu: `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - L'APK sarà in: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build APK release** (per distribuzione)
   - `Build → Generate Signed Bundle / APK`
   - Segui la procedura per creare/usare un keystore

---

## Funzionalità incluse

| Funzione | Dettaglio |
|---|---|
| 🌐 WebView | Carica `https://radioffline.lovable.app` |
| 🔄 Auto-refresh | Ricarica automatica ogni 30 secondi |
| 🎙️ Microfono | Permesso richiesto all'avvio |
| 🎵 Media Controls | Play/Pause dalla lockscreen e notifica |
| 📱 Spotify-like | Integrazione con tasti multimediali |
| 🔔 Notifica | Notifica persistente con controlli |
| 📶 Offline | Messaggio di errore se offline |
| 🔃 Swipe-to-refresh | Tira giù per aggiornare manualmente |

---

## Struttura del progetto

```
RadioStream/
├── app/src/main/
│   ├── java/com/radiostream/app/
│   │   ├── MainActivity.java   ← WebView + logica principale
│   │   └── RadioService.java   ← Foreground service + MediaSession
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── drawable/ic_logo.png
│   │   ├── mipmap-*/ic_launcher.png
│   │   ├── values/strings.xml + styles.xml
│   │   └── xml/network_security_config.xml
│   └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

---

## Modificare l'URL

Nel file `MainActivity.java` alla riga:
```java
private static final String STREAM_URL = "https://radioffline.lovable.app";
```
Sostituisci con il tuo URL.

## Modificare il refresh rate

```java
private static final long AUTO_REFRESH_INTERVAL = 30_000L; // millisecondi
```
Cambia `30_000L` (30 sec) con il valore desiderato.
