# prjcmk-a2

## TODO
- [x] Sostituire Firestore con cmkweb per l'aggiornamento delle release
- [x] Sostituire Firestore con cmkweb per l'aggiornamento delle testate
- [x] Non si riesce a togliere la data di una release una volta impostata
- [x] Non vengono cancellati definitivamente gli ementi con removed=1 alla navigazione (vedi nota)
- [x] Auto complete degli autori non funziona
- [ ] Rivedere IReleaseViewModelItem (vedi nota)
- [x] Comics con sourceId non modificabili (almeno i campi che arrivano da CmkWeb)
- [x] Evienziare in edit comics se sono sourced o meno
- [ ] Creare backup in cartella esterna all'app (download?)
- [x] Non funziona l'annullo di ImportFromOldDatabaseWorker
- [x] Preferibilmente scaricare i comics all'apertura di ComicsSelectorFragment (altrimenti perde l'utilità di mostrare l'ultima release) 
- [ ] Considerare ristampa

- [x] L'import da vecchio db deve indicare quanti comics sono importati come sourced (così da caricare su cmkweb tutti i comics mancanti in modo da avere su app alla fine tutti sourced)

- [x] POSSIBILE BACO CON LE DATE: importando il DB da neon non corrispondono le release 

- [ ] CmkWeb: sourceId meglio che derivi dal comics stesso (hash del nome?) e non creato random (uuid) perché ricreando il DB verrebbero persi i collegamti con i dati salvati nell'app

## Note

### IReleaseViewModelItem
Questa interfaccia riguarda l'UI, non deve stare nel package data
e quindi anche ReleaseHeader
Anche ComicsRelease non deve implementare questa interfaccia, proprio perché vengono mischiati UI e data

### Bug rimozione definitiva elementi
Come riprodurlo:
- selezionare uno o più release da ComicsDetail ed elminarli
- appare la snackbar per l'undo
- prima del timeout tornare indietro all'elenco comics 

Così facendo la snackbar viene dismessa e chiamata la callback (da MainActivity) 
ma il viewModel demandato alla rimozione non è più attivo 
e quindi rimangono elementi sporchi (cioè con removed=1) nel DB

Soluzione? Forzare cancellazione prima di uscire dal fragment?

### Sto perdendo il controllo del DB
Non sono più sicuro di poter migrare dal db della variante "neon" a "tryme" (questa di sviluppo).
Soluzione veloce è creare un NUOVO database, come nome diverso (quindi non più "comikku_database") e copiare tramite routine
i dati dal vecchio DB.