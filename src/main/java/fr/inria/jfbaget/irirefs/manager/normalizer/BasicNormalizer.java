package fr.inria.jfbaget.irirefs.manager.normalizer;

import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.IRIRef.NORMALIZATION;

public class BasicNormalizer implements INormalizer {
	
	@Override
	public IRIRef normalize(IRIRef iriref) {
		return iriref
				.normalizeInPlace(NORMALIZATION.SYNTAX)
				.normalizeInPlace(NORMALIZATION.SCHEME);
	}
}
