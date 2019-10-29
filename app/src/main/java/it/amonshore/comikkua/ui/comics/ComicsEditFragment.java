package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComicsViewModel;
import it.amonshore.comikkua.data.ComicsWithReleases;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;


public class ComicsEditFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    private ComicsViewModel mComicsViewModel;
    private ComicsEditFragmentHelper mHelper;
    private long mComicsId;

    public ComicsEditFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(TransitionInflater.from(getContext())
                .inflateTransition(android.R.transition.move));
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // helper per bindare le view
        mHelper = ComicsEditFragmentHelper.init(inflater, container);
        mComicsId = ComicsDetailFragmentArgs.fromBundle(getArguments()).getComicsId();
        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        mHelper.rootView.findViewById(R.id.comics).setTransitionName("comics_tx_" + mComicsId);

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(this)
                .get(ComicsViewModel.class);

        // recupero l'istanza e poi rimuovo subito l'observer altrimenti verrebbe notificato il salvataggio
        final LiveData<ComicsWithReleases> ld = mComicsViewModel.getComicsWithReleases(mComicsId);
        ld.observe(getViewLifecycleOwner(), new Observer<ComicsWithReleases>() {
            @Override
            public void onChanged(ComicsWithReleases comicsWithReleases) {
                mHelper.setComics(requireContext(), comicsWithReleases);
                ld.removeObserver(this);
            }
        });

        return mHelper.rootView;
    }

    @Override
    public void onAttach(Context context) {
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveComics:
                // aggiorno il comics e torno subito indietro
                if (mHelper.isValid()) {
                    mComicsViewModel.update(mHelper.writeComics().comics);
                    Navigation.findNavController(mHelper.rootView)
                            .navigateUp();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
