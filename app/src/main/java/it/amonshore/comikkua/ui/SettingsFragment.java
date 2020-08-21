package it.amonshore.comikkua.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import it.amonshore.comikkua.LiveDataEx;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.BackupHelper;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsViewModel;

import static it.amonshore.comikkua.workers.ReleaseNotificationWorker.KEY_NOTIFICATIONS_ENABLED;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 456;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 684;
    private static final String BACKUP_FILE_NAME = "comikku_data.bck";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // solo per API >= 26
        final Preference prefNotifications = findPreference("notifications");
        // solo per API < 26
        final Preference prefNotificationEnabled = findPreference(KEY_NOTIFICATIONS_ENABLED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // nascondo "vecchie"
            prefNotificationEnabled.setVisible(false);
            // quella "nuova" è un link alla pagina dei settings dell'app gestita da Android
            prefNotifications.setOnPreferenceClickListener(preference -> {
                final String packageName = requireContext().getPackageName();

                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName));

                return true;
            });
        } else {
            // nascondo quella "nuova"
            prefNotifications.setVisible(false);
            // quella "vecchia" viene ascoltata in NotificationUtils
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_backup, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // nascondo il menu settings
        menu.findItem(R.id.settings).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.importBackup:
                importBackup();
                return true;
            case R.id.exportBackup:
                exportBackup();
                return true;
            case R.id.fixImages:
                fixImages();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importBackup();
            }
        }
    }

    private void importBackup() {
        final Context context = requireContext();
        final File bckFile = Utility.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME);
        LogHelper.d("Try import from " + bckFile);

        if (bckFile.exists()) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // non ho il permesso: contollo se posso mostrare il perché serve
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new android.app.AlertDialog.Builder(context, R.style.DialogTheme)
                            .setTitle(R.string.permission_import_backup_read_title)
                            .setMessage(R.string.permission_import_backup_read_explanation)
                            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE))
                            .show();
                } else {
                    // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app

                    final Snackbar snackbar = Snackbar.make(requireView(), R.string.permission_import_backup_read_denied,
                            Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.settings, v ->
                            startActivity(new Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.getPackageName(), null)))
                    );
                    snackbar.show();
                }

            } else {
                // ho il permesso: avvio la procedura di import
                new AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle("Import backup")
                        .setMessage("Do you want to import the backup?\nExisting data will be removed.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            final BackupHelper backupHelper = new BackupHelper(requireActivity().getApplication());
                            new ImportAsyncTask(context, backupHelper).execute(bckFile);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

            }

        } else {
            // TODO: file di backup non trovato
            new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("Backup not found")
                    .setMessage(String.format("Please make sure that the \"%s\" file is in Downloads folder.", BACKUP_FILE_NAME))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void exportBackup() {
        final Context context = requireContext();
        final File bckFile = Utility.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME);
        LogHelper.d("Try export to " + bckFile);

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // non ho il permesso: contollo se posso mostrare il perché serve
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new android.app.AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle(R.string.permission_export_backup_write_title)
                        .setMessage(R.string.permission_export_backup_write_explanation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE))
                        .show();
            } else {
                // non ho il permesso: l'utente può darlo accedendo direttamente ai settings dell'app

                final Snackbar snackbar = Snackbar.make(requireView(), R.string.permission_export_backup_write_denied,
                        Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.settings, v ->
                        startActivity(new Intent().setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.getPackageName(), null)))
                );
                snackbar.show();
            }

        } else {
            // ho il permesso: avvio la procedura di import
            new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("Export backup")
                    .setMessage("Do you want to backup data to file?\nExisting file will be overwritten.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        final BackupHelper backupHelper = new BackupHelper(requireActivity().getApplication());
                        new ExportAsyncTask(context, backupHelper).execute(bckFile);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void fixImages() {
        final ComicsViewModel comicsViewModel = new ViewModelProvider(requireActivity())
                .get(ComicsViewModel.class);
        final Context context = requireContext();

        LiveDataEx.observeOnce(comicsViewModel.getComics(), this, list -> {
            for (Comics comics : list) {
                if (comics.hasImage() && !ImageHelper.isValidImageFileName(comics.image, comics.id)) {
                    final File srcFile = new File(Uri.parse(comics.image).getPath());
                    final File dstFile = new File(context.getFilesDir(), ImageHelper.newImageFileName(comics.id));
                    LogHelper.d("======> move file from '%s' to '%s'", srcFile, dstFile);
                    if (srcFile.renameTo(dstFile)) {
                        comics.image = Uri.fromFile(dstFile).toString();
                        comicsViewModel.update(comics);
                    }
                }
            }
        });
    }

    private static class ImportAsyncTask extends AsyncTask<File, Void, Integer> {

        // TODO: mostrare dialog con barra progressione (infinita)

        private WeakReference<Context> mWeakContext;
        private BackupHelper mHeper;

        ImportAsyncTask(@NonNull Context context, @NonNull BackupHelper importer) {
            mWeakContext = new WeakReference<>(context);
            mHeper = importer;
        }

        @Override
        protected Integer doInBackground(File... files) {
            final Context context = mWeakContext.get();
            if (context != null) {
                return mHeper.importFromFile(context, files[0], true);
            }
            return BackupHelper.RETURN_ERR;
        }

        @Override
        protected void onPostExecute(Integer count) {
            final Context context = mWeakContext.get();
            if (context != null) {
                if (count == BackupHelper.RETURN_ERR) {
                    Toast.makeText(context, "There was a problem importing data from file.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Import completed.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class ExportAsyncTask extends AsyncTask<File, Void, Integer> {

        // TODO: mostrare dialog con barra progressione (infinita)

        private WeakReference<Context> mWeakContext;
        private BackupHelper mHelper;

        ExportAsyncTask(@NonNull Context context, @NonNull BackupHelper importer) {
            mWeakContext = new WeakReference<>(context);
            mHelper = importer;
        }

        @Override
        protected Integer doInBackground(File... files) {
            final Context context = mWeakContext.get();
            if (context == null) {
                return BackupHelper.RETURN_ERR;
            } else {
                return mHelper.exportToFile(context, files[0]);
            }
        }

        @Override
        protected void onPostExecute(Integer count) {
            final Context context = mWeakContext.get();
            if (context != null) {
                if (count == BackupHelper.RETURN_ERR) {
                    Toast.makeText(context, "There was a problem exporting data to file.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Export completed.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
