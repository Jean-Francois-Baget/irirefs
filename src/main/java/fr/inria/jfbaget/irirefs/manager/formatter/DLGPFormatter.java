package fr.inria.jfbaget.irirefs.manager.formatter;

import java.util.regex.Pattern;

import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.manager.IRIManager.PrefixedIRI;

public class DLGPFormatter implements IFormatter {
	
	private static Pattern simple = Pattern.compile("([a-z][a-zA-Z0-9]*)?");

	@Override
	public String format(IRIRef iriref) {
		String str = iriref.recompose();
    	if (simple.matcher(str).matches())
            return str;
        else 
            return "<" + str + ">";
	}

	@Override
	public String format(PrefixedIRI prefixedIRI) {
		String str = prefixedIRI.iri().recompose();
    	if (simple.matcher(str).matches())
            return prefixedIRI.prefix() + ":" + str;
        else 
            return prefixedIRI.prefix() + ":<" + str + ">";
	}

}
