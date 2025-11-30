package fr.inria.jfbaget.irirefs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;
import org.junit.jupiter.api.Test;

class IRIRefTest {

    private static final boolean DISPLAY = false;

    @Test
    void testRecomposition() {
        List<String> inputs = List.of(
                // Absolute IRIs
                "scheme://user@host:2025/path.to/my.file?query#fragment",
                "scheme://host/path.to/my.file?query#fragment",
                "http://www.example.com/path/to/file?query=abc#frag",
                "https://host:8080/",
                "ftp://user@ftp.host.com:21/downloads/file.txt",
                "mailto:user@example.com",
                "file:///C:/path/to/file.txt",
                "urn:isbn:0451450523",
                "news:comp.infosystems.www.servers.unix",
                "tel:+1-816-555-1212",
                "ldap://[2001:db8::7]/c=GB?objectClass?one",
                "http://example.com./path",

                // Network-path references
                "//host",
                "//user@host",
                "//host:8080",
                "//user@host:8080",
                "//host/path",
                "//host/path?query",
                "//host/path#frag",
                "//host/path?query#frag",
                "//host?",
                "//host#",
                "//host?#",
                "//",

                // Absolute-path references
                "/",
                "/file",
                "/file.txt",
                "/file.ext?query",
                "/file.ext#frag",
                "/file.ext?query#frag",
                "/dir/file.ext",
                "/dir.with.dots/file.with.dots",
                "/path/to/file",
                "/path/to/file?query=abc",
                "/path/to/file#fragment",
                "/path/to/file?query#frag",
                "/?q=abc",
                "/#fragment",
                "/?#",

                // Relative-path references
                "file",
                "file.ext",
                "path/file.ext",
                "path.to/file.ext",
                "path/to/file",
                "segment",
                "../up/one/level",
                "./same/level",
                ".",
                "..",
                "./file",
                "../file",
                "./",
                "../",

                // Query and Fragment only
                "?query",
                "?query=value",
                "?query=1&another=2",
                "#fragment",
                "?query#fragment",
                "?#",
                "#",
                "?",
                "#a/b/c",

                // Weird but valid edge formats
                "///path",
                "////host/path",
                "//host///extra",
                "scheme://",
                "scheme://host",
                "scheme://host#",
                "scheme://host?",
                "scheme://host?#",
                "file:///",
                "mailto:",
                "news:",

                // Combinations with missing parts
                "http://",
                "http://host",
                "http://host/",
                "http://host/path",
                "http://host?query",
                "http://host#frag",
                "http://host/path?query",
                "http://host/path#frag",
                "http://host/path?query#frag",
                "http://user@host",
                "http://user@host:80",
                "http://user@host:80/",
                "http://user@host:80/path",
                "http://user@host:80/path?query#frag",

                // Dot segment edge cases
                "./",
                "../",
                "././",
                "../../",
                "./../",
                ".././",
                "./a/b/../c/./d.html",
                "../a/./b/../c",
                "./a/../b/./c",

                // IPv6 host examples (IP-literal with IPv6address)
                "http://[2001:db8::1]/",
                "http://[2001:db8:0:0:8:800:200C:417A]/index.html",
                "http://[::1]",
                "http://[::1]:8080/path/to/file?query=abc#frag",
                "ftp://user@[2001:db8::7]:21/downloads/file.txt",
                "//[2001:db8::2]/network/path",
                "//[::1]/just/path",
                "ldap://[2001:db8::7]/c=GB?objectClass?one",
                "http://[2001:db8::1:0:0]/",
                "http://[::ffff:192.0.2.128]/",

                // IPvFuture host examples (IP-literal with IPvFuture)
                "http://[v7.fe80::abcd]/",
                "http://[v7.fe80::abcd]/path?query#frag",
                "https://[vF.FF-1~abc]/resource",
                "//[v7.fe80::abcd]/network/path",
                "//[vA.example-1]/foo/bar",
                "http://[v1.a-b_c~!$&'()*+,;=]/",

                // IRIs with non-ASCII Unicode (RFC 3987 only, not valid as plain RFC 3986 URIs)

                // Latin accents + Greek in path/query/fragment
                "http://www.example.com/élève/école?matière=économie#section-π",

                // German umlaut + ß in host and path
                "http://münchen.example/straße/überblick?jahr=2025#übersicht",

                // Cyrillic in path/query/fragment
                "http://www.example.com/путь/страница?ключ=значение#фрагмент",

                // Mixed Latin + CJK in path/query
                "http://www.example.com/ユーザー/プロフィール?表示=日本語#概要",

                // Full IDN host (reg-name with non-ASCII)
                "http://例え.テスト/パス/ページ?クエリ=値#フラグメント",

                // Arabic in host and path
                "http://مثال.إختبار/مسار/صفحة?استعلام=قيمة#جزء",

                // Emoji in path/query/fragment (allowed by RFC 3987 ucschar ranges)
                "http://emoji.example/😀/😇?q=π&x=λ#σ-🚀",

                // Network-path reference with Unicode host and path
                "//ホスト名.example/パス/リソース?種類=テスト#部分",

                // Relative paths with mixed scripts
                "chemin/смешанный/路径",
                "資料/データ/пример",

                // Private-use characters in query (iprivate; IRI-only)
                "http://example.com/chemin?priv=\uE000\uE001",


                // ================================
                // Non-BMP / surrogate-pair Unicode
                // ================================

                // Emoji in path / query / fragment
                // 😀 = U+1F600 = \uD83D\uDE00
                // 🚀 = U+1F680 = \uD83D\uDE80
                // 🌍 = U+1F30D = \uD83C\uDF0D
                // 💻 = U+1F4BB = \uD83D\uDCBB
                "http://example.com/\uD83D\uDE00/\uD83D\uDE80?earth=\uD83C\uDF0D#device=\uD83D\uDCBB",

                // Non-BMP emoji in authority (reg-name)
                "http://\uD83D\uDE00.example.com/path",

                // Gothic and Deseret letters in path (historic scripts, > U+FFFF)
                // 𐍈 = U+10348 = \uD800\uDF48
                // 𐐀 = U+10400 = \uD801\uDC00
                "http://example.com/\uD800\uDF48\uD801\uDC00/section",

                // Network-path reference with non-BMP in host and path
                "//\uD83D\uDE00\uD83D\uDE80.example/\uD83C\uDF0D/\uD83D\uDCBB",

                // Relative IRI with mixed BMP + non-BMP
                "資料/\uD83D\uDE00/データ/\uD800\uDF48",

                "http://example.org/ros&eacute;"
        );
        for (String input : inputs) {
            IRIRef iri = new IRIRef(input);
            assertEquals(input, iri.recompose());
        }
    }

    @Test
    public void testMalformedIriRefsThrowException() {
        List<String> invalidInputs = List.of(
                // Missing scheme before colon
                ":",
                ":/",
                "://",
                "://host/path",


                // Invalid scheme format
                "1scheme:/path",
                "-scheme:/path",

                // Bad delimiter usage
                "http://host/path##frag",
                "http://host/path?query#frag#x"

                // Optionally include:
                // ""   // only if your spec says empty IRIRefs are disallowed
        );

        for (String input : invalidInputs) {
            assertThrows(IRIParseException.class, () -> new IRIRef(input),
                    "Expected ParsingErrorException for input: " + input);
        }
    }

    @Test
    void testRemoveDotSegments() {
        IRIRef base = new IRIRef("ex:");
        List<List<String>> inputs = List.of(
                List.of("/a/b/c/./../../g", "ex:/a/g"), // @see RFC3986 (p. 34)
                List.of("mid/content=5/../6",	"ex:mid/6"), // @see RFC3986 (p. 34)
                List.of("/a/b/c/../..",	"ex:/a/")
        );
        for (List<String> input : inputs) {
            IRIRef iri = new IRIRef(input.get(0));
            String result = iri.resolveInPlace(base).recompose();
            assertEquals(input.get(1), result);
        }
    }

    @Test
    void testResolution() {
        List<List<String>> inputs = List.of(
                List.of("http://www.lirmm.fr/me?query", "#bar", "http://www.lirmm.fr/me?query#bar"),
                List.of("http://www.lirmm.fr/a/", "b", "http://www.lirmm.fr/a/b"),
                List.of("http://www.lirmm.fr/a/", "b", "http://www.lirmm.fr/a/b"),
                List.of("http:/a/?q", "#f", "http:/a/?q#f"),
                List.of("http:/a/b/?q=x", "#frag", "http:/a/b/?q=x#frag")
        );

        for (List<String> input : inputs) {
            IRIRef base = new IRIRef(input.get(0));
            IRIRef relative = new IRIRef(input.get(1));
            String result = relative.resolveInPlace(base).recompose();
            assertEquals(input.get(2), result);
        }

    }

    @Test
    void testResolution2() {
        List<List<String>> inputs = List.of(
                List.of("http://www.lirmm.fr/me?query", "#bar", "http://www.lirmm.fr/me?query#bar"),
                List.of("http://www.lirmm.fr/a/", "b", "http://www.lirmm.fr/a/b"),
                List.of("http://www.lirmm.fr/a/", "b", "http://www.lirmm.fr/a/b"),
                List.of("http:/a/?q", "#f", "http:/a/?q#f"),
                List.of("http:/a/b/?q=x", "#frag", "http:/a/b/?q=x#frag")
        );

        for (List<String> input : inputs) {
            IRIRef base = new IRIRef(input.get(0));
            IRIRef relative = new IRIRef(input.get(1));
            String result = relative.resolve(base).recompose();
            assertEquals(input.get(2), result);
        }
    }

    @Test
    void testResolutionRFC3987NormalExamples() {
        IRIRef base = new IRIRef("http://a/b/c/d;p?q");

        List<List<String>> inputs = List.of(
                List.of("g:h", "g:h"),
                List.of("g", "http://a/b/c/g"),
                List.of("./g", "http://a/b/c/g"),
                List.of("g/", "http://a/b/c/g/"),
                List.of("/g", "http://a/g"),
                List.of("//g", "http://g"),
                List.of("?y", "http://a/b/c/d;p?y"),
                List.of("g?y", "http://a/b/c/g?y"),
                List.of("#s", "http://a/b/c/d;p?q#s"),
                List.of("g#s", "http://a/b/c/g#s"),
                List.of("g?y#s", "http://a/b/c/g?y#s"),
                List.of(";x", "http://a/b/c/;x"),
                List.of("g;x", "http://a/b/c/g;x"),
                List.of("g;x?y#s", "http://a/b/c/g;x?y#s"),
                List.of("", "http://a/b/c/d;p?q"),
                List.of(".", "http://a/b/c/"),
                List.of("./", "http://a/b/c/"),
                List.of("..", "http://a/b/"),
                List.of("../", "http://a/b/"),
                List.of("../g", "http://a/b/g"),
                List.of("../..", "http://a/"),
                List.of("../../", "http://a/"),
                List.of("../../g", "http://a/g")
        );

        for (List<String> input : inputs) {
            IRIRef relative = new IRIRef(input.get(0));
            String result = relative.resolveInPlace(base).recompose();
            assertEquals(input.get(1), result);
        }
    }


    @Test
    void testResolutionRFC3987AbnormalExamples() {
        IRIRef base = new IRIRef("http://a/b/c/d;p?q");

        List<List<String>> inputs = List.of(
                List.of("../../../g", "http://a/g"),
                List.of("../../../../g", "http://a/g"),

                List.of("/./g", "http://a/g"),
                List.of("/../g", "http://a/g"),
                List.of("g.", "http://a/b/c/g."),
                List.of(".g", "http://a/b/c/.g"),
                List.of("g..", "http://a/b/c/g.."),
                List.of("..g", "http://a/b/c/..g"),

                List.of("./../g", "http://a/b/g"),
                List.of("./g/.", "http://a/b/c/g/"),
                List.of("g/./h", "http://a/b/c/g/h"),
                List.of("g/../h", "http://a/b/c/h"),
                List.of("g;x=1/./y", "http://a/b/c/g;x=1/y"),
                List.of("g;x=1/../y", "http://a/b/c/y"),

                List.of("g?y/./x", "http://a/b/c/g?y/./x"),
                List.of("g?y/../x", "http://a/b/c/g?y/../x"),
                List.of("g#s/./x", "http://a/b/c/g#s/./x"),
                List.of("g#s/../x", "http://a/b/c/g#s/../x")
        );

        for (List<String> input : inputs) {
            IRIRef relative = new IRIRef(input.get(0));
            String result = relative.resolveInPlace(base).recompose();
            assertEquals(input.get(1), result);
        }
    }

    @Test
    void testResolutionRFC3987NonStrict() {
        IRIRef base = new IRIRef("http://a/b/c/d;p?q");
        IRIRef relative = new IRIRef("http:g");
        String result = relative.resolveInPlace(base, false).recompose();
        assertEquals("http://a/b/c/g", result);
    }

    @Test
    void testRelativization() {
        List<List<String>> data = List.of(
                List.of("http:", "http:"),
                List.of("http:/", "http:/"),
                List.of("http:a", "http:a"),
                List.of("http:/a", "http:/a"),
                List.of("http:a/", "http:a/"),
                List.of("http:/a/", "http:/a/"),

                List.of("http:", "http:"),
                List.of("http:/", "http:/"),
                List.of("http:/", "http:"),
                List.of("http:", "http:/"),

                List.of("http:/a", "http:/a"),
                List.of("http:a", "http:a"),
                List.of("http:/a", "http:/"),
                List.of("http:/", "http:/a"),



                List.of("http:/a/", "http:/"),
                List.of("http:a/b/c/",       "http:a/b/c/d/e/f"),
                List.of("http:a/b/c",       "http:a/b/c/d/e/f"),
                List.of("http:a/b/c/d/e/f", "http:a/b/c"),
                List.of("http:a/b/c/d/e/f", "http:a/b/c/g/h/i"),
                List.of("http:a/b/c",       "http:a/b"),
                List.of("http:/a/b",        "http:/a/b"),
                List.of("http:/a/b/",        "http:/a/b/"),
                List.of("http:/a/b/",        "http:/a/b"),
                List.of("http:/a/b?q=x",    "http:/a/b?q=x"),
                List.of("http:/a/b?q=x",    "http:/a/b#frag"),
                List.of("http:/a/b/?q=x",    "http:/a/b/#frag"),
                List.of("http:?q=x",    "http:#frag"),
                List.of("http:/a/b?q=x",    "http:/a/b"),
                List.of("http:?q",          "http:#f"),
                List.of("http://a/b",       "https://a/b"),

                List.of("http://a.example.com/path/x", "http://b.example.com/path/y"),

                List.of("http:a/b/c?q=x",    "http:a/b/c"),
                List.of("http:a?q=x",    "http:a"),
                List.of("http://host/a/b?q=x",    "http://host/a/b"),
                List.of("http://host/a/b/?q=x",    "http://host/a/b"),
                List.of("http:?q=x",    "http:"),
                List.of("http:/a/b?q=x", "http:/a/b?q=y"),
                List.of("http:a/b?q=x",  "http:a/b?q=y"),

                List.of("http:a/b?q=x", "http:a/b"),
                List.of("http:a/b?q=x", "http:a/b/"),
                List.of("http:a/b/?q=x", "http:a/b"),
                List.of("http:?q=x", "http:"),
                List.of("http:/?q=x", "http:"),
                List.of("http:q=x", "http:/"),

                List.of("http:/a/b/c/d/e/f",  "http:/g"),
                List.of("http:/a/b/c/d/e/f",  "http:/a/g"),

                List.of("http:a/b/c/d/e/f/g/h/i",  "http:a"),

                List.of("http://example.org/rosé;" ,  "http://example.org/"),

                List.of("http://host/a", "http://host/"),
                List.of("http://host/a/b", "http://host/a/"),
                List.of("http://host/a/b",  "http://host/a//b/"),
                List.of("veryverylongscheme:a/b/c/d/e", "veryverylongscheme:a//b/"),

                List.of("http://host/",  "http://host/"),

                List.of("http:?q", "http:#f"),

                List.of("http:a", "http:a/b")
        );
        for (int index = 0; index < data.size(); index++) {
            IRIRef iribase   = new IRIRef(data.get(index).get(0));
            IRIRef iritarget = new IRIRef(data.get(index).get(0));

            IRIRef relativized = iritarget.relativize(iribase);
            String relStr      = relativized.recompose();
            IRIRef resolved    = relativized.resolve(iribase);
            String resStr      = resolved.recompose();

            assertEquals(iritarget, resStr);
            assertTrue(relativized.recompose().length() <= iritarget.recompose().length());

            if (DISPLAY) {
                System.out.println("=====================\nTest " + index + "\n\"=====================");
                System.out.println("Base      : " + iribase);
                System.out.println("Target    : " + iritarget);
                System.out.println("Relative  : " + relStr);
                System.out.println("Resolved  : " + resStr);
                if (!resStr.equals(iritarget.recompose())) {
                    System.out.println("❌ FAILED: resolve(relativize(target, base), base) != target");
                }
                System.out.println();
            }

        }

    }

}