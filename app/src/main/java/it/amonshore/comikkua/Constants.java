package it.amonshore.comikkua;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

public final class Constants {
    public final static int NOTIFICATION_GROUP_ID = 1;
    public final static String NOTIFICATION_GROUP = "it.amonshore.comikkua.RELEASE_NOTIFICATION";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RELEASE_LOST,
            RELEASE_MISSING,
            RELEASE_DATED,
            RELEASE_DATED_NEXT,
            RELEASE_NOT_PURCHASED,
            RELEASE_PURCHASED,
            RELEASE_OTHER,
            RELEASE_NEW})
    public @interface ReleaseTypeDef {
    }

    public final static int RELEASE_LOST = 10;
    public final static int RELEASE_MISSING = 100;
    public final static int RELEASE_DATED = 20;
    public final static int RELEASE_DATED_NEXT = 21;
    public final static int RELEASE_NOT_PURCHASED = 23;
    public final static int RELEASE_PURCHASED = 24;
    public final static int RELEASE_OTHER = 22;
    public final static int RELEASE_NEW = 200;

    public final static int UNDO_TIMEOUT = 7_000;

    public static final int VIEWHOLDER_ACTION_MENU = 741;
    public static final int VIEWHOLDER_ACTION_CLICK = 406;
    public static final int VIEWHOLDER_ACTION_FOLLOW = 121;

    public final static String KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled";
}
