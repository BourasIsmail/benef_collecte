package ma.entraide.suivi.service;

import ma.entraide.suivi.util.Text;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Duplicate key = sorted(normalised family name, normalised first name) + "|" + birth date.
 * Sorting makes the key insensitive to a swapped family/first name. Normalisation absorbs the usual
 * transliteration variants (MOHAMMED / MOUHAMED / MHAMED ...).
 */
public final class DedupKey {
    private DedupKey() {}

    private static final Pattern DOUBLE = Pattern.compile("(.)\\1+");
    private static final Pattern MOHAMED = Pattern.compile("MOUHAMED|MOHAMAD|MHAMED|MOHMED");
    private static final Pattern ABD = Pattern.compile("^ABD(EL|AL|E)?");

    public static String word(String s) {
        List<String> t = Arrays.stream(Text.normFr(s).split(" ")).filter(x -> !x.isEmpty()).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (t.size() > 1 && List.of("EL", "AL", "ELL").contains(t.get(0))) t.remove(0);
        String w = DOUBLE.matcher(String.join("", t)).replaceAll("$1");
        w = MOHAMED.matcher(w).replaceAll("MOHAMED");
        return ABD.matcher(w).replaceFirst("ABD");
    }

    public static String names(String nomFr, String prenomFr) {
        String a = word(nomFr), b = word(prenomFr);
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    public static String of(String nomFr, String prenomFr, LocalDate dob) {
        return names(nomFr, prenomFr) + "|" + dob;
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++)
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
