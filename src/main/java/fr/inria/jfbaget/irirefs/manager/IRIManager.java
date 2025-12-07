package fr.inria.jfbaget.irirefs.manager;

import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.normalizer.ExtendedComposableNormalizer;
import fr.inria.jfbaget.irirefs.normalizer.IRINormalizer;
import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;
import fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer;
import fr.inria.jfbaget.irirefs.preparator.StringPreparator;

import java.util.HashMap;
import java.util.Map;

/**
 * High-level helper for creating, normalizing and relativizing {@link IRIRef} instances.
 * <p>
 * {@code IRIManager} centralizes three concerns:
 * </p>
 * <ul>
 *   <li>management of a <em>current base IRI</em>, used to resolve relative
 *       references as in RFC&nbsp;3986&nbsp;§5;</li>
 *   <li>management of a set of <em>named prefixes</em> (symbolic base IRIs) that
 *       can be used to resolve compact forms such as {@code prefix:localName};</li>
 *   <li>optional integration of a {@link StringPreparator} and an {@link IRINormalizer}
 *       so that all IRIs created through this manager are consistently prepared,
 *       resolved and normalized.</li>
 * </ul>
 *
 * <p>
 * Typical usage is:
 * </p>
 * <ol>
 *   <li>construct an {@code IRIManager} with a base IRI and, optionally, a
 *       preparator and a normalizer;</li>
 *   <li>declare a few prefixes via {@link #setPrefix(String, String)} or
 *       {@link #setPrefix(String, String, String)};</li>
 *   <li>create IRIs relative to the base or to a prefix using
 *       {@link #createIRI(String)} or {@link #createIRI(String, String)};</li>
 *   <li>optionally, compute compact relative forms using
 *       {@link #relativize(IRIRef)} or {@link #relativizeBest(IRIRef)}.</li>
 * </ol>
 *
 * <p>
 * The manager itself is lightweight and stateful (it tracks the current base and
 * prefix map). It does not perform any caching: every call to {@code createIRI}
 * parses, resolves and normalizes afresh.
 * </p>
 */
public class IRIManager {

    /**
     * Immutable pair consisting of an optional prefix key and a (possibly
     * relative) {@link IRIRef}.
     * <p>
     * Instances of this record are returned by {@link #relativizeBest(IRIRef)}:
     * the {@code prefix} component is either {@code null} (when using the
     * manager's current base) or the key of a declared prefix, and the
     * {@code iri} component is the corresponding (absolute or relative) IRI
     * obtained by relativization.
     * </p>
     *
     * <p>
     * Callers can use this type to reconstruct compact forms such as
     * {@code "prefix:rel/path"} when {@code prefix} is non-{@code null}, or
     * simply use {@code iri.recompose()} when no prefix is involved.
     * </p>
     *
     * @param prefix the chosen prefix key, or {@code null} if the manager's base
     *               IRI was used instead of a named prefix
     * @param iri    the (possibly relativized) IRI reference
     */
    public record PrefixedIRIRef(String prefix, IRIRef iri) {}

    /**
     * The default base IRI used when no explicit base is provided.
     * <p>
     * This must always be a valid absolute IRI, as per
     * <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-5.1">RFC 3986, Section 5.1</a>,
     * and MUST be already normalized.
     */
    private static final String DEFAULT_BASE = "http://www.boreal.inria.fr/";

    private final StringPreparator preparator;
    private final IRINormalizer normalizer;

    private IRIRef base = new IRIRef(DEFAULT_BASE);

    /**
     * A map of declared prefixes to their associated absolute IRIs.
     * <p>
     * Used for resolving prefixed IRI references of the form {@code prefix:localName}.
     * All stored {@link IRIRef} values are absolute and normalized.
     * </p>
     */
    private final Map<String, IRIRef> prefixes = new HashMap<>();

    /**
     * Creates a new {@code IRIManager} with explicit preparation and
     * normalization strategies and an initial base IRI.
     * <p>
     * The {@code iriBase} string is:
     * </p>
     * <ol>
     *   <li>optionally transformed by the supplied {@link StringPreparator}
     *       (if non-{@code null});</li>
     *   <li>parsed into an {@link IRIRef};</li>
     *   <li>resolved against the manager's current base
     *       (initially {@link #DEFAULT_BASE});</li>
     *   <li>normalized in place using the given {@link IRINormalizer};</li>
     *   <li>finally checked to be {@linkplain IRIRef#isAbsolute() absolute}.</li>
     * </ol>
     * <p>
     * The resulting absolute, normalized {@link IRIRef} becomes the manager's
     * base IRI. If, after resolution and normalization, the base is still not
     * absolute, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param preparator string preparator applied before parsing, or {@code null}
     *                   to parse the raw input as-is
     * @param normalizer normalizer applied to all IRIs created by this manager;
     *                   must not be {@code null}
     * @param iriBase    textual base IRI used to initialise this manager; may be
     *                   absolute or relative, but must yield an absolute IRI
     *                   after resolution and normalization
     * @throws IllegalArgumentException if the computed base IRI is not absolute
     */
    public IRIManager(StringPreparator preparator, IRINormalizer normalizer, String iriBase) {
        this.preparator = preparator;
        this.normalizer = normalizer;
        this.base = requireAbsolute(createIRI(iriBase));
    }

    /**
     * Convenience constructor using no {@link StringPreparator} and a
     * {@link StandardComposableNormalizer} configured with
     * {@link RFCNormalizationScheme#STRING} only.
     * <p>
     * This is equivalent to:
     * </p>
     * <pre>{@code
     * new IRIManager(null,
     *                new StandardComposableNormalizer(RFCNormalizationScheme.STRING),
     *                iribase);
     * }</pre>
     * <p>
     * With {@link RFCNormalizationScheme#STRING}, the normalizer performs no
     * transformation at all: the parsed components are preserved as-is
     * (no case folding, no percent-decoding, no path or scheme-based
     * normalization). The supplied {@code iribase} is still:
     * </p>
     * <ol>
     *   <li>parsed into an {@link IRIRef},</li>
     *   <li>resolved against the current base (initially
     *       {@link #DEFAULT_BASE}), and</li>
     *   <li>required to be {@linkplain IRIRef#isAbsolute() absolute},</li>
     * </ol>
     * <p>
     * otherwise an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param iribase textual base IRI used to initialise this manager
     * @throws IllegalArgumentException if the computed base IRI is not absolute
     */
    public IRIManager(String iribase) {
        this(null, new StandardComposableNormalizer(RFCNormalizationScheme.STRING), iribase);
    }

    /**
     * Returns the current base IRI of this manager, as a string.
     * <p>
     * The returned value is the recomposed form of the internally stored
     * {@link IRIRef} base, which is always an absolute, normalized IRI
     * according to the {@link IRINormalizer} used at construction time.
     * </p>
     *
     * @return the current base IRI in textual form
     */
    public String getBase() {
        return this.base.recompose();
    }

    /**
     * Returns the absolute IRI string associated with a declared prefix.
     * <p>
     * This is the recomposed form of the {@link IRIRef} stored for the
     * given {@code prefixKey}, and is always absolute and normalized
     * according to this manager’s {@link IRINormalizer}.
     * </p>
     *
     * @param prefixKey the logical prefix name (for example {@code "ex"})
     * @return the corresponding absolute IRI in textual form
     * @throws IllegalArgumentException if {@code prefixKey} has not been
     *                                  registered via {@link #setPrefix(String, String)}
     *                                  or {@link #setPrefix(String, String, String)}
     */
    public String getPrefix(String prefixKey) {
        return this.get(prefixKey).recompose();
    }

    /**
     * Creates a normalized {@link IRIRef} from a textual IRI or IRI reference,
     * resolved against this manager’s current base.
     * <p>
     * The creation pipeline is:
     * </p>
     * <ol>
     *   <li>optionally prepare {@code iriString} using the configured
     *       {@link StringPreparator} (if non-{@code null});</li>
     *   <li>parse the prepared string into an {@link IRIRef};</li>
     *   <li>resolve it (strictly) against the current {@linkplain #getBase() base}
     *       via {@link IRIRef#resolveInPlace(IRIRef)}; the base is guaranteed
     *       to be absolute by {@link #requireAbsolute(IRIRef)};</li>
     *   <li>normalize the resolved IRI in place using this manager’s
     *       {@link IRINormalizer}.</li>
     * </ol>
     *
     * @param iriString a textual IRI or IRI reference, possibly relative
     * @return an absolute, normalized {@link IRIRef} obtained by resolving
     *         {@code iriString} against the current base
     * @throws fr.inria.jfbaget.irirefs.exceptions.IRIParseException
     *         if parsing fails
     */
    public IRIRef createIRI(String iriString) {
        return createIRI(iriString, this.base);
    }

    /**
     * Creates a normalized {@link IRIRef} from a textual IRI or IRI reference,
     * resolved against the IRI associated with a given prefix.
     * <p>
     * This behaves like {@link #createIRI(String)}, except that the base IRI
     * is taken from the prefix mapping:
     * </p>
     * <pre>{@code
     * IRIRef base = get(prefix);
     * IRIRef result = new IRIRef(iriString, preparator)
     *                     .resolveInPlace(base)
     *                     .normalizeInPlace(normalizer);
     * }</pre>
     * <p>
     * All stored prefix IRIs are guaranteed to be absolute, as they are passed
     * through {@link #requireAbsolute(IRIRef)} when registered.
     * </p>
     *
     * @param prefix    the logical prefix whose absolute IRI will be used as base
     * @param iriString a textual IRI or IRI reference, possibly relative
     * @return an absolute, normalized {@link IRIRef} obtained by resolving
     *         {@code iriString} against the IRI bound to {@code prefix}
     * @throws IllegalArgumentException if {@code prefix} is unknown
     * @throws fr.inria.jfbaget.irirefs.exceptions.IRIParseException
     *         if parsing fails
     */
    public IRIRef createIRI(String prefix, String iriString) {
        return createIRI(iriString, this.get(prefix));

    }

    /**
     * Internal factory method that prepares, parses, resolves and normalizes
     * an {@link IRIRef} against the supplied base.
     * <p>
     * This method is the common implementation behind the public
     * {@code createIRI(...)} overloads.
     * The {@code base} argument is assumed to be an absolute, normalized IRI;
     * {@link IRIManager} maintains this invariant by calling
     * {@link #requireAbsolute(IRIRef)} whenever bases or prefixes are set.
     * </p>
     *
     * @param iriString the raw IRI or IRI reference to parse (possibly relative)
     * @param base      the absolute base IRI to resolve against
     * @return the resolved and normalized {@link IRIRef}
     */
    private IRIRef createIRI(String iriString, IRIRef base) {
        return new IRIRef(iriString, this.preparator)
                .resolveInPlace(base)
                .normalizeInPlace(this.normalizer);
    }

    /**
     * Updates this manager’s current base IRI from a textual IRI or IRI reference.
     * <p>
     * The supplied {@code iriString} is prepared, parsed, resolved against the
     * current base (initially {@link #DEFAULT_BASE}), then normalized using this
     * manager’s {@link IRINormalizer}. The resulting {@link IRIRef} must be
     * {@linkplain IRIRef#isAbsolute() absolute}, otherwise an
     * {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param iriString textual IRI or IRI reference used to compute the new base
     * @throws IllegalArgumentException if the computed IRI is not absolute
     */
    public void setBase(String iriString) {
        this.base = requireAbsolute(createIRI(iriString));
    }

    /**
     * Updates this manager’s current base IRI using a prefix-based reference.
     * <p>
     * The supplied {@code iriString} is prepared, parsed, resolved against the
     * IRI currently bound to {@code prefix}, then normalized. The result must be
     * {@linkplain IRIRef#isAbsolute() absolute}, otherwise an
     * {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param prefix    logical prefix whose IRI will be used as base for resolution
     * @param iriString textual IRI or IRI reference to resolve against that base
     * @throws IllegalArgumentException if {@code prefix} is unknown or if the
     *                                  computed base IRI is not absolute
     */
    public void setBase(String prefix, String iriString) {
        this.base = requireAbsolute(createIRI(prefix, iriString));
    }

    /**
     * Declares or updates a prefix mapping to an absolute, normalized IRI.
     * <p>
     * The supplied {@code iriString} is prepared, parsed, resolved against the
     * current base, then normalized. The resulting IRI must be
     * {@linkplain IRIRef#isAbsolute() absolute}; otherwise an
     * {@link IllegalArgumentException} is thrown. The mapping
     * {@code prefixKey -> IRI} is then stored (replacing any previous value).
     * </p>
     *
     * @param prefixKey logical prefix to register (for example {@code "ex"})
     * @param iriString textual IRI or IRI reference whose normalized absolute
     *                  value will be bound to {@code prefixKey}
     * @throws IllegalArgumentException if the computed IRI is not absolute
     */
    public void setPrefix(String prefixKey, String iriString) {
        this.prefixes.put(prefixKey, requireAbsolute(createIRI(iriString)));
    }

    /**
     * Declares or updates a prefix mapping using a prefix-based base IRI.
     * <p>
     * The supplied {@code iriString} is prepared, parsed, resolved against the
     * IRI bound to {@code prefix}, then normalized. The result must be
     * {@linkplain IRIRef#isAbsolute() absolute}; otherwise an
     * {@link IllegalArgumentException} is thrown. The mapping
     * {@code prefixKey -> IRI} is then stored (replacing any previous value).
     * </p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * manager.setPrefix("exBase", "http://example.org/");
     * manager.setPrefix("foo", "exBase", "v1/");
     * // "foo" now maps to "http://example.org/v1/"
     * }</pre>
     *
     * @param prefixKey logical prefix to register (the new mapping’s key)
     * @param prefix    existing prefix whose IRI will serve as base
     * @param iriString textual IRI or IRI reference to resolve against that base
     * @throws IllegalArgumentException if {@code prefix} is unknown or if the
     *                                  computed IRI is not absolute
     */
    public void setPrefix(String prefixKey, String prefix, String iriString) {
        this.prefixes.put(prefixKey, requireAbsolute(createIRI(prefix, iriString)));
    }

    /**
     * Relativizes the given IRI against this manager’s current base.
     * <p>
     * This is a thin wrapper around {@link IRIRef#relativize(IRIRef)}, using
     * {@link #base} as the base IRI. The current base is guaranteed by
     * {@link #requireAbsolute(IRIRef)} to be an <em>absolute</em> IRI in the
     * RFC&nbsp;3986 sense (has a scheme and no fragment).
     * </p>
     *
     * <p>
     * The {@code iri} argument must be a <em>full IRI</em>, i.e. it must have
     * a scheme. If it is only a relative reference (no scheme),
     * {@link IRIRef#relativize(IRIRef)} will throw an
     * {@link IllegalArgumentException}, as required by its contract.
     * </p>
     *
     * <p>
     * The returned {@link IRIRef} is either:
     * </p>
     * <ul>
     *   <li>a shorter relative reference that still resolves back to
     *       {@code iri} when resolved against the current base, or</li>
     *   <li>an IRI that is textually identical to {@code iri} when keeping
     *       the original form is judged preferable by the relativization
     *       algorithm.</li>
     * </ul>
     *
     * @param iri the (full) IRI to relativize against the current base
     * @return a relative or unchanged {@link IRIRef} that still denotes
     *         the same absolute IRI as {@code iri}
     * @throws IllegalArgumentException if {@code iri} is not a full IRI
     *                                  (i.e. has no scheme)
     */
    public IRIRef relativize(IRIRef iri) {
        return this.relativize(iri, this.base);
    }

    /**
     * Relativizes the given IRI against the IRI bound to a given prefix.
     * <p>
     * This behaves like {@link #relativize(IRIRef)}, but uses the absolute
     * IRI mapped to {@code prefixKey} (via {@link #setPrefix(String, String)}
     * or {@link #setPrefix(String, String, String)}) as the base.
     * </p>
     *
     * <p>
     * The {@code iri} argument must be a full IRI (has a scheme). If it is
     * only a relative reference, the underlying {@link IRIRef#relativize(IRIRef)}
     * will throw an {@link IllegalArgumentException}.
     * </p>
     *
     * @param prefixKey the logical prefix whose bound IRI is used as base
     * @param iri       the (full) IRI to relativize against that base
     * @return a relative or unchanged {@link IRIRef} that still denotes
     *         the same absolute IRI as {@code iri}
     * @throws IllegalArgumentException if {@code prefixKey} is unknown or
     *                                  if {@code iri} is not a full IRI
     */
    public IRIRef relativize(String prefixKey, IRIRef iri) {
        return this.relativize(iri, this.get(prefixKey));
    }

    /**
     * Tries to find the “best” relativized form of the given IRI with respect
     * to the manager's base and all declared prefixes.
     * <p>
     * The method proceeds as follows:
     * </p>
     * <ol>
     *   <li>starts from the original {@code iri} as the current best candidate;</li>
     *   <li>computes {@link #relativize(IRIRef)} against the current base and
     *       keeps it if its recomposed length is shorter;</li>
     *   <li>for each declared prefix key, computes
     *       {@link #relativize(String, IRIRef)} against the corresponding prefix
     *       IRI and compares the total length {@code prefixKey.length() + relativizedLength}
     *       to the current best pair;</li>
     *   <li>returns a {@link PrefixedIRIRef} holding the prefix (possibly
     *       {@code null} when the base is used) and the chosen {@link IRIRef}.</li>
     * </ol>
     *
     * <h3>Performance considerations</h3>
     * <p>
     * This method may be relatively expensive:
     * for each call, it:
     * </p>
     * <ul>
     *   <li>relativizes against the base IRI,</li>
     *   <li>relativizes once per declared prefix,</li>
     *   <li>and recomputes {@link IRIRef#recompositionLength()} several times.</li>
     * </ul>
     * <p>
     * When the same {@code IRIRef} needs to be compacted repeatedly (for example
     * in a loop or for repeated display), callers are encouraged to compute
     * {@code relativizeBest} once and cache the resulting {@link PrefixedIRIRef}
     * instead of invoking this method every time.
     * </p>
     *
     * @param iri the IRI to relativize against the base and all prefixes
     * @return a {@link PrefixedIRIRef} containing the chosen prefix (possibly
     *         {@code null}) and relativized IRI
     */
    public PrefixedIRIRef relativizeBest(IRIRef iri) {
        String currentPrefix = null;
        int currentPrefixLen = 0;
        IRIRef currentIRIRef = iri;
        int currentIRIRefLen = currentIRIRef.recompositionLength();

        IRIRef testedIRIRef = this.relativize(iri);
        int testedIRIRefLen = testedIRIRef.recompositionLength();
        if (testedIRIRefLen < currentIRIRefLen) {
            currentIRIRef = testedIRIRef;
            currentIRIRefLen = testedIRIRefLen;
        }

        for (String prefixKey : this.prefixes.keySet()) {
            testedIRIRef = this.relativize(prefixKey, iri);
            testedIRIRefLen = testedIRIRef.recompositionLength();
            if (prefixKey.length() + testedIRIRefLen < currentPrefixLen + currentIRIRefLen) {
                currentPrefix = prefixKey;
                currentPrefixLen = prefixKey.length();
                currentIRIRef = testedIRIRef;
                currentIRIRefLen = testedIRIRefLen;
            }
        }
        return new PrefixedIRIRef(currentPrefix, currentIRIRef);
    }

    /**
     * Ensures that the given {@link IRIRef} is absolute.
     * <p>
     * This helper is used when setting the manager's base IRI and prefix IRIs:
     * only absolute IRIs (with a scheme and no fragment) are accepted.
     * If {@code iri} is not absolute, an {@link IllegalArgumentException}
     * is thrown.
     * </p>
     *
     * @param iri the IRI to check
     * @return the same {@code iri}, if it is absolute
     * @throws IllegalArgumentException if {@code iri} is not absolute
     */
    private IRIRef requireAbsolute(IRIRef iri) {
        if (iri.isAbsolute()) {
            return iri;
        } else {
            throw new IllegalArgumentException("Cannot create a base from a non absolute IRI, given: " +
                    iri.recompose());
        }
    }

    /**
     * Looks up the {@link IRIRef} associated with the given prefix key.
     * <p>
     * This helper reads from the internal {@link #prefixes} map and is used
     * whenever the manager needs the absolute IRI corresponding to a declared
     * prefix. If the prefix is unknown, an {@link IllegalArgumentException}
     * is thrown.
     * </p>
     *
     * @param prefixKey the declared prefix name to resolve
     * @return the absolute {@link IRIRef} bound to {@code prefixKey}
     * @throws IllegalArgumentException if no IRI has been declared for {@code prefixKey}
     */
    private IRIRef get(String prefixKey) {
        IRIRef iri = prefixes.get(prefixKey);
        if (iri == null) {
            throw new IllegalArgumentException("Unknown prefix: " + prefixKey);
        } else {
            return iri;
        }
    }

    /**
     * Internal helper performing relativization against a given base IRI.
     * <p>
     * This is a thin wrapper around {@link IRIRef#relativize(IRIRef)}:
     * it delegates directly to {@code iri.relativize(base)}.
     * </p>
     *
     * <p>
     * The {@code base} argument is always an absolute IRI (this is enforced
     * whenever bases and prefixes are set via {@link #requireAbsolute(IRIRef)}).
     * The {@code iri} argument must be a <em>full IRI</em>, i.e. it must have
     * a scheme; if it is only a relative reference, the underlying
     * {@link IRIRef#relativize(IRIRef)} call will throw an
     * {@link IllegalArgumentException}.
     * </p>
     *
     * @param iri  the (full) IRI to relativize
     * @param base the absolute base IRI to relativize against
     * @return the relativized or unchanged {@link IRIRef}, as returned by
     *         {@link IRIRef#relativize(IRIRef)}
     * @throws IllegalArgumentException if {@code iri} is not a full IRI
     *                                  (has no scheme)
     */
    private IRIRef relativize(IRIRef iri, IRIRef base) {
        return iri.relativize(base);
    }


    public static void main(String[] args) {
        IRINormalizer normalizer = new ExtendedComposableNormalizer(RFCNormalizationScheme.STRING);
        IRIManager manager = new IRIManager(null, normalizer, "HTTP://www.BOREAL.inria.fr:80/");
        IRIRef test = manager.createIRI("HTTP://www.BOREAL.inria.fr:80");
        IRIRef rel = manager.relativize(test);
        if (rel == null) {System.out.println("NULL");}
        System.out.println(manager.relativize(test));
    }

}
