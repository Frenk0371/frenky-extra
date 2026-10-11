# Verifiche del 2026-10-11

## Diagnosi pipeline

Build #4, job 114411166446: JitPack risolve lo snapshot al commit `32895aedb6`, ma non trova il POM `gradle-master-32895aedb6-1.pom`. Sostituire `-SNAPSHOT` con `master-SNAPSHOT` non risolve la disponibilità dell'artefatto.

Run 38120099770: compilazione riuscita usando il checkout del plugin ufficiale come included build. Era una build intermedia con un solo pacchetto; non costituisce la release finale dei tre provider.

## Release pubblicata e verificata

Run finale [38120575527](https://github.com/Frenk0371/frenky-extra/actions/runs/38120575527): successo, commit sorgente `36a7988189e43f2dd028cec8ccaa3b67099f3302`.

- Test JUnit delle locandine eseguiti nei tre moduli: successo.
- Pubblicati solo `FilmPerTutti.cs3`, `CasaCinema.cs3`, `GuardoSerie.cs3`, tutti versione 2.
- `repo.json` e `plugins.json` scaricati dagli URL pubblici del ramo `builds`.
- Tutti i tre `.cs3` scaricati dagli URL in `plugins.json`: ZIP integri, DEX e manifest presenti, versione e classe plugin corretti.
- Dimensioni effettive: FilmPerTutti 34849 byte, CasaCinema 34848 byte, GuardoSerie 34846 byte.
- SHA-256 calcolati sui file scaricati e confrontati con `fileHash`: tutti corrispondenti.
- I test JUnit coprono il parser delle locandine; non sono test di riproduzione del runtime Android.

## Test effettivi sulle fonti pubbliche

Richieste HTTP reali a homepage, ricerche e schede; nessun bypass. I test non sono stati eseguiti dentro un dispositivo Android con CloudStream. Non confondere i risultati HTTP con test riusciti del provider nell'app.

| Fonte | Test osservato | Risultato | Riproduzione |
| --- | --- | --- | --- |
| FilmPerTutti | Homepage, ricerca DLE `night`, scheda Fright Night 2 | HTTP 200; titoli, locandine CSS, trama e dichiarazione pubblica del lettore presenti | Non confermata: VixSrc restituisce una pagina JavaScript, nessun flusso video verificato |
| CasaCinema | Homepage, ricerca DLE `night`, scheda Fright Night 2 | HTTP 200; titoli, locandine CSS, trama e dichiarazione pubblica del lettore presenti | Non confermata: stesso lettore dinamico |
| GuardoSerie | Homepage, ricerca WordPress `night`, scheda The Night Agent, episodio S1E1 | HTTP 200; titolo, poster e 3 stagioni con 10 collegamenti episodio ciascuna presenti nella scheda campione | Non confermata: iframe pubblico `loadm.cam` raggiungibile, restituisce applicazione JavaScript |

## Limiti aperti

- Nessun test completo ricerca → scheda → riproduzione eseguito nel runtime Android di CloudStream.
- Compatibilità dei lettori dipendente dagli estrattori inclusi nell'app, non certificata su questi campioni.
- FilmPerTutti/CasaCinema non espongono nei campioni esaminati un elenco affidabile degli episodi. Non vengono fabricate 12 stagioni × 24 episodi dal selettore generico presente nei loro script.
- Varianti di ricerca coprono accenti, articoli, anno, punteggiatura e parole intere; nessuna traduzione automatica dei titoli.
- Cambi dominio richiedono verifica e aggiornamento manuale del registro centrale.
- I dati del catalogo possono contenere titoli/episodi annunciati o link indisponibili. Una scheda presente non prova la disponibilità né i diritti di un contenuto.

Questa versione rimane sperimentale finché i test di riproduzione non saranno riusciti.


## Correzione riproduzione — versione 3

Segnalazione reale: “L’allenatore nel pallone” presente nei risultati di FilmPerTutti/CasaCinema, ma CloudStream mostra “link non trovato”.

Causa: la versione 2 delegava VixSrc al registro degli estrattori dell’app. L’app ufficiale esaminata non contiene tale estrattore; inoltre occorre seguire il flusso pubblico API → embed → server HLS attivo.

Correzione: seguire le dichiarazioni pubbliche del lettore ufficiale, conservare i parametri del server attivo e il referer del lettore, e passare la playlist al lettore HLS standard di CloudStream. I parametri di accesso sono solo quelli emessi dal server; non vengono inventati né alterati. Nessun bypass di CAPTCHA, login o DRM. Le risposte non disponibili restano errori.

Test live su IMDb tt0086874, riferito alle schede verificate dei due provider:

- API pubblica: HTTP 200.
- Pagina embed effettiva: HTTP 200.
- Playlist HLS master: HTTP 200; tre varianti dichiarate 480p, 720p, 1080p.
- Playlist della prima variante e primo segmento: scaricati con successo.
- HLS AES-128 identity: chiave standard del manifest pubblicamente accessibile, 16 byte; il lettore HLS gestisce il normale protocollo. Nessun sistema di licenze DRM coinvolto.
- FFprobe sul primo segmento, riprodotto tramite il protocollo HLS nativo: H.264 video e AAC audio, durata 6 secondi.
- I test non equivalgono ancora a una prova sul tablet dell’utente.

GuardoSerie/Loadm rimane non confermato. Il percorso VixSrc è implementato per i film; non risolve gli elenchi incompleti degli episodi DLE.

Verifica finale della versione 3: run 38121657491 riuscito; test JUnit in tutti e tre i moduli passati; pacchetti pubblici scaricati e verificati (versione 3, DEX, manifest, dimensioni, SHA-256). FFmpeg ha decodificato un secondo di video e audio del campione HLS senza errori. Link build: https://github.com/Frenk0371/frenky-extra/actions/runs/38121657491



## Version 4 — device failure investigation

The user confirmed both providers are version 3 on CloudStream 4.8.0. The same film plays on the tablet browser with Player2 (VixSrc), while the extension still reports no links. This disproves a claim of end-to-end success on Android.

Version 4 uses CloudStream's standard SSL-verifying HTTP client for the VixSrc requests and reports the failed stage and HTTP status/exception type. It preserves normal browser-declared URLs, referers and HLS validation. It does not bypass access checks. Cancellation is propagated. Diagnostic messages do not include signed URLs, tokens or response bodies. The root cause on the tablet and playback in CloudStream remain unconfirmed pending device retry.

Validation: GitHub Actions run 38123532061 succeeded, including all three testDebugUnitTest tasks and distribution validation. All three public .cs3 files, plugins.json and repo.json were downloaded again; version 4 manifests, DEX payloads, ZIP integrity, sizes and SHA256 hashes match. No claim of Android playback success is made.


## Version 5 — CasaCinema player recognition

User screenshot from version 4: FilmPerTutti reports API VixSrc HTTP 403. This is a refused API request on the device; browser playback does not establish extension access. No access-control bypass is attempted.

CasaCinema reports no compatible player. Reproduced using the downloaded actual film page: Jsoup sees zero #vidxgo-player nodes despite the literal iframe declaration in the response; an unclosed preceding script swallows the iframe. The public VixSrc declaration and IMDb identifier remain in script data. The parser now accepts this explicitly declared iframe from the original HTML too, without executing scripts. Regression tests cover valid HTML, the malformed CasaCinema structure, series rejection and unrelated page rejection. This fixes discovery; device API access/playback remains to be tested and may return the same 403.

Version 5 validation: GitHub Actions run 38123921127 succeeded (all three testDebugUnitTest tasks, including the player-discovery regression). The public repo.json, plugins.json and three .cs3 files were fetched again; version 5 ZIP/DEX/manifests/sizes and SHA256 hashes match. CasaCinema recognition is corrected; Android playback is still unconfirmed. FilmPerTutti has a confirmed device-side VixSrc API HTTP 403.


## Version 6 — preserve normal application HTTP configuration

The user's tablet browser successfully returned the API JSON with a player src on the same endpoint that reports HTTP 403 in CloudStream. This narrows the failure to a client/session difference; it does not identify the server's reason.

The public VixSrc movie component calls the API through stock Axios before creating the embed iframe. No ad-close prerequisite appears in that component. A separate cloud-browser trial reached the embed but displayed “Please Disable Sandbox”; playback was not verified and no sandbox setting was changed. An external API request using the extension's explicit parameters returned HTTP 200 with src.

Source inspection of NiceHttp 0.4.18 (commit f1dee9dfc5b86a24c65bf8c6e8403d336341dd3e) confirms timeout values are seconds and app.get inherits app.defaultHeaders. Removed the plugin's incomplete User-Agent override instead of introducing a browser fingerprint or an access-control workaround. HLS receives the application's configured User-Agent as well. Corrected the timeout from 20000 seconds to 20 seconds. Non-200 responses now report only a bounded response category, never server text, cookies or signed URLs.

These changes correct request configuration and improve diagnostics; they do not prove why the device received 403 or establish Android playback success. CI and published artifacts must be verified separately.

Version 6 publication verified: GitHub Actions run 38130180362 succeeded on commit 61f82f23594c04f3dcbbfca92123fdc221f8e3fe, executing testDebugUnitTest and distribution validation. The public repo.json, plugins.json and all three .cs3 packages were downloaded; version 6 manifests, ZIP integrity, DEX signatures, file sizes and SHA-256 hashes match. Android playback still requires a device retry.

## Version 7 — diagnostics after Android logcat

User logcat 2026-10-11 12:26: CasaCinema downloads the movie page then reports VixSrc API HTTP 403 (HTML) at 12:25:50; FilmPerTutti does the same at 12:25:55. Neither reaches the embed/HLS stage. Other provider DNS failures are unrelated.

Added bounded diagnostics for the public API path, HTTP status, allowlisted MIME category, final destination after redirects and presence of Cloudflare headers. No query, fragments, credentials, signed paths, response bodies or cookie values are recorded. Header presence is evidence about the response, not proof of the cause of HTTP 403. Privacy regression tests cover redaction and arbitrary header values. No workaround for server access refusal was added. Android playback remains blocked/unconfirmed. CI publication is verified separately.

Version 7 publication verified: run 38132287114 succeeded on commit c36b40923f3544082d3cd81e9c374df50ec7d278, including all three unit-test tasks and distribution validation. Public plugins.json, repo.json and all three .cs3 packages were downloaded and verified: version 7, ZIP integrity, DEX signatures, manifest versions, sizes and SHA-256 hashes. The initial run compiled/tested successfully but publication correctly failed because the validator still expected version 6; the validator was updated before the successful run. No Android playback success is claimed.
