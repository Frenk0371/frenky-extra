# Frenky Extra

Repository CloudStream con **FilmPerTutti v8**, **CasaCinema v8** e **GuardaSerie v2 beta**, installabili come pacchetti `.cs3` indipendenti. L’APK Frenky Stream non viene modificato.

URL repository:

https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/repo.json

## GuardaSerie

Il dominio iniziale è `https://guarda-serie.ovh`, dichiarato dal provider originale GuardaSerie v7 di DieGon e verificato nel browser Work il 13 ottobre 2026. `guardaseries.wiki` è escluso: non raggiungibile durante le verifiche e senza prova di continuità con l’originale.

Ricerca con query UTF-8, poster, schede, stagioni ed episodi dichiarati dalle pagine. Il player VixSrc richiede il contratto pubblico presente nello script della scheda; il solo identificativo TMDB non basta. Prima dell’apertura viene ricontrollato l’episodio sul sito.

Gli estrattori disponibili nell’app sono provati per primi. Se non producono link nativi e non è richiesto il casting, viene proposto il player web interno, visibile e azionato dall’utente. Nessuna intercettazione, injection, esportazione sessioni, modifica TLS o aggiramento di DRM/CAPTCHA. Avanzamento e controlli del lettore nativo non sono integrati nel fallback web.

### Verifiche e limiti

Compilazione e test automatici; test HTTP reale su homepage, ricerca con poster e scheda Breaking Bad, cinque stagioni e 62 episodi. La selezione S5E16 nel browser cambia l’iframe verso il lettore dichiarato per quell’episodio.

L’utente ha confermato video e audio dentro Frenky Stream sul campione Breaking Bad v1. Altri titoli e l’uso su TV rimangono da verificare. La v2 usa la voce distinta **GuardaSerie · Frenky Extra** per evitare la collisione con le altre estensioni omonime; il pacchetto resta GuardaSerie. Il crash degli 883 richiede una nuova prova su questa voce. Nel browser Work il lettore video mostra `Please Disable Sandbox`; nessuna protezione è stata modificata. Il provider resta beta.

GuardoSerie (con la O) viene ritirato su autorizzazione esplicita dell’utente. FilmPerTutti e CasaCinema restano esattamente i pacchetti pubblicati v8, con metadati identici. Le verifiche delle versioni precedenti sono conservate in `TEST_RESULTS.md` come storico.

## Domini centralizzati

Il provider legge `config/domains.json` con cache di un’ora. Il monitor controlla i domini ogni sei ore e aggiorna quelli verificati tramite redirect HTTPS e controlli sulla pagina di destinazione, registrando `identity_review`. Un nome o logo simile, senza redirect dal dominio configurato, non basta. Se la verifica fallisce il registro non viene modificato; vedi i limiti descritti sotto.

## Build e pubblicazione

Plugin Gradle ufficiale da commit `32895aedb6366f5075cb99bbd2e6ce0a7cac325d`; Java 17, Gradle 9.5.0, AGP 9.1.1 e Kotlin 2.4.0. La pipeline compila, testa e verifica ZIP/DEX, manifest, versioni e dimensioni, confrontando byte e metadati dei due provider film con la release precedente.

Le PR producono artefatti di revisione e non pubblicano. La pubblicazione dal ramo `main` aggiorna `builds` solo dopo tutti i controlli. La distribuzione contiene esattamente i tre pacchetti correnti.

Diagnosi aggiornata e valutazione di guardaseriex.wiki: [GUARDASERIE_STATUS.md](GUARDASERIE_STATUS.md).

### Aggiornamento automatico dei domini

Il workflow `check-domains.yml` controlla il registro ogni sei ore (oppure con
avvio manuale) e pubblica su `main` le migrazioni verificate. Il controllo può
essere eseguito senza modifiche con `python scripts/check_domains.py`, oppure
con aggiornamento locale mediante `python scripts/check_domains.py --update`.

Un nuovo dominio viene accettato solo quando il dominio attualmente registrato
vi reindirizza tramite HTTPS, la pagina finale è la homepage, il titolo contiene
il nome della sorgente e la pagina presenta una struttura di navigazione. La
verifica viene registrata nel formato `identity_review` già letto dal plugin;
non serve ricompilare per una modifica al solo registro. La cache del plugin
può ritardare la lettura del nuovo indirizzo fino a un'ora.

Questi controlli riducono gli errori, ma non provano la proprietà del sito. Se il
vecchio dominio non risponde, non effettua un redirect HTTP, restituisce una
protezione antibot o il sito cambia struttura, è necessario un intervento.
Il sistema non indovina domini e non garantisce il funzionamento dei lettori.
La pubblicazione richiede che le regole del ramo consentano il push di
`github-actions[bot]`; in caso contrario il workflow fallisce esplicitamente.
