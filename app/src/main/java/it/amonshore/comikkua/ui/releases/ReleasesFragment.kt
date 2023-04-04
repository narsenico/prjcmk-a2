package it.amonshore.comikkua.ui.releases

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.databinding.FragmentReleasesBinding
import it.amonshore.comikkua.parcelable
import it.amonshore.comikkua.ui.ActionModeController
import it.amonshore.comikkua.ui.BottomSheetDialogHelper
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.ui.ShareHelper
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback

private const val BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.releases.recycler.layout"
private val ACTION_MODE_NAME = ReleasesFragment::class.java.simpleName + "_actionMode"

class ReleasesFragment : Fragment() {

    private val _viewModel: ReleaseViewModel by viewModels()

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _adapter: ReleaseAdapter

    private var _binding: FragmentReleasesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReleasesBinding.inflate(layoutInflater, container, false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeRefresh.setOnRefreshListener(::loadNewReleases)

        val actionModeController = createActionModeController()
        _adapter = createReleasesAdapter(actionModeController)

        _viewModel.notableReleaseItems.observe(viewLifecycleOwner) { items ->
            LogHelper.d("release view model data changed size=${items.size}")
            _adapter.submitList(items)
        }

        _viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiReleaseEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count)
                is UiReleaseEvent.Sharing -> shareReleases(result.releases)
                is UiReleaseEvent.NewReleasesLoaded -> onNewReleasesLoaded(result.count, result.tag)
                is UiReleaseEvent.NewReleasesError -> onNewReleasesError()
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
        setupMenu()

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
        _binding?.run {
            outState.putParcelable(
                BUNDLE_RELEASES_RECYCLER_LAYOUT,
                binding.list.layoutManager?.onSaveInstanceState()
            )
        }
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

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_releases_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.updateReleases) {
                    loadNewReleases()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun loadNewReleases() {
        _listener.dismissSnackBar()
        binding.swipeRefresh.isRefreshing = true
        _viewModel.loadNewReleases()
    }

    private fun onNewReleasesLoaded(count: Int, tag: String) {
        LogHelper.d("New releases: $count with tag '$tag'")
        binding.swipeRefresh.isRefreshing = false
        if (count == 0) {
            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setIcon(R.drawable.ic_release)
                .setView(R.layout.dialog_no_update)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else {
            openNewReleases(requireView(), tag)
        }
    }

    private fun onNewReleasesError() {
        binding.swipeRefresh.isRefreshing = false
        Toast.makeText(
            requireContext(),
            R.string.refresh_release_error,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openComicsDetail(view: View, release: ComicsRelease) {
        val directions: NavDirections = ReleasesFragmentDirections
            .actionDestReleasesToComicsDetailFragment(release.comics.id)
        findNavController(view).navigate(directions)
    }

    private fun openEdit(view: View, release: ComicsRelease) {
        val directions: NavDirections = ReleasesFragmentDirections
            .actionReleasesFragmentToReleaseEditFragment(release.comics.id)
            .setReleaseId(release.release.id)
        findNavController(view).navigate(directions)
    }

    private fun openNewReleases(view: View, tag: String) {
        val directions: NavDirections = ReleasesFragmentDirections
            .actionReleasesFragmentToNewReleaseFragment(tag)
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

    private fun onMarkedAsRemoved(count: Int) {
        _listener.requestSnackBar(
            resources.getQuantityString(R.plurals.release_deleted, count, count),
            Constants.UNDO_TIMEOUT
        ) { canDelete ->
            if (canDelete) {
                LogHelper.d("Delete removed releases")
                _viewModel.deleteRemoved()
            } else {
                LogHelper.d("Undo removed releases")
                _viewModel.undoRemoved()
            }
        }
    }
}