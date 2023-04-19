package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.databinding.FragmentComicsDetailBinding
import it.amonshore.comikkua.toHumanReadable
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.ImageHelperKt
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.ui.createActionModeCallback
import it.amonshore.comikkua.ui.releases.adapter.ReleaseAdapter
import it.amonshore.comikkua.ui.share

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

        val actionModeCallback = createActionModeCallback()
        _adapter = createReleaseAdapter(actionModeCallback)

        _viewModel.getComicsWithReleases(_comicsId)
            .observe(viewLifecycleOwner, createComicsWithReleasesObserver())

        _viewModel.getReleaseViewModelItems(_comicsId)
            .observe(viewLifecycleOwner) { items ->
                LogHelper.d { "release view model data changed size=${items.size}" }
                _adapter.submitList(items)
            }

        _viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiComicsDetailEvent.MarkedAsRemoved -> onMarkedAsRemoved(
                    result.count,
                    result.tag
                )

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

    private fun createActionModeCallback(): ActionMode.Callback {
        return createActionModeCallback(
            menuRes = R.menu.menu_releases_selected,
            onAction = { actionId: Int ->
                with(_adapter.selectionTracker) {
                    when (actionId) {
                        R.id.purchaseReleases -> {
                            // TODO: considerare le multi release
                            _viewModel.togglePurchased(selection.toList())
                            true
                        }

                        R.id.orderReleases -> {
                            // TODO: considerare le multi release
                            _viewModel.toggleOrdered(selection.toList())
                            true
                        }

                        R.id.deleteReleases -> {
                            _viewModel.markAsRemoved(selection.toList())
                            clearSelection()
                            true
                        }

                        R.id.shareReleases -> {
                            _viewModel.getShareableComicsReleases(selection.toList())
                            true
                        }

                        else -> false
                    }
                }
            },
            onDestroy = {
                // action mode distrutta (anche con BACK, che viene gestito internamente all'ActionMode e non può essere evitato)
                _adapter.selectionTracker.clearSelection()
            })
    }

    private fun createReleaseAdapter(
        actionModeCallback: ActionMode.Callback
    ) = ReleaseAdapter.create(
        recyclerView = binding.list,
        useLite = true,
        onSelectionChange = { size ->
            if (size == 0) {
                _listener.onFragmentRequestActionMode(ACTION_MODE_NAME)
            } else {
                _listener.onFragmentRequestActionMode(
                    ACTION_MODE_NAME,
                    getString(R.string.title_selected, size),
                    actionModeCallback
                )
            }
        },
        onReleaseClick = { release ->
            openEdit(binding.root, release)
        },
        onReleaseTogglePurchase = { release ->
            _viewModel.updatePurchased(
                release.release.id,
                !release.release.purchased
            )
        },
        onReleaseToggleOrder = { release ->
            _viewModel.updateOrdered(
                release.release.id,
                !release.release.ordered
            )
        }
    )

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
        val txtComicsSourceId = binding.txtComicsSourceId

        return Observer { comics ->
            _comics = comics
            txtName.text = comics.comics.name
            txtPublisher.text = comics.comics.publisher
            txtAuthors.text = comics.comics.authors
            txtNotes.text = comics.comics.notes
            if (comics.comics.image != null) {
                txtInitial.text = ""
                Glide.with(this)
                    .load(Uri.parse(comics.comics.image))
                    .apply(ImageHelperKt.getInstance(context).circleOptions)
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
                        nextRelease.date.toHumanReadable(context)
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

            binding.comics.imgSourced.visibility = if (comics.comics.isSourced) View.VISIBLE else View.GONE

            if (BuildConfig.DEBUG) {
                txtComicsSourceId.visibility = View.VISIBLE
                txtComicsSourceId.text = comics.comics.sourceId ?: "unsourced"
            }
        }
    }

    private fun loadNewReleases() {
        _listener.resetUndo()
        binding.swipeRefresh.isRefreshing = true
        _viewModel.loadNewReleases(_comics)
    }

    private fun onNewReleasesLoaded(count: Int, tag: String) {
        LogHelper.d { "New releases: $count with tag '$tag'" }
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
        requireActivity().share(releases)
    }

    private fun onMarkedAsRemoved(count: Int, tag: String) {
        _listener.handleUndo(
            resources.getQuantityString(R.plurals.release_deleted, count, count),
            tag
        )
    }
}