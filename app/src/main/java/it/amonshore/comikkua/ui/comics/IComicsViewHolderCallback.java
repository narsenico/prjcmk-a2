package it.amonshore.comikkua.ui.comics;

/**
 * Callback per i ViewHoler riferiti ai comics.
 */
interface IComicsViewHolderCallback {

    /**
     * Notifica il click sull'intero comics.
     *
     * @param comicsId id del comics
     * @param position posizione del Viewholer
     */
    void onComicsClick(long comicsId, int position);

    /**
     * Notifica la selezione del pulsante menu.
     *
     * @param comicsId id del comics
     * @param position posizione del Viewholer
     */
    void onComicsMenuSelected(long comicsId, int position);
}
