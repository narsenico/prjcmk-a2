package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.ImageHelper;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;

import static it.amonshore.comikkua.data.comics.Comics.NEW_COMICS_ID;


public class ComicsFragment extends Fragment {

    private final static String BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics.recycler.layout";
    private final static String BUNDLE_COMICS_LAST_QUERY = "bundle.comics.last.query";

    private OnNavigationFragmentListener mListener;
    private PagedListComicsAdapter mAdapter;
    private ComicsViewModel mComicsViewModel;
    private RecyclerView mRecyclerView;
    private Snackbar mUndoSnackBar;

    public ComicsFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_comics, container, false);

        final String actionModeName = getClass().getSimpleName() + "_actionMode";
        final Context context = requireContext();
        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_comics_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.deleteComics) {
                    final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                    if (tracker.hasSelection()) {
                        // prima elimino eventuali release ancora in fase di undo
                        mComicsViewModel.deleteRemoved();
                        mComicsViewModel.remove(tracker.getSelection(), (ids, count) -> showUndo(ids, count));
                    }
                    tracker.clearSelection();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // action mode distrutta (anche con BACK, che viene gestito internamente all'ActionMode e non può essere evitato)
                mAdapter.getSelectionTracker().clearSelection();
                super.onDestroyActionMode(mode);
            }
        };

        // PROBLEMI:
        // selection changed viene scatenato due volte all'inizio: questo perché il tracker permette la selezione di più item trascinando la selezione

        mAdapter = new PagedListComicsAdapter.Builder(mRecyclerView)
                .withOnItemSelectedListener((keys, size) -> {
                    if (mListener != null) {
                        if (size == 0) {
                            mListener.onFragmentRequestActionMode(null, actionModeName, null);
                        } else {
                            mListener.onFragmentRequestActionMode(actionModeController, actionModeName,
                                    getString(R.string.title_selected, size));
                        }
                    }
                })
                .withComcisCallback(new PagedListComicsAdapter.ComicsCallback() {
                    @Override
                    public void onComicsClick(@NonNull ComicsWithReleases comics) {
                        final NavDirections directions = ComicsFragmentDirections
                                .actionDestComicsToComicsDetailFragment()
                                .setComicsId(comics.comics.id);

                        Navigation.findNavController(requireView()).navigate(directions);
                    }

                    @Override
                    public void onNewRelease(@NonNull ComicsWithReleases comics) {
                        final NavDirections directions = ComicsFragmentDirections
                                .actionDestComicFragmentToReleaseEditFragment()
                                .setComicsId(comics.comics.id)
                                .setSubtitle(R.string.title_release_create);

                        Navigation.findNavController(requireView()).navigate(directions);
                    }
                })
                .withGlide(Glide.with(this))
                .build();

        // recupero il ViewModel per l'accesso ai dati
        // lo lego all'activity perché il fragment viene ricrecato ogni volta (!)
        mComicsViewModel = new ViewModelProvider(requireActivity())
                .get(ComicsViewModel.class);
        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        mComicsViewModel.comicsWithReleasesList.observe(getViewLifecycleOwner(), data -> {
            LogHelper.d("comics viewmodel data changed size=" + data.size());
            mAdapter.submitList(data);
        });

        // ripristino la selezione salvata in onSaveInstanceState
        mAdapter.getSelectionTracker().onRestoreInstanceState(savedInstanceState);

        // la prima volta carico tutti i dati
//        mComicsViewModel.setFilter(null);
        mComicsViewModel.useLastFilter();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ripristino lo stato del layout (la posizione dello scroll)
        // se non trovo savedInstanceState uso lo stato salvato nel view model
        if (savedInstanceState != null) {
            Objects.requireNonNull(mRecyclerView.getLayoutManager())
                    .onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
        } else if (mComicsViewModel != null) {
            Objects.requireNonNull(mRecyclerView.getLayoutManager())
                    .onRestoreInstanceState(mComicsViewModel.states.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView != null) {
            // visto che Navigation ricrea il fragment ogni volta (!)
            // salvo lo stato della lista nel view model in modo da poterlo recuperare se necessario
            //  in onViewCreated
            mComicsViewModel.states.putParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT,
                    Objects.requireNonNull(mRecyclerView.getLayoutManager()).onSaveInstanceState());
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationFragmentListener) {
            mListener = (OnNavigationFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            // ripristino le selezioni
            mAdapter.getSelectionTracker().onSaveInstanceState(outState);
            // salvo lo stato del layout (la posizione dello scroll)
            outState.putParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT,
                    Objects.requireNonNull(mRecyclerView.getLayoutManager())
                            .onSaveInstanceState());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);

        final MenuItem searchItem = menu.findItem(R.id.searchComics);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        // onQueryTextSubmit non viene scatenato su query vuota, quindi non posso caricare tutti i dati

        // TODO: al cambio di configurazione (es orientamento) la query viene persa
        //  è il viewModel che deve tenere memorizzata l'ultima query,
        //  qua al massimo devo apri la SearchView e inizializzarla con l'ultima query da viewModel se non vuota

        if (!TextUtils.isEmpty(mComicsViewModel.getLastFilter())) {
            // lo faccio prima di aver impostato i listener così non scateno più nulla
            searchItem.expandActionView();
            searchView.setQuery(mComicsViewModel.getLastFilter(), false);
            searchView.clearFocus();

            // TODO: non funziona sulla navigazione (es apro il dettaglio di un comics filtrato),
            //  perché viene chiusa la searchView e scatenato onQueryTextChange con testo vuoto
            //  che mi serve così perché quando volutamente la chiudo voglio che il filtro venga pulito
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogHelper.d("onQueryTextSubmit");
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                LogHelper.d("filterName change " + newText);
                mComicsViewModel.setFilter(newText); // TODO: ok ma aggiungere debounce
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.createNewComics) {
            final NavDirections directions = ComicsFragmentDirections
                    .actionDestComicsFragmentToComicsEditFragment()
                    .setComicsId(NEW_COMICS_ID)
                    .setSubtitle(R.string.title_comics_create);

            Navigation.findNavController(requireView()).navigate(directions);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUndo(Long[] ids, int count) {
        if (mUndoSnackBar != null && mUndoSnackBar.isShown()) {
            LogHelper.d("UNDO: dismiss snack");
            mUndoSnackBar.dismiss();
        }

        // mostro messaggio per undo
        // creo la snackbar a livello di activity così non ho grossi problemi quando cambio fragment
        mUndoSnackBar = Snackbar.make(requireActivity().findViewById(android.R.id.content),
                getResources().getQuantityString(R.plurals.comics_deleted, count, count), 7_000)
                // con il pulsante azione ripristino gli elementi rimossi
                .setAction(android.R.string.cancel, v -> mComicsViewModel.undoRemoved())
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        // procedo alla cancellazione effettiva solo dopo il timeout
                        if (event == BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT) {
                            mComicsViewModel.deleteRemoved();
                            // elimino anche le immagini
                            // mi fido del fatto che ids contenga esattamente i comics rimossi con l'istruzione sopra
                            ImageHelper.deleteImageFiles(requireContext(), ids);
                        }
                        LogHelper.d("UNDO: dismissed event=%s", event);
                    }
                });
        LogHelper.d("UNDO: show snack");
        mUndoSnackBar.show();
    }
}
