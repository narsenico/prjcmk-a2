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
     * @param action   azione scatenata sul comics
     */
    void onAction(T comicsId, int position, int action);
}
