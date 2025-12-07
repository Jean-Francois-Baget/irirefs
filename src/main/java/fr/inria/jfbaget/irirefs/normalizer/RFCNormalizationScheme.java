package fr.inria.jfbaget.irirefs.normalizer;

/**
 * Normalization schemes controlling which RFC&nbsp;3987 (and related) steps
 * are used in {@link StandardComposableNormalizer} and its subclasses.
 * <p>
 * Each {@code RFCNormalizationScheme} value represents a single, independent
 * normalization feature (case folding, character normalization,
 * percent-decoding, path normalization, etc.).
 * </p>
 * <p>
 * These schemes correspond directly to the normalization categories defined
 * in RFC&nbsp;3987 section&nbsp;5.3:
 * </p>
 * <ul>
 *   <li>{@link #STRING} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.1">
 *       RFC&nbsp;3987, Simple String Comparison</a>. When used alone, no
 *       transformation is performed and the normalized IRI is identical to
 *       the parsed one.</li>
 *
 *   <li>{@link #SYNTAX} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2">
 *       RFC&nbsp;3987, Syntax-Based Normalization</a>. It is a macro-scheme
 *       that automatically enables {@link #CASE}, {@link #CHARACTER} and
 *       {@link #PCT}. Note that, although RFC&nbsp;3987 considers path
 *       segment normalization ({@link #PATH}) as part of syntax-based
 *       normalization, {@code PATH} is <em>not</em> implicitly included here:
 *       it must be requested explicitly by the caller. This avoids performing
 *       dot-segment removal in situations where it is not desired or not
 *       allowed (e.g. on relative references).</li>
 *
 *   <li>{@link #CASE} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.1">
 *       RFC&nbsp;3987, Case Normalization</a>. It normalizes hexadecimal
 *       digits in percent-encoding triplets to uppercase and may normalize
 *       some components (such as the scheme, or ASCII-only hosts) to lowercase,
 *       depending on the chosen {@link fr.inria.jfbaget.irirefs.normalizer.IRINormalizer}
 *       implementation.</li>
 *
 *   <li>{@link #CHARACTER} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.2">
 *       RFC&nbsp;3987, Character Normalization</a>. It puts components into
 *       Unicode Normalization Form C (NFC), as defined in
 *       <a href="https://www.unicode.org/reports/tr15/tr15-23.html">
 *       Unicode Standard Annex #15</a>.</li>
 *
 *   <li>{@link #PCT} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.3">
 *       RFC&nbsp;3987, Percent-Encoding Normalization</a>. In the standard
 *       {@link StandardComposableNormalizer}, it decodes percent-encoded
 *       octets only when they correspond to an RFC&nbsp;3986
 *       {@code unreserved} character. In {@link ExtendedComposableNormalizer},
 *       the same flag can be interpreted more aggressively and used to decode
 *       sequences of percent-encoded octets when they represent an RFC&nbsp;3987
 *       {@code iunreserved} character.</li>
 *
 *   <li>{@link #PATH} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.4">
 *       RFC&nbsp;3987, Path Segment Normalization</a>. It controls dot-segment
 *       processing (removal of {@code "."} and {@code ".."} segments) when the
 *       {@link IRINormalizer} decides that such normalization is allowed for a
 *       given reference.</li>
 *
 *   <li>{@link #SCHEME} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.3">
 *       RFC&nbsp;3987, Scheme-Based Normalization</a>. It enables
 *       scheme-specific rules, such as removing an explicit port when it
 *       matches the default port for the given scheme, or normalizing empty
 *       paths in the presence of an authority.</li>
 *
 *   <li>{@link #PROTOCOL} corresponds to
 *       <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.4">
 *       RFC&nbsp;3987, Protocol-Based Normalization</a>. It is intended for
 *       application-dependent, protocol-specific normalization rules that may
 *       go beyond RFC&nbsp;3987 and should typically be implemented in a
 *       user-defined subclass of {@link StandardComposableNormalizer} or
 *       {@link ExtendedComposableNormalizer}.</li>
 * </ul>
 */
public enum RFCNormalizationScheme {

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.1">
     * RFC&nbsp;3987, Simple String Comparison</a>.
     * When used alone, no transformation is performed and the normalized
     * IRI is identical to the parsed one.
     */
    STRING,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2">
     * RFC&nbsp;3987, Syntax-Based Normalization</a>.
     * <p>
     * This is a macro-scheme that automatically enables {@link #CASE},
     * {@link #CHARACTER} and {@link #PCT}. Although RFC&nbsp;3987 also
     * associates path segment normalization ({@link #PATH}) with syntax-based
     * normalization, {@code PATH} is not implicitly enabled here and must be
     * requested explicitly by the caller.
     * </p>
     */
    SYNTAX,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.1">
     * RFC&nbsp;3987, Case Normalization</a>.
     * It normalizes hexadecimal digits in percent-encoding triplets to
     * uppercase and may normalize certain components (for example, the
     * scheme or ASCII-only hosts) to lowercase.
     */
    CASE,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.2">
     * RFC&nbsp;3987, Character Normalization</a>.
     * It puts components into Unicode Normalization Form C (NFC).
     */
    CHARACTER,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.3">
     * RFC&nbsp;3987, Percent-Encoding Normalization</a>.
     * <p>
     * In {@link StandardComposableNormalizer}, this decodes percent-encoded
     * octets only when they represent an RFC&nbsp;3986 {@code unreserved}
     * character. In {@link ExtendedComposableNormalizer}, the same scheme
     * may be used to perform a more aggressive (non-standard) normalization,
     * decoding sequences of percent-encoded octets when they represent an
     * RFC&nbsp;3987 {@code iunreserved} character.
     * </p>
     */
    PCT,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.4">
     * RFC&nbsp;3987, Path Segment Normalization</a>.
     * It controls dot-segment processing (removal of {@code "."} and
     * {@code ".."} segments) on paths when allowed by the
     * {@link IRINormalizer} implementation.
     */
    PATH,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.3">
     * RFC&nbsp;3987, Scheme-Based Normalization</a>.
     * It enables scheme-specific rules, such as removing explicit ports
     * when they match the default for the scheme, or normalizing empty
     * paths in the presence of an authority.
     */
    SCHEME,

    /**
     * Corresponds to
     * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.4">
     * RFC&nbsp;3987, Protocol-Based Normalization</a>.
     * Intended for application- and protocol-specific normalization rules
     * that may go beyond RFC&nbsp;3987, typically implemented in user-defined
     * subclasses of {@link StandardComposableNormalizer} or
     * {@link ExtendedComposableNormalizer}.
     */
    PROTOCOL
}