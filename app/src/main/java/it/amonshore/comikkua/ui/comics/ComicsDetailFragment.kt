package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.*
import android.widget.TextView
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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.DateFormatterHelper
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.ComicsDetailViewModelKt
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.comics.UiComicsDetailEvent
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.ui.*
import it.amonshore.comikkua.ui.releases.ReleaseAdapter
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback

class ComicsDetailFragment : Fragment() {

    private val _comicsDetailViewModel: ComicsDetailViewModelKt by viewModels()

    private val _comicsId: Long by lazy {
        ComicsDetailFragmentArgs.fromBundle(requireArguments()).comicsId
    }

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var _adapter: ReleaseAdapter
    private lateinit var _comics: ComicsWithReleases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comics_detail, container, false)

        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        view.findViewById<View>(R.id.comics).transitionName = "comics_tx_$_comicsId"

        _swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh).apply {
            setOnRefreshListener(::readNewReleases)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        val actionModeController = createActionModeController()
        val actionModeName = javaClass.simpleName + "_actionMode"
        _adapter = createReleaseAdapter(
            recyclerView,
            actionModeName,
            actionModeController,
            view
        )

        _comicsDetailViewModel.getComicsWithReleases(_comicsId)
            .observe(viewLifecycleOwner, createComicsWithReleasesObserver(view))

        _comicsDetailViewModel.getReleaseViewModelItems(_comicsId)
            .observe(viewLifecycleOwner) { items ->
                LogHelper.d("release view model data changed size=${items.size}")
                _adapter.submitList(items)
            }

        _comicsDetailViewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiComicsDetailEvent.Sharing -> shareReleases(result.releases)
                is UiComicsDetailEvent.NewReleasesLoaded -> onNewReleasesLoaded(result.count)
                is UiComicsDetailEvent.NewReleasesLoadingError -> onNewReleasesLoadingError(result.error)
                is UiComicsDetailEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count)
                else -> {}
            }
        }

        // ripristino la selezione salvata in onSaveInstanceState
        _adapter.selectionTracker.onRestoreInstanceState(savedInstanceState)
        return view
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

    private fun createReleaseAdapter(
        recyclerView: RecyclerView,
        actionModeName: String,
        actionModeController: ActionModeController,
        view: View
    ) = ReleaseAdapter.Builder(recyclerView)
        .withOnItemSelectedListener { _, size ->
            if (size == 0) {
                _listener.onFragmentRequestActionMode(null, actionModeName, null)
            } else {
                _listener.onFragmentRequestActionMode(
                    actionModeController,
                    actionModeName,
                    getString(R.string.title_selected, size)
                )
            }
        }
        .withReleaseCallback(object : ReleaseCallback {
            override fun onReleaseClick(release: ComicsRelease) {
                openEdit(view, release)
            }

            override fun onReleaseTogglePurchase(release: ComicsRelease) {
                _comicsDetailViewModel.updatePurchased(
                    release.release.id,
                    !release.release.purchased
                )
            }

            override fun onReleaseToggleOrder(release: ComicsRelease) {
                _comicsDetailViewModel.updateOrdered(
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

    private fun createActionModeController() =
        object : ActionModeController(R.menu.menu_releases_selected) {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val tracker = _adapter.selectionTracker
                when (item.itemId) {
                    R.id.purchaseReleases -> {
                        // TODO: considerare le multi release
                        _comicsDetailViewModel.togglePurchased(tracker.selection.toList())
                        return true
                    }
                    R.id.orderReleases -> {
                        // TODO: considerare le multi release
                        _comicsDetailViewModel.toggleOrdered(tracker.selection.toList())
                        return true
                    }
                    R.id.deleteReleases -> {
                        _comicsDetailViewModel.markAsRemoved(tracker.selection.toList())
                        tracker.clearSelection()
                        return true
                    }
                    R.id.shareReleases -> {
                        _comicsDetailViewModel.getShareableComicsReleases(tracker.selection.toList())
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

    private fun createComicsWithReleasesObserver(view: View): Observer<ComicsWithReleases?> {
        val context = requireContext()
        val txtInitial = view.findViewById<TextView>(R.id.txt_comics_initial)
        val txtName = view.findViewById<TextView>(R.id.txt_comics_name)
        val txtPublisher = view.findViewById<TextView>(R.id.txt_comics_publisher)
        val txtAuthors = view.findViewById<TextView>(R.id.txt_comics_authors)
        val txtNotes = view.findViewById<TextView>(R.id.txt_comics_notes)
        val txtLast = view.findViewById<TextView>(R.id.txt_comics_release_last)
        val txtNext = view.findViewById<TextView>(R.id.txt_comics_release_next)
        val txtMissing = view.findViewById<TextView>(R.id.txt_comics_release_missing)

        return Observer { comics ->
            _comics = comics!!
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

    private fun readNewReleases() {
        _listener.dismissSnackbar()
        _swipeRefreshLayout.isRefreshing = true

        _comicsDetailViewModel.readNewReleases(_comics)
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

    private fun onNewReleasesLoaded(count: Int) {
        _swipeRefreshLayout.isRefreshing = false
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

    private fun onNewReleasesLoadingError(error: Exception) {
        _swipeRefreshLayout.isRefreshing = false
        LogHelper.e(error, "Error reading new releases")
        Toast.makeText(
            requireContext(),
            R.string.refresh_release_error,
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun onMarkedAsRemoved(count: Int) {
        _listener.requestSnackbar(
            resources.getQuantityString(R.plurals.release_deleted, count, count),
            Constants.UNDO_TIMEOUT
        ) { canDelete: Boolean ->
            if (canDelete) {
                LogHelper.d("Delete removed releases")
                _comicsDetailViewModel.deleteRemoved()
            } else {
                LogHelper.d("Undo removed releases")
                _comicsDetailViewModel.undoRemoved()
            }
        }
    }
}