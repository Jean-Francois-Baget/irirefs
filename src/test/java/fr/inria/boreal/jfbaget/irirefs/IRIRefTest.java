package fr.inria.boreal.jfbaget.irirefs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class IRIRefTest {

	@Test
	void testRecomposition() {
		List<String> inputs = List.of(
				"scheme://user@host:2025/path.to/my.file?query#fragment",
				"scheme://host/path.to/my.file?query#fragment",
				
				"http://www.lirmm.fr/path/to/my.file?query#fragment",
				"//www.lirmm.fr/path/to/my.file?query#fragment",
				"/path/to/my.file?query#fragment",
				"path/to/my.file?query#fragment",
				"/path/to/?query#fragment",
				"/?query#fragment",
				"?query#fragment",
				"/#fragment",
				"?query"
				);
		for (String input : inputs) {
			IRIRef iri = new IRIRef(input);
			assertEquals(input, iri.recompose());
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
				List.of("http://www.lirmm.fr/a/", "b", "http://www.lirmm.fr/a/b")
				);
		
		for (List<String> input : inputs) {
			IRIRef base = new IRIRef(input.get(0));
			IRIRef relative = new IRIRef(input.get(1));
			String result = relative.resolveInPlace(base).recompose();
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