package fr.inria.boreal.jfbaget.irirefs.manager.formatter;

import fr.inria.boreal.jfbaget.irirefs.IRIRef;
import fr.inria.boreal.jfbaget.irirefs.manager.IRIManager.PrefixedIRI;

public interface IFormatter {
	
	
	public String format(IRIRef iriref);
	
	public String format(PrefixedIRI prefixedIRI);

}
