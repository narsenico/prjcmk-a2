package it.amonshore.comikkua.ui.comics

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.FragmentComicsEditBinding

class ComicsEditFragment : Fragment() {

    private val _viewModel: ComicsEditViewModelKt by viewModels()

    private var _binding: FragmentComicsEditBinding? = null
    private val binding get() = _binding!!
    private var _helper: ComicsEditFragmentHelperKt? = null
    private val helper get() = _helper!!

//    private val mCropImageLauncher =
//        registerForActivityResult<CropImageContractOptions, CropResult>(
//            CropImageContract(),
//            ActivityResultCallback<CropResult> { result: CropResult -> cropImageCallback(result) })
//    private val mRequestPermissionLauncher =
//        registerForActivityResult<String, Boolean>(RequestPermission()) { isGranted: Boolean ->
//            requestPermissionCallback(isGranted)
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComicsEditBinding.inflate(inflater, container, false)
        _helper = ComicsEditFragmentHelperKt(
            requireContext(),
            binding,
            Glide.with(this)
        )

//        // lo stesso nome della transizione è stato assegnato alla view di partenza
//        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
//        _helper.getRootView().findViewById<View>(R.id.comics).transitionName =
//            "comics_tx_$comicsId"

        val comicsId = ComicsEditFragmentArgs.fromBundle(requireArguments()).comicsId
        _viewModel.getComicsWithReleasesOrNull(comicsId)
            .observe(viewLifecycleOwner) {
                helper.setComics(
                    it,
                    savedInstanceState
                )
            }

        _viewModel.publishers.observe(viewLifecycleOwner) {
            helper.setPublishers(it)
        }

        _viewModel.authors.observe(viewLifecycleOwner) {
            helper.setAuthors(it)
        }

        _viewModel.availableComics.observe(viewLifecycleOwner) {
            helper.setAvailableComics(it)
        }

        _viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiComicsEditResult.Saved -> onSaved()
                is UiComicsEditResult.SaveError -> helper.setError(result.errorType)
            }
        }

        return helper.rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _helper = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        helper.saveInstanceState(outState)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_comics_edit, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.saveComics -> {
                        _viewModel.insertOrUpdateComics(helper.getComics())
                        true
                    }
                    R.id.changeImage -> {
                        //                        grabImage()
                        true
                    }
                    R.id.removeImage -> {
                        helper.setComicsImage(null)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onSaved() {
        Navigation.findNavController(binding.root)
            .navigateUp();
    }

//    private fun requestPermissionCallback(isGranted: Boolean) {
//        if (isGranted) {
//            grabImage()
//        }
//    }

//    private fun grabImage() {
//        val context = requireContext()
//        if (ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.CAMERA
//            )
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//
//            // non ho il permesso: contollo se posso mostrare il perché serve
//            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
//                AlertDialog.Builder(context, R.style.DialogTheme)
//                    .setTitle(R.string.permission_camera_comics_title)
//                    .setMessage(R.string.permission_camera_comics_explanation)
//                    .setPositiveButton(
//                        android.R.string.ok
//                    ) { dialog: DialogInterface?, which: Int ->
//                        mRequestPermissionLauncher.launch(
//                            Manifest.permission.CAMERA
//                        )
//                    }
//                    .show()
//            } else {
//                // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app
//                val snackbar = Snackbar.make(
//                    requireView(), R.string.permission_camera_comics_denied,
//                    Snackbar.LENGTH_LONG
//                )
//                snackbar.setAction(
//                    R.string.settings
//                ) { v: View? ->
//                    startActivity(
//                        Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                            .setData(Uri.fromParts("package", context.packageName, null))
//                    )
//                }
//                snackbar.show()
//            }
//        } else {
//            // ho il permesso: avvio la procedura di selezione e crop dell'immagine
//            // il risultato è gestito in onActivityResult
//            // l'immagine è salvata nella cache
//            val options = createCropImageContractOptions()
//            mCropImageLauncher.launch(options)
//        }
//    }
//
//    private fun cropImageCallback(result: CropResult) {
//        LogHelper.d(String.format("crop callback: %s", result))
//        if (result.isSuccessful) {
//            val uriFilePath: String = result.getUriFilePath(requireContext(), true)
//            val resultUri = Uri.parse(uriFilePath)
//            LogHelper.d("Crop result saved to %s", resultUri)
//            _helper!!.setComicsImage(resultUri)
//        } else {
//            LogHelper.e("Crop error", result.error)
//        }
//    }

    // TODO: è da sostituire, ma con cosa?
    //    //  il metodo migliore è usare le coroutines di Kotlink, forse è il caso di migrare il fragment in kotlin???
    //    private static class InsertOrUpdateAsyncTask extends AsyncTask<Void, Void, Long> {
    //
    //        private final WeakReference<View> mWeakView;
    //        private final ComicsViewModel mComicsViewModel;
    //        private final ComicsEditFragmentHelper mHelper;
    //        private final boolean mIsNew;
    //
    //        InsertOrUpdateAsyncTask(View view, ComicsViewModel comicsViewModel,
    //                                ComicsEditFragmentHelper helper) {
    //            mWeakView = new WeakReference<>(view);
    //            mComicsViewModel = comicsViewModel;
    //            mHelper = helper;
    //            mIsNew = mHelper.isNew();
    //        }
    //
    //        @Override
    //        protected Long doInBackground(final Void... params) {
    //            // prima di salvare sul DB applico le modifiche su Comics
    //            final Comics comics = mHelper.writeComics().comics;
    //            // eseguo l'insert o l'updatePurchased asincroni, perché sono già in un thread separato
    //            if (mIsNew) {
    //                // ritorna il nuovo id
    //                long id = mComicsViewModel.insertSync(comics);
    //                comics.id = id;
    //                mHelper.complete(true);
    //                mComicsViewModel.updateSync(comics);
    //                return id;
    //            } else {
    //                // ritorna il numero dei record aggiornati
    //                if (mComicsViewModel.updateSync(comics) == 1) {
    //                    mHelper.complete(true);
    //                    mComicsViewModel.updateSync(comics);
    //                    return comics.id;
    //                } else {
    //                    // c'è stato qualche problema
    //                    mHelper.complete(false);
    //                    return Comics.NO_COMICS_ID;
    //                }
    //            }
    //        }
    //
    //        @Override
    //        protected void onPostExecute(Long comicsId) {
    //            final View view = mWeakView.get();
    //            if (view != null) {
    //                if (comicsId == NO_COMICS_ID) {
    //                    Toast.makeText(view.getContext(),
    //                            R.string.comics_saving_error,
    //                            Toast.LENGTH_LONG).show();
    //                } else {
    //                    final NavDirections directions = ComicsEditFragmentDirections
    //                            .actionDestComicsEditFragmentToComicsDetailFragment(comicsId);
    //
    //                    LogHelper.d("SAVE onPostExecute isMainLoop=%s", Utility.isMainLoop());
    //
    //                    if (mIsNew) {
    //                        // se è nuovo navigo al dettaglio ma elimino questa destinazione dal back stack
    //                        //  in modo che se dal dettaglio vado indietro torno alla lista dei comics
    //                        Navigation.findNavController(view)
    //                                .navigate(directions, new NavOptions.Builder()
    //                                        .setPopUpTo(R.id.comicsEditFragment, true)
    //                                        .build());
    //                    } else {
    //                        // sono in edit, quindi tendenzialmente sono arrivato dal dettaglio
    //                        Navigation.findNavController(view)
    //                                .navigateUp();
    //                    }
    //                }
    //            }
    //        }
    //    }
}