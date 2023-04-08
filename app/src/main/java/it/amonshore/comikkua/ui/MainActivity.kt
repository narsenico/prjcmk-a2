package it.amonshore.comikkua.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.LogHelperKt
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.ActivityMainBinding
import java.time.Duration

class MainActivity : AppCompatActivity(),
    OnNavigationFragmentListener,
    NavController.OnDestinationChangedListener {

    private val _viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var _navController: NavController

    private var _actionMode: ActionMode? = null
    private var _undoSnackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar)
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        _navController = navHostFragment.navController.apply {
            addOnDestinationChangedListener(this@MainActivity)
        }

        val appBarConfiguration = AppBarConfiguration
            .Builder(_navController.graph)
            .build()

        setupWithNavController(
            binding.bottomNav,
            _navController
        )

        setupWithNavController(
            binding.toolbar,
            _navController,
            appBarConfiguration
        )

        _viewModel.setupWorkers()

        // preparo le opzioni per Glide da poter usare in tutta l'app
        ImageHelper.prepareGlideOptions(this)

        if (BuildConfig.DEBUG) {
            Toast.makeText(
                this,
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            _navController.navigate(R.id.action_global_settingsFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFragmentRequestActionMode(
        name: String,
        title: CharSequence?,
        callback: ActionMode.Callback?
    ) {
        if (_actionMode?.tag != name || callback == null) {
            _actionMode?.finish()
            _actionMode = null
        }

        if (callback != null) {
            if (_actionMode == null) {
                _actionMode = startSupportActionMode(callback)?.apply {
                    tag = name
                }
            }
            _actionMode?.title = title
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        _actionMode = mode
    }

    override fun onSupportActionModeFinished(mode: ActionMode) {
        super.onSupportActionModeFinished(mode)
        _actionMode = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        LogHelperKt.d { "onDestinationChanged ${destination.label} (keyboard is closed here)" }
        supportActionBar?.setSubtitle(extractSubtitle(destination, arguments))
        window.hideKeyboard()
        _actionMode?.finish()
        resetUndo()
        binding.bottomNav.visibility = if (mustHideNavigation(destination, arguments)) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    @StringRes
    private fun extractSubtitle(destination: NavDestination, arguments: Bundle?): Int {
        // visto che non posso impostare il sottotitolo direttamente in nav_graph.xml
        //  lo passo come argomento, di tpo reference (string)
        val defValue = destination.arguments["subtitle"]?.defaultValue?.let {
            it as Int
        } ?: 0
        return arguments?.getInt("subtitle", defValue) ?: defValue
    }

    private fun mustHideNavigation(destination: NavDestination, arguments: Bundle?): Boolean {
        val defValue = destination.arguments["hideNavigation"]?.let {
            it.isDefaultValuePresent && it.defaultValue == java.lang.Boolean.TRUE
        } ?: false
        return arguments?.getBoolean("hideNavigation", defValue) ?: defValue
    }

    private fun prepareUndoSnackbar(message: String, tag: String, timeout: Duration): Snackbar {
        return Snackbar.make(binding.bottomNav, message, timeout.toSnackbarTimeout())
            .setAction(android.R.string.cancel) {
                LogHelperKt.d { "handleUndo undo with tag=$tag" }
                _viewModel.undoRemove(tag)
            }
            .onDismissed {
                LogHelperKt.d { "handleUndo finalize with tag=$tag" }
                _viewModel.finalizeRemove(tag)
            }
            .also {
                it.show()
            }
    }

    @Synchronized
    override fun handleUndo(message: String, tag: String, timeout: Duration) {
        resetUndo()

        LogHelperKt.d { "handleUndo with tag=$tag" }
        _undoSnackBar = prepareUndoSnackbar(message, tag, timeout).also { it.show() }
    }

    @Synchronized
    override fun resetUndo() {
        if (_undoSnackBar != null) {
            _undoSnackBar!!.dismiss()
            _undoSnackBar = null
        }
    }
}