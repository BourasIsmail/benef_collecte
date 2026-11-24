package ma.entraide.suivi.service;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    /** At least 10 characters with an upper case letter, a lower case letter and a digit. */
    public static boolean isStrong(String p) {
        if (p == null || p.length() < 10 || p.length() > 100) return false;
        boolean up = false, low = false, dig = false;
        for (char c : p.toCharArray()) {
            if (Character.isUpperCase(c)) up = true;
            else if (Character.isLowerCase(c)) low = true;
            else if (Character.isDigit(c)) dig = true;
        }
        return up && low && dig;
    }
}
