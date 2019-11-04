package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.Bundle;

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

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;

import static it.amonshore.comikkua.data.comics.Comics.NEW_COMICS_ID;


public class ComicsFragment extends Fragment {

    private OnNavigationFragmentListener mListener;
    private PagedListComicsAdapter mAdapter;
    private ComicsViewModel mComicsViewModel;

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
        final RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_comics_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.deleteComics:
                        final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                        if (tracker.hasSelection()) {
                            mComicsViewModel.delete(tracker.getSelection());
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

        mAdapter = new PagedListComicsAdapter.Builder(recyclerView)
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
                .withOnItemActivatedListener((item, e) -> {
                    final View sharedView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    // lo stesso nome della transizione verrà assegnato alla view di arrivo
                    //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
                    final String txName = "comics_tx_" + item.getSelectionKey();
                    sharedView.setTransitionName(txName);

                    final NavDirections directions = ComicsFragmentDirections
                            .actionDestComicsToComicsDetailFragment()
                            .setComicsId(item.getSelectionKey());

                    Navigation.findNavController(getView()).navigate(directions);

                    return false;
                })
                .build();

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(this)
                .get(ComicsViewModel.class);
        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        mComicsViewModel.comicsWithReleasesList.observe(getViewLifecycleOwner(), data -> {
            LogHelper.d("comics viewmodel data changed");
            mAdapter.submitList(data);
        });

        // ripristino la selezione salvata in onSaveInstanceState
        mAdapter.getSelectionTracker().onRestoreInstanceState(savedInstanceState);

        // la prima volta carico tutti i dati
        mComicsViewModel.setFilter(null);

        return view;
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
            mAdapter.getSelectionTracker().onSaveInstanceState(outState);
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
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
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
        switch (item.getItemId()) {
            case R.id.createNewComics:
                final NavDirections directions = ComicsFragmentDirections
                        .actionDestComicsFragmentToComicsEditFragment()
                        .setComicsId(NEW_COMICS_ID);

                Navigation.findNavController(getView()).navigate(directions);

                return true;
            case R.id.deleteComics:
                // TODO: chiedere conferma prima di elminare tutto
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
