package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;


public class ReleasesFragment extends Fragment {

    private final static String BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.releases.recycler.layout";

    private OnNavigationFragmentListener mListener;
    private ReleaseAdapter mAdapter;
    private ReleaseViewModel mReleaseViewModel;
    private RecyclerView mRecyclerView;

    public ReleasesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_releases, container, false);

        final String actionModeName = getClass().getSimpleName() + "_actionMode";
        final Context context = requireContext();
        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_releases_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                switch (item.getItemId()) {
                    case R.id.purchaseReleases:
                        if (tracker.hasSelection()) {
                            // TODO: considerare le multi release
                            mReleaseViewModel.togglePurchased(tracker.getSelection());
                        }
                        // mantengo la selezione
                        return true;
                    case R.id.orderReleases:
                        if (tracker.hasSelection()) {
                            // TODO: considerare le multi release
                            mReleaseViewModel.toggleOrdered(tracker.getSelection());
                        }
                        // mantengo la selezione
                        return true;
                    case R.id.deleteReleases:
                        if (tracker.hasSelection()) {
                            mReleaseViewModel.delete(tracker.getSelection());
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

        mAdapter = new ReleaseAdapter.Builder(mRecyclerView)
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
                .withReleaseCallback(R.menu.menu_releases_popup, new ReleaseAdapter.ReleaseCallback() {
                    @Override
                    public void onReleaseClick(@NonNull ComicsRelease release) {
                        // TODO: considerare le multi release (aprire il dettaglio del comics)
                        openEdit(view, release);
                    }

                    @Override
                    public void onReleaseSwipe(@NonNull ComicsRelease release) {
                        // TODO: considerare le multi release (aprire il dettaglio del comics)
                        mReleaseViewModel.updatePurchased(!release.release.purchased, release.release.id);
                    }

                    @Override
                    public void onReleaseMenuItemSelected(@NonNull MenuItem item, @NonNull ComicsRelease release) {
                        switch (item.getItemId()) {
                            case R.id.gotoComics:
                                openComicsDetail(view, release);
                                break;
                            case R.id.orderRelease:
                                // TODO: considerare le multi release
                                mReleaseViewModel.toggleOrdered(release.release.id);
                                break;
                            default:
                                // TODO: considerare le multi release
                                LogHelper.d("Menu %s selected on release %s #%d",
                                        item.getTitle(), release.comics.name, release.release.number);
                                break;
                        }
                    }
                })
                .build();

        // recupero il ViewModel per l'accesso ai dati
        // lo lego all'activity perché il fragment viene ricrecato ogni volta (!)
        mReleaseViewModel = new ViewModelProvider(requireActivity())
                .get(ReleaseViewModel.class);
        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        mReleaseViewModel.getReleaseViewModelItems().observe(getViewLifecycleOwner(), items -> {
            LogHelper.d("release viewmodel data changed size:" + items.size());
            mAdapter.submitList(items);
        });

        // ripristino la selezione salvata in onSaveInstanceState
        mAdapter.getSelectionTracker().onRestoreInstanceState(savedInstanceState);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ripristino lo stato del layout (la posizione dello scroll)
        // se non trovo savedInstanceState uso lo stato salvato nel view model
        if (savedInstanceState != null) {
            Objects.requireNonNull(mRecyclerView.getLayoutManager())
                    .onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_RELEASES_RECYCLER_LAYOUT));
        } else if (mReleaseViewModel != null) {
            Objects.requireNonNull(mRecyclerView.getLayoutManager())
                    .onRestoreInstanceState(mReleaseViewModel.states.getParcelable(BUNDLE_RELEASES_RECYCLER_LAYOUT));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecyclerView != null) {
            // visto che Navigation ricrea il fragment ogni volta (!)
            // salvo lo stato della lista nel view model in modo da poterlo recuperare se necessario
            //  in onViewCreated
            mReleaseViewModel.states.putParcelable(BUNDLE_RELEASES_RECYCLER_LAYOUT,
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
            outState.putParcelable(BUNDLE_RELEASES_RECYCLER_LAYOUT,
                    Objects.requireNonNull(mRecyclerView.getLayoutManager())
                            .onSaveInstanceState());
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_releases_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.deleteReleases:
                // TODO: implementare cancellazione di tutte le release, chiedere conferma all'utente
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openComicsDetail(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = ReleasesFragmentDirections
                .actionDestReleasesToComicsDetailFragment()
                .setComicsId(release.comics.id);

        Navigation.findNavController(view).navigate(directions);
    }

    private void openEdit(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = ReleasesFragmentDirections
                .actionReleasesFragmentToReleaseEditFragment()
                .setComicsId(release.comics.id)
                .setReleaseId(release.release.id);

        Navigation.findNavController(view).navigate(directions);
    }
}