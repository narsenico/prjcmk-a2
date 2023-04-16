# prjcmk-a2

## TODO
- [x] Sostituire Firestore con cmkweb per l'aggiornamento delle release
- [x] Sostituire Firestore con cmkweb per l'aggiornamento delle testate
- [x] Non si riesce a togliere la data di una release una volta impostata
- [x] Non vengono cancellati definitivamente gli ementi con removed=1 alla navigazione (vedi nota)
- [ ] Auto complete degli autori non funziona
- [ ] Rivedere IReleaseViewModelItem (vedi nota)

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