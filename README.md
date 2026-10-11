# Frenky Extra

Repository separata con **FilmPerTutti**, **CasaCinema** e **GuardoSerie**, ognuno installabile come pacchetto `.cs3` indipendente. Nessuna modifica a `Frenk0371/frenky-cloudstream`.

In CloudStream aggiungere:

https://raw.githubusercontent.com/Frenk0371/frenky-extra/builds/repo.json

## Stato della versione 5

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
