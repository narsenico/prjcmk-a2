package it.amonshore.comikkua.ui.comics

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
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
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.databinding.DialogChooseComicsToFollowBinding
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
                is UiComicsEditResult.Error -> helper.setError(result.errorType)
                is UiComicsEditResult.ComicsToFollowFound -> onComicsToFollowFound(result.comics)
                is UiComicsEditResult.ComicsImageDownloaded -> onComicsImageDownloaded(result.imageUri)
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

                    R.id.changeImageViaWeb -> {
                        _viewModel.downloadComicsImage(helper.getComics())
                        true
                    }

                    R.id.removeImage -> {
                        helper.setComicsImagePath(null)
                        true
                    }

                    R.id.followComics -> {
                        _viewModel.searchComicsToFollow(helper.getComics())
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

    private fun onComicsToFollowFound(comics: List<AvailableComics>) {
        LogHelper.d { "${comics.size} comics to follow found" }

        val context = requireContext()

        val titles = comics.map { ac ->
            buildString {
                append(ac.name)
                append(" - ")
                append(ac.publisher)
                if (ac.version > 0) {
                    append(" (", context.getString(R.string.nth_reprint, ac.version), ")")
                }
            }
        }.toTypedArray()

        var checkedItem = if (helper.getComics().isSourced) {
            helper.getComics().sourceId.let {
                comics.indexOfFirst { ac -> ac.sourceId == it }
            }
        } else {
            -1
        }

        val binding = DialogChooseComicsToFollowBinding.inflate(LayoutInflater.from(context))
        binding.list.apply {
            adapter =
                ArrayAdapter(context, R.layout.select_dialog_singlechoice, titles)
            choiceMode = ListView.CHOICE_MODE_SINGLE
            setItemChecked(checkedItem, true)
        }
        binding.list.setOnItemClickListener { _, _, index, _ ->
            checkedItem = index
        }

        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setView(binding.root)
            .setPositiveButton(R.string.comics_follow) { _, _ ->
                comics.getOrNull(checkedItem)?.let {
                    helper.setSourceId(it.sourceId)
                    _viewModel.downloadComicsImage(helper.getComics())
                }
            }
            .setNeutralButton(R.string.comics_unfollow) { _, _ ->
                helper.setSourceId(null)
            }
            .show()
    }

    private fun onComicsImageDownloaded(uri: Uri) {
        LogHelper.d { "Image downloaded $uri" }
        _cropImageLauncher.launch(cropOptions.copy(uri = uri))
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