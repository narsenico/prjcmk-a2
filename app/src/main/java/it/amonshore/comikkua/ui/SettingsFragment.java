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

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.BackupImporter;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.comics.ComicsRepository;
import it.amonshore.comikkua.data.comics.ComicsViewModel;

public class SettingsFragment extends PreferenceFragmentCompat {

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
        final Preference prefNotificationEnabled = findPreference("notifications_enabled");

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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void importBackup() {
        final Context context = requireContext();
        final File bckFile = Utility.getExternalFile(Environment.DIRECTORY_DOWNLOADS, BACKUP_FILE_NAME);
        LogHelper.d("Try import from " + bckFile);

        if (bckFile.exists()) {
            new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("Import backup")
                    .setMessage("Do you want to import the backup?\nExisting data will be removed.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        final BackupImporter importer = new BackupImporter(requireActivity().getApplication());
                        new ImportAsyncTask(context, importer).execute(bckFile);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        } else {
            // TODO: file di backup non trovato
            new AlertDialog.Builder(context, R.style.DialogTheme)
                    .setTitle("Backup not found")
                    .setMessage(String.format("Please make sure that the \"%s\" file is in Downloads folder.", BACKUP_FILE_NAME))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private static class ImportAsyncTask extends AsyncTask<File, Void, Integer> {

        // TODO: mostrare dialog con barra progressione (infinita)

        private WeakReference<Context> mWeakContext;
        private BackupImporter mImporter;

        ImportAsyncTask(@NonNull Context context, @NonNull BackupImporter importer) {
            mWeakContext = new WeakReference<>(context);
            mImporter = importer;
        }

        @Override
        protected Integer doInBackground(File... files) {
            final Context context = mWeakContext.get();
            if (context != null) {
                Glide.get(context).clearDiskCache();
                // elimino singolarmente tutti i file creati durante il crop dell'immagine dei comics
                final File[] croppedFiles = context.getCacheDir().listFiles((dir, name) -> name.startsWith("cropped"));
                for (File file : croppedFiles) {
                    if (!file.delete()) {
                        LogHelper.w("Cannot delete " + file);
                    }
                }
            }
            return mImporter.importFromFile(files[0], true);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            final Context context = mWeakContext.get();
            if (context != null) {
                Toast.makeText(context, "Import completed", Toast.LENGTH_LONG).show();
            }
        }
    }

}
