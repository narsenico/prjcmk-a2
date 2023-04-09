package it.amonshore.comikkua.ui.releases;

/**
 * Callback per i ViewHoler riferiti alle release.
 */
interface IReleaseViewHolderCallback {

    /**
     * Notifica il click sull'intera release.
     *
     * @param comicsId id del comics
     * @param id       id della release
     * @param position posizione del Viewholer
     */
    void onReleaseClick(long comicsId, long id, int position);

    /**
     * Notifica la selezione del pulsante menu.
     *
     * @param comicsId id del comics
     * @param id       id della release
     * @param position posizione del Viewholer
     */
    void onReleaseMenuSelected(long comicsId, long id, int position);
}
