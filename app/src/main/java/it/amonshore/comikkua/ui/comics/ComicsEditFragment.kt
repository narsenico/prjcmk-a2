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
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.FragmentComicsEditBinding

private val cropOptions = CropImageContractOptions(
    uri = null,
    cropImageOptions = CropImageOptions(
        guidelines = CropImageView.Guidelines.ON,
        cropShape = CropImageView.CropShape.OVAL,
        fixAspectRatio = true,
    )
)

class ComicsEditFragment : Fragment() {

    private val _viewModel: ComicsEditViewModel by viewModels()

    private var _binding: FragmentComicsEditBinding? = null
    private val binding get() = _binding!!
    private var _helper: ComicsEditFragmentHelper? = null
    private val helper get() = _helper!!

    private val _cropImageLauncher =
        registerForActivityResult(CropImageContract(), ::onImageCropped)

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
        _helper = ComicsEditFragmentHelper(
            requireContext(),
            binding,
            Glide.with(this)
        )

//        // lo stesso nome della transizione è stato assegnato alla view di partenza
//        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
//        _helper.getRootView().findViewById<View>(R.id.comics).transitionName =
//            "comics_tx_$comicsId"

        val comicsId = ComicsEditFragmentArgs.fromBundle(requireArguments()).comicsId
        _viewModel.getComicsWithReleasesOrNew(comicsId)
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
        _helper?.run {
            helper.saveInstanceState(outState)
        }
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
                        grabImage()
                        true
                    }
                    R.id.removeImage -> {
                        helper.setComicsImagePath(null)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onSaved() {
        Navigation.findNavController(binding.root)
            .navigateUp()
    }

//    private fun requestPermissionCallback(isGranted: Boolean) {
//        if (isGranted) {
//            grabImage()
//        }
//    }

    private fun grabImage() {
        // TODO: controllare permessi per fotocamera
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
        _cropImageLauncher.launch(cropOptions)
//        }
    }

    private fun onImageCropped(result: CropImageView.CropResult) {
        if (result.isSuccessful) {
            val uriFilePath = result.getUriFilePath(requireContext(), true)
            helper.setComicsImagePath(uriFilePath)
        } else {
            LogHelper.e("Crop error", result.error)
        }
    }
}