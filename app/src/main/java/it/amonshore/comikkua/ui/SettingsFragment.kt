package it.amonshore.comikkua.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import it.amonshore.comikkua.R

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

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        setupMenu()
//    }
//
//    private fun setupMenu() {
//        val menuHost: MenuHost = requireActivity()
//        menuHost.addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                menuInflater.inflate(R.menu.menu_backup, menu)
//
////                        // nascondo il menu settings
////        menu.findItem(R.id.settings).isVisible = false
//            }
//
//            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
//return when (menuItem.itemId) {
//            R.id.importBackup -> {
//                importBackup()
//                true
//            }
//            R.id.exportBackup -> {
//                exportBackup()
//                true
//            }
//            R.id.fixImages -> {
//                fixImages()
//                true
//            }
//            else -> false
//        }
//            }
//
//        })
//    }
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
//    private fun fixImages() {
//        val comicsViewModel = ViewModelProvider(requireActivity())
//            .get(ComicsViewModel::class.java)
//        val context = requireContext()
//        LiveDataEx.observeOnce(comicsViewModel.comics, this) { list: List<Comics> ->
//            for (comics in list) {
//                if (comics.hasImage() && !ImageHelper.isValidImageFileName(
//                        comics.image,
//                        comics.id
//                    )
//                ) {
//                    val srcFile = File(Uri.parse(comics.image).path)
//                    val dstFile = File(context.filesDir, ImageHelper.newImageFileName(comics.id))
//                    LogHelper.d("======> move file from '%s' to '%s'", srcFile, dstFile)
//                    if (srcFile.renameTo(dstFile)) {
//                        comics.image = Uri.fromFile(dstFile).toString()
//                        comicsViewModel.update(comics)
//                    }
//                }
//            }
//        }
//    }
//
//    private class ImportAsyncTask internal constructor(context: Context, importer: BackupHelper) :
//        AsyncTask<File?, Void?, Int>() {
//        // TODO: mostrare dialog con barra progressione (infinita)
//        private val mWeakContext: WeakReference<Context>
//        private val mHeper: BackupHelper
//
//        init {
//            mWeakContext = WeakReference(context)
//            mHeper = importer
//        }
//
//        protected override fun doInBackground(vararg files: File): Int {
//            val context = mWeakContext.get()
//            return if (context != null) {
//                mHeper.importFromFile(context, files[0], true)
//            } else BackupHelper.RETURN_ERR
//        }
//
//        override fun onPostExecute(count: Int) {
//            val context = mWeakContext.get()
//            if (context != null) {
//                if (count == BackupHelper.RETURN_ERR) {
//                    Toast.makeText(
//                        context,
//                        "There was a problem importing data from file.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                } else {
//                    Toast.makeText(context, "Import completed.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private class ExportAsyncTask internal constructor(context: Context, importer: BackupHelper) :
//        AsyncTask<File?, Void?, Int>() {
//        // TODO: mostrare dialog con barra progressione (infinita)
//        private val mWeakContext: WeakReference<Context>
//        private val mHelper: BackupHelper
//
//        init {
//            mWeakContext = WeakReference(context)
//            mHelper = importer
//        }
//
//        protected override fun doInBackground(vararg files: File): Int {
//            val context = mWeakContext.get()
//            return if (context == null) {
//                BackupHelper.RETURN_ERR
//            } else {
//                mHelper.exportToFile(context, files[0])
//            }
//        }
//
//        override fun onPostExecute(count: Int) {
//            val context = mWeakContext.get()
//            if (context != null) {
//                if (count == BackupHelper.RETURN_ERR) {
//                    Toast.makeText(
//                        context,
//                        "There was a problem exporting data to file.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                } else {
//                    Toast.makeText(context, "Export completed.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    companion object {
//        private const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456
//        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 684
//        private const val BACKUP_FILE_NAME = "comikku_data.bck"
//    }
}