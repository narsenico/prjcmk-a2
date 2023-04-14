# prjcmk-a2

## TODO
- [ ] Sostituire Firestore con cmkweb per l'aggiornamento delle release
- [ ] Sostituire Firestore con cmkweb per l'aggiornamento delle testate
- [ ] Non si riesce a togliere la data di una release una volta impostata
- [x] Non vengono cancellati definitivamente gli ementi con removed=1 alla navigazione (vedi nota)

## Note

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