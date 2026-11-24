package ma.entraide.suivi.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Text {
    private Text() {}

    /** Arabic search/normalisation: strip diacritics & tatweel, unify alef/ya/ta marbuta, collapse spaces. */
    public static String normAr(String s) {
        if (s == null) return "";
        String r = s.replaceAll("[\\u064B-\\u0652\\u0640]", "")
                .replaceAll("[أإآ]", "ا").replace('ى', 'ي').replace('ة', 'ه');
        return r.replaceAll("\\s+", " ").trim();
    }

    /** French/Latin: upper case, accents removed, only letters and spaces. */
    public static String normFr(String s) {
        if (s == null) return "";
        String d = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        return d.replaceAll("[^A-Z ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static String searchText(String nomFr, String prenomFr, String nomAr, String prenomAr) {
        return (normFr(nomFr) + " " + normFr(prenomFr) + " " + normAr(nomAr) + " " + normAr(prenomAr)).trim();
    }

    /** Normalises a user supplied search term the same way as search_text. */
    public static String searchTerm(String q) {
        if (q == null || q.isBlank()) return null;
        // a term is either latin or arabic
        boolean arabic = q.codePoints().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
        String r = arabic ? normAr(q) : normFr(q);
        return r.isBlank() ? null : r.replace("%", "").replace("_", "");
    }

    public static String clean(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }
}
