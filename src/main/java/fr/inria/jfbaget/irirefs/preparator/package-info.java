/**
 * String preparation layer applied before IRI parsing.
 * <p>
 * This package defines a small, extensible API for preprocessing raw
 * {@link java.lang.String} values before they are parsed as IRIs or IRI references.
 * Typical use cases include:
 * </p>
 * <ul>
 *   <li>decoding HTML or XML character entities,</li>
 *   <li>applying Unicode normalisation,</li>
 *   <li>performing application-specific cleanup or rewriting of input.</li>
 * </ul>
 *
 * <h2>Main types</h2>
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.preparator.StringPreparator} –
 *       strategy interface for transforming a raw string into a form ready
 *       to be parsed as an IRI. Implementations are expected to be pure,
 *       deterministic and thread-safe.</li>
 *
 *   <li>{@link fr.inria.jfbaget.irirefs.preparator.BasicStringPreparator} –
 *       configurable implementation backed by a global registry of named
 *       {@code String -> String} transformers. It provides a few common
 *       transformers out of the box (e.g. {@code "html4"}, {@code "xml"}
 *       based on {@code StringEscapeUtils}) and allows applications to
 *       register additional ones via
 *       {@link fr.inria.jfbaget.irirefs.preparator.BasicStringPreparator#addTransformer(String, java.util.function.Function)}.</li>
 *
 *   <li>{@link fr.inria.jfbaget.irirefs.preparator.MyStringPreparatorExample} –
 *       demonstration subclass showing how to plug a custom transformer into
 *       the global registry (the {@code "killroy"} example) and how to build
 *       a {@code BasicStringPreparator} pipeline that mixes built-in and
 *       user-defined transformers.</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <p>
 * In most cases, callers will:
 * </p>
 * <ol>
 *   <li>configure a {@link fr.inria.jfbaget.irirefs.preparator.StringPreparator} once (for example a
 *       {@link fr.inria.jfbaget.irirefs.preparator.BasicStringPreparator} with a chosen list of transformer names);</li>
 *   <li>pass that preparator to the
 *       {@link fr.inria.jfbaget.irirefs.IRIRef#IRIRef(String, fr.inria.jfbaget.irirefs.IRIRef.IRITYPE, StringPreparator)
 *       IRIRef constructor}, so that each raw input string is prepared exactly
 *       once before parsing;</li>
 *   <li>optionally extend the global registry with additional transformers
 *       at application startup, if the built-in ones are not sufficient.</li>
 * </ol>
 *
 * <p>
 * The design is intentionally open-ended: the library does not attempt to
 * prescribe a single “correct” preparation policy. Instead, it provides a
 * simple mechanism that applications can adapt to their own requirements
 * while keeping parsing and normalisation logic cleanly separated.
 * </p>
 */
package fr.inria.jfbaget.irirefs.preparator;