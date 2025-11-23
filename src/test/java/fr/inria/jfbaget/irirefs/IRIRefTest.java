package fr.inria.jfbaget.irirefs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;
import org.junit.jupiter.api.Test;

class IRIRefTest {

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
				"./a/../b/./c"
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
			assertEquals(result, input.get(1));
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
	

}