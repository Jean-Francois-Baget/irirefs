package fr.inria.jfbaget.irirefs;

import fr.inria.jfbaget.irirefs.normalizer.IRINormalizer;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;

import java.util.Objects;

/**
 * Represents the <em>authority</em> component of an IRI, as defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC&nbsp;3986</a>
 * and <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>.
 * <p>
 * In the IRI syntax, the authority appears (when present) after the scheme and
 * {@code "//"}, and before the path:
 * </p>
 *
 * <pre>
 *   "//" [ userinfo "@" ] host [ ":" port ]
 * </pre>
 *
 * <p>
 * This class stores the three logical parts:
 * </p>
 * <ul>
 *   <li>{@code user} – the optional userinfo (without the trailing {@code '@'}),</li>
 *   <li>{@code host} – the host name or IP-literal (in canonical string form,
 *       e.g. {@code "example.org"} or {@code "[2001:db8::1]"}),</li>
 *   <li>{@code port} – the optional port number, or {@code null} if not specified.</li>
 * </ul>
 *
 * <p>
 * It is package-private on purpose: library users are expected to interact with
 * {@link IRIRef}, which exposes higher-level getters such as
 * {@code IRIRef.getHost()} and recomposes full IRI strings. {@code IRIAuthority}
 * is an internal helper used during parsing, normalization, resolution and
 * recomposition.
 * </p>
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>If there is <strong>no authority at all</strong>, the enclosing
 *       {@code IRIRef} holds {@code authority == null} and no
 *       {@code IRIAuthority} instance is created.</li>
 *   <li>If there is an <strong>empty authority</strong> (for example
 *       {@code "file:///path"}), then an {@code IRIAuthority} exists with
 *       {@code host.equals("")} (the empty string). In other words, the presence
 *       of an authority is represented by a non-{@code null} {@code IRIAuthority}
 *       whose {@code host} field is never {@code null}.</li>
 *   <li>The {@code port} field is {@code null} both when no {@code ":port"}
 *       part is present and when the port is syntactically empty (e.g.
 *       {@code "example.org:"}); this class does not distinguish those two
 *       cases and only exposes a numeric port when it has been parsed.</li>
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
	 * RFC&nbsp;3986 / 3987.
	 * </p>
	 * <p>
	 * The {@code ListMatch} is expected to have exactly three components in
	 * this order:
	 * </p>
	 * <ol>
	 *   <li>index&nbsp;0: the optional userinfo as a {@link String} result, or
	 *       {@code null} if absent;</li>
	 *   <li>index&nbsp;1: the host as a {@link String} result (in canonical
	 *       textual form, e.g. {@code "example.org"} or {@code "[2001:db8::1]"}).
	 *       For an empty authority (e.g. {@code "file:///path"}), this is the
	 *       empty string {@code ""} and never {@code null};</li>
	 *   <li>index&nbsp;2: the optional port as an {@link Integer} result, or
	 *       {@code null} if no numeric port is present. This implementation does
	 *       not distinguish between “no port” and an explicitly empty port
	 *       (e.g. {@code "example.org:"}).</li>
	 * </ol>
	 *
	 * <p>
	 * No additional validation is performed here: the parser is responsible
	 * for enforcing the grammar, and this constructor only maps the match
	 * structure to the internal fields.
	 * </p>
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
	 * This constructor is meant for programmatic construction (for example
	 * from an {@code IRIRef} factory), as opposed to the parser-based
	 * constructor which consumes a {@link ListMatch}.
	 * </p>
	 * <p>
	 * No syntactic validation is performed here: callers are expected to
	 * provide components that are already valid according to RFC&nbsp;3986 /
	 * 3987 and to respect the invariants documented on this class. In
	 * particular:
	 * </p>
	 * <ul>
	 *   <li>{@code user} – userinfo without the trailing {@code '@'}, or
	 *       {@code null} if absent;</li>
	 *   <li>{@code host} – host name or IP-literal in textual form
	 *       (e.g. {@code "example.org"}, {@code "[2001:db8::1]"}). For an
	 *       empty authority, callers should pass the empty string {@code ""},
	 *       not {@code null};</li>
	 *   <li>{@code port} – numeric port, or {@code null} if no port is
	 *       specified or if the port is syntactically empty.</li>
	 * </ul>
	 *
	 * @param user userinfo part, without the trailing {@code '@'}, or {@code null}
	 * @param host host name or IP-literal; should be non-{@code null}, and the
	 *             empty string {@code ""} should be used for an empty authority
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
	 * but fully safe copy: subsequent changes to the new instance will never
	 * affect {@code other}, and vice versa.
	 * </p>
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
	 * Returns the textual form of this authority, including the leading {@code "//"},
	 * as it would appear inside a full IRI.
	 * <p>
	 * This is equivalent to calling {@link #recompose()}, i.e. it produces:
	 * </p>
	 *
	 * <pre>
	 *   "//" [ userinfo "@" ] host [ ":" port ]
	 * </pre>
	 *
	 * @return the authority string including the leading {@code "//"}
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
	 * {@code null} gracefully. It is typically used when copying optional
	 * authorities inside an {@link IRIRef}:
	 * </p>
	 *
	 * <pre>
	 *   this.authority = IRIAuthority.copy(other.authority);
	 * </pre>
	 *
	 * <p>
	 * Because {@code IRIAuthority} only contains immutable value types
	 * ({@link String}, {@link Integer}), the returned instance is fully
	 * independent but cheap to create. The invariants documented on
	 * {@link IRIAuthority} (notably that {@code host} is never {@code null}
	 * for a non-{@code null} authority) are preserved.
	 * </p>
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
	 * </p>
	 * <ul>
	 *   <li>{@code user} are both equal (or both {@code null}),</li>
	 *   <li>{@code host} are both equal (or both {@code null}),</li>
	 *   <li>{@code port} are both equal (or both {@code null}).</li>
	 * </ul>
	 *
	 * <p>
	 * This is purely a component-wise comparison; it does not perform any
	 * additional normalization (for example case-folding of host names or
	 * default-port suppression) beyond whatever has already been applied when
	 * constructing the instances. In particular, callers are encouraged to
	 * normalize enclosing {@link IRIRef} instances using the same
	 * {@link IRINormalizer} before comparing them if semantic equivalence under
	 * a given normalization policy is desired.
	 * </p>
	 *
	 * @param other the object to compare with this authority
	 * @return {@code true} if {@code other} is an {@code IRIAuthority} with the
	 *         same user, host and port, {@code false} otherwise
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
	 * will therefore always have the same hash code.
	 * </p>
	 *
	 * @return a hash code for this authority
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
	 * in the textual form, for example {@code "user"} in
	 * {@code "user@example.org"}.
	 * </p>
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
	 * For an empty authority (for example {@code "file:///path"}), the host is
	 * the empty string {@code ""}. By construction, the host is never
	 * {@code null} for a non-{@code null} {@code IRIAuthority} instance.
	 * </p>
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
	 * in the textual authority (for example {@code 8080} in
	 * {@code "host:8080"}).
	 * </p>
	 * <p>
	 * This implementation does not distinguish between an absent {@code ":port"}
	 * part and an explicitly empty port (for example {@code "host:"}):
	 * in both cases this method returns {@code null}. Only a syntactically
	 * valid numeric port is exposed as a non-{@code null} value.
	 * </p>
	 *
	 * @return the port number, or {@code null} if no numeric port is available
	 */
	Integer getPort() {
		return this.port;
	}


	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================

	/**
	 * Reconstructs the textual authority component into a fresh {@link String},
	 * including the leading {@code "//"}.
	 * <p>
	 * The result has the form:
	 * </p>
	 *
	 * <pre>
	 *   "//" [ userinfo "@" ] host [ ":" port ]
	 * </pre>
	 *
	 * <p>
	 * where missing components are simply omitted. The {@code host} component is
	 * always written (possibly as the empty string for an empty authority).
	 * </p>
	 *
	 * @return the authority as it should appear in an IRI (starting with {@code "//"})
	 */
	String recompose() {
		return this.recompose(new StringBuilder()).toString();
	}

	/**
	 * Appends the textual authority component to the given {@link StringBuilder},
	 * including the leading {@code "//"}.
	 * <p>
	 * This method writes:
	 * </p>
	 *
	 * <pre>
	 *   "//" [ userinfo "@" ] host [ ":" port ]
	 * </pre>
	 *
	 * <p>
	 * using the current values of {@code user}, {@code host} and {@code port}.
	 * Missing components are skipped (except for {@code host}, which is expected
	 * to be non-{@code null} and may be the empty string for an empty authority).
	 * </p>
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
	 * </p>
	 *
	 * <pre>
	 *   userinfo "@"
	 * </pre>
	 *
	 * <p>
	 * and is omitted entirely if {@code user == null}.
	 * </p>
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
	 * Appends the host part of this authority to the given builder.
	 * <p>
	 * The host is written exactly as stored (for example {@code "example.org"},
	 * {@code "localhost"}, or {@code "[2001:db8::1]"}). For empty authorities,
	 * the host may be the empty string {@code ""}.
	 * </p>
	 * <p>
	 * By design, the {@code host} field should never be {@code null} for a
	 * non-{@code null} {@code IRIAuthority} instance; the {@code null} check
	 * here is purely defensive.
	 * </p>
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
	 * </p>
	 *
	 * <pre>
	 *   ":" port
	 * </pre>
	 *
	 * <p>
	 * and is omitted entirely if {@code port == null}.
	 * </p>
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
	 * by {@link #recompose()}, without actually building a {@link String}.
	 * <p>
	 * Concretely, this counts:
	 * </p>
	 * <ul>
	 *   <li>{@code 2} characters for the leading {@code "//"},</li>
	 *   <li>{@code host.length()} (the host is assumed non-{@code null}),</li>
	 *   <li>if {@code user != null}, {@code user.length() + 1} characters
	 *       for {@code user} plus the trailing {@code '@'},</li>
	 *   <li>if {@code port != null}, {@code port.toString().length() + 1}
	 *       characters for the decimal port and the preceding {@code ':'}.</li>
	 * </ul>
	 *
	 * <p>
	 * This method is used in length-based heuristics (for example when deciding
	 * whether a relative form of an IRI is “worth it”) where only a size estimate
	 * is needed rather than the full authority string.
	 * </p>
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
	// NORMALISATION
	// =================================================================================================================

	/**
	 * Applies the given {@link IRINormalizer} to this authority in place.
	 * <p>
	 * This method delegates the normalization of each component to the
	 * corresponding hooks on {@link IRINormalizer}, passing along the
	 * enclosing IRI's scheme as context:
	 * </p>
	 * <ul>
	 *   <li>{@link IRINormalizer#normalizeUserInfo(String, String)} is applied
	 *       to the {@code user} field,</li>
	 *   <li>{@link IRINormalizer#normalizeHost(String, String)} is applied to
	 *       the {@code host} field,</li>
	 *   <li>{@link IRINormalizer#normalizePort(Integer, String)} is applied to
	 *       the {@code port} field (for example to drop default ports for a
	 *       given scheme).</li>
	 * </ul>
	 *
	 * <p>
	 * No additional logic is performed here: the {@code IRIAuthority} merely
	 * forwards its components to the normalizer and stores the results. The
	 * detailed behavior (case-folding of host names, Unicode normalization,
	 * default-port suppression, etc.) is entirely determined by the chosen
	 * {@link IRINormalizer} implementation and its configuration.
	 * </p>
	 *
	 * @param normalizer the normalization strategy to apply; must not be {@code null}
	 * @param scheme     the scheme of the enclosing IRI, or {@code null} if unknown
	 */
	void normalizeInPlace(IRINormalizer normalizer, String scheme) {
		this.user = normalizer.normalizeUserInfo(this.user, scheme);
		this.host = normalizer.normalizeHost(this.host, scheme);
		this.port = normalizer.normalizePort(this.port, scheme);
	}

}
