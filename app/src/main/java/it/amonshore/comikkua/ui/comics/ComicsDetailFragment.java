package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
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
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.ColorFilterTransformationEx;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.GlideHelper;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import it.amonshore.comikkua.ui.releases.ReleaseAdapter;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;

import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;


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
                .withReleaseCallback(0, new ReleaseAdapter.ReleaseCallback() {
                    @Override
                    public void onReleaseClick(@NonNull ComicsRelease release) {
                        openEdit(view, release);
                    }

                    @Override
                    public void onReleaseTogglePurchase(@NonNull ComicsRelease release) {
                        mReleaseViewModel.updatePurchased(!release.release.purchased, release.release.id);
                    }

                    @Override
                    public void onReleaseToggleOrder(@NonNull ComicsRelease release) {
                        mReleaseViewModel.updateOrdered(!release.release.ordered, release.release.id);
                    }

                    @Override
                    public void onReleaseMenuItemSelected(@NonNull MenuItem item, @NonNull ComicsRelease release) {

                    }
                })
                // uso la versione "lite" con il layout per gli item più compatta
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
//                mInitial.setText(comics.comics.getInitial());
                mName.setText(comics.comics.name);
                mPublisher.setText(comics.comics.publisher);
                mAuthors.setText(comics.comics.authors);
                mNotes.setText(comics.comics.notes);

                if (comics.comics.hasImage()) {
                    mInitial.setText("");
                    Glide.with(this)
                            .load(Uri.parse(comics.comics.image))
                            .apply(GlideHelper.getCircleOptions())
                            .into(new DrawableTextViewTarget(mInitial));
                } else {
                    mInitial.setText(comics.comics.getInitial());
                }

                final Release lastRelease = comics.getLastPurchasedRelease();
                mLast.setText(lastRelease == null ? context.getString(R.string.release_last_none) :
                        context.getString(R.string.release_last, lastRelease.number));

                final Release nextRelease = comics.getNextToPurchaseRelease();
                if (nextRelease != null) {
                    if (nextRelease.date != null) {
                        // TODO: non mi piace, dovrei mostrare la data solo se futura e nel formato ddd dd MMM
                        mNext.setText(context.getString(R.string.release_next_dated, nextRelease.number,
                                DateFormatterHelper.toHumanReadable(context, nextRelease.date, DateFormatterHelper.STYLE_SHORT)));
                    } else {
                        mNext.setText(context.getString(R.string.release_next, nextRelease.number));
                    }
                } else {
                    mNext.setText(context.getString(R.string.release_next_none));
                }

                final int missingCount = comics.getNotPurchasedReleaseCount();
                mMissing.setText(context.getString(R.string.release_missing, missingCount));

                mComicsMenu.setVisibility(View.GONE);
            }
        });

        mReleaseViewModel.getReleaseViewModelItems(mComicsId).observe(getViewLifecycleOwner(), items -> {
            LogHelper.d("release viewmodel data changed size:" + items.size());
            mAdapter.submitList(items);
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
            // ripristino le selezioni
            mAdapter.getSelectionTracker().onSaveInstanceState(outState);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
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
                Navigation.findNavController(requireView())
                        .navigate(ComicsDetailFragmentDirections
                                .actionDestComicsDetailFragmentToComicsEditFragment()
                                .setComicsId(mComicsId));

                return true;
            case R.id.createNewRelease:
                Navigation.findNavController(requireView())
                        .navigate(ComicsDetailFragmentDirections
                                .actionDestComicsDetailFragmentToReleaseEditFragment()
                                .setComicsId(mComicsId));
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
