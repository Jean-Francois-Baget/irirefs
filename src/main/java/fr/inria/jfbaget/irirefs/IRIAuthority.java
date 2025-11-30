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

	IRIAuthority(ListMatch authorityMatch) {
		this.user = (String) authorityMatch.result().get(0).result();
		this.host = (String) authorityMatch.result().get(1).result();
		this.port = (Integer) authorityMatch.result().get(2).result();
	}

	IRIAuthority(String user, String host, Integer port) {
		this.user = user;
		this.host = host;
		this.port = port;
	}

	IRIAuthority(IRIAuthority other) {
		this.user = other.user;
		this.host = other.host;
		this.port = other.port;
	}

	// =================================================================================================================
	// UTILS
	// =================================================================================================================

	@Override
	public String toString() {
		return recompose();
	}

	static IRIAuthority copy(IRIAuthority authority) {
		if (authority == null) {
			return null;
		} else {
			return new IRIAuthority(authority);
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		IRIAuthority that = (IRIAuthority) other;
		return Objects.equals(this.user, that.user)
				&& Objects.equals(this.host, that.host)
				&& Objects.equals(this.port, that.port);
	}

	@Override
	public int hashCode() {
		return Objects.hash(user, host, port);
	}

	// =================================================================================================================
	// GETTERS
	// =================================================================================================================

	String getUser() {
		return this.user;
	}
	
	String getHost() {
		return this.host;
	}
	
	Integer getPort() {
		return this.port;
	}


	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================

	String recompose() {
		return this.recompose(new StringBuilder()).toString();
	}

	StringBuilder recompose(StringBuilder builder) {
		builder.append(AUTHORITY_SEPARATOR);
		this.recomposeUser(builder);
		this.recomposeHost(builder);
		this.recomposePort(builder);
		return builder;
	}
	
	StringBuilder recomposeUser(StringBuilder builder) {
		if (this.user != null) {
			builder.append(this.user).append(USER_SEPARATOR);
		}
		return builder;
	}
	
	StringBuilder recomposeHost(StringBuilder builder) {
		if (this.host != null) { // I don't know if this is necessary
			builder.append(this.host);
		}
		return builder;
	}
	
	StringBuilder recomposePort(StringBuilder builder) {
		if (this.port != null) {
			builder.append(PORT_SEPARATOR).append(this.port);
		}
		return builder;
	}

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
