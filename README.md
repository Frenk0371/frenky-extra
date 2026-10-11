# Frenky Extra

Repository separata con **FilmPerTutti**, **CasaCinema** e **GuardoSerie**, ognuno installabile come pacchetto `.cs3` indipendente. Nessuna modifica a `Frenk0371/frenky-cloudstream`.

In CloudStream aggiungere:

https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/repo.json

## Stato dei provider

Beta: compilazione e disponibilità dei pacchetti non certificano la riproduzione. Consultare `TEST_RESULTS.md` prima di considerare una fonte funzionante.

- Ricerca DLE per FilmPerTutti/CasaCinema, WordPress per GuardoSerie.
- Normalizzazione accenti/articoli/anno e corrispondenze per parole intere. Non traduce automaticamente titoli tra lingue.
- Locandine tramite immagini lazy, metadati e CSS delle schede.
- Episodi GuardoSerie estratti da collegamenti realmente presenti. Nessun episodio creato sulla base di selettori numerici generici.
- Lettori pubblici delegati agli estrattori presenti nella versione installata di CloudStream. Supporto VixSrc per il flusso pubblico API → embed → HLS. Nessun aggiramento DRM, CAPTCHA, autenticazione o browser automatizzato.
- FilmPerTutti/CasaCinema: VixSrc verificato esternamente e nel browser del tablet sul campione L’allenatore nel pallone. La versione 3 fallisce nel CloudStream del tablet; la versione 4 usa il client HTTP dell’app e mostra errori per fase. Riproduzione CloudStream ancora da confermare; cataloghi serie/episodi incompleti.
- Homepage vuota: usare la ricerca.

## Compilazione

Lo snapshot JitPack `com.github.recloudstream:gradle:master-SNAPSHOT` non esponeva il POM richiesto. La pipeline ora compila il plugin **ufficiale** da un commit immutabile, tramite un included build Gradle, con coordinata interna `com.lagradost.cloudstream3:gradle:local`.

Il workflow esegue il checkout di `recloudstream/gradle` a `32895aedb6366f5075cb99bbd2e6ce0a7cac325d`. Toolchain: Java 17, Gradle 9.5.0, AGP 9.1.1, Kotlin 2.4.0. I pacchetti sono verificati (DEX, manifest, versione, elenco e dimensioni) prima di aggiornare `builds`.

## Domini centralizzati

`config/domains.json` viene letto a runtime, con cache di un'ora. Non viene cercato né approvato automaticamente un dominio sconosciuto.

Per un cambio dominio, un manutentore deve verificare l'annuncio/redirect sul dominio originale e l'identità della nuova pagina, poi aggiornare `current_url` e aggiungere:

```json
"identity_review": {
  "approved": true,
  "verified_domain": "nuovo-dominio.example",
  "official_evidence_url": "https://dominio-originale.example/percorso-annuncio",
  "reviewed_at": "2026-10-11"
}
```

Il client richiede HTTPS, dominio verificato corrispondente, evidenza sul dominio originale e marchio della fonte nella nuova homepage. Se la verifica fallisce conserva il dominio originale. Questi controlli integrano la revisione umana; un nome simile da solo non prova l'identità. Aggiornare il registro non richiede ricompilazione.

Versione 5: corretto il riconoscimento del player CasaCinema con HTML malformato. Sul tablet FilmPerTutti riceve HTTP 403 dall’API VixSrc: la riproduzione CloudStream resta bloccata/non confermata. Nessun tentativo di aggirare il rifiuto di accesso.

Versione 6: rimossi gli override dell’User-Agent incompleto nel resolver VixSrc; le richieste usano la configurazione HTTP standard dell’app e il player HLS conserva lo stesso User-Agent configurato. Corretto il timeout NiceHttp da 20000 a 20 secondi. Errori HTTP classificati senza mostrare corpi, cookie o URL firmati. Nessun bypass e nessuna riproduzione Android confermata.

Versione 7: diagnostica delle richieste VixSrc con percorso API pubblico, stato HTTP, tipo MIME e destinazione dopo redirect. Query, frammenti, credenziali, percorsi firmati e contenuti del server sono omessi. La presenza di header Cloudflare non prova la causa del rifiuto. Il log Android delle 12:25 conferma HTTP 403 su entrambi i provider; questa versione raccoglie informazioni, non certifica una correzione della riproduzione.


Versione 8: se l’API VixSrc restituisce HTTP 403, FilmPerTutti/CasaCinema propongono “Apri player interno”. Dopo la scelta dell’utente, la pagina pubblica del film si apre in una WebView visibile dentro CloudStream. Play ed eventuali verifiche restano manuali. Non vengono estratti flussi, trasferiti cookie, automatizzati controlli o modificato l’User-Agent. Questo percorso usa il player web del sito, non il lettore nativo: avanzamento, casting e sottotitoli CloudStream non sono integrati. Le navigazioni principali restano sul dominio VixSrc; HTTPS obbligatorio, accesso ai file e bridge JavaScript disabilitati. La riproduzione sul tablet resta da verificare.


GuardoSerie versione 9: il dominio era già `https://guardoseries.sbs/`; verificati nuovamente ricerca, scheda ed elenco di 30 episodi del campione The Night Agent. L’episodio S1E1 dichiara un iframe pubblico Loadm. Se gli estrattori non producono un link, viene proposta l’apertura della pagina dell’episodio nel player web interno dedicato a GuardoSerie. Il percorso è manuale e resta da provare sul dispositivo; casting e avanzamento CloudStream non sono disponibili. FilmPerTutti e CasaCinema restano alla versione 8: i loro pacchetti e metadati pubblicati sono conservati esattamente dalla pipeline, senza modifiche al loro codice.
