package fr.inria.jfbaget.irirefs;

import fr.inria.jfbaget.nanoparse.matches.ListMatch;

import java.util.Objects;

/**
 * Represents the <em>authority</em> component of an IRI, as defined in RFC 3986
 * and RFC 3987.
 * <p>
 * In the IRI syntax, the authority appears (when present) after the scheme and
 * {@code "//"}, and before the path:
 *
 * <pre>
 *   scheme "://" [ user "@" ] host [ ":" port ]
 * </pre>
 *
 * This class stores the three logical parts:
 * <ul>
 *   <li>{@code user} – the optional userinfo (without the trailing {@code '@'}),</li>
 *   <li>{@code host} – the host or IP-literal (already in canonical string form,
 *       e.g. {@code "example.org"} or {@code "[2001:db8::1]"}),</li>
 *   <li>{@code port} – the optional port number, or {@code null} if absent.</li>
 * </ul>
 *
 * <p>It is package-private on purpose: end users work with {@code IRIRef},
 * which exposes higher-level getters such as {@code getHost()} and a
 * recomposed string form. {@code IRIAuthority} is an internal helper for
 * parsing, resolution and recomposition.
 *
 * <p>Important invariants:
 * <ul>
 *   <li>If there is <strong>no authority at all</strong>, the {@code IRIRef}
 *       instance holds {@code authority == null}.</li>
 *   <li>If there is an <strong>empty authority</strong> (e.g. {@code "file:///…"}),
 *       then an {@code IRIAuthority} exists with {@code host == ""}.</li>
 *   <li>Recomposition via {@link #recompose()} always yields the exact textual
 *       authority part (without the leading {@code "//"}), suitable for
 *       insertion into the full IRI.</li>
 * </ul>
 */
class IRIAuthority {
	
	private String user = null;
	private String host = null;
	private Integer port = null;
	
	private static final String AUTHORITY_SEPARATOR = "//";
	private static final String USER_SEPARATOR = "@";
	private static final String PORT_SEPARATOR = ":";
			
	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	/**
	 * Builds an {@code IRIAuthority} from a parsed {@link ListMatch}.
	 * <p>
	 * This constructor is intended to be used only by the IRI parser. It assumes
	 * that {@code authorityMatch} is the result of the {@code iauthority} rule
	 * (or equivalent) and therefore already syntactically valid according to
	 * RFC 3986 / 3987.
	 *
	 * <p>The {@code ListMatch} is expected to have exactly three components in
	 * this order:
	 * <ol>
	 *   <li>index 0: the optional userinfo as a {@link String} result, or
	 *       {@code null} if absent;</li>
	 *   <li>index 1: the host as a {@link String} result (already in its
	 *       canonical textual form, e.g. {@code "example.org"} or
	 *       {@code "[2001:db8::1]"});</li>
	 *   <li>index 2: the optional port as an {@link Integer} result, or
	 *       {@code null} if absent.</li>
	 * </ol>
	 *
	 * <p>No additional validation is performed here: the parser is responsible
	 * for enforcing the grammar, and this constructor only maps the match
	 * structure to the internal fields.
	 *
	 * @param authorityMatch parsed match for the authority production
	 */
	IRIAuthority(ListMatch authorityMatch) {
		this.user = (String) authorityMatch.result().get(0).result();
		this.host = (String) authorityMatch.result().get(1).result();
		this.port = (Integer) authorityMatch.result().get(2).result();
	}

	/**
	 * Creates an {@code IRIAuthority} from its three logical components.
	 * <p>
	 * This constructor is meant for programmatic construction (e.g. from an
	 * {@code IRIRef} factory or an {@code IRIManager}), as opposed to the
	 * parser-based constructor which consumes a {@link ListMatch}.
	 *
	 * <p>No syntactic validation is performed here: callers are assumed to
	 * provide already-valid components according to RFC 3986 / 3987 and to
	 * whatever normalization policy is in effect:
	 * <ul>
	 *   <li>{@code user} – userinfo without the trailing {@code '@'}, or
	 *       {@code null} if absent;</li>
	 *   <li>{@code host} – host or IP-literal in textual form
	 *       (e.g. {@code "example.org"}, {@code "[2001:db8::1]"}), or
	 *       {@code null} if the authority is empty;</li>
	 *   <li>{@code port} – port number, or {@code null} if absent.</li>
	 * </ul>
	 *
	 * @param user userinfo part, without the trailing {@code '@'}, or {@code null}
	 * @param host host name or IP-literal, or {@code null} for an empty authority
	 * @param port port number, or {@code null} if not specified
	 */
	IRIAuthority(String user, String host, Integer port) {
		this.user = user;
		this.host = host;
		this.port = port;
	}

	/**
	 * Copy constructor.
	 * <p>
	 * Creates a new {@code IRIAuthority} instance with the same component
	 * values as {@code other}. Since all fields are immutable value types
	 * ({@link String} and {@link Integer}), this is effectively a shallow
	 * but fully safe copy: mutating the new instance will never affect
	 * {@code other}, and vice versa.
	 *
	 * @param other authority to copy; must not be {@code null}
	 */
	IRIAuthority(IRIAuthority other) {
		this.user = other.user;
		this.host = other.host;
		this.port = other.port;
	}

	// =================================================================================================================
	// UTILS
	// =================================================================================================================

	/**
	 * Returns the textual form of this authority, as it would appear inside an IRI.
	 * <p>
	 * This is equivalent to {@link #recompose()}, i.e. it produces:
	 * <pre>
	 *   [ user "@" ] host [ ":" port ]
	 * </pre>
	 * without the leading {@code "//"}.
	 */
	@Override
	public String toString() {
		return recompose();
	}

	/**
	 * Creates a defensive copy of the given {@code IRIAuthority}, or {@code null}
	 * if the argument is {@code null}.
	 * <p>
	 * This is a convenience wrapper around the copy constructor that handles
	 * {@code null} gracefully. It is useful when copying optional authority
	 * components from an {@code IRIRef}:
	 *
	 * <pre>
	 *   this.authority = IRIAuthority.copy(other.authority);
	 * </pre>
	 *
	 * Because {@link IRIAuthority} only contains immutable value types
	 * ({@link String}, {@link Integer}), the returned instance is fully
	 * independent but cheap to create.
	 *
	 * @param authority the authority to copy, or {@code null}
	 * @return a new {@code IRIAuthority} with the same values as {@code authority},
	 *         or {@code null} if {@code authority} is {@code null}
	 */
	static IRIAuthority copy(IRIAuthority authority) {
		if (authority == null) {
			return null;
		} else {
			return new IRIAuthority(authority);
		}
	}


	/**
	 * Compares this {@code IRIAuthority} to another object for structural equality.
	 * <p>
	 * Two authorities are considered equal if and only if they are of the same
	 * runtime class and their three components compare equal:
	 * <ul>
	 *   <li>{@code user} are both equal (or both {@code null}),</li>
	 *   <li>{@code host} are both equal (or both {@code null}),</li>
	 *   <li>{@code port} are both equal (or both {@code null}).</li>
	 * </ul>
	 *
	 * <p>Note that this is purely a component-wise comparison; it does not perform
	 * any additional normalization (e.g. case-folding of host names) beyond what
	 * has already been applied when constructing the instances.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		IRIAuthority that = (IRIAuthority) other;
		return Objects.equals(this.user, that.user)
				&& Objects.equals(this.host, that.host)
				&& Objects.equals(this.port, that.port);
	}

	/**
	 * Computes a hash code consistent with {@link #equals(Object)}.
	 * <p>
	 * The hash code is derived from the three logical components of the
	 * authority: {@code user}, {@code host}, and {@code port}. Two
	 * {@code IRIAuthority} instances that are {@linkplain #equals(Object) equal}
	 * will therefore always have the same hash code, making this class safe to
	 * use as a key in hash-based collections.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(user, host, port);
	}

	// =================================================================================================================
	// GETTERS
	// =================================================================================================================

	/**
	 * Returns the userinfo component of this authority.
	 * <p>
	 * This corresponds to the part that would appear before {@code '@'}
	 * in the textual form, e.g. {@code "user"} in {@code "user@example.org"}.
	 *
	 * @return the userinfo string, or {@code null} if no userinfo is present
	 */
	String getUser() {
		return this.user;
	}

	/**
	 * Returns the host component of this authority.
	 * <p>
	 * This is the host name or IP-literal in canonical textual form, e.g.
	 * {@code "example.org"}, {@code "localhost"}, or {@code "[2001:db8::1]"}.
	 * For an empty authority (e.g. {@code file:///path}), the host is the
	 * empty string {@code ""}.
	 *
	 * @return the host string, possibly empty but never {@code null}
	 */
	String getHost() {
		return this.host;
	}

	/**
	 * Returns the port component of this authority.
	 * <p>
	 * If present, this is the numeric port that appears after {@code ':'}
	 * in the textual authority (e.g. {@code 8080} in {@code "host:8080"}).
	 *
	 * @return the port number, or {@code null} if no port is specified
	 */
	Integer getPort() {
		return this.port;
	}


	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================

	/**
	 * Reconstructs the textual authority component, including the leading
	 * {@code "//"}, into a fresh {@link String}.
	 * <p>
	 * The result has the form:
	 * <pre>
	 *   "//" [ user "@" ] host [ ":" port ]
	 * </pre>
	 * where missing components are simply omitted.
	 *
	 * @return the authority as it should appear in an IRI (starting with {@code "//"})
	 */
	String recompose() {
		return this.recompose(new StringBuilder()).toString();
	}

	/**
	 * Appends the textual authority component to the given {@link StringBuilder}.
	 * <p>
	 * This method writes:
	 * <pre>
	 *   "//" [ user "@" ] host [ ":" port ]
	 * </pre>
	 * to {@code builder}, using the current values of {@code user}, {@code host}
	 * and {@code port}. Missing components are skipped.
	 *
	 * @param builder the builder to append to
	 * @return the same builder instance, for chaining
	 */
	StringBuilder recompose(StringBuilder builder) {
		builder.append(AUTHORITY_SEPARATOR);
		this.recomposeUser(builder);
		this.recomposeHost(builder);
		this.recomposePort(builder);
		return builder;
	}

	/**
	 * Appends the userinfo part of this authority to the given builder, if present.
	 * <p>
	 * The userinfo is written as:
	 * <pre>
	 *   user "@"
	 * </pre>
	 * and is omitted entirely if {@code user == null}.
	 *
	 * @param builder the builder to append to
	 * @return the same builder instance, for chaining
	 */
	StringBuilder recomposeUser(StringBuilder builder) {
		if (this.user != null) {
			builder.append(this.user).append(USER_SEPARATOR);
		}
		return builder;
	}

	/**
	 * Appends the host part of this authority to the given builder, if present.
	 * <p>
	 * The host is written exactly as stored (e.g. {@code "example.org"},
	 * {@code "localhost"}, or {@code "[2001:db8::1]"}). For empty authorities,
	 * the host may be the empty string {@code ""}. If {@code host == null},
	 * nothing is appended.
	 *
	 * @param builder the builder to append to
	 * @return the same builder instance, for chaining
	 */
	StringBuilder recomposeHost(StringBuilder builder) {
		if (this.host != null) { // I don't know if this is necessary
			builder.append(this.host);
		}
		return builder;
	}

	/**
	 * Appends the port part of this authority to the given builder, if present.
	 * <p>
	 * The port is written as:
	 * <pre>
	 *   ":" port
	 * </pre>
	 * and is omitted entirely if {@code port == null}.
	 *
	 * @param builder the builder to append to
	 * @return the same builder instance, for chaining
	 */
	StringBuilder recomposePort(StringBuilder builder) {
		if (this.port != null) {
			builder.append(PORT_SEPARATOR).append(this.port);
		}
		return builder;
	}

	/**
	 * Returns the character length of this authority as it would be rendered
	 * by {@link #recompose()}, without actually building the {@link String}.
	 * <p>
	 * Concretely, this counts:
	 * <ul>
	 *   <li>{@code 2} characters for the leading {@code "//"},</li>
	 *   <li>{@code host.length()} (the host is assumed non-{@code null}),</li>
	 *   <li>if {@code user != null}, {@code user.length() + 1} character
	 *       for {@code user} plus the trailing {@code '@'},</li>
	 *   <li>if {@code port != null}, {@code port.toString().length() + 1}
	 *       characters for the decimal port and the preceding {@code ':'}.</li>
	 * </ul>
	 *
	 * <p>This method is used in length-based heuristics (e.g. deciding whether
	 * a relative form is “worth it”) where we only need a size estimate rather
	 * than the full string.
	 *
	 * @return the number of characters in the textual authority, including the
	 *         leading {@code "//"}
	 */
	int recompositionLength() {
		int result = this.host.length() + 2;
		if (this.user != null) {
			result += this.user.length() + 1;
		}
		if (this.port != null) {
			result += this.port.toString().length() + 1;
		}
		return result;
	}

	// =================================================================================================================
	// NORMALISATION (TODO)
	// =================================================================================================================
}
