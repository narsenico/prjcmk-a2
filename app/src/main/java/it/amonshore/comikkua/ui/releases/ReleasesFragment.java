package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.LiveDataEx;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.MultiRelease;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.BottomSheetDialogHelper;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import it.amonshore.comikkua.ui.ShareHelper;
import it.amonshore.comikkua.workers.UpdateReleasesWorker;


public class ReleasesFragment extends Fragment {

    private final static String BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.releases.recycler.layout";

    private OnNavigationFragmentListener mListener;
    private ReleaseAdapter mAdapter;
    private ReleaseViewModel mReleaseViewModel;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this::performUpdate);

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_releases_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                int itemId = item.getItemId();
                if (itemId == R.id.purchaseReleases) {
                    if (tracker.hasSelection()) {
                        // le multi non vengono passate
                        mReleaseViewModel.togglePurchased(tracker.getSelection());
                    }
                    // mantengo la selezione
                    return true;
                } else if (itemId == R.id.orderReleases) {
                    if (tracker.hasSelection()) {
                        // le multi non vengono passate
                        mReleaseViewModel.toggleOrdered(tracker.getSelection());
                    }
                    // mantengo la selezione
                    return true;
                } else if (itemId == R.id.deleteReleases) {
                    if (tracker.hasSelection()) {
                        // prima elimino eventuali release ancora in fase di undo
                        mReleaseViewModel.deleteRemoved();
                        mReleaseViewModel.remove(tracker.getSelection(), count -> showUndo(count));
                    }
                    tracker.clearSelection();
                    return true;
                } else if (itemId == R.id.shareReleases) {
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
                        BottomSheetDialogHelper.show(requireActivity(), R.layout.bottomsheet_release, id -> {
                            if (id == R.id.gotoComics) {
                                openComicsDetail(view, release);
                            } else if (id == R.id.share) {
                                ShareHelper.shareRelease(requireActivity(), release);
                            } else if (id == R.id.deleteRelease) {
                                deleteRelease(release);
                            } else if (id == R.id.search_starshop) {
                                ShareHelper.shareOnStarShop(requireActivity(), release);
                            } else if (id == R.id.search_amazon) {
                                ShareHelper.shareOnAmazon(requireActivity(), release);
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
        if (item.getItemId() == R.id.updateReleases) {
            // TODO: aggiornamento release da remoto
            performUpdate();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performUpdate() {
        // prima dell'aggiornamento elimino eventuali release rimosse nel caso fosse ancora in corso l'undo
        mReleaseViewModel.deleteRemoved();

        final WorkRequest request = new OneTimeWorkRequest.Builder(UpdateReleasesWorker.class)
                .setInputData(new Data.Builder()
                        .putBoolean(UpdateReleasesWorker.PREVENT_NOTIFICATION, true)
                        .build())
                // TODO: non funziona! in mancanza di connessione la richiesta rimane in ENQUEUED, mi aspetto che vada in uno stato di errore
                //  in ogni caso Firestore usa la cache se offline quindi mi va bene togliere il constraint
//                .setConstraints(new Constraints.Builder()
//                        .setRequiredNetworkType(NetworkType.CONNECTED)
//                        .build())
                .build();

        final WorkManager workManager = WorkManager.getInstance(requireContext());
        workManager.enqueue(request);

        mSwipeRefreshLayout.setRefreshing(true);

        // TODO: quando si verificano gli altri stati? CONTROLLARE SUBITO
        // TODO: "marchare" le release inserite con un codice, in modo che possano essere visualizzabilit dall'utente su richiesta
        //  "sono state aggiunte 2 release" => ok ma quali sono? => tap su snackbar => fragment con solo nuove release aggiunte

        workManager.getWorkInfoByIdLiveData(request.getId()).observe(getViewLifecycleOwner(), workInfo -> {
            if (workInfo != null) {
                LogHelper.d("Updating releases state=%s", workInfo.getState());
                switch (workInfo.getState()) {
                    case SUCCEEDED:
                        onUpdateSuccess(workInfo.getOutputData());
                    case FAILED:
                        mSwipeRefreshLayout.setRefreshing(false);
                        break;
                    case BLOCKED:
                    case CANCELLED:
                    case ENQUEUED:
                    case RUNNING:
                        break;
                }
            }
        });
    }

    private void onUpdateSuccess(Data data) {
        final int newReleaseCount = data.getInt(UpdateReleasesWorker.RELEASE_COUNT, 0);
        final String tag = data.getString(UpdateReleasesWorker.RELEASE_TAG);

        LogHelper.d("New releases: %s with tag '%s'", newReleaseCount, tag);

        if (newReleaseCount == 0) {
            new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                    .setIcon(R.drawable.ic_release)
                    .setView(R.layout.dialog_no_update)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        //
                    })
                    .show();
        } else {
//            final View view = getLayoutInflater().inflate(R.layout.dialog_new_update, null);
//            final TextView txtMessage = view.findViewById(R.id.message);
//            txtMessage.setText(getResources().getQuantityString(R.plurals.auto_update_available_message, newReleaseCount, newReleaseCount));
//
//            final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme)
//                    .setIcon(R.drawable.ic_release)
//                    .setView(view)
//                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
//                    });
//            if (tag != null) {
//                builder.setNeutralButton(R.string.auto_update_view, (dialog, which) -> {
//                    dialog.dismiss();
//                    openNewReleases(requireView(), tag);
//                });
//            }
//            builder.show();
            openNewReleases(requireView(), tag);
        }
    }

    private void openComicsDetail(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = ReleasesFragmentDirections
                .actionDestReleasesToComicsDetailFragment(release.comics.id);

        Navigation.findNavController(view).navigate(directions);
    }

    private void openEdit(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = ReleasesFragmentDirections
                .actionReleasesFragmentToReleaseEditFragment(release.comics.id)
                .setReleaseId(release.release.id);

        Navigation.findNavController(view).navigate(directions);
    }

    private void openNewReleases(@NonNull View view, @NonNull String tag) {
        final NavDirections directions = ReleasesFragmentDirections
                .actionReleasesFragmentToNewReleaseFragment(tag);

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