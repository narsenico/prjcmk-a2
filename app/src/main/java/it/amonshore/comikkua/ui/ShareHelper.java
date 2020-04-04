package it.amonshore.comikkua.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.MultiRelease;
import it.amonshore.comikkua.data.release.Release;

public class ShareHelper {

    private static final String TEXT_PLAIN = "text/plain";

    private static String formatRelease(@NonNull Context context, @NonNull Comics comics, @NonNull Release release) {
        if (release.hasDate()) {
            return context.getString(R.string.share_release,
                    comics.name,
                    release.number,
                    DateFormatterHelper.toHumanReadable(context, release.date, DateFormatterHelper.STYLE_SHORT),
                    release.hasNotes() ? release.notes : comics.notes);
        } else {
            return context.getString(R.string.share_release_nodate,
                    comics.name,
                    release.number,
                    release.hasNotes() ? release.notes : comics.notes);
        }
    }

    public static void shareRelease(@NonNull Activity activity, @NonNull ComicsRelease release) {
        if (release instanceof MultiRelease) {
            shareRelease(activity, (MultiRelease) release);
        } else {
            shareText(activity, formatRelease(activity, release.comics, release.release));
        }
    }

    public static void shareRelease(@NonNull Activity activity, @NonNull MultiRelease release) {
        final String[] rows = new String[release.size()];
        rows[0] = formatRelease(activity, release.comics, release.release);
        for (int ii = 1; ii < rows.length; ii++) {
            rows[ii] = formatRelease(activity, release.comics, release.otherReleases.get(ii - 1));
        }
        shareText(activity, rows);
    }

    public static void shareReleases(@NonNull Activity activity, @NonNull List<ComicsRelease> releases) {
        if (!releases.isEmpty()) {
            final String[] rows = new String[releases.size()];
            for (int ii = 0; ii < rows.length; ii++) {
                rows[ii] = formatRelease(activity, releases.get(ii).comics, releases.get(ii).release);
            }
            shareText(activity, rows);
        }
    }

    public static void shareText(@NonNull Activity context, @NonNull String... rows) {
        if (rows.length > 0) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, Utility.join("\n", false, rows));
            intent.setType(TEXT_PLAIN);
            context.startActivity(Intent.createChooser(intent, context.getText(R.string.share_chooser_title)));
        }
    }

}
