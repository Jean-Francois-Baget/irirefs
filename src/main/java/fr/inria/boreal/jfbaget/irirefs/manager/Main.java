package fr.inria.boreal.jfbaget.irirefs.manager;


import java.util.List;

import org.apache.jena.irix.IRIx;

import fr.inria.boreal.jfbaget.irirefs.IRIRef;
import fr.inria.boreal.jfbaget.irirefs.IRIRef.IRITYPE;
import fr.inria.boreal.jfbaget.irirefs.manager.IRIManager.PrefixedIRI;

public class Main {
	
	
	
	 /**
     * Returns a DLGP representation of the environment.
     * @return
     */
    public static String toDLGP(IRIManager env) {
        StringBuilder result = new StringBuilder();
        result.append("@base <" + env.getBase() + ">" + System.lineSeparator());
        for (String prefix : env.getAllPrefixes()) {
            result.append("@prefix " + prefix + ": <" + env.getPrefixed(prefix) + ">" + System.lineSeparator());
        }
        return result.toString();
    }
    
    public static void testRelativisation(String tested, String base) {
    	System.out.println("===========================================");
    	IRIRef baseIRI = new IRIRef(base, IRITYPE.ABS).resolve(); 
    	IRIRef testedIRI = new IRIRef(tested, IRITYPE.IRI).resolve();
    	IRIRef relativizedIRI = testedIRI.relativize(baseIRI);
    	IRIRef resolvedIRI = relativizedIRI.resolve(baseIRI);
    	boolean correct = resolvedIRI.equals(testedIRI);
    	System.out.println("Base:        <" + baseIRI + ">");
    	System.out.println("Tested:      <" + testedIRI + ">");
    	System.out.println("Relativized: <" + relativizedIRI + ">");
    	System.out.println("Resolved:    <" + resolvedIRI + ">");
    	System.out.println("Correct?      " + correct);
    	System.out.println("-----------------With Jena-----------------");
    	IRIx baseIRIx = IRIx.create(base);
    	IRIx testedIRIx = IRIx.create(tested);
    	IRIx relativizedIRIx = baseIRIx.relativize(testedIRIx);
    	IRIx resolvedIRIx = null;
    	System.out.println("Base:        <" + baseIRIx.str() + ">");
    	System.out.println("Tested:      <" + testedIRIx.str() + ">");
    	if (relativizedIRIx != null) {
    		resolvedIRIx = baseIRIx.resolve(relativizedIRIx);
    		System.out.println("Relativized: <" + relativizedIRIx.str() + ">");
    		System.out.println("Resolved:    <" + resolvedIRIx.str() + ">");
    		System.out.println("Correct?      " + resolvedIRIx.equals(testedIRIx));
    	} else {
    		System.out.println("No relativization found");
    		if (correct) {
    			System.out.println("(trying with the relativized found by IRIRef)");
    			relativizedIRIx = IRIx.create(relativizedIRI.recompose());
    			resolvedIRIx = baseIRIx.resolve(relativizedIRIx);
    			System.out.println("Relativized: <" + relativizedIRIx.str() + ">");
        		System.out.println("Resolved:    <" + resolvedIRIx.str() + ">");
        		System.out.println("Correct?      " + resolvedIRIx.equals(testedIRIx));
    		}
    	}
    	System.out.println("===========================================");
    }
    
 
	

    public static void main(String[] args) {
    	

    	
    	IRIRef foo = new IRIRef("http:.//./a");
    	IRIRef res = foo.resolve();
    	System.out.println(res);
    	System.out.println(res.hasRootedPath());
    	System.out.println(res.getSegments());
    	System.out.println(res.getSegments().size());
    	

    	/*
    	IRIRef base1 = new IRIRef("http://www.lirmm.fr/me?query");
    	IRIRef rel1 = new IRIRef("#bar");
    	System.out.println(base1);
    	IRIRef res1 = rel1.resolve(base1);
    	System.out.println(res1);
    	System.out.println(base1.hasRootedPath());
    	System.out.println(rel1.hasRootedPath());
    	System.out.println(res1.hasRootedPath());
    	
    	System.out.println("=================");
    	
    	IRIRef base = new IRIRef("http://a/b/c/d;p?q");
    	IRIRef rel = new IRIRef("g");
    	System.out.println(base);
    	IRIRef res = rel.resolve(base);
    	System.out.println(res);
    	System.out.println(rel.hasRootedPath());
    	System.out.println(res.hasRootedPath());
    	
    	System.out.println("=================");
    	
    	IRIRef test = new IRIRef("http://a/b/c/./g/.").resolve();
    	System.out.println(test);
    	System.out.println(test.hasRootedPath());
    	*/
    	
    	/*
    	
    	// TESTED, BASE
    	List<List<String>> tests = List.of(
    		List.of("ex:a/b/c", "ex:a/b/")	
    	);
    			
    	
    	for (List<String> test : tests) {
    		testRelativisation(test.get(0), test.get(1));
    	}
    	
    	*/
    	
      /*	
      IRIManager env = new IRIManager();
      
      env.setBase("foo/");
      env.setPrefixed("p1", "http://www.lirmm.fr/");
      env.setPrefixed("p2", "foo");
      env.setPrefixed("p3", new PrefixedIRI("p1", "bar/?x=3"));
      String envstr = toDLGP(env);
      System.out.println(envstr);
      
      String iri = env.createIRI(new PrefixedIRI("p3", "a/./b/../foo#bar"));
      System.out.println(iri);
      */
        

        /* 
        IRIx iri = IRIx.create("https://example.com/παράδειγμα?query=テスト#section");

        String iriString = iri.str();
        System.out.println("Full IRI: " + iriString);

        // Convert to a URI object for component extraction
        URI uri;
        try {
            uri = new URI(iriString);
            System.out.println("Scheme: " + uri.getScheme());      // ✅ https
            System.out.println("Host: " + uri.getHost());          // ✅ example.com
            System.out.println("Port: " + uri.getPort());          // ✅ 8080
            System.out.println("Path: " + uri.getPath());          // ✅ /παράδειγμα
            System.out.println("Query: " + uri.getQuery());        // ✅ query=テスト
            System.out.println("Fragment: " + uri.getFragment());  // ✅ section
        }
        catch (URISyntaxException e) {
            System.out.println("Error");
        }

        */
        /* 
        Environment env = new Environment();
        env.set_prefixed("p1", "http://www.lirmm.fr/");
        env.set_prefixed("p2", "foo");
        env.set_prefixed("p3", "p1", "bar/");
        String envstr = env.toDLGP();
        System.out.println(envstr);

        String str1 = env.createIRI("foo");
        System.out.println(str1);
        String str2 = env.createIRI("http://example.com");
        System.out.println(str2);
        String str3 = env.createIRI("p2", "bar");
        System.out.println(str3);
        String str4 = env.createIRI("p3", "bar");
        System.out.println(str4);
        String str5 = env.createIRI("p1", "http://www.example.com");
        System.out.println(str5);        

        String dlgp1 = env.toDLGP("http://www.integraal.fr/test");
        System.out.println(dlgp1);
        String dlgp2 = env.toDLGP("http://www.lirmm.fr/test");
        System.out.println(dlgp2);
        String dlgp3 = env.toDLGP("http://www.lirmm.fr/bar/foo");
        System.out.println(dlgp3);
        String dlgp4 = env.toDLGP("http://www.integraal.fr/test/again");
        System.out.println(dlgp4);
        String dlgp5 = env.toDLGP("http://www.lirmm.fr/bar/foo/qwux");
        System.out.println(dlgp5);

        */

        /* 
       // Création d'un nouvel environnement 
       Environment env = new Environment("http://www.lirmm.fr/");
       // le prefixe p1 est associé à une IRI
       env.set_prefixed("p1", "http://www.inria.fr/");
       // le préfixe p2 est associé à la résolution de foo/ par la base
       env.set_prefixed("p2", "foo/");
       // le préfixe p3 est associé à la résolution de bar par l'IRI associée à p1
       env.set_prefixed("p3", "p1", "bar/");

       System.out.println(env.toDLGP());

       // création d'une IRI résolue par la base
       String test1 = env.createIRI("final");
       System.out.println(test1);

       // création d'une IRI résolue par une base qui ne sert à rien.
       String test2 = env.createIRI("http://example.com/");
       System.out.println(test2);

       // Création d'une IRI résolue par un préfixe
       String test3 = env.createIRI("p3", "/foo/bar");
       System.out.println(test3);
       
       String res1 = env.toDLGP("http://www.inria.fr/bar/toto");
       System.out.println(res1);

       String res2 = env.toDLGP("http://www.inria.fr/ex");
       System.out.println(res2);

       String res3 = env.toDLGP("http://www.inria.fr/bar/toto/tata");
       System.out.println(res3);

       String res4 = env.toDLGP("http://www.lirmm.fr/bar/toto/tata");
       System.out.println(res4);

       String res5 = env.toDLGP("http://www.lirmm.fr/bar");
       System.out.println(res5);

       String res6 = env.toDLGP("http://www.example.com/foo");
       System.out.println(res6); */
    }
    

}
