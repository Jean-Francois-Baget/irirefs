package fr.inria.jfbaget.irirefs.normalizer;

import org.junit.jupiter.api.Test;

import fr.inria.jfbaget.irirefs.IRIRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class StandardComposableNormalizerTest {


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
        IRINormalizer normalizer = new StandardComposableNormalizer(RFCNormalizationScheme.PCT);

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
        IRINormalizer normalizerNoChar = new StandardComposableNormalizer(); // aucun MODE

        String noChar1 = normalizerNoChar.normalizeUserInfo(decomposed, "http");
        String noChar2 = normalizerNoChar.normalizeUserInfo(composed, "http");

        // Les chaînes restent différentes (formes Unicode distinctes)
        assertNotEquals(noChar1, noChar2, "Without CHARACTER, forms should differ");

        // Avec CHARACTER : NFC appliqué
        IRINormalizer normalizerChar = new StandardComposableNormalizer(RFCNormalizationScheme.CHARACTER);

        String char1 = normalizerChar.normalizeUserInfo(decomposed, "http");
        String char2 = normalizerChar.normalizeUserInfo(composed, "http");

        // Les deux formes deviennent égales après NFC
        assertEquals(char1, char2, "With CHARACTER, decomposed and composed forms should be equal");
    }



    @Test
    void host_ascii_only_is_lowercased_and_pct_uppercased() {
        IRINormalizer normalizer = new StandardComposableNormalizer(
                RFCNormalizationScheme.SYNTAX, RFCNormalizationScheme.SCHEME); // CASE + PCT + CHARACTER + SCHEME

        IRIRef iri = new IRIRef("HttP://ExAmPle.COM%3a80/");
        IRIRef norm = iri.normalizeInPlace(normalizer);

        // scheme lowercased, host lowercased, %3a -> %3A, default port 80 removed
        assertEquals("http://example.com%3A80/", norm.toString());
    }
}