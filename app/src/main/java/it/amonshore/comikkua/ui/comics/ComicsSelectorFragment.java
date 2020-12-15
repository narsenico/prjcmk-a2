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
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.web.AvailableComics;
import it.amonshore.comikkua.data.web.CmkWebViewModelKt;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import it.amonshore.comikkua.workers.RefreshComicsWorker;

/**
 * Mostra tutti i comics disponibili per l'auto aggiornamento.
 * Serve per far selezionare all'utnete un nuovo comics da inserire nel proprio elenco.
 * <p>
 * TODO: leggere elenco comcis dalla rete (rendere generico adapter facendogli gestire anche AvailableComics o fare un adapter nuovo?)
 */
public class ComicsSelectorFragment extends Fragment {

    private final static String BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics_selector.recycler.layout";
    private final static String BUNDLE_COMICS_LAST_QUERY = "bundle.comics_selector.last.query";

    private OnNavigationFragmentListener mListener;
    private AvailableComicsAdapter mAdapter;
    private CmkWebViewModelKt mCmkWebViewModel;
    private RecyclerView mRecyclerView;

    public ComicsSelectorFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_comics_selector, container, false);

        final Context context = requireContext();
        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        mAdapter = new AvailableComicsAdapter.Builder(mRecyclerView)
                .withComicsCallback(new AvailableComicsAdapter.ComicsCallback() {
                    @Override
                    public void onComicsClick(@NonNull AvailableComics comics) {
                        // TODO
                    }

                    @Override
                    public void onComicsMenuSelected(@NonNull AvailableComics comics) {
                        // TODO
                    }
                })
                .withGlide(Glide.with(this))
                .build();

//        // recupero il ViewModel per l'accesso ai dati
//        // lo lego all'activity perché il fragment viene ricrecato ogni volta (!)
//        mCmkWebViewModel = new ViewModelProvider(requireActivity())
//                .get(CmkWebViewModel.class);
//        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
//        mCmkWebViewModel.getFilteredAvailableComics().observe(getViewLifecycleOwner(), data -> {
//            LogHelper.d("comics viewmodel paging data changed");
//            mAdapter.submitData(getLifecycle(), data);
//        });
//
////        // ripristino la selezione salvata in onSaveInstanceState
////        mAdapter.getSelectionTracker().onRestoreInstanceState(savedInstanceState);
//
//        // la prima volta carico tutti i dati
//        mCmkWebViewModel.setFilter(null);

        mCmkWebViewModel = new ViewModelProvider(requireActivity())
                .get(CmkWebViewModelKt.class);

//        mCmkWebViewModel.getAvailableComics().observe(getViewLifecycleOwner(), data -> {
//            LogHelper.d("comics viewmodel paging data changed");
//            mAdapter.submitData(getLifecycle(), data);
//        });

        mCmkWebViewModel.getAvailableComics().observe(getViewLifecycleOwner(), data -> {
            LogHelper.d("submitList count=%s", data == null ? -1 : data.size());
            mAdapter.submitList(data);
        });

//        // TODO: la prima volta non deve caricare nulla
//        mCmkWebViewModel.useLastFilter();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        // ripristino lo stato del layout (la posizione dello scroll)
//        // se non trovo savedInstanceState uso lo stato salvato nel view model
//        if (savedInstanceState != null) {
//            Objects.requireNonNull(mRecyclerView.getLayoutManager())
//                    .onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
//        } else if (mCmkWebViewModel != null) {
//            Objects.requireNonNull(mRecyclerView.getLayoutManager())
//                    .onRestoreInstanceState(mCmkWebViewModel.states.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (mRecyclerView != null) {
//            // visto che Navigation ricrea il fragment ogni volta (!)
//            // salvo lo stato della lista nel view model in modo da poterlo recuperare se necessario
//            //  in onViewCreated
//            mCmkWebViewModel.states.putParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT,
//                    Objects.requireNonNull(mRecyclerView.getLayoutManager()).onSaveInstanceState());
//        }
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
            // salvo lo stato del layout (la posizione dello scroll)
            outState.putParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT,
                    Objects.requireNonNull(mRecyclerView.getLayoutManager())
                            .onSaveInstanceState());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics_selector_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);

        final MenuItem searchItem = menu.findItem(R.id.searchComics);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        // onQueryTextSubmit non viene scatenato su query vuota, quindi non posso caricare tutti i dati

        // TODO: al cambio di configurazione (es orientamento) la query viene persa
        //  è il viewModel che deve tenere memorizzata l'ultima query,
        //  qua al massimo devo apri la SearchView e inizializzarla con l'ultima query da viewModel se non vuota

        if (!TextUtils.isEmpty(mCmkWebViewModel.getFilter())) {
            // lo faccio prima di aver impostato i listener così non scateno più nulla
            searchItem.expandActionView();
            searchView.setQuery(mCmkWebViewModel.getFilter(), false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogHelper.d("onQueryTextSubmit");
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                LogHelper.d("setFilter %s", newText);
                mCmkWebViewModel.setFilter(newText); // TODO: ok ma aggiungere debounce
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            performUpdate();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performUpdate() {
        final WorkRequest request = new OneTimeWorkRequest.Builder(RefreshComicsWorker.class)
                .build();

        final WorkManager workManager = WorkManager.getInstance(requireContext());
        workManager.enqueue(request);

        workManager.getWorkInfoByIdLiveData(request.getId()).observe(getViewLifecycleOwner(), workInfo -> {
            if (workInfo != null) {
                LogHelper.d("Updating available comics state=%s", workInfo.getState());
                switch (workInfo.getState()) {
                    case SUCCEEDED:
                        final int count = workInfo.getOutputData().getInt(RefreshComicsWorker.REFRESHING_COUNT, 0);
                        Toast.makeText(requireContext(), "Refreshing " + count, Toast.LENGTH_SHORT).show();
                        break;
                    case FAILED:
                        Toast.makeText(requireContext(), "Refreshing failed", Toast.LENGTH_SHORT).show();
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
}
