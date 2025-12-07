/**
 * High-level management utilities for creating, normalizing and relativizing IRIs.
 *
 * <p>
 * The central class in this package is {@link fr.inria.jfbaget.irirefs.manager.IRIManager},
 * which provides a convenient façade around {@link fr.inria.jfbaget.irirefs.IRIRef}:
 * </p>
 *
 * <ul>
 *   <li>It maintains a current <em>base IRI</em>, always absolute and normalized,
 *       used to resolve relative references.</li>
 *   <li>It can apply an optional {@link fr.inria.jfbaget.irirefs.preparator.StringPreparator}
 *       before parsing, and an {@link fr.inria.jfbaget.irirefs.normalizer.IRINormalizer}
 *       (typically a {@link fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer}
 *       or {@link fr.inria.jfbaget.irirefs.normalizer.ExtendedComposableNormalizer})
 *       after parsing, so that all IRIs managed by this class follow a consistent
 *       preparation/normalization policy.</li>
 *   <li>It manages a set of named <em>prefix IRIs</em>, allowing callers to
 *       resolve and relativize IRIs with respect to different declared bases.</li>
 *   <li>It can relativize a given {@link fr.inria.jfbaget.irirefs.IRIRef} with
 *       respect to the base IRI or to a declared prefix, and can compute a
 *       “best” relativization (shortest textual representation) together with
 *       the chosen prefix via {@code relativizeBest}.</li>
 * </ul>
 *
 * <p>
 * Note that {@code relativizeBest} may be relatively costly if many prefixes
 * are declared, since it iterates over all of them and compares string lengths
 * to pick the shortest representation. Callers that repeatedly apply the same
 * relativization to the same {@code IRIRef} may wish to cache or reuse the
 * resulting {@code PrefixedIRIRef} instead of recomputing it each time.
 * </p>
 */
package fr.inria.jfbaget.irirefs.manager;