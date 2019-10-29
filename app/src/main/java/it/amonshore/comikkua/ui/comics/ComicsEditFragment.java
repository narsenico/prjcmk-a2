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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComicsViewModel;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;


public class ComicsEditFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    private TextView mInitial, mName, mPublisher, mAuthors, mNotes,
            mLast, mNext, mMissing;
    private ImageView mComicsMenu;

    private ComicsViewModel mComicsViewModel;
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
        final View view = inflater.inflate(R.layout.fragment_comics_edit, container, false);

        mComicsId = ComicsDetailFragmentArgs.fromBundle(getArguments()).getComicsId();
        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        view.findViewById(R.id.comics).setTransitionName("comics_tx_" + mComicsId);

        mInitial = view.findViewById(R.id.txt_comics_initial);
        mName = view.findViewById(R.id.txt_comics_name);
        mPublisher = view.findViewById(R.id.txt_comics_publisher);
        mAuthors = view.findViewById(R.id.txt_comics_authors);
        mNotes = view.findViewById(R.id.txt_comics_notes);
        mLast = view.findViewById(R.id.txt_comics_release_last);
        mNext = view.findViewById(R.id.txt_comics_release_next);
        mMissing = view.findViewById(R.id.txt_comics_release_missing);
        mComicsMenu = view.findViewById(R.id.img_comics_menu);

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(this)
                .get(ComicsViewModel.class);
        mComicsViewModel.getComicsWithReleases(mComicsId).observe(getViewLifecycleOwner(), comics -> {
            if (comics != null) {
                mInitial.setText(comics.comics.name.substring(0, 1));
                mName.setText(comics.comics.name);
                mPublisher.setText(comics.comics.publisher);
                mAuthors.setText(comics.comics.authors);
                mNotes.setText(comics.comics.notes);

                final Context context = requireContext();

                final Release lastRelease = comics.getLastPurchasedRelease();
                mLast.setText(lastRelease == null ? context.getString(R.string.release_last_none):
                        context.getString(R.string.release_last, lastRelease.number));

                final Release nextRelease = comics.getNextToPurchaseRelease();
                mNext.setText(nextRelease == null ? context.getString(R.string.release_next_none) :
                        context.getString(R.string.release_next, nextRelease.number));

                final int missingCount = comics.getMissingReleaseCount();
                mMissing.setText(context.getString(R.string.release_missing, missingCount));

                mComicsMenu.setVisibility(View.GONE);
            }
        });

        return view;
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

                // TODO: salvare con update
                // mComicsViewModel.update();

                Navigation.findNavController(getView())
                        .navigateUp();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
