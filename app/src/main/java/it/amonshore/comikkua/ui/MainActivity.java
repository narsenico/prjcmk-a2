package it.amonshore.comikkua.ui;

import android.net.Uri;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.view.ActionMode;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;

public class MainActivity extends AppCompatActivity implements
        OnNavigationFragmentListener,
        NavController.OnDestinationChangedListener {

    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navController.addOnDestinationChangedListener(this);

        final AppBarConfiguration appBarConfiguration = new AppBarConfiguration
                .Builder(navController.getGraph())
                .build();

        NavigationUI.setupWithNavController((BottomNavigationView)findViewById(R.id.bottom_nav),
                navController);

        NavigationUI.setupWithNavController(findViewById(R.id.toolbar),
                navController,
                appBarConfiguration);

        // parrebbe del tutto inutile visto che ho già configurato la navigazione con la toolbar qua sopra
        /*NavigationUI.setupActionBarWithNavController(this, navController,
                appBarConfiguration);*/
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
    public void onDestinationChanged(@NonNull NavController controller,
                                     @NonNull NavDestination destination,
                                     @Nullable Bundle arguments) {
        LogHelper.d("onDestinationChanged %s (keyboard is closed here)", destination.getLabel());
        // imposto il sottotitolo che viene passato come argomento della destinazione
        getSupportActionBar().setSubtitle(extractSubtitle(destination));
        // chiudo sempre la tasteira eventualmente aperta
        Utility.hideKeyboard(getWindow().getDecorView());
        // chiudo l'eventuale actionMode eventualmente aperta su richiesta del fragment
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private String extractSubtitle(@NonNull NavDestination destination) {
        // visto che non posso impostare il sottotitolo direttamente in nav_graph.xml
        //  lo passo come argomento, di tpo reference (string)
        final NavArgument arg = destination.getArguments().get("subtitle");
        if (arg != null) {
            final Object value = arg.getDefaultValue();
            if (value != null) {
                return getString((Integer) value);
            }
        }
        return null;
    }

}
