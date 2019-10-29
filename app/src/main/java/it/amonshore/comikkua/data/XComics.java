package it.amonshore.comikkua.data;

public class XComics {
    public long Id;
    public String Name;
    public String Publisher;
    public String Authors;
    public String Notes;
    public String LastRelease;
    public String NextRelease;
    public String MissingReleases;

    public static XComics fromString(long id, String text) {
        String[] tokens = text.split(";");
        int idx = 0;
        XComics comics = new XComics();
        comics.Id = id;
        comics.Name = tokens[idx++];
        comics.Publisher = tokens[idx++];
        comics.Authors = tokens[idx++];
        comics.Notes = tokens[idx++];
        comics.LastRelease = tokens[idx++];
        comics.NextRelease = tokens[idx++];
        comics.MissingReleases = tokens[idx++];
        return comics;
    }
}
