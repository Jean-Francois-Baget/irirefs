package fr.inria.jfbaget.irirefs.preparator;


import static org.junit.jupiter.api.Assertions.*;

import fr.inria.jfbaget.irirefs.IRIRef;
import org.junit.jupiter.api.Test;

import java.util.List;


public class BasicStringPreparatorTest {

    @Test
    void testPreparationHTML4() {
        List<List<String>> dataHTML4 = List.of(
                // Simple named entity
                List.of("Ros&eacute;", "Rosé"),
                // Markup-like content
                List.of("&lt;b&gt;Bold&lt;/b&gt;", "<b>Bold</b>"),
                // Mixed comparison operators
                List.of("5 &lt; 10 &amp;&amp; 10 &gt; 5", "5 < 10 && 10 > 5"),
                // Non-breaking spaces + euro sign
                List.of("Price&nbsp;=&nbsp;10&nbsp;&euro;", "Price\u00A0=\u00A010\u00A0€"),
                // Decimal numeric entity (smiley)
                List.of("Smiley: &#128512;", "Smiley: 😀"),
                // Hex numeric entity (same smiley)
                List.of("Hex: &#x1F600;", "Hex: 😀"),
                // Already decoded content should be unchanged
                List.of("Already decoded é and €", "Already decoded é and €"),
                // Unknown entity should be preserved as-is
                List.of("Unknown &madeup; stays", "Unknown &madeup; stays"),
                // Mixture of known + unknown + plain text
                List.of("X &lt; Y &unknown; &gt; Z", "X < Y &unknown; > Z")
    );
        BasicStringPreparator preparator = new BasicStringPreparator(List.of("html4"));
        for (List<String> pair : dataHTML4) {
            String prepared = preparator.transform(pair.get(0));
            assertEquals(pair.get(1), prepared);
        }
    }

    @Test
    void testPreparationXML() {
        List<List<String>> dataXML = List.of(
                // Simple named entity: XML does NOT know &eacute;, so it stays as-is
                List.of("Ros&eacute;", "Ros&eacute;"),
                // Markup-like content: same as HTML4
                List.of("&lt;b&gt;Bold&lt;/b&gt;", "<b>Bold</b>"),
                // Mixed comparison operators: same as HTML4
                List.of("5 &lt; 10 &amp;&amp; 10 &gt; 5", "5 < 10 && 10 > 5"),
                // Non-breaking spaces + euro sign:
                // XML does NOT know &nbsp; or &euro; ⇒ string unchanged
                List.of("Price&nbsp;=&nbsp;10&nbsp;&euro;", "Price&nbsp;=&nbsp;10&nbsp;&euro;"),
                // Decimal numeric entity (smiley) – supported in XML
                List.of("Smiley: &#128512;", "Smiley: 😀"),
                // Hex numeric entity (same smiley) – also supported
                List.of("Hex: &#x1F600;", "Hex: 😀"),
                // Already decoded content should be unchanged
                List.of("Already decoded é and €", "Already decoded é and €"),
                // Unknown entity should be preserved as-is
                List.of("Unknown &madeup; stays", "Unknown &madeup; stays"),
                // Mixture of known + unknown + plain text
                // &lt; and &gt; decoded, &unknown; kept
                List.of("X &lt; Y &unknown; &gt; Z", "X < Y &unknown; > Z")
        );
        BasicStringPreparator preparator = new BasicStringPreparator(List.of("xml"));
        for (List<String> pair : dataXML) {
            String prepared = preparator.transform(pair.get(0));
            assertEquals(pair.get(1), prepared);
        }
    }

    @Test
    void testPreparationForIRIRef() {
        List<List<String>> data = List.of(
                // 1. Path with a simple named entity
                List.of(
                        "http://example.com/Ros&eacute;",
                        "http://example.com/Rosé"
                ),

                // 2. Path with another named entity
                List.of(
                        "http://example.com/caf&eacute;-noir",
                        "http://example.com/café-noir"
                ),

                // 3. Relative IRI with entities
                List.of(
                        "../R&eacute;sum&eacute;.html",
                        "../Résumé.html"
                ),

                // 4. Query string with &amp; encoded
                List.of(
                        "http://example.com/search?q=Tom&amp;Jerry",
                        "http://example.com/search?q=Tom&Jerry"
                ),

                // 5. Fragment with a non-breaking space entity
                List.of(
                        "http://example.com/page#Section&nbsp;1",
                        "http://example.com/page#Section\u00A01"
                ),

                // 6. Path with a numeric entity (emoji)
                List.of(
                        "http://example.com/smiley-&#128512;",
                        "http://example.com/smiley-😀"
                ),
                // 7. HTML4 encoding of reserved characters
                List.of(
                        "http:&#47;&#47;example.com&#47;path&#47;to&#47;file",
                        "http://example.com/path/to/file"
                ),
                // 8. HTML4 encoding of reserved characters
                List.of(
                        "../dir&#47;subdir&#47;file.html",
                        "../dir/subdir/file.html"
                )
        );

        BasicStringPreparator preparator = new BasicStringPreparator(List.of("html4"));
        for (List<String> pair : data) {
            IRIRef expected = new IRIRef(pair.get(1));
            IRIRef translated = new IRIRef(pair.get(0), preparator);
            assertEquals(expected, translated);
        }


    }


}
