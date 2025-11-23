package fr.inria.jfbaget.irirefs.manager.formatter;

import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.manager.IRIManager.PrefixedIRI;

public interface IFormatter {
	
	
	public String format(IRIRef iriref);
	
	public String format(PrefixedIRI prefixedIRI);

}
