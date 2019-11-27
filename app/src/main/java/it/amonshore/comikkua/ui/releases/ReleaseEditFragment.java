package it.amonshore.comikkua.ui.releases;

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

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;

public class ReleaseEditFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    private ComicsViewModel mComicsViewModel;
    private ReleaseViewModel mReleaseViewModel;
    private ReleaseEditFragmentHelper mHelper;
    private long mComicsId;
    private long mReleaseId;
    // immodificabile
    private ComicsWithReleases mComics;

    public ReleaseEditFragment() {
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
        final ReleaseEditFragmentArgs args = ReleaseEditFragmentArgs.fromBundle(requireArguments());
        mListener.onSubtitleChanged(args.getSubtitle());

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(requireActivity())
                .get(ComicsViewModel.class);
        mReleaseViewModel = new ViewModelProvider(requireActivity())
                .get(ReleaseViewModel.class);

        // helper per bindare le view
        mHelper = ReleaseEditFragmentHelper.init(inflater, container,
                mReleaseViewModel,
                getViewLifecycleOwner(),
                getParentFragmentManager(),
                Glide.with(this));

        final Bundle arguments = requireArguments();
        // id del comics, deve esistere sempre
        mComicsId = args.getComicsId();
        // id della release da editare, può essere NEW_RELEASE_ID per la creazione di una nuova release
        mReleaseId = args.getReleaseId();

        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        mHelper.getRootView().findViewById(R.id.release).setTransitionName("release_tx_" + mReleaseId);

        // prima di tutto devo recupera il comics
        final LiveData<ComicsWithReleases> ldComics = mComicsViewModel.getComicsWithReleases(mComicsId);
        ldComics.observe(getViewLifecycleOwner(), new Observer<ComicsWithReleases>() {
            @Override
            public void onChanged(ComicsWithReleases comicsWithReleases) {
                mComics = comicsWithReleases;
                if (mReleaseId == Release.NEW_RELEASE_ID) {
                    mHelper.setRelease(requireContext(), mComics, null, savedInstanceState);
                } else {
                    final LiveData<Release> ldRelease = mReleaseViewModel.getRelease(mReleaseId);
                    ldRelease.observe(getViewLifecycleOwner(), new Observer<Release>() {
                        @Override
                        public void onChanged(Release release) {
                            mHelper.setRelease(requireContext(), mComics, release, savedInstanceState);
                            ldRelease.removeObserver(this);
                        }
                    });
                }

                ldComics.removeObserver(this);
            }
        });

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
        inflater.inflate(R.menu.menu_releases_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.saveReleases) {
            // controllo che i dati siano validi
            mHelper.isValid(valid -> {
                // eseguo il salvataggio in manera asincrona
                //  al termine navigo vergo la destinazione
                new InsertAsyncTask(getView(), mReleaseViewModel, mHelper.isNew())
                        .execute(mHelper.createReleases());
            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class InsertAsyncTask extends AsyncTask<Release, Void, Integer> {

        private WeakReference<View> mWeakView;
        private ReleaseViewModel mReleaseViewModel;
        private boolean mIsNew;

        InsertAsyncTask(View view, ReleaseViewModel releaseViewModel, boolean isNew) {
            mWeakView = new WeakReference<>(view);
            mReleaseViewModel = releaseViewModel;
            mIsNew = isNew;
        }

        @Override
        protected Integer doInBackground(Release... releases) {
            // per prima cosa elimino tutte le release esistenti con gli stessi numeri
            // così potrò "sovrascrierle"
            int[] numbers = new int[releases.length];
            for (int ii = 0; ii < numbers.length; ii++) {
                numbers[ii] = releases[ii].number;
            }
            int delCount = mReleaseViewModel.deleteByNumberSync(releases[0].comicsId, numbers);
            LogHelper.d("DELETED OLD %d releases", delCount);
            return mReleaseViewModel.insertSync(releases).length;
        }

        @Override
        protected void onPostExecute(Integer count) {
            LogHelper.d("INSERTED %d releases", count);
            final View view = mWeakView.get();
            if (view != null) {
                if (count > 0) {
                    Navigation.findNavController(view)
                            .navigateUp();
                } else {
                    Toast.makeText(view.getContext(),
                            R.string.comics_saving_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
