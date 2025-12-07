package fr.inria.jfbaget.irirefs.normalizer;

import org.junit.jupiter.api.Test;

import fr.inria.jfbaget.irirefs.IRIRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ExtendedComposableNormalizerTest {


    @Test
    void pct_decodes_unreserved_in_userinfo() {
        List<List<String>> data = List.of(
                // 1) Unreserved simples : %7E -> ~, %41 -> 'A'
                List.of("%7E%41", "~A"),

                // 2) Mélange : ~ et A décodés, ':' (réservé) et ' ' + '%' restent encodés
                //    %7e -> ~, %3a -> ":" (réservé, reste %3a), %20 -> " " (pas unreserved), %25 -> "%"
                List.of("%7e%3a%20%25", "~%3a%20%25"),

                // 3) Texte + séquence pct : seul %7e est décodé, %3a reste encodé
                List.of("user%7e%3a%41", "user~%3aA"),

                // 4) Rien de décodable côté unreserved : tout reste inchangé
                List.of("Already%20encoded", "Already%20encoded")
        );
        IRINormalizer normalizer = new ExtendedComposableNormalizer(RFCNormalizationScheme.PCT);

        for (List<String> pair : data) {
            String user = normalizer.normalizeUserInfo(pair.get(0), "http");
            assertEquals(pair.get(1), user);
        }
    }

    @Test
    void pct_decodes_iunreserved_in_userinfo() {
        List<List<String>> data = List.of(
                // 1) Même cas ASCII que PCT : %7E%41 -> ~A
                List.of("%7E%41", "~A"),

                // 2) UTF-8 2 octets : %C3%A9 -> "é" (U+00E9)
                List.of("%C3%A9", "é"),

                // 3) UTF-8 3 octets : %E2%82%AC -> "€" (U+20AC)
                List.of("%E2%82%AC", "€"),

                // 4) UTF-8 4 octets (non-BMP) : %F0%9F%98%80 -> "😀" (U+1F600)
                List.of("%F0%9F%98%80", "😀"),

                // 5) Mélange ASCII + UTF-8 dans une même run
                //    %7E -> "~", %C3%A9 -> é, %E2%82%AC -> €
                List.of("%7E%C3%A9%E2%82%AC", "~é€"),

                // 6) Bytes valides mais pas iunreserved (espace et '%') : restent encodés
                List.of("%20%25", "%20%25"),

                // 7) UTF-8 invalide : C3 devrait avoir une continuation 10xxxxxx, "28" ne l’est pas
                //    -> la séquence est laissée telle quelle
                List.of("%C3%28", "%C3%28"),

                // 8) Comme 7, mais un truc valide arrive après
                List.of("%C3%28%C3%A9", "%C3%28é")

        );
        IRINormalizer normalizer = new ExtendedComposableNormalizer(RFCNormalizationScheme.PCT);

        for (List<String> pair : data) {
            String user = normalizer.normalizeUserInfo(pair.get(0), "http");
            assertEquals(pair.get(1), user);
        }
    }

    @Test
    void character_mode_normalizes_equivalent_unicode_forms_in_userinfo() {
        // 'u' + COMBINING ACUTE ACCENT + 'ser'
        String decomposed = "u\u0301ser";
        // 'ú' (U+00FA) + 'ser'
        String composed   = "\u00FAser";

        // Sans CHARACTER : aucune normalisation Unicode
        IRINormalizer normalizerNoChar = new ExtendedComposableNormalizer(); // aucun MODE

        String noChar1 = normalizerNoChar.normalizeUserInfo(decomposed, "http");
        String noChar2 = normalizerNoChar.normalizeUserInfo(composed, "http");

        // Les chaînes restent différentes (formes Unicode distinctes)
        assertNotEquals(noChar1, noChar2, "Without CHARACTER, forms should differ");

        // Avec CHARACTER : NFC appliqué
        IRINormalizer normalizerChar = new ExtendedComposableNormalizer(RFCNormalizationScheme.CHARACTER);

        String char1 = normalizerChar.normalizeUserInfo(decomposed, "http");
        String char2 = normalizerChar.normalizeUserInfo(composed, "http");

        // Les deux formes deviennent égales après NFC
        assertEquals(char1, char2, "With CHARACTER, decomposed and composed forms should be equal");
    }

    @Test
    void ipct_then_character_normalizes_literal_decomposed_and_pct_composed_in_userinfo() {
        // Forme décomposée : u + COMBINING ACUTE ACCENT + "ser"
        String decomposed = "u\u0301ser";
        // Forme composée encodée en UTF-8 puis PCT : %C3%BA = "ú"
        String composedPct = "%C3%BAser";

        // 1) IPCT seul : décode %C3%BA -> "ú", mais ne fait pas de NFC
        IRINormalizer ipctOnly = new ExtendedComposableNormalizer(RFCNormalizationScheme.PCT);

        String noChar1 = ipctOnly.normalizeUserInfo(decomposed, "http");
        String noChar2 = ipctOnly.normalizeUserInfo(composedPct, "http");

        // Visuellement identiques, mais pas égales en termes de code points
        assertNotEquals(noChar1, noChar2,
                "Without CHARACTER, decomposed and composed+IPCT forms should differ");

        // 2) IPCT + CHARACTER : IPCT décode %C3%BA -> "ú", puis NFC aligne les deux formes
        IRINormalizer ipctAndChar = new ExtendedComposableNormalizer(
                RFCNormalizationScheme.PCT, RFCNormalizationScheme.CHARACTER);

        String char1 = ipctAndChar.normalizeUserInfo(decomposed, "http");
        String char2 = ipctAndChar.normalizeUserInfo(composedPct, "http");

        assertEquals(char1, char2,
                "With CHARACTER, decomposed literal and PCT-encoded composed form should be equal");
    }

    @Test
    void host_ascii_only_is_lowercased_and_pct_uppercased() {
        IRINormalizer normalizer = new ExtendedComposableNormalizer(
                RFCNormalizationScheme.SYNTAX, RFCNormalizationScheme.SCHEME); // CASE + PCT + CHARACTER + SCHEME

        IRIRef iri = new IRIRef("HttP://ExAmPle.COM%3a80/");
        IRIRef norm = iri.normalizeInPlace(normalizer);

        // scheme lowercased, host lowercased, %3a -> %3A, default port 80 removed
        assertEquals("http://example.com%3A80/", norm.toString());
    }
}