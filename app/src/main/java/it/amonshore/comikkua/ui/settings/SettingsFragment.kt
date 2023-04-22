package it.amonshore.comikkua.ui.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Data
import androidx.work.WorkInfo
import it.amonshore.comikkua.R
import it.amonshore.comikkua.ui.showCancellableDialog
import it.amonshore.comikkua.ui.showConfirmDialog
import it.amonshore.comikkua.ui.showErrorDialog

class SettingsFragment : PreferenceFragmentCompat() {

    private val _viewModel: SettingsViewModel by viewModels()
    private var _dialog: Dialog? = null

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("notifications")?.run {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
                true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        _viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiSettingsResult.ComicsDeleted -> onComicsDeleted()
                is UiSettingsResult.ComicsDeletingError -> onComicsDeletingError()
            }
        }

        _viewModel.backupStatus.observe(viewLifecycleOwner, ::onBackupStatusChanged)

        _viewModel.importOldDatabaseStatus.observe(viewLifecycleOwner, ::onImportOldDatabaseStatus)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_backup, menu)
                menu.findItem(R.id.settings).isVisible = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.importOldDatabase -> {
                        importOldDatabase()
                        true
                    }

                    R.id.exportBackup -> {
                        exportBackup()
                        true
                    }

                    R.id.clearDatabase -> {
                        clearDatabase()
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun exportBackup() {
        _viewModel.startBackupExport()
        _dialog = showCancellableDialog(
            activity = activity ?: throw IllegalStateException("Cannot create dialog"),
            title = getString(R.string.backup_title),
            message = getString(R.string.exporting_backup),
            onCancel = _viewModel::cancelBackupExport
        )
    }

    private fun importOldDatabase() {
        _viewModel.startOldDatabaseImport()
        _dialog = showCancellableDialog(
            activity = activity ?: throw IllegalStateException("Cannot create dialog"),
            title = getString(R.string.backup_title),
            message = getString(R.string.importing_old_database),
            onCancel = _viewModel::cancelOldDatabaseImport
        )
    }

    private fun clearDatabase() {
        showConfirmDialog(
            activity = activity ?: throw IllegalStateException("Cannot create dialog"),
            title = getString(R.string.confirm_title),
            message = getString(R.string.confirm_delete_comics_with_confirm_phrase, "DELETE"),
            confirmPhrase = "DELETE",
            onAccept = _viewModel::deleteAllComicsAndImages
        )
    }

    private fun onComicsDeleted() {
        Toast.makeText(context, R.string.comics_deleted, Toast.LENGTH_LONG).show()
    }

    private fun onComicsDeletingError() {
        showErrorDialog(
            activity = activity ?: throw IllegalStateException("Cannot create dialog"),
            title = getString(R.string.error),
            message = getString(R.string.comics_delete_error)
        )
    }


    private fun onBackupStatusChanged(workInfo: WorkInfo) {
        when (workInfo.state) {
            WorkInfo.State.CANCELLED, WorkInfo.State.SUCCEEDED -> _dialog?.dismiss()
            WorkInfo.State.FAILED, WorkInfo.State.BLOCKED -> {
                _dialog?.dismiss()
                showErrorDialog(
                    activity = activity ?: throw IllegalStateException("Cannot create dialog"),
                    title = getString(R.string.error),
                    message = getString(R.string.backup_error)
                )
            }

            else -> {}
        }
    }

    private fun onImportOldDatabaseStatus(workInfo: WorkInfo) {
        when (workInfo.state) {
            WorkInfo.State.CANCELLED, WorkInfo.State.SUCCEEDED -> _dialog?.dismiss()
            WorkInfo.State.FAILED, WorkInfo.State.BLOCKED -> {
                _dialog?.dismiss()
                showErrorDialog(
                    activity = activity ?: throw IllegalStateException("Cannot create dialog"),
                    title = getString(R.string.error),
                    message = getString(workInfo.outputData.getImportOldDatabaseErrorStringRes())
                )
            }

            else -> {}
        }
    }

    private fun Data.getImportOldDatabaseErrorStringRes() = when (getString("reason")) {
        "connection-error" -> R.string.import_old_database_connection_error
        else -> R.string.import_old_database_error
    }
}