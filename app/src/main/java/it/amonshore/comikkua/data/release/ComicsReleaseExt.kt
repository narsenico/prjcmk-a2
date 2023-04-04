package it.amonshore.comikkua.data.release

enum class ComicsReleaseJoinType(val value: Int) {
    None(0),
    MissingReleases(MissingRelease.TYPE)
}

// TODO: da testare
fun List<ComicsRelease>.toReleaseViewModelItems(joinType: ComicsReleaseJoinType): List<IReleaseViewModelItem> {
    val items = mutableListOf<IReleaseViewModelItem>()
    var lastHeader: ReleaseHeader? = null
    var lastMulti: MultiRelease? = null
    var lastType = 0
    var totalCount = 0
    var purchasedCount = 0
    var headerCount = 0L

    for ((ii, cr) in withIndex()) {
        if (lastHeader == null || lastType != cr.type) {
            // la tipologia del dato è cambiata, creo un nuovo header
            val header = ReleaseHeader(++headerCount, cr.type)
            items.add(header)

            if (lastHeader != null) {
                lastHeader.totalCount = totalCount
                lastHeader.purchasedCount = purchasedCount
            }

            lastHeader = header
            lastType = cr.type
            totalCount = 0
            purchasedCount = 0
        }

        if (joinType != ComicsReleaseJoinType.None && ii > 0 && canBeGrouped(
                joinType,
                get(ii - 1),
                cr
            )
        ) {
            // la release precedente e questa possono essere raggruppate
            if (lastMulti == null) {
                // se non ho ancora creato un multi, lo creo adesso sostituendo in items l'ultimo valore
                val prev = get(ii - 1)
                lastMulti = MultiRelease()
                lastMulti.comics = prev.comics
                // la release principale è quella precedente (cioè la prima in ordine)
                lastMulti.release = prev.release
                // poi vengono le altre
                lastMulti.otherReleases = mutableListOf() // TODO: buuu! non può essere mutabile
                lastMulti.otherReleases.add(cr.release)
                items[items.size - 1] = lastMulti
            } else {
                // il multi era già creato, aggiungo la release e basta
                lastMulti.otherReleases.add(cr.release)
            }
        } else {
            // quella corrente non può essere raggruppata con la precedente
            // la inserisco così come è e annullo il multi in modo che venga ricreato il prossimo giro se necessario
            lastMulti = null
            items.add(cr)
        }

        ++totalCount
        if (cr.release.purchased) {
            ++purchasedCount
        }
    }

    if (lastHeader != null) {
        lastHeader.totalCount = totalCount
        lastHeader.purchasedCount = purchasedCount
    }

    return items
}

private fun canBeGrouped(
    joinType: ComicsReleaseJoinType,
    cr1: ComicsRelease,
    cr2: ComicsRelease
): Boolean =
    cr1.type == joinType.value && cr2.type == joinType.value && cr1.comics.id == cr2.comics.id