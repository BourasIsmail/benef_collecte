package ma.entraide.suivi.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DedupKeyTest {
    private static final LocalDate DOB = LocalDate.of(2012, 3, 12);

    @Test
    void transliterationVariantsGiveTheSameKey() {
        String a = DedupKey.of("El Alaoui", "Mohammed", DOB);
        assertEquals(a, DedupKey.of("ALAOUI", "Mohamed", DOB));
        assertEquals(a, DedupKey.of("Alaoui", "Mouhamed", DOB));
        assertEquals(a, DedupKey.of("al-alaoui", "MHAMED", DOB));
        assertEquals("ALAOUI|MOHAMED|2012-03-12", a);
    }

    @Test
    void swappedFamilyAndFirstNameGiveTheSameKey() {
        assertEquals(DedupKey.of("Balouk", "Ilham", DOB), DedupKey.of("Ilham", "Balouk", DOB));
    }

    @Test
    void accentsAndPunctuationAreIgnored() {
        assertEquals(DedupKey.of("Zaïd", "Hélène", DOB), DedupKey.of("ZAID", "HELENE", DOB));
        assertEquals(DedupKey.word("O'Brien"), DedupKey.word("O Brien"));
    }

    @Test
    void differentBirthDateGivesDifferentKey() {
        assertNotEquals(DedupKey.of("Alaoui", "Mohamed", DOB), DedupKey.of("Alaoui", "Mohamed", DOB.plusDays(1)));
    }

    @Test
    void abdVariants() {
        assertEquals(DedupKey.word("Abdellah"), DedupKey.word("Abdallah"));
        assertEquals(DedupKey.word("Abdel Ilah"), DedupKey.word("Abdelilah"));
    }

    @Test
    void levenshtein() {
        assertEquals(0, DedupKey.levenshtein("ABC", "ABC"));
        assertEquals(1, DedupKey.levenshtein("ALAOUI", "ALAOUY"));
        assertEquals(3, DedupKey.levenshtein("KITTEN", "SITTING"));
    }

    @Test
    void passwordPolicy() {
        assertTrue(PasswordPolicy.isStrong("Taza#Secure2026"));
        assertFalse(PasswordPolicy.isStrong("short1A"));
        assertFalse(PasswordPolicy.isStrong("alllowercase123"));
        assertFalse(PasswordPolicy.isStrong("ALLUPPERCASE123"));
        assertFalse(PasswordPolicy.isStrong("NoDigitsHereAtAll"));
        assertFalse(PasswordPolicy.isStrong(null));
    }
}
