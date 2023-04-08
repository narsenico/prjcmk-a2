package it.amonshore.comikkua.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import it.amonshore.comikkua.R
import it.amonshore.comikkua.workers.BackupWorker

class SettingsFragment : PreferenceFragmentCompat() {

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
                    R.id.importBackup -> {
                        importBackup()
                        true
                    }
                    R.id.exportBackup -> {
                        exportBackup()
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun importBackup() {
        TODO()
    }

    fun exportBackup() {
        val workManager = WorkManager.getInstance(requireContext())
        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .build()
        workManager.enqueueUniqueWork(
            BackupWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )

        // TODO: mostrare risultato export (ci mette poco, non c'è bisogno di progress bar)
//        workManager.getWorkInfoByIdLiveData(request.id).observe(viewLifecycleOwner) {
//
//        }
    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
//            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                importBackup()
//            }
//        }
//    }
//
//    private fun importBackup() {
//        val context = requireContext()
//        val bckFile = Utility.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME)
//        LogHelper.d("Try import from $bckFile")
//        if (bckFile.exists()) {
//            if (ContextCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                )
//                != PackageManager.PERMISSION_GRANTED
//            ) {
//
//                // non ho il permesso: contollo se posso mostrare il perché serve
//                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                    AlertDialog.Builder(context, R.style.DialogTheme)
//                        .setTitle(R.string.permission_import_backup_read_title)
//                        .setMessage(R.string.permission_import_backup_read_explanation)
//                        .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
//                            requestPermissions(
//                                arrayOf(
//                                    Manifest.permission.READ_EXTERNAL_STORAGE
//                                ),
//                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
//                            )
//                        }
//                        .show()
//                } else {
//                    // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app
//                    val snackbar = Snackbar.make(
//                        requireView(), R.string.permission_import_backup_read_denied,
//                        Snackbar.LENGTH_LONG
//                    )
//                    snackbar.setAction(
//                        R.string.settings
//                    ) { v: View? ->
//                        startActivity(
//                            Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                                .setData(Uri.fromParts("package", context.packageName, null))
//                        )
//                    }
//                    snackbar.show()
//                }
//            } else {
//                // ho il permesso: avvio la procedura di import
//                androidx.appcompat.app.AlertDialog.Builder(context, R.style.DialogTheme)
//                    .setTitle("Import backup")
//                    .setMessage("Do you want to import the backup?\nExisting data will be removed.")
//                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
//                        val backupHelper = BackupHelper(requireActivity().application)
//                        ImportAsyncTask(context, backupHelper).execute(bckFile)
//                    }
//                    .setNegativeButton(android.R.string.cancel, null)
//                    .show()
//            }
//        } else {
//            // TODO: file di backup non trovato
//            androidx.appcompat.app.AlertDialog.Builder(context, R.style.DialogTheme)
//                .setTitle("Backup not found")
//                .setMessage(
//                    String.format(
//                        "Please make sure that the \"%s\" file is in Downloads folder.",
//                        BACKUP_FILE_NAME
//                    )
//                )
//                .setPositiveButton(android.R.string.ok, null)
//                .show()
//        }
//    }
//
//    private fun exportBackup() {
//        val context = requireContext()
//        val bckFile = Utility.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME)
//        LogHelper.d("Try export to $bckFile")
//        if (ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//
//            // non ho il permesso: contollo se posso mostrare il perché serve
//            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                AlertDialog.Builder(context, R.style.DialogTheme)
//                    .setTitle(R.string.permission_export_backup_write_title)
//                    .setMessage(R.string.permission_export_backup_write_explanation)
//                    .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
//                        requestPermissions(
//                            arrayOf(
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE
//                            ),
//                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
//                        )
//                    }
//                    .show()
//            } else {
//                // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app
//                val snackbar = Snackbar.make(
//                    requireView(), R.string.permission_export_backup_write_denied,
//                    Snackbar.LENGTH_LONG
//                )
//                snackbar.setAction(
//                    R.string.settings
//                ) { v: View? ->
//                    startActivity(
//                        Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                            .setData(Uri.fromParts("package", context.packageName, null))
//                    )
//                }
//                snackbar.show()
//            }
//        } else {
//            // ho il permesso: avvio la procedura di import
//            androidx.appcompat.app.AlertDialog.Builder(context, R.style.DialogTheme)
//                .setTitle("Export backup")
//                .setMessage("Do you want to backup data to file?\nExisting file will be overwritten.")
//                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
//                    val backupHelper = BackupHelper(requireActivity().application)
//                    ExportAsyncTask(context, backupHelper).execute(bckFile)
//                }
//                .setNegativeButton(android.R.string.cancel, null)
//                .show()
//        }
//    }
//

//
//    companion object {
//        private const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456
//        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 684
//        private const val BACKUP_FILE_NAME = "comikku_data.bck"
//    }
}