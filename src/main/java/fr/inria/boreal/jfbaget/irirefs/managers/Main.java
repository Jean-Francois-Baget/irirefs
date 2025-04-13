package fr.inria.boreal.jfbaget.irirefs.managers;

import java.util.regex.Pattern;

import fr.inria.boreal.jfbaget.irirefs.IRIRef;

public class Main extends AbstractIRIManager{
	
	private static Pattern simple = Pattern.compile("([a-z][a-zA-Z0-9]*)?");
	
	@Override
	protected IRIRef normalize(IRIRef iri) {
		return iri;
	}
	
	 /**
     * Returns a DLGP representation of the environment.
     * @return
     */
    public String toDLGP() {
        StringBuilder result = new StringBuilder();
        result.append("@base <" + this.getBase() + ">" + System.lineSeparator());
        for (String prefix : this.getAllPrefixes()) {
            result.append("@prefix " + prefix + ": <" + this.getPrefixed(prefix) + ">" + System.lineSeparator());
        }
        return result.toString();
    }
    
    @Override
    public String displayIRI(IRIRef iriref) {
    	String str = iriref.recompose();
    	if (simple.matcher(str).matches())
            return str;
        else 
            return "<" + str + ">";
    }
    
    @Override
    public String displayPrefixedIRI(PrefixedIRI prefixedIRI) {
    	String str = prefixedIRI.iri().recompose();
    	if (simple.matcher(str).matches())
            return prefixedIRI.prefix() + ":" + str;
        else 
            return prefixedIRI.prefix() + ":<" + str + ">";
    }
	

    public static void main(String[] args) {

      Main env = new Main();
      
      env.setBase("foo/");
      env.setPrefixed("p1", "http://www.lirmm.fr/");
      env.setPrefixed("p2", "foo");
      env.setPrefixed("p3", new PrefixedIRI("p1", "bar/?x=3"));
      String envstr = env.toDLGP();
      System.out.println(envstr);
      
      String iri = env.createIRI(new PrefixedIRI("p3", "a/./b/../foo#bar"));
      System.out.println(iri);
        

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
