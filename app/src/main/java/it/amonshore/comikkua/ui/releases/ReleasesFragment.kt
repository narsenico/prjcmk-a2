package it.amonshore.comikkua.ui.releases

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.util.Consumer
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import com.bumptech.glide.Glide
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.data.release.ReleaseViewModelKt
import it.amonshore.comikkua.data.release.UiReleaseEvent
import it.amonshore.comikkua.databinding.FragmentReleasesBinding
import it.amonshore.comikkua.parcelable
import it.amonshore.comikkua.ui.ActionModeController
import it.amonshore.comikkua.ui.BottomSheetDialogHelper
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.ui.ShareHelper
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback
import it.amonshore.comikkua.workers.UpdateReleasesWorker
import it.amonshore.comikkua.workers.enqueueUpdateReleasesWorker

private const val BUNDLE_RELEASES_RECYCLER_LAYOUT = "bundle.releases.recycler.layout"

class ReleasesFragment : Fragment() {

    private val _releaseViewModel: ReleaseViewModelKt by viewModels()

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

        val actionModeName = javaClass.simpleName + "_actionMode"
        val actionModeController = createActionModeController()
        _adapter = createReleasesAdapter(actionModeName, actionModeController)

        binding.swipeRefresh.setOnRefreshListener(::performUpdate)

        _releaseViewModel.notableReleaseItems.observe(viewLifecycleOwner) { items ->
            LogHelper.d("release view model data changed size=${items.size}")
            _adapter.submitList(items)
        }

        _releaseViewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiReleaseEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count)
                is UiReleaseEvent.Sharing -> shareReleases(result.releases)
            }
        }

        // ripristino la selezione salvata in onSaveInstanceState
        _adapter.selectionTracker.onRestoreInstanceState(savedInstanceState)
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
                _releaseViewModel.states.parcelable(
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
        _releaseViewModel.states.putParcelable(
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
        // ripristino le selezioni
        _adapter.selectionTracker.onSaveInstanceState(outState)
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
                        if (tracker.hasSelection()) {
                            // le multi non vengono passate
                            _releaseViewModel.togglePurchased(tracker.selection.toList())
                        }
                        return true
                    }
                    R.id.orderReleases -> {
                        if (tracker.hasSelection()) {
                            // le multi non vengono passate
                            _releaseViewModel.toggleOrdered(tracker.selection.toList())
                        }
                        return true
                    }
                    R.id.deleteReleases -> {
                        _releaseViewModel.markAsRemoved(tracker.selection.toList())
                        tracker.clearSelection()
                        return true
                    }
                    R.id.shareReleases -> {
                        _releaseViewModel.getShareableComicsReleases(tracker.selection.toList())
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
        actionModeName: String,
        actionModeController: ActionModeController
    ) = ReleaseAdapter.Builder(binding.list)
        .withOnItemSelectedListener { _, size ->
            if (size == 0) {
                _listener.onFragmentRequestActionMode(null, actionModeName, null)
            } else {
                _listener.onFragmentRequestActionMode(
                    actionModeController, actionModeName,
                    getString(R.string.title_selected, size)
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
                _releaseViewModel.updatePurchased(
                    release.release.id,
                    !release.release.purchased
                )
            }

            override fun onReleaseToggleOrder(release: ComicsRelease) {
                // le multi non vengono passate qua
                _releaseViewModel.updateOrdered(
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
                    performUpdate()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun performUpdate() {
        // TODO: ma poi perché usare un workmanager qua? fare tutto nel viewmodel che fai prima

        // TODO: non mi piace, è cmq legato al lifeCycle ma mi piacerebbe gestirlo nel viewModel (se è il posto giusto)
        // TODO: anche gestire qua lo swipe non mi piace, usare uno "state" loading nel viewModel
        binding.swipeRefresh.isRefreshing = true
        enqueueUpdateReleasesWorker(requireActivity(),
            Consumer { data: Data ->
                LogHelper.d("Releases updated data=%s", data)
                binding.swipeRefresh.isRefreshing = false
                onUpdateSuccess(data)
            },
            Runnable {
                LogHelper.w("Failing updating releases")
                binding.swipeRefresh.isRefreshing = false
            }
        )
    }

    private fun onUpdateSuccess(data: Data) {
        val newReleaseCount = data.getInt(UpdateReleasesWorker.RELEASE_COUNT, 0)
        val tag = data.getString(UpdateReleasesWorker.RELEASE_TAG)
        LogHelper.d("New releases: %s with tag '%s'", newReleaseCount, tag)
        if (newReleaseCount == 0) {
            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                .setIcon(R.drawable.ic_release)
                .setView(R.layout.dialog_no_update)
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int -> }
                .show()
        } else {
            openNewReleases(requireView(), tag!!)
        }
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
        TODO()
//        // nel caso di multi chideo conferma
//        if (release is MultiRelease) {
//            val multiRelease = release
//            AlertDialog.Builder(requireContext(), R.style.DialogTheme)
//                .setTitle(release.comics.name)
//                .setMessage(getString(R.string.confirm_delete_multi_release, multiRelease.size()))
//                .setPositiveButton(android.R.string.yes) { dialog: DialogInterface?, which: Int ->
//                    // prima elimino eventuali release ancora in fase di undo
//                    mReleaseViewModel!!.deleteRemoved()
//                    mReleaseViewModel!!.remove(multiRelease.allReleaseId) { count: Int ->
//                        showUndo(
//                            count
//                        )
//                    }
//                }
//                .setNegativeButton(android.R.string.no, null)
//                .show()
//        } else {
//            // prima elimino eventuali release ancora in fase di undo
//            mReleaseViewModel!!.deleteRemoved()
//            mReleaseViewModel!!.remove(release.release.id) { count: Int -> showUndo(count) }
//        }
    }

    private fun onMarkedAsRemoved(count: Int) {
        _releaseViewModel.deleteRemoved()

//        // TODO: questo non porta a nulla di buono!!! _releaseViewModel non è più disponibile se navigo da altre parti!!!
//        _listener.requestSnackbar(
//            resources.getQuantityString(R.plurals.release_deleted, count, count),
//            Constants.UNDO_TIMEOUT
//        ) { canDelete: Boolean ->
//            if (canDelete) {
//                LogHelper.d("Delete removed releases")
//                _releaseViewModel.deleteRemoved()
//            } else {
//                LogHelper.d("Undo removed releases")
//                _releaseViewModel.undoRemoved()
//            }
//        }
    }
}