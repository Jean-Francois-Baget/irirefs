package fr.inria.jfbaget.irirefs;

import java.io.IOException;
import java.util.List;

import fr.inria.jfbaget.irirefs.normalizer.ExtendedComposableNormalizer;
import fr.inria.jfbaget.irirefs.normalizer.IRINormalizer;
import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;
import fr.inria.jfbaget.irirefs.parser.IRIRefParser;
import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;

import fr.inria.jfbaget.irirefs.parser.IRIRefValidator;
import fr.inria.jfbaget.irirefs.preparator.StringPreparator;
import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;

/**
 * Represents an Internationalized Resource Identifier (IRI) reference as defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>.
 * <p>
 * An {@code IRIRef} is a mutable, structured view over a textual IRI or IRI reference:
 * the input string is parsed once into its logical components (scheme, authority,
 * path, query, fragment), which can then be inspected, resolved against a base,
 * relativized, normalized, and finally recomposed as a string.
 * </p>
 *
 * <h2>Parsing and validation</h2>
 * <p>
 * Instances are created from a string and an {@link IRITYPE} hint using the
 * {@link IRIRefParser}-based {@link IRIRefValidator}. The parser is strictly
 * conformant to RFC&nbsp;3987 and rejects any input that does not match the
 * chosen grammar rule. Optionally, a {@link StringPreparator} can be provided
 * to transform the input (for example HTML/XML entity unescaping) before parsing.
 * </p>
 *
 * <h2>Resolution, relativization and normalization</h2>
 * <p>
 * Once constructed, an {@code IRIRef} can be:
 * </p>
 * <ul>
 *   <li><em>resolved</em> against an absolute base IRI
 *       (see {@link #resolveInPlace(IRIRef, boolean)} and {@link #resolve(IRIRef, boolean)}),</li>
 *   <li><em>relativized</em> against a base IRI, producing a possibly shorter reference
 *       (see {@link #relativize(IRIRef)}),</li>
 *   <li><em>normalized</em> according to a pluggable {@link IRINormalizer} strategy
 *       (see {@link #normalizeInPlace(IRINormalizer)}).</li>
 * </ul>
 * <p>
 * These operations follow the algorithms and terminology of RFC&nbsp;3986/3987
 * (for example for dot-segment removal and path merging) and are designed to
 * be composable: {@code resolve()} is generally expected to be called before
 * {@code normalize()}, and relativization uses the internal structured form
 * rather than string-based heuristics.
 * </p>
 *
 * <h2>String representation and equality</h2>
 * <p>
 * The canonical string form of an {@code IRIRef} is obtained via {@link #recompose()}
 * (also used by {@link #toString()}). Equality and hash code are defined in terms
 * of this recomposed form: two {@code IRIRef} instances are considered equal if
 * their {@code recompose()} strings are equal, and an {@code IRIRef} can also be
 * compared directly to a {@link String} containing the same textual representation.
 * </p>
 *
 * <h2>Mutability and thread-safety</h2>
 * <p>
 * {@code IRIRef} instances are mutable and not thread-safe. Methods such as
 * {@code removeDotSegmentsInPlace}, {@code normalizeInPlace} and {@code relativize} may
 * change the internal components. Use {@link #copy()} to obtain an independent
 * deep copy when you need to preserve the original value.
 * </p>
 */
public class IRIRef {

	/**
	 * Identifies the logical components of an IRI.
	 * <p>
	 * This enumeration is primarily intended for configuration or
	 * component-specific behavior (for example, in normalization strategies),
	 * and not for structural navigation of an {@link IRIRef} instance.
	 * </p>
	 * <p>
	 * The constants correspond to the pieces defined by RFC&nbsp;3986/3987:
	 * scheme, authority (broken down into user info, host and port), path
	 * (and individual path segments), query and fragment.
	 * </p>
	 */
	public enum PART {
		SCHEME,
		AUTHORITY,
		USERINFO,
		HOST,
		PORT,
		PATH,
		SEGMENT,
		QUERY,
		FRAGMENT
	}
	
	private String scheme = null;
	private IRIPath path = null; // Voir plus tard avec constructeur vide
	private String query = null;
	private String fragment = null;
	private IRIAuthority authority = null;
	




	/**
	 * Defines the expected syntactic category of the input when parsing an IRI
	 * string into an {@link IRIRef}.
	 * <p>
	 * Each value maps to a specific top-level rule in {@link IRIRefParser}:
	 * </p>
	 * <ul>
	 *   <li>{@link #ANY} &rarr; {@link IRIRefParser#IRIREF}</li>
	 *   <li>{@link #IRI} &rarr; {@link IRIRefParser#IRI}</li>
	 *   <li>{@link #REL} &rarr; {@link IRIRefParser#RELATIVE}</li>
	 *   <li>{@link #ABS} &rarr; {@link IRIRefParser#ABSOLUTE}</li>
	 * </ul>
	 * <p>
	 * Using a more specific {@code IRITYPE} allows the caller to enforce that
	 * the parsed string has the expected shape (for example, that it is an
	 * absolute IRI without fragment), otherwise an {@link IRIParseException}
	 * is thrown.
	 * </p>
	 */
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

	
	private static final IRIRefValidator parser = new IRIRefValidator();



	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	/**
	 * Creates a new {@code IRIRef} by parsing the given string as a generic
	 * IRI reference.
	 * <p>
	 * This constructor accepts any kind of IRI reference supported by the
	 * underlying grammar (absolute IRI, relative reference, or full IRI with
	 * fragment), and does not apply any pre-processing to the input string.
	 * Parsing is performed according to RFC&nbsp;3987 using the
	 * {@link IRITYPE#ANY} rule.
	 * </p>
	 *
	 * @param iriString the textual representation of the IRI reference to parse
	 * @throws IRIParseException if the input does not represent a valid IRI
	 *                           according to the {@link IRITYPE#ANY} grammar
	 */
	public IRIRef(String iriString) throws IRIParseException {
		this(iriString, IRITYPE.ANY, null);
	}

	/**
	 * Creates a new {@code IRIRef} by parsing the given string according to the
	 * specified syntactic category.
	 * <p>
	 * This is equivalent to calling
	 * {@link #IRIRef(String, IRITYPE, StringPreparator) IRIRef(iriString, type, null)}:
	 * no pre-processing is applied to the input string before parsing.
	 * </p>
	 *
	 * @param iriString the textual representation of the IRI to parse
	 * @param type      the expected category of the input (absolute IRI, relative
	 *                  reference, etc.), used to select the appropriate
	 *                  {@link IRIRefParser} rule
	 * @throws IRIParseException if the input does not represent a valid IRI of
	 *                           the requested {@code type}
	 */
	public IRIRef(String iriString, IRITYPE type) throws IRIParseException {
		this(iriString, type, null);
	}

	/**
	 * Creates a new {@code IRIRef} by first transforming the input string with the
	 * given {@link StringPreparator}, then parsing the result as a generic IRI
	 * reference.
	 * <p>
	 * This constructor is useful when IRI strings come from contexts such as
	 * HTML or XML and need entity unescaping or other pre-processing before being
	 * validated against RFC&nbsp;3987. Parsing is performed using the
	 * {@link IRITYPE#ANY} rule.
	 * </p>
	 *
	 * @param iriString   the raw input string to transform and parse
	 * @param preparator  the {@link StringPreparator} used to transform the input
	 *                    before parsing; may be {@code null} if no transformation
	 *                    is required
	 * @throws IRIParseException if the transformed string does not represent a
	 *                           valid IRI according to the {@link IRITYPE#ANY} grammar
	 */
	public IRIRef(String iriString, StringPreparator preparator) throws IRIParseException {
		this(iriString, IRITYPE.ANY, preparator);
	}

	/**
	 * Creates a new {@code IRIRef} by optionally transforming the input string
	 * and then parsing it according to the specified syntactic category.
	 * <p>
	 * If {@code preparator} is non-{@code null}, it is applied to the input
	 * {@code iriString} before parsing, which allows callers to handle
	 * source-specific encodings (for example HTML or XML entity escapes) in a separate,
	 * configurable step. The resulting string is then validated by the
	 * {@link IRIRefValidator} using the grammar rule associated with the given
	 * {@link IRITYPE}.
	 * </p>
	 *
	 * @param iriString   the textual representation of the IRI to parse
	 * @param type        the expected syntactic category of the input, used to
	 *                    select the top-level parser rule
	 * @param preparator  an optional {@link StringPreparator} to apply before
	 *                    parsing; may be {@code null}
	 * @throws IRIParseException if the transformed input does not represent a
	 *                           valid IRI of the requested {@code type}
	 */
	public IRIRef(String iriString, IRITYPE type, StringPreparator preparator) throws IRIParseException {
		this(IRIRef.parse(iriString, type, preparator));
	}

	/**
	 * Parses the given string into a {@link ListMatch} according to the selected
	 * {@link IRITYPE}, optionally applying a {@link StringPreparator} beforehand.
	 * <p>
	 * If {@code preparator} is non-{@code null}, it is first used to transform
	 * {@code iriString} (for example to unescape HTML or XML entities). The
	 * transformed string is then passed to the {@link IRIRefValidator}, which
	 * enforces that:
	 * </p>
	 * <ul>
	 *   <li>the chosen top-level grammar rule (as given by
	 *       {@link IRITYPE#getRuleName()}) matches successfully, and</li>
	 *   <li>the match consumes the entire transformed input (no trailing
	 *       characters are allowed).</li>
	 * </ul>
	 * <p>
	 * On failure, an {@link IRIParseException} is thrown describing the type of
	 * IRI expected and the position where parsing stopped.
	 * </p>
	 *
	 * @param iriString   the raw input string to parse
	 * @param type        the syntactic category to enforce when parsing
	 * @param preparator  an optional {@link StringPreparator} applied before
	 *                    parsing; may be {@code null}
	 * @return the {@link ListMatch} representing the full parse tree for the IRI
	 * @throws IRIParseException if the transformed input does not form a valid
	 *                           IRI of the requested {@code type}
	 */
	private static ListMatch parse(String iriString, IRITYPE type, StringPreparator preparator) throws IRIParseException {
		String prepedIriString = (preparator == null)? iriString : preparator.transform(iriString);

		IMatch iriMatch = parser.read(prepedIriString, 0, type.getRuleName());
		if (!iriMatch.success() || iriMatch.end() != prepedIriString.length()) {
			throw new IRIParseException(String.format(
					"The string \"%s\" does not represent a valid %s, stopped parsing at position %d.",
					iriString, type.getRuleName(), iriMatch.end()));
		}
		return ((ListMatch)iriMatch);
	}

	/**
	 * Internal constructor used after successful parsing.
	 * <p>
	 * It interprets the hierarchical structure of the {@link ListMatch} produced
	 * by {@link IRIRefParser} to populate the scheme, authority, path, query and
	 * fragment components of this {@code IRIRef}. This constructor assumes that
	 * the supplied match corresponds to a valid top-level IRI rule and performs
	 * no additional validation.
	 * </p>
	 *
	 * @param match the parsed match tree for an IRI, absolute IRI or relative
	 *              reference
	 * @throws AssertionError if the top-level rule name is unsupported
	 */
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

	/**
	 * Copy constructor.
	 * <p>
	 * Creates a new {@code IRIRef} that is a deep copy of the supplied instance:
	 * mutable subcomponents such as {@link IRIAuthority} and {@link IRIPath} are
	 * cloned so that changes to the new object do not affect the original.
	 *
	 * @param other the {@code IRIRef} to copy; must not be {@code null}
	 */
	public IRIRef(IRIRef other) {
		this.scheme = other.scheme;
		this.authority = (other.authority == null)? null: new IRIAuthority(other.authority);
		this.path = new IRIPath(other.path);
		this.query = other.query;
		this.fragment = other.fragment;
	}

	/**
	 * Internal constructor used by relativization and similar operations to
	 * assemble an {@code IRIRef} directly from its components.
	 * <p>
	 * No validation is performed here: it is the caller's responsibility to
	 * ensure that the provided components form a syntactically valid IRI
	 * when recomposed.
	 * </p>
	 *
	 * @param scheme    the scheme component, or {@code null} for a relative
	 *                  reference
	 * @param authority the authority component, or {@code null} if none
	 * @param path      the path component; must not be {@code null}
	 * @param query     the query component, or {@code null} if none
	 * @param fragment  the fragment component, or {@code null} if none
	 */
	private IRIRef(String scheme, IRIAuthority authority, IRIPath path, String query, String fragment) {
		this.scheme = scheme;
		this.authority = authority;
		this.path =	path;
		this.query = query;
		this.fragment = fragment;
	}



	// =================================================================================================================
	// UTILS
	// =================================================================================================================

	/**
	 * Returns a deep copy of this {@code IRIRef}.
	 * <p>
	 * The returned instance is mutable and independent from the original:
	 * it has its own {@link IRIAuthority} and {@link IRIPath} instances, so
	 * subsequent modifications on the copy do not affect this object.
	 * </p>
	 *
	 * @return a new {@code IRIRef} instance with identical components
	 */
	public IRIRef copy() {
	    return new IRIRef(this);
	}

	/**
	 * Compares this {@code IRIRef} to another object for equality.
	 * <p>
	 * Equality is defined in terms of the recomposed string representation:
	 * </p>
	 * <ul>
	 *   <li>if {@code other} is a {@link String}, it is compared to the result
	 *       of {@link #recompose()},</li>
	 *   <li>if {@code other} is an {@code IRIRef}, their {@code recompose()}
	 *       results are compared,</li>
	 *   <li>otherwise, this method returns {@code false}.</li>
	 * </ul>
	 * <p>
	 * This equality check does <em>not</em> perform any normalization by itself.
	 * Callers are strongly encouraged to normalize both {@code IRIRef} instances
	 * beforehand, using the <em>same</em> {@link IRINormalizer} strategy, if they
	 * expect two syntactically different IRIs that are equivalent under a given
	 * normalization policy to compare as equal.
	 * </p>
	 *
	 * @param other the object to compare with this {@code IRIRef}
	 * @return {@code true} if the two objects have the same textual IRI
	 *         representation after recomposition, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other instanceof String s) return recompose().equals(s);
		if (other instanceof IRIRef iri) return recompose().equals(iri.recompose());
		return false;
	}

	/**
	 * Returns a hash code consistent with {@link #equals(Object)}.
	 * <p>
	 * The hash code is computed from the recomposed string representation
	 * of this {@code IRIRef}, i.e. from {@link #recompose()}. As a result,
	 * two {@code IRIRef} instances that are equal according to
	 * {@code equals(Object)} will have the same hash code.
	 * </p>
	 *
	 * @return the hash code of this {@code IRIRef}
	 */
	@Override
	public int hashCode() {
		return this.recompose().hashCode();
	}

	/**
	 * Returns the standard string representation of this IRIRef, as defined
     * by <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>.
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
	 * @return the scheme string (e.g. {@code "http"}), or {@code null} if this is a
	 *         relative reference
	 */
	public String getScheme() {
		return this.scheme;
	}

	/**
	 * Returns the user information component of the authority, or {@code null}
	 * if either no authority is present or the authority has no user info.
	 *
	 * @return the user info string (e.g. {@code "user"} in {@code "user@host"}),
	 *         or {@code null} if no user info is available
	 */
	public String getUser() {
		return (this.authority == null) ? null : this.authority.getUser();
	}

	/**
	 * Returns the host component of the authority.
	 * <p>
	 * If no authority is present on this IRI, this method returns {@code null}.
	 * If an authority is present but its host is explicitly empty, this method
	 * returns the empty string {@code ""}. Callers that only care about the
	 * presence of any host value should typically use {@link #hasHost()}.
	 * </p>
	 *
	 * @return the host string (for example {@code "example.org"}), the empty
	 *         string if the host is explicitly empty, or {@code null} if no
	 *         authority is present
	 */
	public String getHost() {
		return (this.authority == null) ? null : this.authority.getHost();
	}

	/**
	 * Returns the port component of the authority, or {@code null} if either
	 * no authority is present or no numeric port is specified.
	 * <p>
	 * This implementation does not distinguish between an absent port and an
	 * explicitly empty port as in {@code "http://example.org:"}: in both cases
	 * {@code null} is returned. Only a syntactically valid numeric port will be
	 * exposed as a non-{@code null} value.
	 * </p>
	 *
	 * @return the port number, or {@code null} if no numeric port is available
	 */
	public Integer getPort()  {
		return (this.authority == null) ? null : this.authority.getPort();
	}

	/**
	 * Returns the full path of this IRI as a single string.
	 * <p>
	 * The returned value is the recomposed form of the underlying
	 * {@link IRIPath}, which may be empty, rooted (starting with {@code "/"})
	 * or relative, depending on how this {@code IRIRef} was constructed or
	 * resolved.
	 * </p>
	 *
	 * @return the string representation of the path (for example {@code "a/b/c"}
	 *         or {@code "/a/b/c"}); never {@code null}
	 */
	public String getPath() {
		return this.path.recompose();
	}

	/**
	 * Returns the individual segments of the path as an unmodifiable {@link List}.
	 * <p>
	 * The returned list is an unmodifiable view of the underlying segment list
	 * maintained by {@link IRIPath}. Any attempt to modify it will result in an
	 * {@link UnsupportedOperationException}, but subsequent mutations of this
	 * {@code IRIRef}'s path (for example via resolution or normalization) will be
	 * reflected in the view.
	 * </p>
	 *
	 * @return an unmodifiable view of the path segments; never {@code null}
	 */
	public List<String> getSegments() {
		return this.path.getSegments();
	}

	/**
	 * Returns the query component of this IRI, or {@code null} if none is present.
	 *
	 * @return the query string (for example {@code "key=value"}), or {@code null}
	 *         if no query is specified
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Returns the fragment component of this IRI, or {@code null} if none is present.
	 *
	 * @return the fragment string (for example {@code "section1"}), or {@code null}
	 *         if no fragment is specified
	 */
	public String getFragment() {
		return this.fragment;
	}

	/**
	 * Returns whether this IRI has a scheme component.
	 *
	 * @return {@code true} if a scheme is present, {@code false} otherwise
	 */
	public boolean hasScheme() {
		return this.scheme != null;
	}

	/**
	 * Returns whether this IRI has an authority component.
	 * <p>
	 * When this method returns {@code false}, methods such as {@link #getUser()},
	 * {@link #getHost()} or {@link #getPort()} will also return {@code null}.
	 * </p>
	 *
	 * @return {@code true} if an authority is present, {@code false} otherwise
	 */
	public boolean hasAuthority() {
		return this.authority != null;
	}

	/**
	 * Returns whether this IRI has user information in its authority component.
	 * <p>
	 * This method returns {@code true} if an authority is present and its user
	 * info value is not {@code null}. The user info may be the empty string if
	 * it is explicitly present but empty ({@code http://@host/path}).
	 * </p>
	 *
	 * @return {@code true} if user information is present (possibly empty),
	 *         {@code false} otherwise
	 */
	public boolean hasUser() {
		return this.authority != null && this.authority.getUser() != null;
	}

	/**
	 * Returns whether this IRI has a host component in its authority.
	 * <p>
	 * This method returns {@code true} if an authority is present and its host
	 * value is not {@code null}. The host may be the empty string {@code ""} if
	 * it is explicitly present but empty (for example {@code file:///C:}).
	 * Callers that require a non-empty host should additionally check
	 * {@code !getHost().isEmpty()}.
	 * </p>
	 *
	 * @return {@code true} if a host component is present (possibly empty),
	 *         {@code false} otherwise
	 */
	public boolean hasHost() {
		// the second condition should be redundant, but let's make sure
		return  this.authority != null && this.authority.getHost() != null;
	}

	/**
	 * Returns whether this IRI has an explicit numeric port in its authority.
	 * <p>
	 * This method returns {@code true} only when a syntactically valid numeric
	 * port has been parsed. It returns {@code false} both when there is no
	 * {@code ":port"} part at all and when the port is explicitly empty, as in
	 * {@code "http://example.org:"}, since this implementation encodes both
	 * cases as a {@code null} port.
	 * </p>
	 *
	 * @return {@code true} if an authority is present and a numeric port value
	 *         is set, {@code false} otherwise
	 */
	public boolean hasPort() {
		return this.authority != null && this.authority.getPort() != null;
	}

	/**
	 * Returns whether this IRI has an empty path.
	 * <p>
	 * An empty path is one that contributes no characters to the textual
	 * representation (for example in {@code "http://example.org"}). This is
	 * distinct from a rooted path {@code "/"} or a non-empty relative path
	 * such as {@code "a/b"}.
	 * </p>
	 *
	 * @return {@code true} if the path is empty, {@code false} otherwise
	 */
	public boolean hasEmptyPath() {
		return this.path.isEmptyPath();
	}

	/**
	 * Returns whether this IRI has a rooted path.
	 * <p>
	 * A rooted path is one whose textual representation starts with
	 * {@code "/"}, such as {@code "/a/b"}. This is distinct from an empty
	 * path (no characters) or a non-rooted relative path (for example
	 * {@code "a/b"}).
	 * </p>
	 *
	 * @return {@code true} if the path is rooted, {@code false} otherwise
	 */
	public boolean hasRootedPath() {
		return this.path.isRooted();
	}

	/**
	 * Returns whether this IRI has a query component.
	 * <p>
	 * Note that {@code "foo?"} does have a query component, which is empty
	 * rather than {@code null}.
	 * </p>
	 *
	 * @return {@code true} if a query is present (possibly empty),
	 *         {@code false} otherwise
	 */
	public boolean hasQuery() {
		return this.query != null;
	}

	/**
	 * Returns whether this IRI has a fragment component.
	 * <p>
	 * Note that {@code "foo#"} does have a fragment component, which is empty
	 * rather than {@code null}.
	 * </p>
	 *
	 * @return {@code true} if a fragment is present (possibly empty),
	 *         {@code false} otherwise
	 */
	public boolean hasFragment() {
		return this.fragment != null;
	}

	/**
	 * Returns whether this reference is a full IRI (i.e. has a scheme).
	 * <p>
	 * This method simply tests the presence of the scheme component and does
	 * not enforce any additional constraints (for example on the presence or
	 * absence of a fragment). In particular, an IRI such as
	 * {@code "http://example.org#frag"} is considered an IRI even though it is
	 * not {@linkplain #isAbsolute() absolute} in the RFC&nbsp;3986 sense.
	 * </p>
	 *
	 * @return {@code true} if a scheme is present, {@code false} otherwise
	 */
	public boolean isIRI() {
		return this.scheme != null;
	}

	/**
	 * Returns whether this IRI is absolute in the sense of RFC&nbsp;3986/3987.
	 * <p>
	 * In this implementation, an IRI is considered absolute if it has a
	 * non-{@code null} scheme and no fragment component. For example:
	 * </p>
	 * <ul>
	 *   <li>{@code "http://example.org/path"} &rarr; {@code true}</li>
	 *   <li>{@code "http://example.org/path#frag"} &rarr; {@code false}</li>
	 *   <li>{@code "/path"} &rarr; {@code false}</li>
	 * </ul>
	 *
	 * @return {@code true} if this IRI has a scheme and no fragment,
	 *         {@code false} otherwise
	 */
	public boolean isAbsolute (){
		return this.scheme != null && this.fragment == null;
	}

	/**
	 * Returns whether this reference is relative (i.e. has no scheme).
	 * <p>
	 * This is the logical counterpart of {@link #isIRI()} and simply tests
	 * that the scheme component is {@code null}. A relative reference may
	 * still have an authority, path, query or fragment.
	 * </p>
	 *
	 * @return {@code true} if this reference has no scheme, {@code false} otherwise
	 */
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
	 * in <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>,
	 * including the scheme, authority, path, query, and fragment components,
	 * if present.
	 * </p>
	 *
	 * @return the full string representation of this IRI
	 */
	public String recompose() {
		try {
			return this.recompose(new StringBuilder()).toString();
		} catch (IOException ex) {
			throw new AssertionError("Unexpected IOException with StringBuilder", ex);
		}
	}

	/**
	 * Appends the string representation of this IRI reference to the given
	 * {@link Appendable}, following the standard IRI recomposition rules from
	 * <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>.
	 * <p>
	 * The output includes the scheme, authority, path, query, and fragment,
	 * when present. The supplied {@code builder} is not cleared before use;
	 * this method simply appends to its current contents and returns it,
	 * which allows efficient concatenation in higher-level code.
	 * </p>
	 *
	 * @param builder the {@link Appendable} to append the IRI representation to
	 * @return the same {@link Appendable}, allowing for method chaining
	 */
	public Appendable recompose(Appendable builder) throws IOException {
		this.recomposeScheme(builder);
		if (this.authority != null) {
			this.authority.recompose(builder);
		}
		this.path.recompose(builder);
		this.recomposeQuery(builder);
		this.recomposeFragment(builder);
		return builder;
	}

	/**
	 * Appends the scheme component (followed by {@code ":"}) if present.
	 */
	private void recomposeScheme(Appendable builder) throws IOException {
		if (this.scheme != null) {
			builder.append(this.scheme);
			builder.append(":");
		}
	}

	/**
	 * Appends the query component (prefixed by {@code "?"}) if present.
	 */
	private void recomposeQuery(Appendable builder) throws IOException {
		if (this.query != null) {
			builder.append("?");
			builder.append(this.query);
		}
	}

	/**
	 * Appends the fragment component (prefixed by {@code "#"}) if present.
	 */
	private void recomposeFragment(Appendable builder) throws IOException {
		if (this.fragment != null) {
			builder.append("#");
			builder.append(this.fragment);
		}
	}

	/**
	 * Computes the length, in characters, of the string representation that
	 * would be produced by {@link #recompose()}, without actually allocating
	 * a {@link String}.
	 * <p>
	 * This method mirrors the recomposition logic and sums the lengths of
	 * all present components:
	 * scheme (plus {@code ":"} if non-{@code null}), authority
	 * (including the leading {@code "//"}), path, query (plus {@code "?"})
	 * and fragment (plus {@code "#"}).
	 * It is primarily intended for heuristics such as choosing the shortest
	 * relative or prefixed form of an IRI.
	 * </p>
	 *
	 * @return the number of characters in the recomposed IRI
	 */
	public int recompositionLength() {
		int len = 0;
		if (scheme != null) {
			len += scheme.length() + 1;   // "scheme:"
		}
		if (authority != null) {
			len += authority.recompositionLength(); // authority length already encodes "//"
		}
		len += path.recompositionLength(); // toujours présent, même vide
		if (query != null) {
			len += 1 + query.length();     // "?" + query
		}
		if (fragment != null) {
			len += 1 + fragment.length();  // "#" + fragment
		}
		return len;
	}


	// =================================================================================================================
	// RESOLUTION
	// =================================================================================================================


	/**
	 * Resolves this IRI reference against a given base IRI, following the algorithm
	 * defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.2">
	 * RFC&nbsp;3986, Section&nbsp;5.2</a>, adapted to IRIs.
	 * <p>
	 * This method modifies the current instance in place by:
	 * </p>
	 * <ul>
	 *   <li>checking that {@code base} is {@linkplain #isAbsolute() absolute}
	 *       (has a scheme and no fragment),</li>
	 *   <li>optionally discarding this reference's scheme when it matches the
	 *       base scheme and {@code strict} is {@code false} (legacy compatibility
	 *       behavior allowed by RFC&nbsp;3986),</li>
	 *   <li>merging or inheriting the scheme, authority and query components
	 *       from {@code base} as appropriate, and</li>
	 *   <li>normalizing the resulting path by removing dot segments
	 *       (i.e. applying the {@code remove_dot_segments} algorithm from the RFC).</li>
	 * </ul>
	 * <p>
	 * Only dot-segment removal is performed as part of resolution; any further
	 * normalization (for example case folding or percent-encoding normalization)
	 * should be applied separately via an {@link IRINormalizer}.
	 * </p>
	 *
	 * @param base   the absolute {@link IRIRef} to resolve against; must not be relative
	 * @param strict whether to perform strict RFC-compliant resolution ({@code true}),
	 *               or allow the non-strict behavior where a scheme equal to the base
	 *               scheme may be discarded ({@code false})
	 * @return this {@link IRIRef}, updated with the resolved components (for method chaining)
	 * @throws IllegalArgumentException if the base IRI is not absolute
	 * @see #resolveInPlace(IRIRef)
	 * @see #resolve(IRIRef, boolean)
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
				if (this.path.isEmptyPath()) {
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
		return this;
	}


	/**
	 * Resolves this IRI reference against the given base IRI using strict
	 * resolution rules.
	 * <p>
	 * This is equivalent to calling
	 * {@link #resolveInPlace(IRIRef, boolean) resolveInPlace(base, true)}:
	 * the legacy non-strict behavior (discarding a scheme identical to the
	 * base scheme) is disabled. As in RFC&nbsp;3986, dot segments are removed
	 * from the resulting path as part of the resolution process.
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

	/**
	 * Resolves this IRI reference against the given base IRI using strict
	 * resolution rules, returning a new {@code IRIRef} instance.
	 * <p>
	 * This is a non-mutating counterpart of {@link #resolveInPlace(IRIRef)}:
	 * it first creates a deep copy of this {@code IRIRef}, then resolves that
	 * copy against {@code base} with {@code strict = true}.
	 * </p>
	 *
	 * @param base the absolute {@link IRIRef} to resolve against
	 * @return a new {@link IRIRef} representing the resolved IRI
	 * @throws IllegalArgumentException if the base IRI is not absolute
	 * @see #resolveInPlace(IRIRef)
	 * @see #resolve(IRIRef, boolean)
	 */
	public IRIRef resolve(IRIRef base) throws IllegalArgumentException {
		return new IRIRef(this).resolveInPlace(base, true);
	}

	/**
	 * Resolves this IRI reference against the given base IRI, returning a new
	 * {@code IRIRef} instance and allowing the caller to select strict or
	 * non-strict behavior.
	 * <p>
	 * This is the non-mutating counterpart of
	 * {@link #resolveInPlace(IRIRef, boolean)}: it first creates a deep copy
	 * of this {@code IRIRef}, then resolves that copy against {@code base}
	 * with the given {@code strict} flag.
	 * </p>
	 *
	 * @param base   the absolute {@link IRIRef} to resolve against
	 * @param strict whether to perform strict RFC-compliant resolution ({@code true}),
	 *               or allow the legacy compatibility behavior ({@code false})
	 * @return a new {@link IRIRef} representing the resolved IRI
	 * @throws IllegalArgumentException if the base IRI is not absolute
	 * @see #resolveInPlace(IRIRef, boolean)
	 * @see #resolve(IRIRef)
	 */
	public IRIRef resolve(IRIRef base, boolean strict) throws IllegalArgumentException {
		return new IRIRef(this).resolveInPlace(base, strict);
	}


	// =================================================================================================================
	// RELATIVISATION
	// =================================================================================================================

	/**
	 * Computes a relative form of this IRI with respect to the given absolute base.
	 * <p>
	 * Conceptually, the result of relativizing {@code this} against {@code base}
	 * is an {@code IRIRef} {@code r} such that resolving {@code r} against
	 * {@code base} (using strict resolution rules) reconstructs {@code this},
	 * up to the standard dot-segment removal on the path:
	 * </p>
	 *
	 * <pre>
	 *     r = this.relativize(base);
	 *     r.resolve(base).recompose().equals(this.resolve(base).recompose())  // intended invariant
	 * </pre>
	 *
	 * <p>
	 * Among all such admissible forms, the implementation is designed to return
	 * an {@code r} whose textual representation is as short as possible, as
	 * measured by {@link #recompositionLength()}. In particular, it compares:
	 * </p>
	 * <ul>
	 *   <li>the original absolute IRI,</li>
	 *   <li>a possible network-path reference (no scheme, with authority), and</li>
	 *   <li>possible path-relative references (no scheme, no authority),</li>
	 * </ul>
	 * <p>
	 * and selects the representation with minimal recomposition length, breaking
	 * ties in favour of more compact or more conventional forms when applicable.
	 * No formal proof of optimality is provided, but the algorithm is intended
	 * to be length-optimal within these categories and no counterexample is
	 * currently known.
	 * </p>
	 * <h3>Preconditions</h3>
	 * <ul>
	 *   <li>{@code base} must be {@linkplain #isAbsolute() absolute} (has a scheme and no fragment),</li>
	 *   <li>{@code this} must have a scheme (i.e. {@link #isRelative()} must be {@code false}).</li>
	 * </ul>
	 * If these conditions are not met, an {@link IllegalArgumentException} is thrown.
	 *
	 * <h3>High-level behaviour</h3>
	 * <p>
	 * The method first checks whether relativization is structurally meaningful:
	 * </p>
	 * <ul>
	 *   <li>If the schemes differ, or if {@code base} has an authority while this IRI
	 *       does not, relativization is not attempted and a copy of this IRI is returned.</li>
	 *   <li>If both IRIs have an authority but the authorities differ, the result is a
	 *       <em>network-path reference</em>: the scheme is dropped, but the authority and
	 *       path are preserved (for example {@code "http://host/path"} becomes
	 *       {@code "//host/path"}).</li>
	 *   <li>If both authorities are {@code null}, but {@code base} has a rooted path and
	 *       this IRI has a non-rooted path, no safe relativization is possible and a copy
	 *       of this IRI is returned.</li>
	 * </ul>
	 *
	 * <h3>Path relativization and length heuristic</h3>
	 * <p>
	 * When schemes and authorities are compatible, the method delegates to
	 * {@link IRIPath#relativize(IRIPath, boolean, int)} to compute a candidate
	 * relative path. A cost threshold is derived from the components that would be
	 * omitted if relativization succeeds:
	 * </p>
	 * <ul>
	 *   <li>if an authority is present, the threshold is the recomposition length
	 *       of the authority (including {@code "//"}),</li>
	 *   <li>otherwise, the threshold is {@code scheme.length() + 1} (for
	 *       {@code "scheme:"}).</li>
	 * </ul>
	 * <p>
	 * The path relativization tries hard to produce a relative path whose
	 * recomposed length is strictly below this threshold. If no such path can be
	 * found, the method falls back to:
	 * </p>
	 * <ul>
	 *   <li>returning an unchanged copy of this IRI when there is no authority, or</li>
	 *   <li>returning a network-path reference (scheme removed, authority kept)
	 *       when an authority is present.</li>
	 * </ul>
	 *
	 * <h3>Query handling</h3>
	 * <p>
	 * In the special case where {@code base} has a query and this IRI does not,
	 * the method avoids producing an empty relative reference that would inherit
	 * {@code base}'s query upon resolution. In such situations, the path
	 * relativization may be forced to produce a non-empty path, and if that is
	 * not possible, the method falls back as described above.
	 * </p>
	 * <p>
	 * When a path-relative form is found and both IRIs share the same query,
	 * the result may have an empty path and no query, relying on standard
	 * resolution rules to reconstruct the original absolute IRI from the base.
	 * </p>
	 *
	 * @param base the absolute base IRI to relativize against; must have a scheme
	 *             and no fragment
	 * @return a new {@code IRIRef} representing a relative or network-path form of
	 *         this IRI when beneficial, or an unchanged absolute copy otherwise
	 * @throws IllegalArgumentException if {@code base} is not absolute or if this
	 *                                  IRI has no scheme
	 */
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
	// NORMALISATION
	// =================================================================================================================

	/**
	 * Applies the given {@link IRINormalizer} to this {@code IRIRef} in place.
	 * <p>
	 * The normalizer is responsible for deciding which components to transform
	 * (scheme, authority, path, query, fragment) and how to transform them
	 * (for example case normalization, Unicode normalization, percent-encoding
	 * normalization, scheme-based port suppression, path dot-segment removal,
	 * etc.). This method simply calls the appropriate hooks on the normalizer
	 * in a well-defined order:
	 * </p>
	 * <ol>
	 *   <li>normalize the scheme,</li>
	 *   <li>if present, normalize the authority (user info, host, port),</li>
	 *   <li>normalize the path,</li>
	 *   <li>normalize the query,</li>
	 *   <li>normalize the fragment.</li>
	 * </ol>
	 * <p>
	 * No additional logic is applied here; in particular, whether dot segments
	 * are removed from the path depends entirely on the {@code IRINormalizer}
	 * implementation and its configuration (for example the use of
	 * {@link RFCNormalizationScheme#PATH} in a composable normalizer).
	 * </p>
	 *
	 * @param normalizer the normalization strategy to apply; must not be {@code null}
	 * @return this {@code IRIRef}, after in-place normalization (for method chaining)
	 */
	public IRIRef normalizeInPlace(IRINormalizer normalizer){
		this.scheme = normalizer.normalizeScheme(this.scheme);
		if (this.hasAuthority()) {
			this.authority.normalizeInPlace(normalizer, this.scheme);
		}
		this.path.normalizeInPlace(normalizer, this.scheme, this.authority);
		this.query = normalizer.normalizeQuery(this.query, this.scheme);
		this.fragment = normalizer.normalizeFragment(this.fragment, this.scheme);
		return this;
	}

	/**
	 * Returns a normalized copy of this {@code IRIRef}, using the given
	 * {@link IRINormalizer}.
	 * <p>
	 * This is the non-mutating counterpart of
	 * {@link #normalizeInPlace(IRINormalizer)}: it first creates a deep copy
	 * of this {@code IRIRef}, then applies the normalizer to that copy.
	 * </p>
	 *
	 * @param normalizer the normalization strategy to apply; must not be {@code null}
	 * @return a new {@code IRIRef} representing the normalized IRI
	 * @see #normalizeInPlace(IRINormalizer)
	 */
	public IRIRef normalize(IRINormalizer normalizer){
		return new IRIRef(this).normalizeInPlace(normalizer);
	}

	/**
	 * Applies the RFC&nbsp;3986 {@code remove_dot_segments} algorithm to this
	 * IRI's path in place, without using any base IRI.
	 * <p>
	 * Only the path component is modified; the scheme, authority, query and
	 * fragment are left unchanged. This is equivalent to the dot-segment
	 * removal step that normally occurs as part of IRI resolution, but exposed
	 * here as an independent operation for callers that already have a
	 * self-contained IRI and only want to normalize its path.
	 * </p>
	 *
	 * @return this {@code IRIRef}, after in-place dot-segment removal (for
	 *         method chaining)
	 */
	public IRIRef removeDotSegmentsInPlace() throws IllegalArgumentException {
		if (this.isRelative()) {
			throw new IllegalArgumentException("Cannot resolve a Relative IRI without providing a base.");
		}
		this.path.removeDotSegments();
		return this;

	}

	/**
	 * Returns a new {@code IRIRef} obtained by applying the RFC&nbsp;3986
	 * {@code remove_dot_segments} algorithm to this IRI's path, without using
	 * any base IRI.
	 * <p>
	 * This is the non-mutating counterpart of
	 * {@link #removeDotSegmentsInPlace()}: it first creates a deep copy of this
	 * {@code IRIRef}, then removes dot segments from the copy's path.
	 * All other components (scheme, authority, query, fragment) are preserved.
	 * </p>
	 *
	 * @return a new {@code IRIRef} whose path has been normalized by
	 *         dot-segment removal
	 * @see #removeDotSegmentsInPlace()
	 */
	public IRIRef removeDotSegments() throws IllegalArgumentException {
		return new IRIRef(this).removeDotSegmentsInPlace();
	}



	public static void main(String[] args) {

		IRIRef iri = new IRIRef("HTTP://%7e%3a%4b%5C@www.lirmm.fr:80/../.");

		IRINormalizer normalizer = new ExtendedComposableNormalizer(
				RFCNormalizationScheme.SYNTAX,
				RFCNormalizationScheme.PATH,
				RFCNormalizationScheme.SCHEME,
				RFCNormalizationScheme.PCT);
		System.out.println(iri.normalizeInPlace(normalizer));

	}

}
