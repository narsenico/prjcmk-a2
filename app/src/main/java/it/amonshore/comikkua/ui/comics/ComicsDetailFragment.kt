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
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import it.amonshore.comikkua.*
import it.amonshore.comikkua.data.comics.ComicsViewModel
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.ReleaseViewModel
import it.amonshore.comikkua.ui.*
import it.amonshore.comikkua.ui.releases.ReleaseAdapter
import it.amonshore.comikkua.ui.releases.ReleaseAdapter.ReleaseCallback

class ComicsDetailFragment : Fragment() {

    private val _comicsViewModel: ComicsViewModel by viewModels()
    private val _releaseViewModel: ReleaseViewModel by viewModels()

    private val _comicsId: Long by lazy {
        ComicsDetailFragmentArgs.fromBundle(requireArguments()).comicsId
    }

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var _adapter: ReleaseAdapter
    private lateinit var mComics: ComicsWithReleases

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
        val actionModeName = javaClass.simpleName + "_actionMode"
        val context = requireContext()

        // lo stesso nome della transizione è stato assegnato alla view di partenza
        //  il nome deve essere univoco altrimenti il meccanismo non saprebbe quali viste animare
        view.findViewById<View>(R.id.comics).transitionName = "comics_tx_$_comicsId"

        _swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh).apply {
            setOnRefreshListener(::performUpdate)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(context)
        }
        val txtInitial = view.findViewById<TextView>(R.id.txt_comics_initial)
        val txtName = view.findViewById<TextView>(R.id.txt_comics_name)
        val txtPublisher = view.findViewById<TextView>(R.id.txt_comics_publisher)
        val txtAuthors = view.findViewById<TextView>(R.id.txt_comics_authors)
        val txtNotes = view.findViewById<TextView>(R.id.txt_comics_notes)
        val txtLast = view.findViewById<TextView>(R.id.txt_comics_release_last)
        val txtNext = view.findViewById<TextView>(R.id.txt_comics_release_next)
        val txtMissing = view.findViewById<TextView>(R.id.txt_comics_release_missing)

        val actionModeController: ActionModeController =
            object : ActionModeController(R.menu.menu_releases_selected) {
                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    val tracker = _adapter.selectionTracker
                    when (item.itemId) {
                        R.id.purchaseReleases -> {
                            if (tracker.hasSelection()) {
                                // TODO: considerare le multi release
                                _releaseViewModel.togglePurchased(tracker.selection)
                            }
                            // mantengo la selezione
                            return true
                        }
                        R.id.orderReleases -> {
                            if (tracker.hasSelection()) {
                                // TODO: considerare le multi release
                                _releaseViewModel.toggleOrdered(tracker.selection)
                            }
                            // mantengo la selezione
                            return true
                        }
                        R.id.deleteReleases -> {
                            if (tracker.hasSelection()) {
                                // prima elimino eventuali release ancora in fase di undo
                                _releaseViewModel.deleteRemoved()
                                _releaseViewModel.remove(tracker.selection) { count: Int ->
                                    showUndo(
                                        count
                                    )
                                }
                            }
                            tracker.clearSelection()
                            return true
                        }
                        R.id.shareReleases -> {
                            LiveDataEx.observeOnce(
                                _releaseViewModel.getComicsReleases(tracker.selection),
                                viewLifecycleOwner
                            ) { items: List<ComicsRelease?>? ->
                                ShareHelper.shareReleases(
                                    requireActivity(),
                                    items!!
                                )
                            }
                            // mantengo la selezione
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

        _adapter = ReleaseAdapter.Builder(recyclerView)
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
                    _releaseViewModel.updatePurchased(
                        !release.release.purchased,
                        release.release.id
                    )
                }

                override fun onReleaseToggleOrder(release: ComicsRelease) {
                    _releaseViewModel.updateOrdered(!release.release.ordered, release.release.id)
                }

                override fun onReleaseMenuSelected(release: ComicsRelease) {
                    // non gestito
                }
            }) // uso la versione "lite" con il layout per gli item più compatta
            .useLite()
            .build()

        _comicsViewModel.getComicsWithReleases(_comicsId)
            .observe(viewLifecycleOwner) { comics ->
                mComics = comics ?: throw NullPointerException("comics cannot be null")
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

        _releaseViewModel.getReleaseViewModelItems(_comicsId)
            .observe(viewLifecycleOwner) { items ->
                LogHelper.d("release viewmodel data changed size=${items.size}")
                _adapter.submitList(items)
            }

        // ripristino la selezione salvata in onSaveInstanceState
        _adapter.selectionTracker.onRestoreInstanceState(savedInstanceState)
        return view
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
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

    private fun performUpdate() {
        _listener.dismissSnackbar()
        _swipeRefreshLayout.isRefreshing = true

        // cerco tutte le nuove release e le aggiungo direttamente
        _releaseViewModel.getNewReleases(mComics)
            .observe(viewLifecycleOwner) { releases ->
                if (releases != null) {
                    val size = releases.size
                    if (size > 0) {
                        _releaseViewModel.insert(*releases.toTypedArray())
                        Toast.makeText(
                            requireContext(),
                            resources.getQuantityString(
                                R.plurals.auto_update_available_message,
                                size,
                                size
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
                } else {
                    // in realtà è null in caso di errore
                    Toast.makeText(requireContext(), R.string.auto_update_zero, Toast.LENGTH_SHORT)
                        .show()
                }
                _swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun openEdit(view: View, release: ComicsRelease) {
        val directions: NavDirections = ComicsDetailFragmentDirections
            .actionDestComicsDetailFragmentToReleaseEditFragment(release.comics.id)
            .setReleaseId(release.release.id)
        findNavController(view).navigate(directions)
    }

    private fun showUndo(count: Int) {
        _listener.requestSnackbar(
            resources.getQuantityString(R.plurals.release_deleted, count, count),
            Constants.UNDO_TIMEOUT
        ) { canDelete: Boolean ->
            if (canDelete) {
                LogHelper.d("Delete removed releases")
                _releaseViewModel.deleteRemoved()
            } else {
                LogHelper.d("Undo removed releases")
                _releaseViewModel.undoRemoved()
            }
        }
    }
}