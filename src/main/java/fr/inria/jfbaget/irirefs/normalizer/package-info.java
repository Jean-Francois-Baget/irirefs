/**
 * Normalization strategies and utilities for {@link fr.inria.jfbaget.irirefs.IRIRef}.
 *
 * <p>
 * This package defines the abstraction and reference implementations used to
 * normalize <em>already-parsed</em> IRIs and IRI references. Parsing and
 * syntactic validation are handled elsewhere (see
 * {@link fr.inria.jfbaget.irirefs.parser}), while normalization here is an
 * optional, post-parse step that can be tuned to application needs.
 * </p>
 *
 * <h2>Core abstraction</h2>
 *
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.normalizer.IRINormalizer} – strategy
 *       interface used by {@link fr.inria.jfbaget.irirefs.IRIRef#normalizeInPlace(IRINormalizer)}
 *       to normalize each IRI component (scheme, authority, path, query,
 *       fragment). Implementations are expected to be side-effect free and
 *       reusable.</li>
 * </ul>
 *
 * <h2>Reference implementations</h2>
 *
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer} –
 *       RFC&nbsp;3987–oriented normalizer built from a set of composable
 *       {@link fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme}s
 *       (CASE, CHARACTER, PCT, PATH, SCHEME, etc.). It follows the
 *       syntax-based and scheme-based normalization rules of RFC&nbsp;3987,
 *       with conservative handling of percent-encodings (only decoding
 *       RFC&nbsp;3986 {@code unreserved} characters).</li>
 *
 *   <li>{@link fr.inria.jfbaget.irirefs.normalizer.ExtendedComposableNormalizer} –
 *       a non-standard, more aggressive variant that interprets
 *       {@link fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme#PCT}
 *       using UTF-8 and RFC&nbsp;3987 {@code iunreserved} characters. It is
 *       intended for applications that want IRIs to be canonicalized as
 *       UTF-8-based identifiers.</li>
 * </ul>
 *
 * <h2>Configuration via schemes</h2>
 *
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme} –
 *       enumeration of normalization features corresponding to RFC&nbsp;3987
 *       §5.3 (STRING, SYNTAX, CASE, CHARACTER, PCT, PATH, SCHEME, PROTOCOL).
 *       Callers compose these flags to obtain the desired normalization
 *       behavior when constructing a {@code *ComposableNormalizer}.</li>
 * </ul>
 *
 * <p>
 * Library users typically:
 * </p>
 * <ol>
 *   <li>Parse an input string into an {@link fr.inria.jfbaget.irirefs.IRIRef},</li>
 *   <li>Choose or implement an {@link fr.inria.jfbaget.irirefs.normalizer.IRINormalizer}
 *       policy (for example a {@link fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer}
 *       with a specific set of {@link fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme}s),</li>
 *   <li>Apply it via {@link fr.inria.jfbaget.irirefs.IRIRef#normalizeInPlace(IRINormalizer)}
 *       or {@link fr.inria.jfbaget.irirefs.IRIRef#normalize(IRINormalizer)} to obtain a
 *       normalized IRI suitable for comparison or storage.</li>
 * </ol>
 */
package fr.inria.jfbaget.irirefs.normalizer;