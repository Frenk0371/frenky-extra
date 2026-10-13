# Frenky Extra

Repository CloudStream con **FilmPerTutti v8**, **CasaCinema v8** e **GuardaSerie v1 beta**, installabili come pacchetti `.cs3` indipendenti. L’APK Frenky Stream non viene modificato.

URL repository:

https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/repo.json

## GuardaSerie

Il dominio iniziale è `https://guarda-serie.ovh`, dichiarato dal provider originale GuardaSerie v7 di DieGon e verificato nel browser Work il 13 ottobre 2026. `guardaseries.wiki` è escluso: non raggiungibile durante le verifiche e senza prova di continuità con l’originale.

Ricerca con query UTF-8, poster, schede, stagioni ed episodi dichiarati dalle pagine. Il player VixSrc richiede il contratto pubblico presente nello script della scheda; il solo identificativo TMDB non basta. Prima dell’apertura viene ricontrollato l’episodio sul sito.

Gli estrattori disponibili nell’app sono provati per primi. Se non producono link nativi e non è richiesto il casting, viene proposto il player web interno, visibile e azionato dall’utente. Nessuna intercettazione, injection, esportazione sessioni, modifica TLS o aggiramento di DRM/CAPTCHA. Avanzamento e controlli del lettore nativo non sono integrati nel fallback web.

### Verifiche e limiti

Compilazione e test automatici; test HTTP reale su homepage, ricerca con poster e scheda Breaking Bad, cinque stagioni e 62 episodi. La selezione S5E16 nel browser cambia l’iframe verso il lettore dichiarato per quell’episodio.

La riproduzione nativa/WebView Android e l’uso su TV rimangono da verificare. Nel browser Work il lettore video mostra `Please Disable Sandbox`; nessuna protezione è stata modificata. Il provider resta beta.

GuardoSerie (con la O) viene ritirato su autorizzazione esplicita dell’utente. FilmPerTutti e CasaCinema restano esattamente i pacchetti pubblicati v8, con metadati identici. Le verifiche delle versioni precedenti sono conservate in `TEST_RESULTS.md` come storico.

## Domini centralizzati

Il provider legge `config/domains.json` con cache di un’ora. Il monitor controlla i domini ogni sei ore, senza approvare automaticamente nuovi host. Un cambio richiede revisione dell’annuncio/redirect sul dominio originale e registrazione di `identity_review` con `approved`, `verified_domain`, `official_evidence_url` e `reviewed_at`. Un nome o logo simile non basta. Se la verifica fallisce viene mantenuto il dominio originale.

## Build e pubblicazione

Plugin Gradle ufficiale da commit `32895aedb6366f5075cb99bbd2e6ce0a7cac325d`; Java 17, Gradle 9.5.0, AGP 9.1.1 e Kotlin 2.4.0. La pipeline compila, testa e verifica ZIP/DEX, manifest, versioni e dimensioni, confrontando byte e metadati dei due provider film con la release precedente.

Le PR producono artefatti di revisione e non pubblicano. La pubblicazione dal ramo `main` aggiorna `builds` solo dopo tutti i controlli. La distribuzione contiene esattamente i tre pacchetti correnti.
