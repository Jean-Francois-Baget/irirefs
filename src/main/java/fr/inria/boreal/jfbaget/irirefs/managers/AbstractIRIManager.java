package fr.inria.boreal.jfbaget.irirefs.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
//import java.util.regex.Pattern;

import fr.inria.boreal.jfbaget.irirefs.IRIRef;
import fr.inria.boreal.jfbaget.irirefs.IRIRef.IRITYPE;
import fr.inria.boreal.jfbaget.irirefs.exceptions.IRIParseException;

/**
 * An IRIManager
 */
public abstract class AbstractIRIManager implements IManager{
	
	/**
	 * Represents a prefix-IRI pair used for resolution or registration.
	 * <p>
	 * This record bundles a prefix (e.g., {@code "ex"}) with an {@link IRIRef} that is intended
	 * to be resolved using the IRI base associated with the prefix.
	 * Commonly used in prefixed notations such as {@code ex:foo}.
	 * </p>
	 *
	 * @param prefix the identifier associated with a base IRI (must be registered in the manager)
	 * @param iri the relative or absolute {@link IRIRef} to resolve against the prefix base
	 *
	 * @see IRIRef
	 * @see AbstractIRIManager#createIRI(PrefixedIRI)
	 */
	protected static record PrefixedIRI(String prefix, IRIRef iri) {
		
		/**
	     * Constructs a {@code PrefixedIRI} from a string representation of the IRIRef.
	     * <p>
	     * This is a convenience constructor that internally parses the {@code iriString}
	     * into an {@link IRIRef} object.
	     * </p>
	     *
	     * @param prefix the prefix to use (e.g., {@code "ex"})
	     * @param iriString the string representation of the IRIRef to associate with the prefix
	     * @throws IRIParseException if the {@code iriString} is not a valid IRIRef
	     *
	     * @see IRIRef#IRIRef(String)
	     */
		public PrefixedIRI(String prefix, String iri) throws IRIParseException{
			this(prefix, new IRIRef(iri));
		}
	}
	
	
	/**
	 * The default base IRI used when no explicit base is provided.
	 * <p>
	 * This must always be a valid absolute IRI, as per
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1">RFC 3986, Section 5.1</a>.
	 * </p>
	 * @see {@link #AbstractIRIManager()} 
	 * Need to modify the javadoc there if {@link #DEFAULT_BASE} is modified.
	 */
    private static final String DEFAULT_BASE = "http://www.boreal.inria.fr/";
    
    /**
     * The base IRI used for resolving relative IRI references.
     * <p>
     * This is always stored as an absolute, normalized {@link IRIRef}.
     * </p>
     */
    private IRIRef base;
    
    /**
     * A map of declared prefixes to their associated absolute IRIs.
     * <p>
     * Used for resolving prefixed IRI references of the form {@code prefix:localName}.
     * All stored {@link IRIRef} values are absolute and normalized.
     * </p>
     */
    private HashMap<String, IRIRef> prefixes;


    /**
     * Constructs a new {@code AbstractIRIManager} with the given base IRI.
     * <p>
     * The provided IRI string is parsed as an {@link IRIRef} using the {@link IRITYPE#ABS} rule,
     * then resolved and normalized based on the manager’s normalization policy.
     * The result must be an absolute IRI, as required by
     * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1">RFC 3986, Section 5.1</a>.
     * </p>
     *
     * <p>
     * The internal prefix map is initialized as empty.
     * </p>
     *
     * @param iriString the string representation of the base IRI (must be absolute)
     * @throws IRIParseException if the input string is not a valid absolute IRI
     *
     * @see IRIRef
     * @see IRITYPE#ABS
     */
    public AbstractIRIManager(String iriString) throws IRIParseException{
        this.base = this.normalize(new IRIRef(iriString, IRITYPE.ABS).resolve());
        this.prefixes = new HashMap<>();
    }

    /**
     * Constructs a new {@code AbstractIRIManager} using a default base IRI.
     * <p>
     * The default base IRI is:
     * {@code "http://www.boreal.inria.fr/"}.
     * </p>
     * This is equivalent to {@code new AbstractIRIManager("http://www.integraal.fr/")}.
     *
     * @throws IRIParseException if the default base IRI is not a valid absolute IRI
     *
     * @see #AbstractIRIManager(String)
     */
    public AbstractIRIManager() throws IRIParseException{
        this(DEFAULT_BASE);
    }
    
    
   /**
    * Normalizes the given {@link IRIRef} according to a strategy defined by the subclass.
    * <p>
    * This method <b>must</b> be overridden in concrete subclasses to enforce a consistent normalization policy
    * across all IRIs managed by this instance. It is called internally when setting the base or creating IRIs.
    * </p>
    * <p>
    * Subclasses may implement standard normalization steps defined in 
    * <a href="https://tools.ietf.org/html/rfc3986#section-6" target="_blank">RFC 3986, Section 6</a>, such as:
    * </p>
    * <ul>
    *   <li>Lowercasing the scheme and host (if applicable)</li>
    *   <li>Removing default ports</li>
    *   <li>Percent-encoding normalization</li>
    * </ul>
    *
    * @param iri the {@link IRIRef} to normalize; the manager ensures it has always been resolved 
    * (it is thus a full IRI without dot segments)
    * @return a normalized {@link IRIRef}
    *
    * @see IRIRef#resolve()
    */
    protected abstract IRIRef normalize(IRIRef iri);
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBase(String iriString) throws IRIParseException, IllegalArgumentException {
    	this.setNormalizedBase(this.createIRIRef(iriString));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setBase(PrefixedIRI prefixedIRI) throws IllegalArgumentException {
    	this.setNormalizedBase(this.createIRIRef(prefixedIRI));
    }
    
    /**
     * Resolves and normalizes the given {@link IRIRef}, then sets it as base.
     * <p>
     * This method is intended for internal or subclass use. It ensures that
     * any {@link IRIRef} provided from an external source (e.g., parser) is
     * first resolved against the current base and normalized, before being used
     * as the new base IRI.
     * </p>
     *
     * @param iriref the IRIRef to resolve and normalize as base
     * @throws IllegalArgumentException if the resulting IRI is not absolute
     *
     * @see #normalize(IRIRef)
     * @see #setNormalizedBase(IRIRef)
     */
    protected void setBase(IRIRef iriref) throws IllegalArgumentException {
    	this.setNormalizedBase(this.createIRIRef(iriref));
    }
    
    /**
     * Directly sets the base IRI from a resolved and normalized {@link IRIRef}.
     * <p>
     * This method assumes the input has already been processed according to the
     * manager's resolution and normalization policy. No further transformations
     * will be applied. It is intended for internal use only.
     * </p>
     *
     * @param iriref a fully resolved and normalized absolute IRIRef
     * @throws IllegalArgumentException if the IRIRef is not absolute
     */
    private void setNormalizedBase(IRIRef iriref) throws IllegalArgumentException {
    	this.checkAbsolute(iriref);
        this.base = iriref;
    }
    
    /**
     * Ensures the given {@link IRIRef} is absolute (i.e., has a scheme and no fragment).
     *
     * @param iriref the IRIRef to test
     * @throws IllegalArgumentException if the IRIRef is not absolute
     *
     * @see IRIRef#isAbsolute()
     */
    private void checkAbsolute(IRIRef iriref) throws IllegalArgumentException {
    	if (!iriref.isAbsolute()) {
    		throw new IllegalArgumentException("A base can only be an absolute IRI, given: \"" + iriref.recompose() + "\"");
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBase() {
        return this.base.recompose();
    } 
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrefixed(String key, String iriString) throws IRIParseException, IllegalArgumentException {
    	this.setNormalizedPrefixed(key, this.createIRIRef(iriString));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrefixed(String key, PrefixedIRI prefixedIRI) throws IllegalArgumentException {
    	this.setNormalizedPrefixed(key, this.createIRIRef(prefixedIRI));
    }
    
    /**
     * Resolves and normalizes the given {@link IRIRef}, then associates it with the provided prefix key.
     * <p>
     * This method is intended for internal or subclass use. It ensures that any externally provided {@link IRIRef}
     * is made compatible with this manager’s normalization and resolution policy before being stored.
     * </p>
     *
     * @param key the prefix key to associate with the resolved and normalized IRIRef
     * @param iriref a potentially unresolved and preferably unnormalized IRIRef
     * @throws IllegalArgumentException if the resulting IRIRef is not absolute, or if the key is {@code null}
     *
     * @see #createIRIRef(IRIRef)
     * @see #setNormalizedPrefixed(String, IRIRef)
     */
    protected void setPrefixed(String key, IRIRef iriref) throws IllegalArgumentException {
    	 this.setNormalizedPrefixed(key, this.createIRIRef(iriref));
    }
    
    /**
     * Associates a fully resolved and normalized {@link IRIRef} with the given prefix key.
     * <p>
     * This method is the final step in prefix registration. It expects the provided IRIRef to already be
     * resolved and normalized according to the manager's policy. The prefix key must not be {@code null},
     * but it may be empty or contain whitespace.
     * </p>
     *
     * @param key the prefix label to associate
     * @param iriref the absolute, normalized {@link IRIRef} to associate
     * @throws IllegalArgumentException if the IRIRef is not absolute, or the prefix key is {@code null}
     *
     * @see #checkAbsolute(IRIRef)
     * @see #setPrefixed(String, IRIRef)
     */
    private void setNormalizedPrefixed(String key, IRIRef iriref) throws IllegalArgumentException {
    	this.checkAbsolute(iriref);
    	this.checkNullKey(key);
        this.prefixes.put(key, iriref);
    }
    
    /**
     * Ensures that the provided prefix key is not {@code null}.
     * <p>
     * Used as a defensive check when associating a prefix to an IRIRef.
     * Keys may be empty or contain whitespace, but must not be {@code null}.
     * </p>
     *
     * @param key the prefix key to check
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    private void checkNullKey(String key) throws IllegalArgumentException {
    	if (key == null) {
    		throw new IllegalArgumentException("Cannot associate a base IRIRef to a null key.");
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllPrefixes() {
        return Collections.unmodifiableSet(this.prefixes.keySet());
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefixed(String key) throws NoSuchElementException {
        return this.getPrefixedIRIRef(key).recompose();
    }
    
    /**
     * Retrieves the IRIRef currently associated with a given prefix key.
     * <p>
     * This method is used internally to resolve prefixed IRIs or to inspect the environment.
     * </p>
     *
     * @param key the prefix to look up
     * @return the {@link IRIRef} associated with the prefix
     * @throws NoSuchElementException if the prefix is not registered in the current environment
     *
     * @see #setPrefixed(String, IRIRef)
     * @see java.util.NoSuchElementException
     */
    private IRIRef getPrefixedIRIRef(String key) throws NoSuchElementException {
        IRIRef prefixed = this.prefixes.get(key);
        if (prefixed == null) {
            throw new NoSuchElementException("No IRIRef is associated with prefix \"" + key + "\" in the current environment.");
        }
        else {
            return prefixed;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String createIRI(String iriString) throws IRIParseException {
    	return this.createIRIRef(new IRIRef(iriString)).recompose();
    }
    
    /**
     * Resolves and normalizes a pre-parsed IRIRef against this manager's base and normalization strategy.
     * <p>
     * This method is intended for internal or subclass use only. It ensures the provided {@link IRIRef}
     * is processed in a consistent way with the manager’s configuration before returning its string representation.
     * </p>
     * <p>
     * <b>Warning:</b> Passing an already normalized {@code IRIRef} that was built using
     * a different normalization strategy may lead to unexpected results. Use with caution.
     * </p>
     *
     * @param iriref the {@link IRIRef} to resolve and normalize
     * @return the resulting string representation of the normalized IRI
     */
    protected String createIRI(IRIRef iriref) {
    	return this.createIRIRef(iriref).recompose();
    }
    
    /**
     * Resolves and normalizes the provided IRIRef against the current base.
     * <p>
     * This internal method ensures that the returned IRIRef is fully resolved and
     * conforms to the manager's normalization policy.
     * </p>
     *
     * @param iriref the input IRIRef to be resolved and normalized
     * @return the resulting normalized, resolved IRIRef
     */
    private IRIRef createIRIRef(IRIRef iriref) {
    	return this.normalize(iriref.resolve(this.base));
    }
    
    /**
     * Parses the given IRI string into an {@link IRIRef}, then resolves and normalizes it.
     * <p>
     * This method delegates to {@link #createIRIRef(IRIRef)} to apply the manager's resolution
     * and normalization strategy using the current base IRI.
     * </p>
     *
     * @param iriString the raw IRIRef string to process
     * @return a resolved and normalized {@link IRIRef}
     * @throws IRIParseException if the string does not represent a valid IRIRef
     */
    private IRIRef createIRIRef(String iriString) throws IRIParseException {
    	return this.createIRIRef(new IRIRef(iriString));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String createIRI(PrefixedIRI prefixedIRI) throws NoSuchElementException {
        return this.createIRIRef(prefixedIRI).recompose();
    }
    
    /**
     * Resolves and normalizes a {@link PrefixedIRI} to produce an internal {@link IRIRef}.
     * <p>
     * The prefix must have been previously declared using {@link #setPrefixed(String, String)} or similar.
     * </p>
     *
     * @param prefixedIRI a prefixed IRIRef to resolve and normalize
     * @return the normalized, fully-resolved IRIRef
     * @throws NoSuchElementException if the prefix is not declared
     */
    private IRIRef createIRIRef(PrefixedIRI prefixedIRI) throws NoSuchElementException {
        return this.normalize(prefixedIRI.iri.resolve(this.getPrefixedIRIRef(prefixedIRI.prefix))); 
    }
    
    
    
    private IRIRef relativize(IRIRef iriref) {
    	return iriref.relativize(this.base);
    }
    
    private IRIRef relativize(IRIRef iriref, String key) {
    	return iriref.relativize(this.getPrefixedIRIRef(key));
    }
    
    public String display(IRIRef iriref) {
    	if (iriref.isRelative()) {
    		throw new IllegalArgumentException("Cannot display a relative IRI, given \"" + iriref.recompose() + "\"");
    	}
    	String candidate = this.displayIRI(iriref);
    	String test = this.display(this.relativize(iriref));
    	if (! test.equals(IRIRef.RELATIVIZE_ERROR) && test.length() < candidate.length()) {
    		candidate = test;
    	}
    	for (String key : this.getAllPrefixes()) {
    		test = this.displayPrefixedIRI(new PrefixedIRI(key, this.relativize(iriref, key)));
    		if (! test.equals(IRIRef.RELATIVIZE_ERROR) && test.length() < candidate.length()) {
        		candidate = test;
        	}
    	}
    	return candidate;
    }
    
    public abstract String displayIRI(IRIRef iriref);
    
    public abstract String displayPrefixedIRI(PrefixedIRI prefixedIRI);
    

    /**
     * Returns the shortest possible DLGP representation of an IRI according to the environment.
     * @param iri a string representing an IRI (not a relative)
     * @return the shortest possible DLGP representation of iri according to the environment.
     * @throws IRIException when iri is neither a relative or an IRI
     * @throws IllegalArgumentException when iri is a relative
     */
    public String toDLGP(String iri) throws IRIParseException, IllegalArgumentException {
    	/*
        IRIx irix = IRIx.create(iri);
        if (irix.isRelative()) {
            throw new IllegalArgumentException("Only IRIs can be written in DLGP, and \"" + iri + "\" is a relative.");
        }
        String candidate;
        if (simple.matcher(iri).matches()) 
            candidate = iri;
        else
            candidate = "<" + iri + ">"; 
        String test = relativize(irix, this.base);
        if (!test.equals(":") && test.length() < candidate.length()) {
            candidate = test;
        } 
        for (Map.Entry<String, IRIx> entry : this.prefixes.entrySet()) {
            test = relativize(irix, entry.getValue());
            if (!test.equals(":") && entry.getKey().length() + test.length() <= candidate.length()) {
                candidate = entry.getKey() + ":" + test;
            } 
        }
        return candidate;
        */
    	return "Not done";
    }


    private static String relativize(IRIRef value, IRIRef base) {
    	/*
        IRIx testx = base.relativize(value);
        if (testx != null) {
            String test = testx.str();
            if (simple.matcher(test).matches())
                return test;
            else 
                return "<" + test + ">";
        }
        else
            return ":"; // This cannot be an IRI, nor a relative
        */
    	return "Not done";
    }

}
