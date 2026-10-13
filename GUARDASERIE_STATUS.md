# GuardaSerie — verifica del 13 ottobre 2026

## Campione Android v1

L’utente ha confermato avvio, video e audio dentro Frenky Stream per l’episodio provato di Breaking Bad. Non è una certificazione di tutti i titoli. La scheda degli 883 ha mostrato NullPointerException a GuardaSerieProvider.kt:59; il messaggio standard Cloudflare non ne dimostra la causa.

Nelle foto ci sono tre sorgenti attive con nome GuardaSerie. Il runtime dell’APK usa APIHolder.apiMap, costruita da api.name a indice: i nomi duplicati si sovrascrivono. È un conflitto verificato nel codice, ma non prova da solo quale provider abbia generato il crash osservato.

La v2 mantiene il nome del pacchetto e del manifest GuardaSerie, ma usa il nome API univoco **GuardaSerie · Frenky Extra**. Le nuove ricerche sono instradate a questa API; le vecchie schede salvate con nome GuardaSerie possono ancora riferirsi ad altre estensioni. Nessuna altra sorgente o APK viene modificato/rimosso.

Metadati opzionali applicati solo quando presenti; dati degli episodi serializzati senza correzione URL. Test aggiunti per metadati assenti e serie in corso, oltre al test HTTP reale degli 883. La risoluzione del crash sul tablet richiede una nuova prova con la voce distinta.

## guardaseriex.wiki

Il dominio esatto fornito dall’utente è https://guardaseriex.wiki/. Il browser Work ha verificato homepage, ricerca https://guardaseriex.wiki/index.php?story=Breaking+Bad&do=search&subaction=search e scheda https://guardaseriex.wiki/serietv-popolari/192-visualizza-breaking-bad-2.html.

Il sito usa una struttura DLE distinta dall’originale guarda-serie.ovh. Non è stata trovata prova verificata di continuità/gestione comune; non viene registrato come cambio di dominio approvato.

La scheda dichiara un iframe VixSrc per S1E1 con IMDb tt0903747, ma il selettore crea sempre MAX_SEASONS=12 e MAX_EPISODES=24 per ogni titolo. Questi limiti non costituiscono un elenco reale di stagioni/episodi e non vengono trasformati in puntate CloudStream. Il catalogo completo richiesto per un’integrazione affidabile non è verificato. Nessuna seconda stagione viene inventata dagli indicatori riassuntivi della pagina degli 883.
