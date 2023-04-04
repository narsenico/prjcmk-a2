package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.DateFormatterHelper
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.databinding.FragmentComicsDetailBinding
import it.amonshore.comikkua.ui.*
import it.amonshore.comikkua.ui.releases.ReleaseAdapter
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback

private val ACTION_MODE_NAME = ComicsDetailFragment::class.java.simpleName + "_actionMode"

class ComicsDetailFragment : Fragment() {

    private val _viewModel: ComicsDetailViewModel by viewModels()

    private val _comicsId: Long by lazy {
        ComicsDetailFragmentArgs.fromBundle(requireArguments()).comicsId
    }

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _adapter: ReleaseAdapter
    private lateinit var _comics: ComicsWithReleases

    private var _binding: FragmentComicsDetailBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentComicsDetailBinding.inflate(layoutInflater, container, false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeRefresh.setOnRefreshListener(::loadNewReleases)

//        // lo stesso nome della transizione è stato assegnato alla view di partenza
//        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
//        view.findViewById<View>(R.id.comics).transitionName = "comics_tx_$_comicsId"

        val actionModeController = createActionModeController()
        _adapter = createReleaseAdapter(actionModeController)

        _viewModel.getComicsWithReleases(_comicsId)
            .observe(viewLifecycleOwner, createComicsWithReleasesObserver())

        _viewModel.getReleaseViewModelItems(_comicsId)
            .observe(viewLifecycleOwner) { items ->
                LogHelper.d("release view model data changed size=${items.size}")
                _adapter.submitList(items)
            }

        _viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiComicsDetailEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count)
                is UiComicsDetailEvent.Sharing -> shareReleases(result.releases)
                is UiComicsDetailEvent.NewReleasesLoaded -> onNewReleasesLoaded(
                    result.count,
                    result.tag
                )
                is UiComicsDetailEvent.NewReleasesError -> onNewReleasesError()
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
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_comics_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.editComics -> {
                        findNavController(requireView())
                            .navigate(
                                ComicsDetailFragmentDirections
                                    .actionDestComicsDetailFragmentToComicsEditFragment()
                                    .setComicsId(_comicsId)
                            )
                        true
                    }
                    R.id.createNewRelease -> {
                        findNavController(requireView())
                            .navigate(
                                ComicsDetailFragmentDirections
                                    .actionDestComicsDetailFragmentToReleaseEditFragment(_comicsId)
                                    .setSubtitle(R.string.title_release_create)
                            )
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun createActionModeController() =
        object : ActionModeController(R.menu.menu_releases_selected) {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val tracker = _adapter.selectionTracker
                when (item.itemId) {
                    R.id.purchaseReleases -> {
                        // TODO: considerare le multi release
                        _viewModel.togglePurchased(tracker.selection.toList())
                        return true
                    }
                    R.id.orderReleases -> {
                        // TODO: considerare le multi release
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
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // action mode distrutta (anche con BACK, che viene gestito internamente all'ActionMode e non può essere evitato)
                _adapter.selectionTracker.clearSelection()
                super.onDestroyActionMode(mode)
            }
        }

    private fun createReleaseAdapter(
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
                openEdit(binding.root, release)
            }

            override fun onReleaseTogglePurchase(release: ComicsRelease) {
                _viewModel.updatePurchased(
                    release.release.id,
                    !release.release.purchased
                )
            }

            override fun onReleaseToggleOrder(release: ComicsRelease) {
                _viewModel.updateOrdered(
                    release.release.id,
                    !release.release.ordered
                )
            }

            override fun onReleaseMenuSelected(release: ComicsRelease) {
                // non gestito
            }
        }) // uso la versione "lite" con il layout per gli item più compatta
        .useLite()
        .build()

    private fun createComicsWithReleasesObserver(): Observer<ComicsWithReleases> {
        val context = requireContext()
        val txtInitial = binding.comics.txtComicsInitial
        val txtName = binding.comics.txtComicsName
        val txtPublisher = binding.comics.txtComicsPublisher
        val txtAuthors = binding.comics.txtComicsAuthors
        val txtNotes = binding.comics.txtComicsNotes
        val txtLast = binding.comics.txtComicsReleaseLast
        val txtNext = binding.comics.txtComicsReleaseNext
        val txtMissing = binding.comics.txtComicsReleaseMissing

        return Observer { comics ->
            _comics = comics
            txtName.text = comics.comics.name
            txtPublisher.text = comics.comics.publisher
            txtAuthors.text = comics.comics.authors
            txtNotes.text = comics.comics.notes
            if (comics.comics.hasImage()) {
                txtInitial.text = ""
                Glide.with(this)
                    .load(Uri.parse(comics.comics.image))
                    .apply(ImageHelper.getGlideCircleOptions())
                    .into(DrawableTextViewTarget(txtInitial))
            } else {
                txtInitial.text = comics.comics.initial
            }
            val lastRelease = comics.lastPurchasedRelease
            txtLast.text =
                if (lastRelease == null) context.getString(R.string.release_last_none) else context.getString(
                    R.string.release_last,
                    lastRelease.number
                )
            val nextRelease = comics.nextToPurchaseRelease
            if (nextRelease != null) {
                if (nextRelease.date != null) {
                    // TODO: non mi piace, dovrei mostrare la data solo se futura e nel formato ddd dd MMM
                    txtNext.text = context.getString(
                        R.string.release_next_dated, nextRelease.number,
                        DateFormatterHelper.toHumanReadable(
                            context,
                            nextRelease.date,
                            DateFormatterHelper.STYLE_SHORT
                        )
                    )
                } else {
                    txtNext.text = context.getString(
                        R.string.release_next,
                        nextRelease.number
                    )
                }
            } else {
                txtNext.text = context.getString(R.string.release_next_none)
            }
            val missingCount = comics.notPurchasedReleaseCount
            txtMissing.text = context.getString(R.string.release_missing, missingCount)
        }
    }

    private fun loadNewReleases() {
        _listener.dismissSnackBar()
        binding.swipeRefresh.isRefreshing = true
        _viewModel.loadNewReleases(_comics)
    }

    private fun onNewReleasesLoaded(count: Int, tag: String) {
        LogHelper.d("New releases: $count with tag '$tag'")
        binding.swipeRefresh.isRefreshing = false
        if (count > 0) {
            Toast.makeText(
                requireContext(),
                resources.getQuantityString(
                    R.plurals.auto_update_available_message,
                    count,
                    count
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.auto_update_zero,
                Toast.LENGTH_SHORT
            ).show()
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

    private fun openEdit(view: View, release: ComicsRelease) {
        val directions: NavDirections = ComicsDetailFragmentDirections
            .actionDestComicsDetailFragmentToReleaseEditFragment(release.comics.id)
            .setReleaseId(release.release.id)
        findNavController(view).navigate(directions)
    }

    private fun shareReleases(releases: List<ComicsRelease>) {
        ShareHelper.shareReleases(
            requireActivity(),
            releases
        )
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