package it.amonshore.comikkua.ui.releases

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.databinding.FragmentNewReleasesBinding
import it.amonshore.comikkua.parcelable
import it.amonshore.comikkua.ui.ActionModeController
import it.amonshore.comikkua.ui.BottomSheetDialogHelper
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.ui.ShareHelper
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback

private const val BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.new_releases.recycler.layout"
private val ACTION_MODE_NAME = NewReleasesFragment::class.java.simpleName + "_actionMode"

/**
 * Vengono mostrate le release con un certo tag.
 * Utilizzata per mostrate all'utente le ultime release aggiunte a fronte di un aggiornamento automatico.
 */
class NewReleasesFragment : Fragment() {

    private val _viewModel: NewReleasesViewModel by viewModels()

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _adapter: ReleaseAdapter

    private var _binding: FragmentNewReleasesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewReleasesBinding.inflate(layoutInflater, container, false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val actionModeController = createActionModeController()
        _adapter = createReleasesAdapter(actionModeController)

        val tag = NewReleasesFragmentArgs.fromBundle(requireArguments()).tag
        LogHelper.d("NEW RELEASES %s", tag)

        _viewModel.getReleaseViewModelItems(tag)
            .observe(viewLifecycleOwner) { items ->
                LogHelper.d("release view model data changed size:${items.size}")
                _adapter.submitList(items)
            }

        _viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiNewReleasesEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count, result.tag)
                is UiNewReleasesEvent.Sharing -> shareReleases(result.releases)
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ripristino lo stato del layout (la posizione dello scroll)
        // se non trovo savedInstanceState uso lo stato salvato nel view model
        if (savedInstanceState != null) {
            binding.list.layoutManager?.onRestoreInstanceState(
                savedInstanceState.parcelable(
                    BUNDLE_RELEASES_RECYCLER_LAYOUT
                )
            )
        } else {
            binding.list.layoutManager?.onRestoreInstanceState(
                _viewModel.states.parcelable(
                    BUNDLE_RELEASES_RECYCLER_LAYOUT
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // visto che Navigation ricrea il fragment ogni volta (!)
        // salvo lo stato della lista nel view model in modo da poterlo recuperare se necessario
        //  in onViewCreated
        _viewModel.states.putParcelable(
            BUNDLE_RELEASES_RECYCLER_LAYOUT,
            binding.list.layoutManager?.onSaveInstanceState()
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _listener = if (context is OnNavigationFragmentListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnNavigationFragmentListener")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // salvo lo stato del layout (la posizione dello scroll)
        outState.putParcelable(
            BUNDLE_RELEASES_RECYCLER_LAYOUT,
            binding.list.layoutManager?.onSaveInstanceState()
        )
    }

    private fun createActionModeController() =
        object : ActionModeController(R.menu.menu_releases_selected) {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val tracker = _adapter.selectionTracker
                when (item.itemId) {
                    R.id.purchaseReleases -> {
                        // le multi non vengono passate
                        _viewModel.togglePurchased(tracker.selection.toList())
                        return true
                    }
                    R.id.orderReleases -> {
                        // le multi non vengono passate
                        _viewModel.toggleOrdered(tracker.selection.toList())
                        return true
                    }
                    R.id.deleteReleases -> {
                        _viewModel.markAsRemoved(tracker.selection.toList())
                        tracker.clearSelection()
                        return true
                    }
                    R.id.shareReleases -> {
                        _viewModel.getShareableComicsReleases(tracker.selection.toList())
                        return true
                    }
                    else -> return false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // action mode distrutta (anche con BACK, che viene gestito internamente all'ActionMode e non può essere evitato)
                _adapter.selectionTracker.clearSelection()
                super.onDestroyActionMode(mode)
            }
        }

    private fun createReleasesAdapter(
        actionModeController: ActionModeController
    ) = ReleaseAdapter.Builder(binding.list)
        .withOnItemSelectedListener { _, size ->
            if (size == 0) {
                _listener.onFragmentRequestActionMode(ACTION_MODE_NAME)
            } else {
                _listener.onFragmentRequestActionMode(
                    ACTION_MODE_NAME,
                    getString(R.string.title_selected, size),
                    actionModeController
                )
            }
        }
        .withReleaseCallback(object : ReleaseCallback {
            override fun onReleaseClick(release: ComicsRelease) {
                // se è una multi release apro il dettaglio del comics
                if (release is MultiRelease) {
                    openComicsDetail(binding.root, release)
                } else {
                    openEdit(binding.root, release)
                }
            }

            override fun onReleaseTogglePurchase(release: ComicsRelease) {
                // le multi non vengono passate qua
                _viewModel.updatePurchased(
                    release.release.id,
                    !release.release.purchased
                )
            }

            override fun onReleaseToggleOrder(release: ComicsRelease) {
                // le multi non vengono passate qua
                _viewModel.updateOrdered(
                    release.release.id,
                    !release.release.ordered
                )
            }

            override fun onReleaseMenuSelected(release: ComicsRelease) {
                BottomSheetDialogHelper.show(
                    requireActivity(), R.layout.bottomsheet_release,
                    ShareHelper.formatRelease(requireContext(), release)
                ) { id: Int ->
                    when (id) {
                        R.id.gotoComics -> {
                            openComicsDetail(binding.root, release)
                        }
                        R.id.share -> {
                            ShareHelper.shareRelease(requireActivity(), release)
                        }
                        R.id.deleteRelease -> {
                            deleteRelease(release)
                        }
                        R.id.search_starshop -> {
                            ShareHelper.shareOnStarShop(requireActivity(), release)
                        }
                        R.id.search_amazon -> {
                            ShareHelper.shareOnAmazon(requireActivity(), release)
                        }
                        R.id.search_popstore -> {
                            ShareHelper.shareOnPopStore(requireActivity(), release)
                        }
                        R.id.search_google -> {
                            ShareHelper.shareOnGoogle(requireActivity(), release)
                        }
                    }
                }
            }
        })
        .withGlide(Glide.with(this))
        .build()

    private fun openComicsDetail(view: View, release: ComicsRelease) {
        val directions: NavDirections = NewReleasesFragmentDirections
            .actionDestComicsDetail(release.comics.id)
        findNavController(view).navigate(directions)
    }

    private fun openEdit(view: View, release: ComicsRelease) {
        val directions: NavDirections = NewReleasesFragmentDirections
            .actionReleaseEdit(release.comics.id)
            .setReleaseId(release.release.id)
        findNavController(view).navigate(directions)
    }

    private fun shareReleases(releases: List<ComicsRelease>) {
        ShareHelper.shareReleases(
            requireActivity(),
            releases
        )
    }

    private fun deleteRelease(release: ComicsRelease) {
        if (release is MultiRelease) {
            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setTitle(release.comics.name)
                .setMessage(getString(R.string.confirm_delete_multi_release, release.size()))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    _viewModel.markAsRemoved(release.allReleaseId.toList())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            _viewModel.markAsRemoved(listOf(release.release.id))
        }
    }

    private fun onMarkedAsRemoved(count: Int, tag: String) {
        _listener.handleUndo(
            resources.getQuantityString(R.plurals.release_deleted, count, count),
            tag
        )
    }
}