package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.bumptech.glide.Glide
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.databinding.FragmentComicsSelectorBinding
import it.amonshore.comikkua.ui.OnNavigationFragmentListener
import it.amonshore.comikkua.workers.RefreshComicsWorker

/**
 * Mostra tutti i comics disponibili per l'auto aggiornamento.
 * Serve per far selezionare all'utnete un nuovo comics da inserire nel proprio elenco.
 */
class ComicsSelectorFragment : Fragment() {

    private val _comicsSelectorViewModel: ComicsSelectorViewModel by viewModels()
    private lateinit var _listener: OnNavigationFragmentListener
    private lateinit var _binding: FragmentComicsSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentComicsSelectorBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding.list.layoutManager = LinearLayoutManager(context)

        val adapter = AvailableComicsAdapter.Builder(_binding.list)
            .withComicsCallback(object : AvailableComicsAdapter.ComicsCallback {
                override fun onComicsFollowed(comics: AvailableComics) {
                    _comicsSelectorViewModel.followComics(comics)
                }

                override fun onComicsMenuSelected(comics: AvailableComics) {
                    TODO("Implementare menu contestuale")
                }
            })
            .withGlide(Glide.with(this))
            .build()

        _comicsSelectorViewModel.getNotFollowedComics()
            .observe(viewLifecycleOwner) { data ->
                LogHelper.d("not followed comics count=${data.size}")
                adapter.submitList(data)
                _binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            }

        _binding.txtSearch.doAfterTextChanged {
            _comicsSelectorViewModel.filter = it?.toString() ?: ""
        }

        return _binding.root
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
                if (menuItem.itemId == R.id.refresh) {
                    performUpdate()
                    return true
                }

                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // TODO: leggere direttamente da web senza usare worker
    private fun performUpdate() {
        val request: WorkRequest =
            OneTimeWorkRequest.Builder(RefreshComicsWorker::class.java).build()
        val workManager = WorkManager.getInstance(requireContext())
        workManager.enqueue(request)
        workManager.getWorkInfoByIdLiveData(request.id)
            .observe(viewLifecycleOwner) { workInfo: WorkInfo? ->
                if (workInfo != null) {
                    LogHelper.d("Updating available comics state=%s", workInfo.state)
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val count =
                                workInfo.outputData.getInt(RefreshComicsWorker.REFRESHING_COUNT, 0)
                            Toast.makeText(
                                requireContext(),
                                "Refreshing $count",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        WorkInfo.State.FAILED -> Toast.makeText(
                            requireContext(),
                            "Refreshing failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        WorkInfo.State.BLOCKED, WorkInfo.State.CANCELLED, WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {}
                    }
                }
            }
    }

    companion object {
        private const val BUNDLE_COMICS_RECYCLER_LAYOUT = "bundle.comics_selector.recycler.layout"
        private const val BUNDLE_COMICS_LAST_QUERY = "bundle.comics_selector.last.query"
    }
}