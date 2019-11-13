package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import it.amonshore.comikkua.ui.releases.ReleaseAdapter;

import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class ComicsDetailFragment extends Fragment {

    private OnNavigationFragmentListener mListener;

    private TextView mInitial, mName, mPublisher, mAuthors, mNotes,
            mLast, mNext, mMissing;
    private ImageView mComicsMenu;
    private ReleaseAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private ComicsViewModel mComicsViewModel;
    private ReleaseViewModel mReleaseViewModel;
    private long mComicsId;

    public ComicsDetailFragment() {
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
        final View view = inflater.inflate(R.layout.fragment_comics_detail, container, false);

        final String actionModeName = getClass().getSimpleName() + "_actionMode";
        final Context context = requireContext();
        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

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

        mAdapter = new ReleaseAdapter.Builder(mRecyclerView)
                .withReleaseCallback(0, new ReleaseAdapter.ReleaseCallback() {
                    @Override
                    public void onReleaseClick(@NonNull ComicsRelease release) {
                        openEdit(view, release);
                    }

                    @Override
                    public void onReleaseTogglePurchase(@NonNull ComicsRelease release) {

                    }

                    @Override
                    public void onReleaseToggleOrder(@NonNull ComicsRelease release) {

                    }

                    @Override
                    public void onReleaseMenuItemSelected(@NonNull MenuItem item, @NonNull ComicsRelease release) {

                    }
                })
                // TODO: aggiungere i listeners
                .useLite()
                .build();

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(requireActivity())
                .get(ComicsViewModel.class);
        // TODO: lo devo tenere legato al fragment e non all'activity?
        mReleaseViewModel = new ViewModelProvider(this)
                .get(ReleaseViewModel.class);
        // recupero l'istanza e poi rimuovo subito l'observer altrimenti verrebbe notificato il salvataggio
        // TODO: beh non dovrei cmq aggiornare i tre contatori?
        mComicsViewModel.getComicsWithReleases(mComicsId).observe(getViewLifecycleOwner(), comics -> {
            if (comics != null) {
                mInitial.setText(comics.comics.getInitial());
                mName.setText(comics.comics.name);
                mPublisher.setText(comics.comics.publisher);
                mAuthors.setText(comics.comics.authors);
                mNotes.setText(comics.comics.notes);

                final Context context1 = requireContext();

                final Release lastRelease = comics.getLastPurchasedRelease();
                mLast.setText(lastRelease == null ? context1.getString(R.string.release_last_none) :
                        context1.getString(R.string.release_last, lastRelease.number));

                final Release nextRelease = comics.getNextToPurchaseRelease();
                mNext.setText(nextRelease == null ? context1.getString(R.string.release_next_none) :
                        context1.getString(R.string.release_next, nextRelease.number));

                final int missingCount = comics.getNotPurchasedReleaseCount();
                mMissing.setText(context1.getString(R.string.release_missing, missingCount));

                mComicsMenu.setVisibility(View.GONE);

//                LogHelper.d("release count " + comics.getReleaseCount());
//                if (comics.getReleaseCount() > 0) {
//                    for (Release release : comics.releases) {
//                        LogHelper.d(" - id:%s #%s, %s (purchased %s) ",
//                                release.id, release.number, release.date, release.purchased);
//                    }
//                }
            }
        });

        mReleaseViewModel.getReleaseViewModelItems(mComicsId).observe(getViewLifecycleOwner(), items -> {
            LogHelper.d("release viewmodel data changed size:" + items.size());
            mAdapter.submitList(items);
        });

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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_comics_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.editComics:

                final NavDirections directions = ComicsDetailFragmentDirections
                        .actionDestComicsDetailFragmentToComicsEditFragment()
                        .setComicsId(mComicsId);

                Navigation.findNavController(getView()).navigate(directions);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openEdit(@NonNull View view, @NonNull ComicsRelease release) {
        final NavDirections directions = ComicsDetailFragmentDirections
                .actionDestComicsDetailFragmentToReleaseEditFragment()
                .setComicsId(release.comics.id)
                .setReleaseId(release.release.id);

        Navigation.findNavController(view).navigate(directions);
    }
}
