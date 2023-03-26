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
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsViewModelKt
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.databinding.FragmentComicsBinding
import it.amonshore.comikkua.parcelable
import it.amonshore.comikkua.ui.*

private const val BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics.recycler.layout"
//private const val BUNDLE_COMICS_LAST_QUERY = "bundle.comics.last.query"

class ComicsFragment : Fragment() {

    private val _comicsViewModelKt: ComicsViewModelKt by viewModels()

    private lateinit var _binding: FragmentComicsBinding
    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _adapter: PagedListComicsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentComicsBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding.list.layoutManager = LinearLayoutManager(requireContext())

        val actionModeName = javaClass.simpleName + "_actionMode"
        val actionModeController = createActionModeController()
        _adapter = createComicsAdapter(actionModeName, actionModeController)

        // mi metto in ascolto del cambiamto dei dati (via LiveData) e aggiorno l'adapter di conseguenza
        _comicsViewModelKt.getComicsWithReleasesPaged()
            .observe(viewLifecycleOwner) { data ->
                LogHelper.d("comics viewmodel paging data changed")
                _adapter.submitData(lifecycle, data)
            }

        // ripristino la selezione salvata in onSaveInstanceState
        _adapter.selectionTracker.onRestoreInstanceState(savedInstanceState)
        _comicsViewModelKt.useLastFilter()

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()

        // ripristino lo stato del layout (la posizione dello scroll)
        // se non trovo savedInstanceState uso lo stato salvato nel view model
        if (savedInstanceState != null) {
            _binding.list.layoutManager?.onRestoreInstanceState(
                savedInstanceState.parcelable(
                    BUNDLE_COMICS_RECYCLER_LAYOUT
                )
            )
        } else {
            _binding.list.layoutManager?.onRestoreInstanceState(
                _comicsViewModelKt.states.parcelable(
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
        _comicsViewModelKt.states.putParcelable(
            BUNDLE_COMICS_RECYCLER_LAYOUT,
            _binding.list.layoutManager?.onSaveInstanceState()
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
            BUNDLE_COMICS_RECYCLER_LAYOUT,
            _binding.list.layoutManager?.onSaveInstanceState()
        )
    }

    private fun createActionModeController() =
        object : ActionModeController(R.menu.menu_comics_selected) {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId == R.id.deleteComics) {
                    val tracker = _adapter.selectionTracker
                    val ids = tracker.selection.toList()
                    _comicsViewModelKt.markAsRemoved(ids) { count ->
                        showUndo(
                            ids,
                            count
                        )
                    }
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
        actionModeName: String,
        actionModeController: ActionModeController
    ) = PagedListComicsAdapter.Builder(_binding.list)
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
        .withComicsCallback(object : PagedListComicsAdapter.ComicsCallback {
            override fun onComicsClick(comics: ComicsWithReleases) {
                val directions: NavDirections = ComicsFragmentDirections
                    .actionDestComicsToComicsDetailFragment(comics.comics.id)
                findNavController(requireView()).navigate(directions)
            }

            override fun onComicsMenuSelected(comics: ComicsWithReleases) {
                BottomSheetDialogHelper.show(
                    requireActivity(), R.layout.bottomsheet_comics,
                    ShareHelper.formatComics(comics.comics)
                ) { id: Int ->
                    when (id) {
                        R.id.createNewRelease -> {
                            openNewRelease(_binding.root, comics)
                        }
                        R.id.share -> {
                            ShareHelper.shareComics(requireActivity(), comics.comics)
                        }
                        R.id.deleteComics -> {
                            val ids = listOf(comics.comics.id)
                            _comicsViewModelKt.markAsRemoved(ids) { count ->
                                showUndo(
                                    ids,
                                    count
                                )
                            }
                        }
                        R.id.search_starshop -> {
                            ShareHelper.shareOnStarShop(requireActivity(), comics.comics)
                        }
                        R.id.search_amazon -> {
                            ShareHelper.shareOnAmazon(requireActivity(), comics.comics)
                        }
                        R.id.search_popstore -> {
                            ShareHelper.shareOnPopStore(requireActivity(), comics.comics)
                        }
                        R.id.search_google -> {
                            ShareHelper.shareOnGoogle(requireActivity(), comics.comics)
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
                if (!TextUtils.isEmpty(_comicsViewModelKt.lastFilter)) {
                    // lo faccio prima di aver impostato i listener così non scateno più nulla
                    searchItem.expandActionView()
                    searchView.setQuery(_comicsViewModelKt.lastFilter, false)
                    searchView.clearFocus()

                    // TODO: non funziona sulla navigazione (es apro il dettaglio di un comics filtrato),
                    //  perché viene chiusa la searchView e scatenato onQueryTextChange con testo vuoto
                    //  che mi serve così perché quando volutamente la chiudo voglio che il filtro venga pulito
                }
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        LogHelper.d("onQueryTextSubmit")
                        return true
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        LogHelper.d("filterName change $newText")
                        _comicsViewModelKt.filter = newText // TODO: ok ma aggiungere debounce
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

    private fun showUndo(ids: List<Long>, count: Int) {
        // uso il contesto applicativo per eliminare le immagini perché il contesto del fragment
        // non è più valido se navigo da un'altra parte
        val context = requireContext().applicationContext
        _listener.requestSnackbar(
            resources.getQuantityString(R.plurals.comics_deleted, count, count),
            Constants.UNDO_TIMEOUT
        ) { canDelete: Boolean ->
            if (canDelete) {
                LogHelper.d("Delete removed comics")
                _comicsViewModelKt.deleteRemoved()
                // elimino anche le immagini
                // mi fido del fatto che ids contenga esattamente i comics rimossi con l'istruzione sopra
                ImageHelper.deleteImageFiles(context, *ids.toTypedArray())
            } else {
                LogHelper.d("Undo removed comics")
                _comicsViewModelKt.undoRemoved()
            }
        }
    }
}