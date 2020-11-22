package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.LiveDataEx;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.MultiRelease;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import it.amonshore.comikkua.ui.ShareHelper;

/**
 * Vengono mostrate le release con un certo tag.
 * Utilizzata per mostrate all'utente le ultime release aggiunte a fronte di un aggiornamento automatico.
 */
public class NewReleasesFragment extends Fragment {

    private final static String BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.new_releases.recycler.layout";

    private OnNavigationFragmentListener mListener;
    private ReleaseAdapter mAdapter;
    private ReleaseViewModel mReleaseViewModel;
    private RecyclerView mRecyclerView;

    public NewReleasesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_new_releases, container, false);

        final String actionModeName = getClass().getSimpleName() + "_actionMode";
        final Context context = requireContext();
        final String tag = NewReleasesFragmentArgs.fromBundle(requireArguments()).getTag();

        LogHelper.d("NEW RELEASES %s", tag);

        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_releases_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                switch (item.getItemId()) {
                    case R.id.purchaseReleases:
                        if (tracker.hasSelection()) {
                            // le multi non vengono passate
                            mReleaseViewModel.togglePurchased(tracker.getSelection());
                        }
                        // mantengo la selezione
                        return true;
                    case R.id.orderReleases:
                        if (tracker.hasSelection()) {
                            // le multi non vengono passate
                            mReleaseViewModel.toggleOrdered(tracker.getSelection());
                        }
                        // mantengo la selezione
                        return true;
                    case R.id.deleteReleases:
                        if (tracker.hasSelection()) {
                            // prima elimino eventuali release ancora in fase di undo
                            mReleaseViewModel.deleteRemoved();
                            mReleaseViewModel.remove(tracker.getSelection(), count -> showUndo(count));
                        }
                        tracker.clearSelection();
                        return true;
                    case R.id.shareReleases:
                        LiveDataEx.observeOnce(mReleaseViewModel.getComicsReleases(tracker.getSelection()), getViewLifecycleOwner(),
                                items -> ShareHelper.shareReleases(requireActivity(), items));
                        // mantengo la selezione
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
                .withReleaseCallback(new ReleaseAdapter.ReleaseCallback() {
                    @Override
                    public void onReleaseClick(@NonNull ComicsRelease release) {
                        // se è una multi release apro il dettaglio del comics
                        if (release instanceof MultiRelease) {
                            openComicsDetail(view, release);
                        } else {
                            openEdit(view, release);
                        }
                    }

                    @Override
                    public void onReleaseTogglePurchase(@NonNull ComicsRelease release) {
                        // le multi non vengono passate qua
                        mReleaseViewModel.updatePurchased(!release.release.purchased, release.release.id);
                    }

                    @Override
                    public void onReleaseToggleOrder(@NonNull ComicsRelease release) {
                        // le multi non vengono passate qua
                        mReleaseViewModel.updateOrdered(!release.release.ordered, release.release.id);
                    }

                    @Override
                    public void onReleaseMenuSelected(@NonNull ComicsRelease release) {
                        ReleaseBottomSheetDialogHelper.show(requireActivity(), id -> {
                            if (id == R.id.gotoComics) {
                                openComicsDetail(view, release);
                            } else if (id == R.id.share) {
                                ShareHelper.shareRelease(requireActivity(), release);
                            } else if (id == R.id.deleteRelease) {
                                deleteRelease(release);
                            } else if (id == R.id.search_starshop) {
                                ShareHelper.shareOnStarShop(requireActivity(), release);
                            }
                        });
                    }
                })
                .withGlide(Glide.with(this))
                .build();

        // recupero il ViewModel per l'accesso ai dati
        // lo lego all'activity perché il fragment viene ricrecato ogni volta (!)
        mReleaseViewModel = new ViewModelProvider(requireActivity())
                .get(ReleaseViewModel.class);
        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        mReleaseViewModel.getReleaseViewModelItems(tag).observe(getViewLifecycleOwner(), items -> {
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

    private void openComicsDetail(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = NewReleasesFragmentDirections
                .actionDestComicsDetail(release.comics.id);

        Navigation.findNavController(view).navigate(directions);
    }

    private void openEdit(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = NewReleasesFragmentDirections
                .actionReleaseEdit(release.comics.id)
                .setReleaseId(release.release.id);

        Navigation.findNavController(view).navigate(directions);
    }

    private void deleteRelease(@NonNull ComicsRelease release) {
        // nel caso di multi chideo conferma
        if (release instanceof MultiRelease) {
            final MultiRelease multiRelease = (MultiRelease) release;
            new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                    .setTitle(release.comics.name)
                    .setMessage(getString(R.string.confirm_delete_multi_release, multiRelease.size()))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        // prima elimino eventuali release ancora in fase di undo
                        mReleaseViewModel.deleteRemoved();
                        mReleaseViewModel.remove(multiRelease.getAllReleaseId(), this::showUndo);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            // prima elimino eventuali release ancora in fase di undo
            mReleaseViewModel.deleteRemoved();
            mReleaseViewModel.remove(release.release.id, this::showUndo);
        }
    }

    private void showUndo(int count) {
        mListener.requestSnackbar(getResources().getQuantityString(R.plurals.release_deleted, count, count),
                Constants.UNDO_TIMEOUT,
                (canDelete) -> {
                    if (canDelete) {
                        LogHelper.d("Delete removed releases");
                        mReleaseViewModel.deleteRemoved();
                    } else {
                        LogHelper.d("Undo removed releases");
                        mReleaseViewModel.undoRemoved();
                    }
                });
    }
}