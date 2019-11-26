package it.amonshore.comikkua.ui.comics;

abstract class ComicsViewHolderCallback {

    abstract void onComicsClick(long comicsId, int position);
    abstract void onNewRelease(long comicsId, int position);
}
