package it.amonshore.comikkua.ui.releases

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.FragmentReleaseEditBinding
import it.amonshore.comikkua.ui.OnNavigationFragmentListener

private data class Id(val comicsId: Long, val releaseId: Long)

class ReleaseEditFragment : Fragment() {

    private val _viewModel: ReleaseEditViewModel by viewModels()

    private val _id: Id by lazy {
        val args = ReleaseEditFragmentArgs.fromBundle(requireArguments())
        Id(args.comicsId, args.releaseId)
    }

    private lateinit var _listener: OnNavigationFragmentListener

    private var _binding: FragmentReleaseEditBinding? = null
    private val binding get() = _binding!!
    private var _helper: ReleaseEditFragmentHelper? = null
    private val helper get() = _helper!!

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        sharedElementEnterTransition = TransitionInflater.from(context)
//            .inflateTransition(android.R.transition.move)
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReleaseEditBinding.inflate(layoutInflater, container, false)
        _helper = ReleaseEditFragmentHelper(
            requireContext(),
            binding,
            parentFragmentManager,
            Glide.with(this)
        )

//        // lo stesso nome della transizione è stato assegnato alla view di partenza
//        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
//        helper.rootView.findViewById<View>(R.id.release).transitionName =
//            "release_tx_${_id.releaseId}"

        _viewModel.getComicsAndRelease(_id.comicsId, _id.releaseId)
            .observe(viewLifecycleOwner) { (comics, release) ->
                helper.setComics(comics)
                helper.setRelease(release, savedInstanceState)
            }

        _viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                UiReleaseEditResult.Inserted -> insertDone()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _listener = if (context is OnNavigationFragmentListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnNavigationFragmentListener")
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_releases_edit, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.saveReleases) {
                    helper.isValid {
                        // TODO: la lambda ritorna se valido
                        _viewModel.insertReleases(helper.createReleases().toList())
                    }
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun insertDone() {
        findNavController().navigateUp()
    }
}