/**
 * Parser and validation support for IRIs and IRI references.
 * <p>
 * This package provides the NanoParse-based grammar and related helpers
 * used to implement {@code IRIRef}:
 * </p>
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.parser.IRIRefParser} –
 *       sets up all grammar rules for RFC&nbsp;3987 IRIs and IRI references
 *       (full IRI, absolute-IRI, relative reference, and components such as
 *       scheme, authority, path, query and fragment). It is used internally
 *       by {@code IRIRef} to parse input strings.</li>
 *
 *   <li>{@link fr.inria.jfbaget.irirefs.parser.IRIRefValidator} –
 *       a small helper built on top of {@code IRIRefParser} that validates
 *       individual components (scheme, host, path, etc.) in isolation.
 *       It is not yet used by {@code IRIRef}, but is intended to support
 *       future component-based constructors such as
 *       {@code new IRIRef(scheme, host, path, ...)} where each part is
 *       checked independently.</li>
 * </ul>
 *
 * <h2>Intended usage</h2>
 * <p>
 * For most applications, {@link fr.inria.jfbaget.irirefs.IRIRef} is the
 * primary entry point and there is no need to interact with this package
 * directly. The classes here are useful for:
 * </p>
 * <ul>
 *   <li>internal parsing and validation within the library,</li>
 *   <li>tests that need to exercise specific grammar rules,</li>
 *   <li>advanced users who want low-level access to the RFC&nbsp;3987 grammar
 *       or to validate components separately.</li>
 * </ul>
 *
 * <p>
 * The NanoParse API itself is not documented here; readers interested in the
 * underlying parser combinators should refer to the NanoParse documentation.
 * </p>
 */
package fr.inria.jfbaget.irirefs.parser;