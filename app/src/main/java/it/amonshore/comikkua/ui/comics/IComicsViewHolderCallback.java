package it.amonshore.comikkua.ui.comics;

/**
 * Callback per i ViewHoler riferiti ai comics.
 */
interface IComicsViewHolderCallback<T> {

    /**
     * Notifica il click sull'intero comics.
     *
     * @param comicsId id del comics
     * @param position posizione del Viewholer
     */
    void onComicsClick(T comicsId, int position);

    /**
     * Notifica la selezione del pulsante menu.
     *
     * @param comicsId id del comics
     * @param position posizione del Viewholer
     */
    void onComicsMenuSelected(T comicsId, int position);
}
