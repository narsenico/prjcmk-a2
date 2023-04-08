package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.databinding.FragmentComicsBinding
import it.amonshore.comikkua.parcelable
import it.amonshore.comikkua.ui.*

//private const val BUNDLE_COMICS_LAST_QUERY = "bundle.comics.last.query"
private val ACTION_MODE_NAME = ComicsFragment::class.java.simpleName + "_actionMode"
private const val BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics.recycler.layout"

class ComicsFragment : Fragment() {

    private val _viewModel: ComicsViewModel by viewModels()

    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _adapter: PagedListComicsAdapter

    private var _binding: FragmentComicsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComicsBinding.inflate(layoutInflater, container, false)
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        val actionModeController = createActionModeController()
        _adapter = createComicsAdapter(actionModeController)

        _viewModel.comicsWithReleasesPaged
            .observe(viewLifecycleOwner) { data ->
                LogHelper.d("comics viewmodel paging data changed")
                _adapter.submitData(lifecycle, data)
            }

        _viewModel.events
            .observe(viewLifecycleOwner) { result ->
                when (result) {
                    is UiComicsEvent.MarkedAsRemoved -> onMarkedAsRemoved(result.count, result.tag)
                }
            }

//        _comicsViewModelKt.useLastFilter()
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
                    BUNDLE_COMICS_RECYCLER_LAYOUT
                )
            )
        } else {
            binding.list.layoutManager?.onRestoreInstanceState(
                _viewModel.states.parcelable(
                    BUNDLE_COMICS_RECYCLER_LAYOUT
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
            BUNDLE_COMICS_RECYCLER_LAYOUT,
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
                BUNDLE_COMICS_RECYCLER_LAYOUT,
                binding.list.layoutManager?.onSaveInstanceState()
            )
        }
    }

    private fun createActionModeController() =
        object : ActionModeController(R.menu.menu_comics_selected) {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId == R.id.deleteComics) {
                    val tracker = _adapter.selectionTracker
                    val ids = tracker.selection.toList()
                    _viewModel.markAsRemoved(ids)
                    tracker.clearSelection()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // action mode distrutta (anche con BACK, che viene gestito internamente all'ActionMode e non può essere evitato)
                _adapter.selectionTracker.clearSelection()
                super.onDestroyActionMode(mode)
            }
        }

    /**
     * TODO: selection changed viene scatenato due volte all'inizio:
     *  questo perché il tracker permette la selezione di più item trascinando la selezione
     */
    private fun createComicsAdapter(
        actionModeController: ActionModeController
    ) = PagedListComicsAdapter.Builder(binding.list)
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
        .withComicsCallback(object : PagedListComicsAdapter.ComicsCallback {
            override fun onComicsClick(comics: ComicsWithReleases) {
                val directions: NavDirections = ComicsFragmentDirections
                    .actionDestComicsToComicsDetailFragment(comics.comics.id)
                findNavController(requireView()).navigate(directions)
            }

            override fun onComicsMenuSelected(comics: ComicsWithReleases) {
                BottomSheetDialogHelper.show(
                    requireActivity(), R.layout.bottomsheet_comics,
                    comics.comics.toSharable()
                ) { id: Int ->
                    when (id) {
                        R.id.createNewRelease -> {
                            openNewRelease(binding.root, comics)
                        }
                        R.id.share -> {
                            requireActivity().share(comics.comics)
                        }
                        R.id.deleteComics -> {
                            val ids = listOf(comics.comics.id)
                            _viewModel.markAsRemoved(ids)
                        }
                        R.id.search_starshop -> {
                            requireActivity().shareOnStarShop(comics.comics)
                        }
                        R.id.search_amazon -> {
                            requireActivity().shareOnAmazon(comics.comics)
                        }
                        R.id.search_popstore -> {
                            requireActivity().shareOnPopStore(comics.comics)
                        }
                        R.id.search_google -> {
                            requireActivity().shareOnGoogle(comics.comics)
                        }
                    }
                }
            }
        })
        .withGlide(Glide.with(this))
        .build()

    private fun openNewRelease(view: View, comics: ComicsWithReleases) {
        val directions: NavDirections = ComicsFragmentDirections
            .actionDestComicFragmentToReleaseEditFragment(comics.comics.id)
            .setSubtitle(R.string.title_release_create)
        findNavController(view).navigate(directions)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_comics_fragment, menu)

                val searchItem = menu.findItem(R.id.searchComics)
                val searchView = searchItem.actionView as SearchView

                // onQueryTextSubmit non viene scatenato su query vuota, quindi non posso caricare tutti i dati

                // TODO: al cambio di configurazione (es orientamento) la query viene persa
                //  è il viewModel che deve tenere memorizzata l'ultima query,
                //  qua al massimo devo apri la SearchView e inizializzarla con l'ultima query da viewModel se non vuota
                if (!TextUtils.isEmpty(_viewModel.lastFilter)) {
                    // lo faccio prima di aver impostato i listener così non scateno più nulla
                    searchItem.expandActionView()
                    searchView.setQuery(_viewModel.lastFilter, false)
                    searchView.clearFocus()

                    // TODO: non funziona sulla navigazione (es apro il dettaglio di un comics filtrato),
                    //  perché viene chiusa la searchView e scatenato onQueryTextChange con testo vuoto
                    //  che mi serve così perché quando volutamente la chiudo voglio che il filtro venga pulito
                }

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        _viewModel.filter = newText // TODO: ok ma aggiungere debounce
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.createNewComics -> {
                        val directions: NavDirections = ComicsFragmentDirections
                            .actionDestComicsFragmentToComicsEditFragment()
                            .setComicsId(Comics.NEW_COMICS_ID)
                            .setSubtitle(R.string.title_comics_create)
                        findNavController(requireView()).navigate(directions)
                        return true
                    }
                    R.id.openComicsSelector -> {
                        val directions: NavDirections = ComicsFragmentDirections
                            .actionDestComicFragmentToComicsSelectorFragment()
                        findNavController(requireView()).navigate(directions)
                        return true
                    }
                    else -> return false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onMarkedAsRemoved(count: Int, tag: String) {
        _listener.handleUndo(
            resources.getQuantityString(R.plurals.comics_deleted, count, count),
            tag
        )
    }
}