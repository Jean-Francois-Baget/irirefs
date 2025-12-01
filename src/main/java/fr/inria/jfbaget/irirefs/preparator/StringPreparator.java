package fr.inria.jfbaget.irirefs.preparator;

/**
 * Strategy interface for preprocessing raw {@link String} values before they are
 * parsed as IRIs or IRI references.
 * <p>
 * This interface is intended to support the “preparation for comparison” step
 * described in RFC&nbsp;3987&nbsp;§5.2, as well as other application-specific
 * normalisation steps (for example HTML or XML entity decoding) that must be
 * applied before constructing an {@code IRIRef}.
 * </p>
 *
 * <p>
 * The library provides a default implementation, {@code BasicStringPreparator},
 * which covers the most usual cases (such as HTML4 and XML entity decoding) but
 * is <strong>not</strong> meant to be an exhaustive or definitive implementation
 * of all possible string preparation rules. The design is intentionally
 * extensible: callers are encouraged to provide their own {@code StringPreparator}
 * implementations or to extend {@code BasicStringPreparator} when additional or
 * different preparation logic is required.
 * </p>
 *
 * <p>
 * Implementations are expected to be:
 * </p>
 * <ul>
 *   <li>side-effect free (pure functions from {@code String} to {@code String});</li>
 *   <li>thread-safe, so that the same instance can be reused safely across threads;</li>
 *   <li>deterministic: the same input should always produce the same output.</li>
 * </ul>
 *
 * <p>
 * Typical usage is to pass a {@code StringPreparator} to the {@code IRIRef}
 * constructor so that the input is prepared once, immediately before parsing.
 * </p>
 */
public interface StringPreparator {

    /**
     * Transforms the given input string according to the preparation rules
     * implemented by this instance.
     *
     * <p>
     * The exact set of transformations is implementation-dependent. Common
     * examples include:
     * </p>
     * <ul>
     *   <li>decoding of HTML or XML character entities;</li>
     *   <li>Unicode normalisation;</li>
     *   <li>any application-specific cleanup required before IRI parsing.</li>
     * </ul>
     *
     * @param input the raw input string to transform; implementations may accept
     *              {@code null} and typically either return {@code null} or the
     *              unmodified input in that case
     * @return the transformed string, ready to be passed to the IRI parser
     */
    public String transform(String input);

}
