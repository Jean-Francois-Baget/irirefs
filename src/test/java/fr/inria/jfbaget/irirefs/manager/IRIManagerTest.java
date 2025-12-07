package fr.inria.jfbaget.irirefs.manager;


import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;
import fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fr.inria.jfbaget.irirefs.IRIRef;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class IRIManagerTest {

    private static IRIManager manager;

    @BeforeAll
    static void setUpOnce() {
        manager = new IRIManager(
                null,
                new StandardComposableNormalizer(
                        RFCNormalizationScheme.SYNTAX,
                        RFCNormalizationScheme.SCHEME),
                "foo");
        // base is http://www.boreal.inria.fr/foo
        manager.setPrefix("ex1", "bar/");
        // ex1 is http://www.boreal.inria.fr/bar/
        manager.setPrefix("ex2", "ex1","baz");
        // ex2 is http://www.boreal.inria.fr/bar/baz
        manager.setPrefix("ex1", "ex2","bar/");
        // ex1 is http://www.boreal.inria.fr/bar/bar/
        manager.setBase("ex1", "foo/");
        // base is now http://www.boreal.inria.fr/bar/bar/foo/
        manager.setPrefix("Ihis_Is_Far_Too_Long_To_Be_Useful", "http://test/");
    }

    @Test
    void testManagerBase() {
        String expectedBase = "http://www.boreal.inria.fr/bar/bar/foo/";
        assertEquals(expectedBase, manager.getBase());
    }

    @Test
    void testManagerKeys() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("ex1", "http://www.boreal.inria.fr/bar/bar/"),
                Map.entry("ex2", "http://www.boreal.inria.fr/bar/baz")
        );
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            assertEquals(expected.get(key), new IRIRef(manager.getPrefix(key)).recompose());
        }
    }

    @Test
    void testRelativizeAll() {
        Map<String, IRIManager.PrefixedIRIRef> expected = Map.ofEntries(
                Map.entry("http://www.boreal.inria.fr/bar/bar/foo/",
                        new IRIManager.PrefixedIRIRef(null, new IRIRef(""))),

                Map.entry("http://www.boreal.inria.fr/bar/foo/",
                        new IRIManager.PrefixedIRIRef("ex2", new IRIRef("foo/"))),

                Map.entry("has://Nothing/to/do/with/it",
                        new IRIManager.PrefixedIRIRef(null,
                                new IRIRef("has://nothing/to/do/with/it"))),
                Map.entry("http://test/foo",
                        new IRIManager.PrefixedIRIRef(null,
                                new IRIRef("//test/foo"))),
                Map.entry("https://test/foo",
                        new IRIManager.PrefixedIRIRef(null,
                                new IRIRef("https://test/foo")))
        );

        for (Map.Entry<String, IRIManager.PrefixedIRIRef> entry : expected.entrySet()) {
            IRIManager.PrefixedIRIRef result = manager.relativizeBest(manager.createIRI(entry.getKey()));
            assertEquals(entry.getValue(), result);
        }

    }


}
