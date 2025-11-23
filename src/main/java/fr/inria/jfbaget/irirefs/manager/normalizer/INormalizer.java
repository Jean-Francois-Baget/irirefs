package fr.inria.jfbaget.irirefs.manager.normalizer;

import fr.inria.jfbaget.irirefs.IRIRef;

/**
 * Strategy interface for IRIRef normalization.
 * <p>
 * An {@code INormalizer} defines how {@link IRIRef} instances should be normalized
 * before storage, comparison, or stringification. It abstracts over different normalization
 * strategies (syntax-based, scheme-based, path simplification, etc.) and enables pluggable behavior
 * in {@code IRIManager}-like components.
 * </p>
 *
 * <p>
 * A typical implementation normalizes an IRIRef <i>in place</i> using one or more
 * calls to {@link IRIRef#normalizeInPlace(NORMALIZATION)} and returns the modified instance
 * for potential chaining.
 * </p>
 *
 * <h3>Use Case</h3>
 * This interface is designed to support modular IRI resolution frameworks, allowing different
 * applications (e.g., semantic web, DLGP, RDF parsers) to define their own normalization policies.
 * It is especially useful when you want to:
 * <ul>
 *   <li>Standardize IRI comparisons</li>
 *   <li>Ensure consistent formatting of stored or displayed IRIs</li>
 *   <li>Enforce RFC-compliant simplification of paths, schemes, ports, etc.</li>
 * </ul>
 *
 * <h3>Implementing Classes</h3>
 * Classes implementing {@code INormalizer} should document their strategy clearly and ensure
 * that normalization only applies to resolved IRIs unless explicitly safe to do otherwise.
 *
 * @see IRIRef
 * @see IRIRef#normalizeInPlace(NORMALIZATION)
 */
public interface INormalizer {
	
	/**
	 * Defines the contract for normalizing {@link IRIRef} objects.
	 * <p>
	 * Implementations are expected to apply one or more normalization strategies
	 * (e.g., syntax, scheme-based, path segment simplification) to make IRIs canonical.
	 * </p>
	 *
	 * <h3>Behavioral Notes</h3>
	 * <ul>
	 *   <li>This method <b>should</b> modify the input IRIRef <i>in place</i> and return it, to enable method chaining.</li>
	 *   <li>It <b>should</b> delegate to built-in normalization logic in {@link IRIRef},
	 *       such as {@link IRIRef#normalizeInPlace(NORMALIZATION)}.</li>
	 *   <li>An {@link IllegalArgumentException} <b>should</b> be thrown if normalization is unsafe or invalid,
	 *       such as attempting path normalization on a relative IRI —
	 *       see <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.4" target="_blank">RFC 3987, Section 5.3.2.4</a>.
	 *   </li>
	 * </ul>
	 *
	 * <h3>Example Implementation</h3>
	 * <pre><code>
	 * public IRIRef normalize(IRIRef iriref) {
	 *     return iriref
	 *         .normalizeInPlace(NORMALIZATION.SYNTAX)
	 *         .normalizeInPlace(NORMALIZATION.SCHEME);
	 * }
	 * </code></pre>
	 *
	 * @param iriref the IRIRef to normalize
	 * @return the normalized IRIRef (same instance, modified in place)
	 * @throws IllegalArgumentException if the input cannot be safely normalized
	 */
	public IRIRef normalize(IRIRef iriref) throws IllegalArgumentException;
	

}
