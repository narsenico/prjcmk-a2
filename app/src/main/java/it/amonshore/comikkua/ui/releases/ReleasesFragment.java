package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import it.amonshore.comikkua.data.ReleasesViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;


public class ReleasesFragment extends Fragment {

    private OnNavigationFragmentListener mListener;
    private ReleasesRecyclerViewAdapter mAdapter;
    private ReleasesViewModel mReleasesViewModel;

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
        final RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        final ActionModeController actionModeController = new ActionModeController(R.menu.menu_releases_selected) {
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.deleteReleases:
                        final SelectionTracker<Long> tracker = mAdapter.getSelectionTracker();
                        if (tracker.hasSelection()) {
                            mReleasesViewModel.delete(tracker.getSelection());
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

        mAdapter = new ReleasesRecyclerViewAdapter.Builder(recyclerView)
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
                    // TODO: naviga all'editor della release
                    LogHelper.d("Release click key=%s hotspot=%s", item.getSelectionKey(), item.inSelectionHotspot(e));
                    return false;
                })
                .build();

        // recupero il ViewModel per l'accesso ai dati
        mReleasesViewModel = new ViewModelProvider(this)
                .get(ReleasesViewModel.class);
        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        mReleasesViewModel.comicsReleaseList.observe(getViewLifecycleOwner(), data -> {
            LogHelper.d("viewmodel data changed");
            mAdapter.submitList(data);
        });

        // ripristino la selezione salvata in onSaveInstanceState
        mAdapter.getSelectionTracker().onRestoreInstanceState(savedInstanceState);

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
        inflater.inflate(R.menu.menu_releases_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // TODO
        return super.onOptionsItemSelected(item);
    }
}