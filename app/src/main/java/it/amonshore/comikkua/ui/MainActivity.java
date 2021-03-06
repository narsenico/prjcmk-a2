package it.amonshore.comikkua.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.web.FirebaseRepository;
import it.amonshore.comikkua.workers.ReleaseNotificationWorker;
import it.amonshore.comikkua.workers.UpdateReleasesWorker;

public class MainActivity extends AppCompatActivity implements
        OnNavigationFragmentListener,
        NavController.OnDestinationChangedListener {

    private ActionMode mActionMode;
    private BottomNavigationView mBottomNavigationView;
    private NavController mNavController;
    private Snackbar mSnackBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        mBottomNavigationView = findViewById(R.id.bottom_nav);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mNavController.addOnDestinationChangedListener(this);

        final AppBarConfiguration appBarConfiguration = new AppBarConfiguration
                .Builder(mNavController.getGraph())
                .build();

        NavigationUI.setupWithNavController(mBottomNavigationView,
                mNavController);

        NavigationUI.setupWithNavController(findViewById(R.id.toolbar),
                mNavController,
                appBarConfiguration);

        // parrebbe del tutto inutile visto che ho già configurato la navigazione con la toolbar qua sopra
        /*NavigationUI.setupActionBarWithNavController(this, navController,
                appBarConfiguration);*/

        if (BuildConfig.DEBUG && BuildConfig.VERSION_CODE < 2) {
            WorkManager.getInstance(this).cancelAllWork();
        }

        // preparo il worker per le notifiche sulle nuove uscite
        ReleaseNotificationWorker.setup(this, this);
        // preparo il worker per aggiornare le uscite da remoto
        UpdateReleasesWorker.setup(this, this);

        // preparo le opzioni per Glide da poter usare in tutta l'app
        ImageHelper.prepareGlideOptions(this);

        if (BuildConfig.DEBUG) {
            Toast.makeText(this,
                    String.format("%s (%s) - %s",
                            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                            FirebaseRepository.getProjectId()),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            mNavController.navigate(R.id.action_global_settingsFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // TODO: intercettare in qualche modo la selezione di un item dal fragment in modo da attivare l'actionMode?
    }

    @Override
    public void onFragmentRequestActionMode(@Nullable ActionMode.Callback callback, String name, CharSequence title) {
        if (mActionMode != null && (!name.equals(mActionMode.getTag()) || callback == null)) {
            mActionMode.finish();
            mActionMode = null;
        }

        if (callback != null) {
            if (mActionMode == null) {
                mActionMode = startSupportActionMode(callback);
                mActionMode.setTag(name);
            }
            mActionMode.setTitle(title);
        }
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        mActionMode = mode;
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        mActionMode = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull NavDestination destination,
                                     @Nullable Bundle arguments) {
        LogHelper.d("onDestinationChanged %s (keyboard is closed here)", destination.getLabel());
        // imposto il sottotitolo che viene passato come argomento della destinazione
        // i singoli fragment possono sovrascrivere il default chiamando direttamente onSubtitleChanged
        getSupportActionBar().setSubtitle(extractSubtitle(destination, arguments));
        // chiudo sempre la tasteira eventualmente aperta
        Utility.hideKeyboard(getWindow().getDecorView());
        // chiudo l'eventuale actionMode eventualmente aperta su richiesta del fragment
        if (mActionMode != null) {
            mActionMode.finish();
        }
        // chiudo l'eventuale snackbar
        dismissSnackbar();

        if (canHideNavigation(destination, arguments)) {
            mBottomNavigationView.setVisibility(View.GONE);
        } else {
            mBottomNavigationView.setVisibility(View.VISIBLE);
        }
    }

    @StringRes
    private int extractSubtitle(@NonNull NavDestination destination, @Nullable Bundle arguments) {
        // visto che non posso impostare il sottotitolo direttamente in nav_graph.xml
        //  lo passo come argomento, di tpo reference (string)
        final NavArgument arg = destination.getArguments().get("subtitle");
        int defValue = 0;
        if (arg != null) {
            final Object value = arg.getDefaultValue();
            if (value != null) {
                defValue = (Integer) value;
            }
        }
        return arguments == null ? defValue : arguments.getInt("subtitle", defValue);
    }

    private boolean canHideNavigation(@NonNull NavDestination destination, @Nullable Bundle arguments) {
        final NavArgument arg = destination.getArguments().get("hideNavigation");
        final boolean defValue = arg != null &&
                arg.isDefaultValuePresent() &&
                arg.getDefaultValue().equals(Boolean.TRUE);
        return arguments == null ? defValue : arguments.getBoolean("hideNavigation", defValue);
    }

    @Override
    public void requestSnackbar(@NonNull String text, int timeout, @NonNull ICallback<Boolean> callback) {
        dismissSnackbar();

        mSnackBar = Snackbar.make(findViewById(android.R.id.content), text, timeout)
                .setAction(android.R.string.cancel, v -> callback.onCallback(false))
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event == BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT ||
                                event == BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL) {
                            callback.onCallback(true);
                        }
                        LogHelper.d("SNACKBAR: dismissed event=%s", event);
                    }
                });
        // ancoro alla menu sul fondo in modo che non vi si sovrapponga
        if (mBottomNavigationView.getVisibility() == View.VISIBLE) {
            mSnackBar.setAnchorView(mBottomNavigationView);
        }
        LogHelper.d("SNACKBAR: show snack");
        mSnackBar.show();
    }

    @Override
    public void dismissSnackbar() {
        if (mSnackBar != null && mSnackBar.isShown()) {
            LogHelper.d("SNACKBAR: dismiss snack");
            mSnackBar.dismiss();
        }
    }
}
