package it.amonshore.comikkua.data;

public class ReleaseHeader {
    public String Title;
    public String Info;

    public static ReleaseHeader fromString(String text) {
        String[] tokens = text.split(";");
        int idx = 0;
        ReleaseHeader header = new ReleaseHeader();
        header.Title = tokens[idx++];
        header.Info = tokens[idx++];
        return header;
    }
}
