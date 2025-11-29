package fr.inria.jfbaget.irirefs;

import java.util.List;

import fr.inria.jfbaget.irirefs.parser.IRIRefParser;
import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;

import fr.inria.jfbaget.irirefs.parser.IRIRefValidator;
import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;


public class
IRIRef {
	
	private String scheme = null;
	private IRIPath path = null; // Voir plus tard avec constructeur vide
	private String query = null;
	private String fragment = null;
	private IRIAuthority authority = null;
	
	private boolean isResolved = false;
	private boolean isNormalized = false;
	private boolean isFreezed = false;
	private String recomposedString = null;
	
	/**
	 * String returned by relativize when relativization has failed. 
	 * </p>
	 * Value chosen because it is not an IRIRef
	 */
	public static final String RELATIVIZE_ERROR = ":";
	
	
	public enum IRITYPE {
		ANY(IRIRefParser.IRIREF),
		IRI(IRIRefParser.IRI),
		REL(IRIRefParser.RELATIVE),
		ABS(IRIRefParser.ABSOLUTE);
		
		private final String ruleName;
		IRITYPE(String ruleName) {
			this.ruleName = ruleName;
		}
		public String getRuleName() {
			return this.ruleName;
		}
	};
	
	public enum NORMALIZATION {
		/**
		 * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.1">RFC 3987, Simple String Comparison</a>
		 */
		STRING,
		/**
		 * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2">RFC 3987, Syntax-Based Normalization</a>
		 * <p>
		 * Regroups {@link #CASE}, {@link #CHARACTER}, {@link #PCT}
		 * <p>
		 * Though technically in Syntax-based Normalization, Path Segment Normalization {@link #PATH} 
		 * will not be called by {@link #SYNTAX}, since it should have already been done by a call to {@link IRIRef#resolve(IRIRef)}.
		 * Path Segment Normalization should then be called explicitely if required.
		 */
		SYNTAX, 
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.1">RFC 3987, Case Normalization</a>
		 */
		CASE,
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.2">RFC 3987, Character Normalization</a>
		 */
		CHARACTER,
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.3">RFC 3987, Percent-Encoding Normalization</a>
		 */
		PCT,
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.4">RFC 3987, Path Segment Normalization</a>
		 */
		PATH,
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.3">RFC 3987, Scheme-Based Normalization</a>
		 */
		SCHEME,
		/**
		 *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.4">RFC 3987, Protocol-Based Normalization</a>
		 */
		PROTOCOL
	}
	
	private static final IRIRefValidator parser = new IRIRefValidator();



	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	public IRIRef(String iriString) throws IRIParseException {
		this(iriString, IRITYPE.ANY);
	}

	public IRIRef(String iriString, IRITYPE type) throws IRIParseException {
		this(parse(iriString, type));
	}

	public IRIRef(IRIRef other) {
		this.scheme = other.scheme;
		this.authority = (other.authority == null)? null: new IRIAuthority(other.authority);
		this.path = new IRIPath(other.path);
		this.query = other.query;
		this.fragment = other.fragment;
		this.isResolved = other.isResolved;
		this.isNormalized = other.isNormalized;
		this.isFreezed = false;
	}

	private static ListMatch parse(String iriString, IRITYPE type) throws IRIParseException {
		IMatch iriMatch = parser.read(iriString, 0, type.getRuleName());
		if (!iriMatch.success() || iriMatch.end() != iriString.length()) {
			throw new IRIParseException(String.format(
					"The string \"%s\" does not represent a valid %s, stopped parsing at position %d.",
					iriString, type.getRuleName(), iriMatch.end()));
		}
		return ((ListMatch)iriMatch);
	}

	private IRIRef(ListMatch match) {
		IMatch pathAuthorityMatch;

		switch(match.reader().getName()) {
			case IRIRefParser.IRI: {
				this.fragment = (String) match.result().get(3).result();
				// Intentional fallthrough: continue as in ABSOLUTE
			}
			case IRIRefParser.ABSOLUTE: {
				this.scheme = (String) match.result().get(0).result();
				this.query = (String) match.result().get(2).result();
				pathAuthorityMatch = match.result().get(1);
				break;
			}
			case IRIRefParser.RELATIVE: {
				this.query = (String) match.result().get(1).result();
				this.fragment = (String) match.result().get(2).result();
				pathAuthorityMatch = match.result().get(0);
				break;
			}
			default: {
				throw new AssertionError(String.format(
						"Unsupported IRI type: \"%s\". Expected one of: IRI, absolute_IRI, irelative_ref.",
						match.reader().getName()
				));
			}
		}
		if (pathAuthorityMatch.reader().getName().equals(IRIRefParser.HIERARCHICAL)) {
			this.authority = new IRIAuthority((ListMatch) ((ListMatch)pathAuthorityMatch ).result().get(0));
			this.path = new IRIPath(((ListMatch) pathAuthorityMatch).result().get(1));
		} else {
			this.path = new IRIPath(pathAuthorityMatch);
		}
	}

	private IRIRef(String scheme, IRIAuthority authority, IRIPath path, String query, String fragment) {
		this.scheme = scheme;
		this.authority = authority;
		this.path =	path;
		this.query = query;
		this.fragment = fragment;
	}


	/*

	
	public IRIRef(String scheme, String user, String host, int port, boolean rooted, List<String> path, String query, String fragment) {
		this.scheme = scheme;
		if (user != null || host != null ||  port >= 0) {
			this.authority = new IRIAuthority(user, host, port);
		}
		// RAJOUTER VERIFICATION DE PATH
		this.path = new IRIPath(rooted, path);
		this.query = query;
		this.fragment = fragment;
	}

	 */

	// =================================================================================================================
	// UTILS
	// =================================================================================================================
	
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

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other instanceof String s) return recompose().equals(s);
		if (other instanceof IRIRef iri) return recompose().equals(iri.recompose());
		return false;
	}

	@Override
	public int hashCode() {
		return this.recompose().hashCode();
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


	// =================================================================================================================
	// GETTERS
	// =================================================================================================================
	
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
	
	//public boolean hasEmptyPath() {
	//	return this.path.getSegments().isEmpty();
	//}
	
	public boolean hasRootedPath() {
		return this.path.isRooted();
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

	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================
	
	
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


	// =================================================================================================================
	// RESOLUTION
	// =================================================================================================================


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
			if (!this.hasAuthority()) {
				if (base.hasAuthority()) { 
					this.authority = IRIAuthority.copy(base.authority);
				} else {
					this.authority = null;
				}
				if (this.path.isEmpty()) {
					this.path.copyInPlace(base.path);
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

	// =================================================================================================================
	// RELATIVISATION
	// =================================================================================================================


	public IRIRef relativize(IRIRef base) throws IllegalArgumentException {
		if (! base.isAbsolute()) {
			throw new IllegalArgumentException(
					"IRI relativisation requires an absolute base (with a scheme and no fragment). Provided: "
							+ base.recompose());
		}
		if (this.isRelative()) {
			throw new IllegalArgumentException(
					"IRI relativisation requires an IRI (with a scheme). Provided: " + this.recompose());
		}

		if (! this.scheme.equals(base.scheme) || (this.authority == null && base.authority != null))
			return this.copy();

		// if authorities differ (and we are sure that this.authority is not null), remove the scheme
		if (this.authority != null && ! this.authority.equals(base.authority))
			return new IRIRef(null, this.authority, this.path, this.query, this.fragment);

		// when both authorities are null, it is possible that base path is rooted and this path is not
		// in that case it will be impossible to relativize paths, and thus we have to keep something
		// before the path, it can only be the scheme
		if (this.authority == null && !this.hasRootedPath() && base.hasRootedPath())
			return this.copy();

		// FROM NOW WE ARE SURE THAT SCHEMES AND AUTHORITIES ARE EQUAL

		boolean mustFindPath = ! this.hasQuery() && base.hasQuery();
		int maxCost;
		if (this.hasAuthority())
			maxCost = this.authority.recompositionLength();
		else
			maxCost = this.scheme.length() + 1;

		IRIPath newPath = this.path.relativize(base.path, mustFindPath, maxCost);

		if (newPath == null && !this.hasAuthority())
			return this.copy();

		if (newPath == null)
			return new IRIRef(null, this.authority, this.path, this.query, this.fragment);

		if (newPath.getSegments().isEmpty() && ! newPath.isRooted() && this.hasQuery() && this.query.equals(base.query))
			return new IRIRef(null, null, newPath, null, this.fragment);


		return new IRIRef(null, null, newPath, this.query, this.fragment);
	}
	
	// =================================================================================================================
	// NORMALISATION (TO DO)
	// =================================================================================================================

	
	public IRIRef normalize(NORMALIZATION norm) {
		return  new IRIRef(this).normalizeInPlace(norm);
	}
	
	
	
	
	public IRIRef normalizeInPlace(NORMALIZATION norm) {
		return this;
	}

	// =================================================================================================================
	// FREEZING (TO DO?)
	// =================================================================================================================


	/**
	 * Freezes this {@code IRIRef}, marking it as immutable and safe for use in equality
	 * and hashing operations. Once frozen, the internal state of the IRIRef may no longer
	 * be modified through resolution, normalization, or path manipulation.
	 * <p>
	 * Freezing is only allowed after the IRIRef has been resolved, either explicitly via
	 * {@link #resolveInPlace(IRIRef, boolean)} or implicitly via {@link #normalizeInPlace(NORMALIZATION)}.
	 * Attempting to freeze an unresolved IRIRef will result in an exception.
	 * </p>
	 *
	 * @return this {@code IRIRef}, for fluent chaining
	 * @throws IllegalArgumentException if the IRIRef has not been resolved
	 * @see #resolveInPlace(IRIRef, boolean)
	 * @see #normalizeInPlace(NORMALIZATION)
	 */
	public IRIRef freeze() {
		if (! this.isResolved) {
			throw new IllegalArgumentException("Can only freeze an IRIRef that has been resolved.");
		}
		this.isFreezed = true;
		return this;
	}




	// =================================================================================================================
	// TESTS
	// =================================================================================================================

	private static void check(String base, String target) {
		IRIRef iribase   = new IRIRef(base);
		IRIRef iritarget = new IRIRef(target);

		IRIRef relativized = iritarget.relativize(iribase);
		String relStr      = relativized.recompose();
		IRIRef resolved    = relativized.resolve(iribase);
		String resStr      = resolved.recompose();

		System.out.println("Base      : " + base);
		System.out.println("Target    : " + target);
		System.out.println("Relative  : " + relStr);
		System.out.println("Resolved  : " + resStr);
		if (!resStr.equals(target)) {
			System.out.println("❌ FAILED: resolve(relativize(target, base), base) != target");
		}
		System.out.println();
	}

	public static void main(String[] args) {
		check("http:", "http:");

		check("http:/a", "http:/");

		check("http:a/b/c/",       "http:a/b/c/d/e/f");
		check("http:a/b/c",       "http:a/b/c/d/e/f");
		check("http:a/b/c/d/e/f", "http:a/b/c");
		check("http:a/b/c/d/e/f", "http:a/b/c/g/h/i");
		check("http:a/b/c",       "http:a/b");
		check("http:/a/b",        "http:/a/b");
		check("http:/a/b/",        "http:/a/b/");
		check("http:/a/b/",        "http:/a/b");
		check("http:/a/b?q=x",    "http:/a/b?q=x");
		check("http:/a/b?q=x",    "http:/a/b#frag");
		check("http:/a/b/?q=x",    "http:/a/b/#frag");
		check("http:?q=x",    "http:#frag");
		check("http:/a/b?q=x",    "http:/a/b");
		check("http:?q",          "http:#f");
		check("http://a/b",       "https://a/b");
		check("http://a.example.com/path/x", "http://b.example.com/path/y");

		check("http:a/b/c?q=x",    "http:a/b/c");
		check("http:a?q=x",    "http:a");
		check("http://host/a/b?q=x",    "http://host/a/b");
		check("http://host/a/b/?q=x",    "http://host/a/b");
		check("http:?q=x",    "http:");
		check("http:/a/b?q=x", "http:/a/b?q=y");
		check("http:a/b?q=x",  "http:a/b?q=y");

		check("http:a/b?q=x", "http:a/b");
		check("http:a/b?q=x", "http:a/b/");
		check("http:a/b/?q=x", "http:a/b");
		check("http:?q=x", "http:");
		check("http:/?q=x", "http:");
		check("http:q=x", "http:/");

		check("http:/a/b/c/d/e/f",  "http:/g");
		check("http:/a/b/c/d/e/f",  "http:/a/g");

		check("http:a/b/c/d/e/f/g/h/i",  "http:a");

		check("http://example.org/rosé;" ,  "http://example.org/");

		check("http://host/a", "http://host/");
		check("http://host/a/b", "http://host/a/");
		check("http://host/a/b",  "http://host/a//b/");

		check("http://host/",  "http://host/");




	}

}
