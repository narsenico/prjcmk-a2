package it.amonshore.comikkua.ui.comics;

import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.R;

/**
 * Helper per facilitare la visualizzazione di un dialog con scelta multipla.
 * Selezionata un scelta, il dialog viene dismesso e quindi chiamata la callback.
 * Meglio dismetterlo che semplicemente chiuderlo con hide per evitare vari casi dove il dialog
 * non funziona più correttamente, vedi a causa della ricreazione dell'activity ma anche mostrare
 * il dialog dello share crea problemi.
 */
class ComicsBottomSheetDialogHelper {

    static void show(@NonNull FragmentActivity activity, @NonNull ICallback<Integer> callback) {
        final View sheetView = activity.getLayoutInflater().inflate(R.layout.bottomsheet_comics, null);
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
        bottomSheetDialog.setContentView(sheetView);
        sheetView.findViewById(R.id.createNewRelease).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.createNewRelease));
        sheetView.findViewById(R.id.share).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.share));
        sheetView.findViewById(R.id.deleteComics).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.deleteComics));
        sheetView.findViewById(R.id.search_starshop).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.search_starshop));
        sheetView.findViewById(R.id.search_amazon).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.search_amazon));
        sheetView.findViewById(R.id.search_animeclick).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.search_animeclick));
        sheetView.findViewById(R.id.search_popstore).setOnClickListener(v -> notifyAndClose(bottomSheetDialog, callback, R.id.search_popstore));
//            bottomSheetDialog.setOnDismissListener(dialog -> LogHelper.d("BottomSheetDialog dismiss"));
        bottomSheetDialog.show();
    }

    private static void notifyAndClose(BottomSheetDialog dialog, @NonNull ICallback<Integer> callback, final int id) {
        dialog.dismiss();
        callback.onCallback(id);
    }
}
