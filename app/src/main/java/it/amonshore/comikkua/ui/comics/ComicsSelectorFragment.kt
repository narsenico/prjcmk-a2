package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.FragmentComicsSelectorBinding
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.ui.comics.adapter.AvailableComicsAdapter

//private const val BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics_selector.recycler.layout"
//private const val BUNDLE_COMICS_LAST_QUERY = "bundle.comics_selector.last.query"

class ComicsSelectorFragment : Fragment() {

    private val _viewModel: ComicsSelectorViewModel by viewModels()

    private lateinit var _listener: OnNavigationFragmentListener

    private var _binding: FragmentComicsSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComicsSelectorBinding.inflate(layoutInflater)
        binding.list.layoutManager = LinearLayoutManager(context)

        val adapter = AvailableComicsAdapter.create(
            recyclerView = binding.list,
            onAvailableComicsFollow = { comics -> _viewModel.followComics(comics) },
            onAvailableComicsMenuClick = { TODO() }
        )

        _viewModel.filteredNotFollowedComics
            .observe(viewLifecycleOwner) { data ->
                LogHelper.d { "not followed comics count=${data.size}" }
                adapter.submitList(data)
                binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            }

        _viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiComicsSelectorEvent.AvailableComicsLoading -> onAvailableComicsLoading()
                is UiComicsSelectorEvent.AvailableComicsLoaded -> onAvailableComicsLoaded(result.count)
                is UiComicsSelectorEvent.AvailableComicsError -> onAvailableComicsError()
            }
        }

        binding.txtSearch.doAfterTextChanged {
            _viewModel.filter = it?.toString() ?: ""
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
        //        // ripristino lo stato del layout (la posizione dello scroll)
//        // se non trovo savedInstanceState uso lo stato salvato nel view model
//        if (savedInstanceState != null) {
//            Objects.requireNonNull(mRecyclerView.getLayoutManager())
//                    .onRestoreInstanceState(savedInstanceState.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
//        } else if (mCmkWebViewModel != null) {
//            Objects.requireNonNull(mRecyclerView.getLayoutManager())
//                    .onRestoreInstanceState(mCmkWebViewModel.states.getParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT));
//        }
    }
//
//    override fun onPause() {
//        super.onPause()
//        //        if (mRecyclerView != null) {
////            // visto che Navigation ricrea il fragment ogni volta (!)
////            // salvo lo stato della lista nel view model in modo da poterlo recuperare se necessario
////            //  in onViewCreated
////            mCmkWebViewModel.states.putParcelable(BUNDLE_COMICS_RECYCLER_LAYOUT,
////                    Objects.requireNonNull(mRecyclerView.getLayoutManager()).onSaveInstanceState());
////        }
//    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _listener = if (context is OnNavigationFragmentListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnNavigationFragmentListener")
        }
    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        // salvo lo stato del layout (la posizione dello scroll)
//        outState.putParcelable(
//            BUNDLE_COMICS_RECYCLER_LAYOUT,
//            _recyclerView.layoutManager?.onSaveInstanceState()
//        )
//    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_comics_selector_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.refresh -> {
                        loadAvailableComics()
                        return true
                    }

                    R.id.deleteComics -> {
                        deleteAvailableComics()
                        return true
                    }

                    else -> return false
                }

            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun deleteAvailableComics() {
        _viewModel.deleteAvailableComics()
    }

    private fun loadAvailableComics() {
        _viewModel.loadAvailableComics()
    }

    private fun onAvailableComicsLoading() {
        Toast.makeText(
            requireContext(),
            getString(R.string.loading_available_comics),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onAvailableComicsLoaded(count: Int) {
        LogHelper.d { "New available comics: $count" }
        val msgId = if (count > 0)
            R.string.notification_available_comics_loaded
        else
            R.string.notification_available_comics_loaded_zero
        Toast.makeText(
            requireContext(),
            msgId,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onAvailableComicsError() {
        Toast.makeText(
            requireContext(),
            R.string.refresh_available_comics_error,
            Toast.LENGTH_SHORT
        ).show()
    }
}