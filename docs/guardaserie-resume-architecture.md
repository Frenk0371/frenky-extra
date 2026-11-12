# GuardaSerie: analisi di `Continua a guardare`

## Stato attuale
- La v6 utilizza un dialog con WebView per i video privi di link restituiti a CloudStream.
- Il video si riproduce ma il player web non invoca il percorso di salvataggio usato da CloudStream.
- **Questa nota non implementa né rilascia una funzione.**

## Codice CloudStream verificato (upstream `recloudstream/cloudstream`, `master`)
- `app/src/main/java/com/lagradost/cloudstream3/ui/player/GeneratorPlayer.kt`: durante la riproduzione chiama `DataStoreHelper.setViewPosAndResume(...)`.
- `app/src/main/java/com/lagradost/cloudstream3/utils/DataStoreHelper.kt`: `setLastWatched(parentId, episodeId, episode, season)` salva la voce; `setViewPos(id, pos, dur)` salva la posizione solo se la durata è almeno 30 secondi; `setViewPosAndResume` gestisce anche l'episodio successivo e lo stato visto.
- `app/src/main/java/com/lagradost/cloudstream3/ui/home/HomeViewModel.kt`: `getResumeWatching()` legge gli ID salvati, poi richiede anche un `DownloadHeaderCached` (nome, URL, API, tipo, poster). Se manca il dato in cache o nel backup, la voce non appare.

## Implicazione tecnica
Una WebView standalone non conosce automaticamente gli ID interni di CloudStream, la durata e la posizione del video e non produce necessariamente il `DownloadHeaderCached`. Scrivere direttamente le preferenze da un'estensione non è un contratto pubblico stabile: può generare voci non apribili o perdere dati.

## Approcci da valutare
1. **Preferito:** riproduzione autorizzata attraverso il player nativo quando il provider restituisce un `ExtractorLink` effettivamente riproducibile. Cronologia e posizione vengono gestite dal flusso esistente dell'app. Preservare sempre il fallback WebView.
2. **Se si dispone del sorgente dell'app modificata:** aggiungere nell'app una piccola interfaccia esplicita per registrare una visione web, utilizzando gli ID creati dalla schermata della serie e il relativo header cache. La sola visita alla pagina non deve essere registrata come riproduzione.
3. **Senza API supportate e senza sorgente app:** evitare scritture dirette a DataStore e dichiarare la limitazione.

## Verifiche prima del rilascio
- Episodio riprodotto realmente vs semplice apertura della scheda.
- Episodio e stagione corretti, voce cliccabile anche dopo il riavvio.
- Posizione temporale solo se realmente osservabile e affidabile.
- Nessuna regressione per audio, schermo intero e tasto Indietro.
- Nessuna modifica alla v6 pubblicata prima dei test.

Issue: #12.
