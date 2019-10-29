package it.amonshore.comikkua.data;

public class ReleaseItem {
    public int Type;
    public Object Value;

    public static ReleaseItem fromString(Class<?> type, String text) {
        ReleaseItem item = new ReleaseItem();
        if (type == ReleaseHeader.class) {
            item.Type = 0;
            item.Value = ReleaseHeader.fromString(text);
        } else {
            item.Type = 1;
            item.Value = XRelease.fromString(text);
        }
        return item;
    }
}
