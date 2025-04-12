package fr.inria.boreal.jfbaget.irirefs;


import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import fr.inria.boreal.jfbaget.nanoparse.IMatch;
import fr.inria.boreal.jfbaget.irirefs.exceptions.IRIParseException;
import fr.inria.boreal.jfbaget.nanoparse.matches.ListMatch;

/**
 * Represents an Internationalized Resource Identifier (IRI) reference.
 * 
 *
 * @author ...
 * @version ...
 * @since ...
 */
public class IRIRef {
	
	private String scheme = null;
	private final  IRIPath path = new IRIPath(List.of()); // Voir plus tard avec constructeur vide
	private String query = null;
	private String fragment = null;
	private IRIAuthority authority = null;
	
	private boolean isResolved = false;
	private boolean isNormalized = false;
	private boolean isFreezed = false;
	private String recomposedString = null;
	
	
	public static  enum IRITYPE {
		ANY(IRIParser.IRIREF), 
		IRI(IRIParser.IRI), 
		REL(IRIParser.RELATIVE), 
		ABS(IRIParser.ABSOLUTE);
		
		private final String ruleName;

		IRITYPE(String ruleName) {
			this.ruleName = ruleName;
		}

		private String getRuleName() {
			return this.ruleName;
		}
		
		private static final Map<String, IRITYPE> LOOKUP = new HashMap<>();

	    static {
	        for (IRITYPE t : IRITYPE.values()) {
	            LOOKUP.put(t.getRuleName(), t);
	        }
	    }
	};
	
	private static final IRIParser parser = new IRIParser();
	

	// CONSTRUCTORS
	
	/**
	 * Constructs an IRI reference by parsing the given string, 
	 * accepting any valid IRI reference form (IRI, absolute IRI or relative IRI).
	 *
	 * @param iriString {@link String} - the IRI string to parse
	 * @throws IRIParseException if the input is not a valid IRI reference
	 */
	public IRIRef(String iriString) throws IRIParseException {
	    this(iriString, IRITYPE.ANY);
	}
	
	
	/**
	 * Constructs an IRI reference from its string representation,
	 * validating it against a specific IRI type.
	 *
	 * @param input {@link String} - the IRI string to parse
	 * @param type {@link IRITYPE} - the expected type of IRI, e.g., {@code IRITYPE.ANY} (for any IRI Reference), 
	 * {@code IRITYPE.IRI} (for an IRI), {@code IRITYPE.ABS} (for an absolute IRI) or {@code IRITYPE.REL} (for a relative IRI).
	 * @throws IRIParseException if the input is not a valid IRI of the specified type,
	 *                            or if the parsed input does not consume the full string
	 */
	public IRIRef(String iriString, IRITYPE type) throws IRIParseException {
	    // Parse the input string from position 0 using the specified rule
	    IMatch iriMatch = parser.read(iriString, 0, type.getRuleName());

	    // Validate that parsing succeeded and the entire string was consumed
	    if (iriMatch.success() && iriMatch.end() == iriString.length()) {
	        this.initializeFromMatch((ListMatch)iriMatch);
	    } else {
	        throw new IRIParseException(String.format(
	        		"The string \"%s\" does not represent a valid %s, stopped parsing at position %d.",
	        		iriString, type.getRuleName(), iriMatch.end())
	        );
	    }
	}
	
	/**
	 * Builds an IRI Reference from the result of an IRIParser.
	 * @param parsed
	 */
	public IRIRef(IMatch parsed) throws IRIParseException, IllegalArgumentException{
		if (! parsed.success()) {
			throw new IRIParseException("The argument given was not a successful match.");
		}
		String name = parsed.reader().getName();
		if (name.equals("IRI") || name.equals("irelative_ref") || name.equals("absolute_IRI")) {
			this.initializeFromMatch((ListMatch)parsed);
		}
		else {
			throw new IllegalArgumentException("The argument given");
		}
	}
	
	public IRIRef(IRIRef other) {
		this.scheme = other.scheme;
		if (other.authority != null) {
			this.authority = new IRIAuthority(other.authority);
		}
		this.path.addAll(other.path);
		this.query = other.query;
		this.fragment = other.fragment;
		this.isResolved = other.isResolved;
		this.isNormalized = other.isNormalized;
		this.isFreezed = false;
	}
	
	
	public IRIRef(String scheme, String user, String host, int port, List<String> path, String query, String fragment) {
		this.initializeFromFields(scheme, user, host, port, path, query, fragment);
	}
	
	/**
	 * Returns a deep copy of this {@code IRIRef}.
	 * The returned instance is mutable and independent from the original,
	 * regardless of whether this object is frozen or not.
	 *
	 * @return a new {@code IRIRef} instance with identical components
	 */
	public IRIRef copy() {
	    return new IRIRef(this);
	}
	
	/**
	 * Returns the scheme component of this IRI, or {@code null} if none is present.
	 *
	 * @return the scheme string (e.g., {@code "http"}) or {@code null}
	 */
	public String getScheme() {
		return this.scheme;
	}
	
	/**
	 * Returns the user information component of the authority, or {@code null} if not specified.
	 *
	 * @return the user string (e.g., {@code "user"} in {@code user@host}) or {@code null}
	 */
	public String getUser() {
		return this.authority.getUser();
	}
	
	/**
	 * Returns the host component of the authority, or {@code null} if not specified.
	 *
	 * @return the host string (e.g., {@code "example.org"}) or {@code null}
	 */
	public String getHost() {
		return this.authority.getHost();
	}
	
	/**
	 * Returns the port component of the authority, or {@code null} if not specified.
	 *
	 * @return the port number, or {@code null}
	 */
	public Integer getPort() {
		return this.authority.getPort();
	}
	
	/**
	 * Returns the full path of this IRI as a single string.
	 * Segments are joined using {@code "/"}.
	 *
	 * @return the string representation of the path (e.g., {@code "a/b/c"})
	 */
	public String getPath() {
		return this.path.recompose();
	}
	
	/**
	 * Returns the individual segments of the path as an unmodifiable list.
	 *
	 * @return a read-only list of path segments
	 */
	public List<String> getSegments() {
		return this.path.getSegments();
	}
	
	/**
	 * Returns the query component of this IRI, or {@code null} if not present.
	 *
	 * @return the query string (e.g., {@code "key=value"}) or {@code null}
	 */
	public String getQuery() {
		return this.query;
	}
	
	public String getFragment() {
		return this.fragment;
	}
	
	public boolean hasScheme() {
		return this.scheme != null;
	}
	
	public boolean hasAuthority() {
		return this.authority != null;
	}
	
	public boolean hasUser() {
		return this.authority != null && this.authority.getUser() != null;
	}
	
	public boolean hasHost() {
		return  this.authority != null && this.authority.getHost() != null;
	}
	
	public boolean hasPort() {
		 return this.authority != null && this.authority.getPort() != null;
	}
	
	public boolean hasQuery() {
		return this.query != null;
	}
	
	public boolean hasEmptyPath() {
		return this.path.isEmpty();
	}
	
	public boolean hasFragment() {
		return this.fragment != null;
	}
	
	public boolean isIRI() {
		return this.scheme != null;
	}
	
	public boolean isAbsolute (){
		return this.scheme != null && this.fragment == null;
	}
	
	public boolean isRelative() {
		return this.scheme == null;
	}
	
	
	/**
	 * Reconstructs the string representation of this IRI reference
	 * from its parsed or constructed components.
	 * <p>
	 * The resulting string follows the IRI recomposition rules as defined
	 * in <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC 3987</a>,
	 * including the scheme, authority, path, query, and fragment components,
	 * if present.
	 * </p>
	 *
	 * @return the full string representation of this IRI
	 */
	public String recompose() {
		return this.recompose(new StringBuilder()).toString();
	}
	
	public String recompose(boolean ansiColor) {
		StringBuilder builder = new StringBuilder();
		if (ansiColor) {
			this.recomposeColor(builder);
		} else {
			this.recompose(builder);
		}
		return builder.toString();
	}
	
	/**
	 * Freezes this {@code IRIRef}, marking it as immutable and safe for use in equality
	 * and hashing operations. Once frozen, the internal state of the IRIRef may no longer
	 * be modified through resolution, normalization, or path manipulation.
	 * <p>
	 * Freezing is only allowed after the IRIRef has been resolved, either explicitly via
	 * {@link #resolveInPlace(IRIRef, boolean)} or implicitly via {@link #normalizeInPlace()}.
	 * Attempting to freeze an unresolved IRIRef will result in an exception.
	 * </p>
	 *
	 * @return this {@code IRIRef}, for fluent chaining
	 * @throws IllegalArgumentException if the IRIRef has not been resolved
	 * @see #resolveInPlace(IRIRef, boolean)
	 * @see #normalizeInPlace()
	 */
	public IRIRef freeze() {
		if (! this.isResolved) {
			throw new IllegalArgumentException("Can only freeze an IRIRef that has been resolved.");
		}
		this.isFreezed = true;
		return this;
	}
	
	/**
	 * Appends the string representation of this IRIRef to the given {@link StringBuilder},
	 * following the standard IRI recomposition rules from 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC 3987</a>.
	 * <p>
	 * The output includes the scheme, authority, path, query, and fragment, when present.
	 * This method is useful for efficient concatenation or partial recomposition.
	 * </p>
	 *
	 * @param builder the {@link StringBuilder} to append the IRI representation to
	 * @return the same {@link StringBuilder}, allowing for method chaining
	 */
	public StringBuilder recompose(StringBuilder builder) {
		if (this.isFreezed && this.recomposedString != null) {
			builder.append(this.recomposedString);
		} else {
			int marker = builder.length();
			this.recomposeScheme(builder);
			if (this.authority != null) {
				this.authority.recompose(builder);
			}
			this.path.recompose(builder);
			this.recomposeQuery(builder);
			this.recomposeFragment(builder);
			if (this.isFreezed) {
				this.recomposedString = builder.substring(marker);
			}
		}
		return builder;
	}
	
	public void recomposeColor(StringBuilder builder) {
		builder.append("\u001B[34m");
		this.recomposeScheme(builder);
		builder.append("\u001B[35m");
		if (this.authority != null) {
			this.authority.recompose(builder);
		} 
		builder.append("\u001B[32m");
		this.path.recompose(builder);
		builder.append("\u001B[33m");
		this.recomposeQuery(builder);
		builder.append("\u001B[31m");
		this.recomposeFragment(builder);
		builder.append("\u001B[0m");
	}
	
	/**
	 * Returns the standard string representation of this IRIRef, as defined
	 * by <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC 3987</a>.
	 * <p>
	 * This is equivalent to calling {@link #recompose()} and includes the scheme,
	 * authority, path, query, and fragment components, when present.
	 * </p>
	 *
	 * @return the IRIRef as a string
	 */
	@Override
	public String toString() {
	    return this.recompose();
	}
	
	/**
	 * Resolves this IRI reference against a given base IRI, following the algorithm 
	 * defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.2">RFC 3986, Section 5.2</a>.
	 * This method modifies the current IRI in place by merging it with the base, normalizing
	 * the resulting path, and inheriting components such as scheme, authority, or query 
	 * when appropriate.
	 * <p>
	 * If {@code strict} is {@code false}, the resolution tolerates non-standard behavior
	 * where the scheme of a relative IRI may match the base and be removed. This is sometimes
	 * allowed for backward compatibility with legacy implementations.
	 * </p>
	 *
	 * @param base the absolute {@link IRIRef} to resolve against; must not be relative
	 * @param strict whether to perform strict RFC-compliant resolution ({@code true}), 
	 *               or allow legacy compatibility behavior ({@code false})
	 * @return this {@link IRIRef}, updated with the resolved components (for method chaining)
	 * @throws IllegalArgumentException if the base IRI is not absolute
	 */
	public IRIRef resolveInPlace(IRIRef base, boolean strict) throws IllegalArgumentException {
		// RFC3986 (p. 28): A base URI must conform to the <absolute-URI> syntax rule
		if (! base.isAbsolute()) {
			throw new IllegalArgumentException("IRI resolution requires an absolute base. Provided: " + base.recompose());
		}
		// RFC3986 (p. 37): Some parsers allow the scheme name to be present in a relative
		// reference if it is the same as the base URI scheme. Its use should be 
		// avoided but is allowed for backward compatibility.
		if (!strict && base.scheme.equals(this.scheme)) {
			this.scheme = null;
		}
		if (this.scheme == null) {
			this.scheme = base.scheme;
			if (!this.hasAuthority() && base.hasAuthority()) {
				this.authority = new IRIAuthority(base.authority);
				if (this.hasEmptyPath()) {
					this.path.resolveEmptyPath(base.path);
					//this.path.clear();
					//this.path.addAll(base.path);
					if (this.query == null) {
						this.query = base.query;
					}
				} else  {
					this.path.resolveNonEmptyPath(base.path, base.hasAuthority());
				}
			}
		}
		this.path.removeDotSegments();
		this.isResolved = true;
		return this;
	}
	
	
	/**
	 * Resolves this IRI reference against the given base IRI using strict resolution rules,
	 * as defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.2">RFC 3986, Section 5.2</a>.
	 * <p>
	 * This is equivalent to calling {@link #resolveInPlace(IRIRef, boolean)} with {@code strict} set to {@code true}.
	 * The resolution strictly adheres to the standard IRI resolution algorithm, and does not apply any legacy compatibility rules.
	 * </p>
	 *
	 * @param base the absolute {@link IRIRef} to resolve against
	 * @return this {@link IRIRef}, updated with the resolved components (for method chaining)
	 * @throws IllegalArgumentException if the base IRI is not absolute
	 * @see #resolveInPlace(IRIRef, boolean)
	 */
	public IRIRef resolveInPlace(IRIRef base) throws IllegalArgumentException {
		return this.resolveInPlace(base, true);
	}
	
	public IRIRef resolve(IRIRef base) throws IllegalArgumentException {
		return new IRIRef(this).resolveInPlace(base, true);
	}
	
	
	public IRIRef resolve(IRIRef base, boolean strict) throws IllegalArgumentException {
		return new IRIRef(this).resolveInPlace(base, strict);
	}
	
	public IRIRef resolveInPlace() throws IllegalArgumentException {
		if (this.isRelative()) {
			throw new IllegalArgumentException("Cannot resolve a Relative IRI without providing a base.");
		}
		this.path.removeDotSegments();
		this.isResolved = true;
		return this;
		
	}
	
	public IRIRef resolve() throws IllegalArgumentException {
		return new IRIRef(this).resolveInPlace();
	}
	
	
	
	
	
	
	
	public IRIRef relativize(IRIRef base) throws IllegalArgumentException {
		if (! this.isResolved || ! base.isResolved) {
			throw new IllegalArgumentException("Can only relativize a resolved IRI against a resolved base.");
		}
		IRIRef result = new IRIRef(this);
		if (result.scheme.equals(base.scheme)) {
			result.scheme = null;
			if (Objects.equals(result.authority, base.authority)) {
				result.authority = null;
				ListIterator<String> baseIt = base.path.listIterator();
				ListIterator<String> resultIt = result.path.listIterator();
				boolean same = true;
				String currentBase = null;
				String currentResult = null;
				while(baseIt.hasNext() && resultIt.hasNext() && same) {
					currentBase = baseIt.next();
					currentResult = resultIt.next();
					if (!currentBase.equals(currentResult)) {
						same = false;
					}
				}
				if (same && !baseIt.hasNext() && !resultIt.hasNext()) {
					result.path.clear();
					if (Objects.equals(result.query, base.query)) {
						result.query = null;
					}
				}
				System.out.println("Base: " + currentBase + ", result: " + currentResult);
			}
		}
		
		return result;
		
	}
	
	
	public IRIRef normalize() {
		return  new IRIRef(this).normalizeInPlace();
	}
	
	
	
	
	public IRIRef normalizeInPlace() {
		return this;
	}

	
	
	
	
	
	private void recomposeScheme(StringBuilder builder) {
		if (this.scheme != null) {
			builder.append(this.scheme);
			builder.append(":");
		}
	}
	

	
	private void recomposeQuery(StringBuilder builder) {
		if (this.query != null) {
			builder.append("?");
			builder.append(this.query);
		}
	}
	
	private void recomposeFragment(StringBuilder builder) {
		if (this.fragment != null) {
			builder.append("#");
			builder.append(this.fragment);
		}
	}
	
	
	private void initializeFromMatch(ListMatch match) {
		String iritype = match.reader().getName();
		switch (iritype) {
			case "IRI": {
				this.fragment = (String) match.result().get(3).result();
			}
			case "absolute_IRI" : {
				this.scheme = (String) match.result().get(0).result();
				this.query = (String) match.result().get(2).result();
				this.initializeFromAuthorityPathMatch(match.result().get(1));
				break;
			}
			case "irelative_ref" : {
				this.query = (String) match.result().get(1).result();
				this.fragment = (String) match.result().get(2).result();
				this.initializeFromAuthorityPathMatch(match.result().get(0));
				break;
			}
			default: {
			    throw new IllegalArgumentException(String.format(
			        "Unsupported IRI type: \"%s\". Expected one of: IRI, absolute_IRI, irelative_ref.",
			        iritype
			    ));
			}
		}
	}
	
	private void initializeFromAuthorityPathMatch(IMatch match) throws IllegalArgumentException {
		String iritype = match.reader().getName();
		switch(iritype) {
			case "_seq_ihier_part": {
				this.authority = new IRIAuthority((ListMatch)((ListMatch)match).result().get(0));
				this.path.addFromMatch(((ListMatch)match).result().get(1));
				break;
			}
			default : {
				this.path.addFromMatch(match);
			}
		}
	}
	
	
	
	private void initializeFromFields(String scheme, String user, String host, Integer port, List<String> path, String query, String fragment) {
		// Here I should match the parameters against the parser...
		this.scheme = scheme;
		this.authority = new IRIAuthority(user, host, port);
		this.path.addAll(path);
		this.query = query;
		this.fragment = fragment;
	}

}
