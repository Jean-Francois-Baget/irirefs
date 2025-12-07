/**
 * Internal utility classes used by the IRIRefs library.
 * <p>
 * This package currently contains:
 * </p>
 * <ul>
 *   <li>{@link fr.inria.jfbaget.irirefs.utils.CharSlice} –
 *       a lightweight, immutable {@link java.lang.CharSequence} view over a substring
 *       of a {@link java.lang.String}, used primarily by normalizers and parsing-related
 *       code to avoid allocating intermediate strings when scanning percent-
 *       encoded runs or other character ranges.</li>
 * </ul>
 *
 * <h2>Intended usage</h2>
 * <p>
 * The types in this package are considered internal helpers for the
 * {@code fr.inria.jfbaget.irirefs} and {@code fr.inria.jfbaget.irirefs.normalizer}
 * packages. They are small, self-contained utilities that may be useful in other
 * contexts, but they are not the main public surface of the library.
 * </p>
 */
package fr.inria.jfbaget.irirefs.utils;
