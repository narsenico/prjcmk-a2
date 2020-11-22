package it.amonshore.comikkua.ui;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import it.amonshore.comikkua.ICallback;

/**
 * Helper per facilitare la visualizzazione di un dialog con scelta multipla.
 * Selezionata un scelta, il dialog viene dismesso e quindi chiamata la callback.
 * Meglio dismetterlo che semplicemente chiuderlo con hide per evitare vari casi dove il dialog
 * non funziona più correttamente, vedi a causa della ricreazione dell'activity ma anche mostrare
 * il dialog dello share crea problemi.
 */
public class BottomSheetDialogHelper {

    private final static String CHILD_TAG = "BottomSheetDialogHelper";

    /**
     * Mostra un {@link BottomSheetDialog} al quale ogni elemento figlio con tag "BottomSheetDialogHelper"
     * è associato un listener all'evento click che rimanda alla calback.
     *
     * @param activity Acitivy di riferimento
     * @param resource Layout
     * @param callback Callback chiamata al click di un elemento
     */
    public static void show(@NonNull FragmentActivity activity, @LayoutRes int resource, @NonNull ICallback<Integer> callback) {
        final ViewGroup sheetView = (ViewGroup) activity.getLayoutInflater().inflate(resource, null);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        bottomSheetDialog.setContentView(sheetView);

        final int childCount = sheetView.getChildCount();
        View child;
        for (int ii = 0; ii < childCount; ii++) {
            child = sheetView.getChildAt(ii);
            if (CHILD_TAG.equals(child.getTag())) {
                child.setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, v.getId()));
            }
        }
        bottomSheetDialog.show();
    }

    private static void notifyAndClose(BottomSheetDialog dialog, @NonNull ICallback<Integer> callback, final int id) {
        dialog.dismiss();
        callback.onCallback(id);
    }
}
