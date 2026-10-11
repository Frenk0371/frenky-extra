# Verifiche del 2026-10-11

## Diagnosi pipeline

Build #4, job 114411166446: JitPack risolve lo snapshot al commit `32895aedb6`, ma non trova il POM `gradle-master-32895aedb6-1.pom`. Sostituire `-SNAPSHOT` con `master-SNAPSHOT` non risolve la disponibilità dell'artefatto.

Run 38120099770: compilazione riuscita usando il checkout del plugin ufficiale come included build. Era una build intermedia con un solo pacchetto; non costituisce la release finale dei tre provider.

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
