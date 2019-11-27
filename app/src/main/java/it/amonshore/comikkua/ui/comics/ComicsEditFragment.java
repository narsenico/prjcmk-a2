package it.amonshore.comikkua.ui.comics;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.ui.OnNavigationFragmentListener;
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;

import static android.app.Activity.RESULT_OK;
import static it.amonshore.comikkua.data.comics.Comics.NEW_COMICS_ID;
import static it.amonshore.comikkua.data.comics.Comics.NO_COMICS_ID;


public class ComicsEditFragment extends Fragment {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 583;

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
        final ComicsEditFragmentArgs args = ComicsEditFragmentArgs.fromBundle(requireArguments());
        mListener.onSubtitleChanged(args.getSubtitle());

        // recupero il ViewModel per l'accesso ai dati
        mComicsViewModel = new ViewModelProvider(requireActivity())
                .get(ComicsViewModel.class);

        // helper per bindare le view
        mHelper = ComicsEditFragmentHelper.init(inflater, container,
                mComicsViewModel,
                getViewLifecycleOwner(),
                Glide.with(this));

        // id del comics da editra, può essere NEW_COMICS_ID per la creazione di un nuovo comics
        mComicsId = args.getComicsId();

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
        switch (item.getItemId()) {
            case R.id.saveComics:
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
            case R.id.changeImage:
                grabImage();
                return true;
            case R.id.removeImage:
                mHelper.setComicsImage(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            final CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final Uri resultUri = result.getUri();
                LogHelper.d("Crop result " + resultUri);

                // TODO: rinominare il file e salvare il percorso in comics
                //  il nome del file deve contenere una parte variabile progressiva
                //  in modo che la cache per la vecchia immagine venga invalidata

                mHelper.setComicsImage(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                LogHelper.e("Crop error", result.getError());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                grabImage();
            }
        }
    }

    private void grabImage() {
        final Context context = requireContext();

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // non ho il permesso: contollo se posso mostrare il perché serve
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                new AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle(R.string.permission_camera_comics_title)
                        .setMessage(R.string.permission_camera_comics_explanation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                requestPermissions(new String[]{Manifest.permission.CAMERA},
                                        MY_PERMISSIONS_REQUEST_CAMERA))
                        .show();
            } else {
                // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app

                final Snackbar snackbar = Snackbar.make(requireView(), R.string.permission_camera_comics_denied,
                        Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.permission_camera_comics_settings, v ->
                        startActivity(new Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.getPackageName(), null)))
                );
                snackbar.show();
            }
        } else {
            // ho il permesso: avvio la procedura di selezione e crop dell'immagine
            // il risultato è gestito in onActivityResult
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setFixAspectRatio(true)
                    .start(requireContext(), this);
        }
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
            // eseguo l'insert o l'updatePurchased asincroni, perché sono già in un thread separato
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
                        Navigation.findNavController(view)
                                .navigateUp();
                    }
                }
            }
        }
    }

}
