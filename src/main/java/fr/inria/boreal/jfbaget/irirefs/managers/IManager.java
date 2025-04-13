package fr.inria.boreal.jfbaget.irirefs.managers;

import java.util.NoSuchElementException;
import java.util.Set;

import fr.inria.boreal.jfbaget.irirefs.IRIRef;
import fr.inria.boreal.jfbaget.irirefs.exceptions.IRIParseException;
import fr.inria.boreal.jfbaget.irirefs.managers.AbstractIRIManager.PrefixedIRI;

public interface IManager {
	/**
	 * Sets the base absolute IRI for this manager using the provided IRI string.
	 * <p>
	 * Parses the input string into an {@link IRIRef}, resolves it against the current base IRI, 
	 * and normalizes it according to the manager's configured behavior.
	 * The resulting IRI must be absolute, as required by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1" target="_blank">
	 * RFC 3986, Section 5.1 — Establishing a Base URI</a>.
	 * </p>
	 *
	 * @param iriString the string representation of the new base IRI
	 * @throws IRIParseException if the input is not a valid IRIRef
	 * @throws IllegalArgumentException if the resulting IRIRef is not absolute
	 *
	 * @see #createIRIRef(String)
	 * @see IRIRef#isAbsolute()
	 */
	public void setBase(String iriString) throws IRIParseException, IllegalArgumentException;
	
	/**
	 * Sets the base absolute IRI using a {@link PrefixedIRI} composed of a prefix and an IRIRef.
	 * <p>
	 * This method resolves the {@code iri} part of the {@code prefixedIRI} against the IRI previously
	 * associated with the provided prefix, then normalizes the result. The resolved IRI is then
	 * stored as the new base IRI.
	 * The resulting IRI must be absolute, as required by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1" target="_blank">
	 * RFC 3986, Section 5.1 — Establishing a Base URI</a>.
	 * </p>
	 *
	 * @param prefixedIRI a {@link PrefixedIRI} containing the prefix and IRIRef to resolve
	 * @throws IllegalArgumentException if the resulting IRI is not absolute
	 *
	 * @see #createIRI(PrefixedIRI)
	 * @see IRIRef#isAbsolute()
	 *
	 * <p><b>Note:</b> If you construct the {@link PrefixedIRI} from strings, be aware that 
	 * {@code IRIParseException} or {@code NoSuchElementException} may be thrown during its construction,
	 * not by this method.</p>
	 */
	public void setBase(PrefixedIRI prefixedIRI) throws IllegalArgumentException;
	
	/**
	 * Returns the string representation of the current base IRI.
	 * <p>
	 * The base is always stored as an absolute, normalized IRI.
	 * </p>
	 *
	 * @return the recomposed base IRI as a string
	 */
	public String getBase();
	
	/**
	 * Associates a prefix with an absolute IRI, using the provided IRI string.
	 * <p>
	 * The input string is parsed into an {@link IRIRef}, resolved against the current base IRI,
	 * and normalized according to the manager’s configured behavior.
	 * The resulting IRI must be absolute, as required by
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.1" target="_blank">
	 * RFC 3987, Section 5.1 — Establishing a Base IRI</a>.
	 * </p>
	 * <p>
	 * Prefix keys may be empty or contain whitespace, though it is discouraged for clarity.
	 * Null keys are forbidden and will trigger an exception.
	 * </p>
	 *
	 * @param key the prefix identifier to associate (e.g., {@code "ex"}, {@code "rdf"}, or {@code ""})
	 * @param iriString the string to parse into an absolute IRIRef associated with the prefix
	 *
	 * @throws IRIParseException if the input string is not a valid IRIRef
	 * @throws IllegalArgumentException if the resulting IRI is not absolute, or if the prefix is null
	 *
	 * @see #setBase(String)
	 * @see #createIRI(String)
	 * @see IRIRef#isAbsolute()
	 */
	public void setPrefixed(String key, String iriString) throws IRIParseException, IllegalArgumentException;
	
	/**
	 * Associates a prefix with an absolute IRI using a structured {@link PrefixedIRI} input.
	 * <p>
	 * The {@code prefixedIRI} object holds a prefix (already declared in the environment) and a relative IRIRef.
	 * This method resolves that IRIRef against the IRI associated with the prefix, normalizes the result, 
	 * and stores the resulting absolute IRIRef under the given key.
	 * </p>
	 * <p>
	 * The prefix key may be empty or contain whitespace (though discouraged), but must not be {@code null}.
	 * </p>
	 *
	 * @param key the prefix label to associate
	 * @param prefixedIRI a {@link PrefixedIRI} containing the base prefix and the relative IRIRef
	 *
	 * @throws IllegalArgumentException if the resulting IRI is not absolute, or the prefix key is {@code null}
	 *
	 * @apiNote This method does <b>not</b> itself throw {@code IRIParseException}, or {@code NoSuchElementException} 
	 * but such an exception may be raised earlier when constructing the {@code PrefixedIRI} object from a string.
	 *
	 * @see #createIRI(PrefixedIRI)
	 * @see IRIRef#resolve(IRIRef)
	 * @see IRIRef#isAbsolute()
	 */
	public void setPrefixed(String key, PrefixedIRI prefixedIRI) throws IllegalArgumentException;
	
	/**
	 * Returns the set of all prefix labels currently registered in the environment.
	 * <p>
	 * These are the keys used in {@code setPrefixed(...)} to associate IRIs, and may be used later
	 * to resolve prefixed IRIs or introspect the environment state.
	 * </p>
	 *
	 * @return an unmodifiable {@link Set} of all prefix keys currently stored in this manager
	 *
	 * @see #setPrefixed(String, String)
	 * @see #getPrefixed(String)
	 */
	public Set<String> getAllPrefixes();
	
	/**
	 * Retrieves the absolute IRI string associated with the given prefix.
	 * <p>
	 * The prefix must have previously been declared using {@link #setPrefixed(String, String)} or a related method.
	 * The returned IRI is guaranteed to be normalized and absolute, following the manager's internal policy.
	 * </p>
	 *
	 * @param key the prefix label to look up
	 * @return the normalized, absolute IRI string associated with the prefix
	 *
	 * @throws NoSuchElementException if the prefix has not been declared
	 *
	 * @see #getAllPrefixes()
	 * @see #setPrefixed(String, String)
	 */
	public String getPrefixed(String key) throws NoSuchElementException;
	
	/**
	 * Creates a normalized full IRI from the given IRI string.
	 * <p>
	 * The input string is parsed into an {@link IRIRef}, resolved against the current base IRI
	 * as defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1" target="_blank">
	 * RFC 3986, Section 5.1 — Establishing a Base URI</a>, and normalized according to the manager’s
	 * configured behavior. The resulting IRI is returned in its recomposed string form.
	 * </p>
	 *
	 * @param iriString the string representation of a relative or full IRI
	 * @return the normalized, resolved full IRI as a string
	 * @throws IRIParseException if the input string is not a valid IRIRef
	 *
	 * @see #setBase(String)
	 * @see IRIRef#resolve(IRIRef)
	 * @see IRIRef#normalize()
	 */
	public String createIRI(String value) throws IRIParseException;
	
	/**
	 * Creates a normalized full IRI from a {@link PrefixedIRI}.
	 * <p>
	 * The IRI part of the input is resolved against the full IRI associated with the provided prefix,
	 * and normalized according to the manager's configuration.
	 * </p>
	 *
	 * @param prefixedIRI a pair of {@code (prefix, IRIRef)} where the IRIRef is relative to the prefix
	 * @return the string representation of the resolved, normalized IRI
	 * @throws NoSuchElementException if the prefix is not associated with any IRI
	 *
	 * @see #setPrefixed(String, String)
	 * @see IRIRef#resolve(IRIRef)
	 */
	public String createIRI(PrefixedIRI prefixedIRI) throws IRIParseException, NoSuchElementException;
	
}

