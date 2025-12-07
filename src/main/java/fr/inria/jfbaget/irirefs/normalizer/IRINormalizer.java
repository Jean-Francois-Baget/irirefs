package fr.inria.jfbaget.irirefs.normalizer;


import fr.inria.jfbaget.irirefs.IRIRef;

/**
 * Strategy interface used by {@link IRIRef} to normalize already-parsed IRI
 * components.
 * <p>
 * An {@code IRINormalizer} is not involved in parsing or validation. Instead,
 * {@link IRIRef} calls these methods <em>after</em> a string has been
 * successfully parsed into its components, and uses the returned values as
 * the normalized representation of those components.
 * </p>
 *
 * <p>
 * The primary entry point is {@link IRIRef#normalizeInPlace(IRINormalizer)},
 * which invokes this interface roughly as follows:
 * </p>
 *
 * <pre>{@code
 * public IRIRef normalizeInPlace(IRINormalizer normalizer) {
 *     this.scheme = normalizer.normalizeScheme(this.scheme);
 *     if (this.hasAuthority()) {
 *         // IRIAuthority uses the same normalizer for userinfo/host/port
 *         this.authority.normalizeInPlace(normalizer, this.scheme);
 *     }
 *     // IRIPath uses the same normalizer for path decisions and segments
 *     this.path.normalizeInPlace(normalizer, this.scheme, this.authority);
 *     this.query    = normalizer.normalizeQuery(this.query, this.scheme);
 *     this.fragment = normalizer.normalizeFragment(this.fragment, this.scheme);
 *     return this;
 * }
 * }</pre>
 *
 * <p>
 * Internally, the {@code IRIRef} implementation delegates to its authority
 * and path objects, which in turn consult this {@code IRINormalizer} for
 * userinfo/host/port normalization and for path-related decisions (such as
 * whether to replace an empty path with {@code "/"} or to remove dot-segments),
 * as well as for per-segment normalization. These internal methods are
 * package-private and not part of this interface.
 * </p>
 *
 * <p>
 * Implementations are expected to be side effect free and reusable. A single
 * {@code IRINormalizer} instance can be shared by multiple {@link IRIRef}
 * instances. The intent is to allow different normalization policies
 * (including custom, application-specific ones) without coupling
 * {@code IRIRef} to any particular implementation.
 * </p>
 */
public interface IRINormalizer {

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized scheme.
     * <p>
     * {@code scheme} is the scheme as parsed by {@code IRIRef}, or
     * {@code null} when the reference is relative. The value returned by this
     * method is stored back into the {@link IRIRef} as its scheme.
     * </p>
     *
     * @param scheme the parsed scheme, or {@code null} for a relative reference
     * @return the scheme to use in the normalized {@code IRIRef} (may be {@code null})
     */
    String normalizeScheme(String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized userinfo.
     * <p>
     * {@code user} is the userinfo component as parsed by {@code IRIRef},
     * or {@code null} if no userinfo was present. {@code scheme} is the
     * scheme of the reference, or {@code null} for a relative reference.
     * The returned value is stored back as the {@code userinfo} component.
     * </p>
     *
     * @param user   the parsed userinfo, or {@code null} if none
     * @param scheme the scheme of the reference, or {@code null} if relative
     * @return the userinfo to use in the normalized {@code IRIRef}
     *         (may be {@code null})
     */
    String normalizeUserInfo(String user, String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized host.
     * <p>
     * {@code host} is the host component as parsed by {@code IRIRef}. It is
     * never {@code null} when this method is invoked; the absence of an
     * authority is handled by {@code IRIRef} before calling this method.
     * {@code scheme} is the scheme of the reference, or {@code null} if the
     * reference is relative.
     * </p>
     *
     * @param host   the parsed host (never {@code null} when called)
     * @param scheme the scheme of the reference, or {@code null} if relative
     * @return the host to use in the normalized {@code IRIRef}
     */
    String normalizeHost(String host, String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized port.
     * <p>
     * {@code port} is the numeric port as parsed by {@code IRIRef},
     * or {@code null} if no explicit port was present. {@code scheme} is
     * the scheme of the reference, or {@code null} for a relative reference.
     * The returned value is stored back as the {@code port} component; a
     * {@code null} return value is interpreted as “no explicit port”.
     * </p>
     *
     * @param port   the parsed port, or {@code null} if none
     * @param scheme the scheme of the reference, or {@code null} if relative
     * @return the port to use in the normalized {@code IRIRef},
     *         or {@code null} if no explicit port should be kept
     */
    Integer normalizePort(Integer port, String scheme);

    /**
     * Called by {@link IRIRef} to decide whether an empty path should be
     * replaced by {@code "/"} for a given reference.
     * <p>
     * {@code scheme} is the scheme of the reference, or {@code null}
     * for a relative reference. {@code hasAuthority} indicates whether
     * an authority component was present when parsing the IRI.
     * {@code IRIRef} uses the boolean result to decide whether to
     * substitute an empty path with {@code "/"}.
     * </p>
     *
     * @param scheme       the scheme of the reference, or {@code null} if relative
     * @param hasAuthority {@code true} if an authority component is present
     * @return {@code true} if {@code IRIRef} should normalize an empty path
     *         to {@code "/"}, {@code false} otherwise
     */
    boolean shouldNormalizeEmptyWithSlash(String scheme, boolean hasAuthority);

    /**
     * Called by {@link IRIRef} to decide whether dot-segment processing
     * (removal of {@code "."} and {@code ".."} path segments) is allowed
     * for a given reference.
     * <p>
     * {@code scheme} is the scheme of the reference, or {@code null}
     * for a relative reference. {@code IRIRef} will only perform its
     * dot-segment algorithm if this method returns {@code true}.
     * </p>
     *
     * @param scheme the scheme of the reference, or {@code null} if relative
     * @return {@code true} if {@code IRIRef} may apply dot-segment removal
     *         on the path, {@code false} otherwise
     */
    boolean shouldRemoveDotsInPath(String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized
     * representation of a single path segment.
     * <p>
     * {@code segment} is a single segment as parsed by {@code IRIRef},
     * i.e. the substring between two {@code '/'} separators (or between a
     * separator and the start/end of the path). It is never {@code null}
     * when this method is invoked. {@code scheme} is the scheme of the
     * reference, or {@code null} for a relative reference.
     * The returned value is reassembled by {@code IRIRef} into the
     * normalized path.
     * </p>
     *
     * @param segment the parsed path segment (never {@code null} when called)
     * @param scheme  the scheme of the reference, or {@code null} if relative
     * @return the segment to use in the normalized path
     */
    String normalizeSegment(String segment, String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized
     * {@code query} component.
     * <p>
     * {@code query} is the substring following {@code '?'} in the parsed
     * reference, without the leading {@code '?'}, or {@code null} if no
     * query was present. {@code scheme} is the scheme of the reference,
     * or {@code null} for a relative reference. The returned value is
     * stored back as the {@code query} component of the {@code IRIRef}.
     * </p>
     *
     * @param query  the parsed query string, or {@code null} if none
     * @param scheme the scheme of the reference, or {@code null} if relative
     * @return the query to use in the normalized {@code IRIRef}
     *         (may be {@code null})
     */
    String normalizeQuery(String query, String scheme);

    /**
     * Called by {@link IRIRef} to obtain the (possibly) normalized
     * {@code fragment} component.
     * <p>
     * {@code fragment} is the substring following {@code '#'} in the parsed
     * reference, without the leading {@code '#'}, or {@code null} if no
     * fragment was present. {@code scheme} is the scheme of the reference,
     * or {@code null} for a relative reference. The returned value is stored
     * back as the {@code fragment} component of the {@code IRIRef}.
     * </p>
     *
     * @param fragment the parsed fragment, or {@code null} if none
     * @param scheme   the scheme of the reference, or {@code null} if relative
     * @return the fragment to use in the normalized {@code IRIRef}
     *         (may be {@code null})
     */
    String normalizeFragment(String fragment, String scheme);

}
