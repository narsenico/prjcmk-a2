package it.amonshore.comikkua.data;

public class XRelease {
    public String Title;
    public String Numbers;
    public String Date;
    public String Info;
    public String Notes;
    public String Purchased;
    public String Ordered;

    public static XRelease fromString(String text) {
        String[] tokens = text.split(";");
        int idx = 0;
        XRelease release = new XRelease();
        release.Title = tokens[idx++];
        release.Numbers = tokens[idx++];
        release.Date = tokens[idx++];
        release.Info = tokens[idx++];
        release.Notes = tokens[idx++];
        release.Purchased = tokens[idx++];
        release.Ordered = tokens[idx++];
        return release;
    }
}
