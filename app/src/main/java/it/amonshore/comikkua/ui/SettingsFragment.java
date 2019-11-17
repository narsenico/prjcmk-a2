package it.amonshore.comikkua.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import it.amonshore.comikkua.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // su Android O mostro il link ai setting di sistema riguardo alle notifiche

        final Preference prefNotifications = findPreference("notifications");
        if (prefNotifications != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                prefNotifications.setOnPreferenceClickListener(preference -> {
                    final String packageName = requireContext().getPackageName();

                    startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName));

                    return true;
                });
            } else {
                prefNotifications.setVisible(false);
            }
        }
    }
}
