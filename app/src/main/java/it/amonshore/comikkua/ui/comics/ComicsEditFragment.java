package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;

import static it.amonshore.comikkua.data.comics.Comics.NEW_COMICS_ID;
import static it.amonshore.comikkua.data.comics.Comics.NO_COMICS_ID;


public class ComicsEditFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    private ComicsViewModel mComicsViewModel;
    private ComicsEditFragmentHelper mHelper;
    private long mComicsId;

    public ComicsEditFragment() {
    }

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
        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(this)
                .get(ComicsViewModel.class);

        // helper per bindare le view
        mHelper = ComicsEditFragmentHelper.init(inflater, container,
                mComicsViewModel,
                getViewLifecycleOwner());

        // id del comics da editra, può essere NEW_COMICS_ID per la creazione di un nuovo comics
        mComicsId = ComicsDetailFragmentArgs.fromBundle(getArguments()).getComicsId();

        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        mHelper.getRootView().findViewById(R.id.comics).setTransitionName("comics_tx_" + mComicsId);

        if (mComicsId == NEW_COMICS_ID) {
            mHelper.setComics(requireContext(), null, savedInstanceState);
        } else {
            // recupero l'istanza e poi rimuovo subito l'observer altrimenti verrebbe notificato il salvataggio
            final LiveData<ComicsWithReleases> ld = mComicsViewModel.getComicsWithReleases(mComicsId);
            ld.observe(getViewLifecycleOwner(), new Observer<ComicsWithReleases>() {
                @Override
                public void onChanged(ComicsWithReleases comicsWithReleases) {
                    mHelper.setComics(requireContext(), comicsWithReleases, savedInstanceState);
                    ld.removeObserver(this);
                }
            });
        }

        return mHelper.getRootView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mHelper != null) {
            mHelper.saveInstanceState(outState);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.saveComics) {
            // controllo che i dati siano validi
            mHelper.isValid(valid -> {
                if (valid) {
                    // eseguo il salvataggio in manera asincrona
                    //  al termine navigo vergo la destinazione
                    new InsertOrUpdateAsyncTask(getView(), mComicsViewModel, mHelper.isNew())
                            // prima di salvare scrivo i dati nel comics
                            .execute(mHelper.writeComics().comics);
                }
            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class InsertOrUpdateAsyncTask extends AsyncTask<Comics, Void, Long> {

        private WeakReference<View> mWeakView;
        private ComicsViewModel mComicsViewModel;
        private boolean mIsNew;

        InsertOrUpdateAsyncTask(View view, ComicsViewModel comicsViewModel, boolean isNew) {
            mWeakView = new WeakReference<>(view);
            mComicsViewModel = comicsViewModel;
            mIsNew = isNew;
        }

        @Override
        protected Long doInBackground(final Comics... params) {
            // eseguo l'insert o l'update asincroni, perché sono già in un thread separato
            if (mIsNew) {
                // ritorna il nuovo id
                return mComicsViewModel.insertSync(params[0]);
            } else {
                // ritorna il numero dei record aggiornati
                if (mComicsViewModel.updateSync(params[0]) == 1) {
                    return params[0].id;
                } else {
                    // c'è stato qualche problema
                    return Comics.NO_COMICS_ID;
                }
            }
        }

        @Override
        protected void onPostExecute(Long comicsId) {
            final View view = mWeakView.get();
            if (view != null) {
                if (comicsId == NO_COMICS_ID) {
                    Toast.makeText(view.getContext(),
                            R.string.comics_saving_error,
                            Toast.LENGTH_LONG).show();
                } else {
                    final NavDirections directions = ComicsEditFragmentDirections
                            .actionDestComicsEditFragmentToComicsDetailFragment()
                            .setComicsId(comicsId);

                    if (mIsNew) {
                        // se è nuovo navigo al dettaglio ma elimino questa destinazione dal back stack
                        //  in modo che se dal dettaglio vado indietro torno alla lista dei comics
                        Navigation.findNavController(view)
                                .navigate(directions, new NavOptions.Builder()
                                        .setPopUpTo(R.id.comicsEditFragment, true)
                                        .build());
                    } else {
                        // sono in edit, quindi tendenzialmente sono arrivato dal dettaglio
                        // TODO: se arrivo da altre parti? (ad esempio dalla lista release?)
                        Navigation.findNavController(view)
                                .navigateUp();
                    }
                }
            }
        }
    }

}
