/**
 * Core representation of IRI references and their components.
 * <p>
 * This package provides the main building blocks for working with
 * Internationalized Resource Identifiers (IRIs) as defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC&nbsp;3986</a>
 * and <a href="https://datatracker.ietf.org/doc/html/rfc3987">RFC&nbsp;3987</a>.
 * The central class is {@link fr.inria.jfbaget.irirefs.IRIRef}, which models a
 * full IRI reference and offers parsing, recomposition, resolution and
 * relativization primitives.
 * </p>
 *
 * <h2>Main types</h2>
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.IRIRef} –
 *       high-level representation of an IRI reference, with accessors for
 *       scheme, authority, path, query and fragment, and methods for:
 *       <ul>
 *         <li>parsing from a string (optionally after a
 *             {@code StringPreparator}),</li>
 *         <li>recomposing the IRI string,</li>
 *         <li>resolving against a base IRI (RFC&nbsp;3986&nbsp;§5.2),</li>
 *         <li>relativizing against a base IRI with length-aware heuristics,</li>
 *         <li>delegating normalization to an {@code IRINormalizer}.</li>
 *       </ul>
 *   </li>
 *   <li>{@link fr.inria.jfbaget.irirefs.IRIAuthority} –
 *       internal representation of the authority component (userinfo, host,
 *       port). This class is package-private and is not intended to be used
 *       directly by library consumers; it is exposed via {@code IRIRef} getters
 *       such as {@code getHost()} and {@code getPort()}.</li>
 *   <li>{@link fr.inria.jfbaget.irirefs.IRIPath} –
 *       internal canonical representation of the path component, modeled as a
 *       non-empty list of string segments. It implements RFC-compliant
 *       dot-segment removal, path resolution and path relativization, and is
 *       used exclusively from {@code IRIRef} and related code.</li>
 * </ul>
 *
 * <h2>Parsing and validation</h2>
 * <p>
 * {@link fr.inria.jfbaget.irirefs.IRIRef} instances are constructed from
 * strings using a dedicated parser in the {@code parser} package based on
 * NanoParse and a grammar that follows RFC&nbsp;3987 as closely as possible.
 * The parsing phase is responsible for syntactic validation; the classes in
 * this package assume that parsed components are already well-formed.
 * </p>
 *
 * <h2>Resolution and relativization</h2>
 * <p>
 * Resolution of a reference against a base IRI is implemented by
 * {@link fr.inria.jfbaget.irirefs.IRIRef#resolveInPlace(IRIRef, boolean)}
 * (and its non-mutating variants), which delegate the path-merge step to
 * {@link fr.inria.jfbaget.irirefs.IRIPath}. The algorithm closely follows
 * RFC&nbsp;3986&nbsp;§5.2, including dot-segment removal.
 * </p>
 * <p>
 * Relativization is implemented by
 * {@link fr.inria.jfbaget.irirefs.IRIRef#relativize(IRIRef)} and
 * {@link fr.inria.jfbaget.irirefs.IRIPath#relativize(IRIPath, boolean, int)}.
 * Conceptually, the goal is to find a relative reference {@code r} such that
 * resolving {@code r} against the base reconstructs the original IRI, and,
 * among all such forms, to choose one with minimal recomposed length when it
 * is beneficial to do so.
 * </p>
 *
 * <h2>Normalization</h2>
 * <p>
 * Normalization is deliberately decoupled from parsing and representation.
 * An {@link fr.inria.jfbaget.irirefs.normalizer.IRINormalizer} implementation
 * controls:
 * </p>
 * <ul>
 *   <li>scheme, host and userinfo casing and Unicode form,</li>
 *   <li>percent-encoding canonicalization,</li>
 *   <li>optional dot-segment removal on paths,</li>
 *   <li>scheme-based adjustments (e.g. default-port suppression).</li>
 * </ul>
 * <p>
 * {@link fr.inria.jfbaget.irirefs.IRIRef#normalizeInPlace(
 * fr.inria.jfbaget.irirefs.normalizer.IRINormalizer)} orchestrates this by
 * calling into {@code IRIAuthority} and {@code IRIPath}. Different
 * {@code IRINormalizer} configurations allow for RFC-style syntactic
 * normalization or more application-specific canonical forms.
 * </p>
 *
 * <h2>Equality and recommended usage</h2>
 * <p>
 * {@link fr.inria.jfbaget.irirefs.IRIRef#equals(Object)} compares IRI
 * references based on their recomposed string representation. For semantic
 * equality under a given normalization policy (case-folding, percent-decoding,
 * etc.), callers are encouraged to normalize IRIs first using the same
 * {@code IRINormalizer}, then compare the resulting {@code IRIRef} instances.
 * </p>
 *
 * <h2>Related packages</h2>
 * <ul>
 *   <li>{@code fr.inria.jfbaget.irirefs.parser} –
 *       NanoParse-based parser and validator for IRI strings.</li>
 *   <li>{@code fr.inria.jfbaget.irirefs.normalizer} –
 *       pluggable normalization strategies ({@code IRINormalizer},
 *       {@code StandardComposableNormalizer}, {@code ExtendedComposableNormalizer}).</li>
 *   <li>{@code fr.inria.jfbaget.irirefs.preparator} –
 *       string pre-processing before parsing (HTML/XML entity decoding, etc.).</li>
 *   <li>{@code fr.inria.jfbaget.irirefs.manager} –
 *       higher-level IRI manager that combines base IRIs, prefixes, parsing,
 *       resolution and normalization for application use.</li>
 * </ul>
 */
package fr.inria.jfbaget.irirefs;

