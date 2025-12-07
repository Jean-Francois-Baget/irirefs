package fr.inria.jfbaget.irirefs.normalizer;

import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.parser.IRIRefParser;
import fr.inria.jfbaget.irirefs.utils.CharSlice;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class StandardComposableNormalizer implements IRINormalizer{

    /**
     * Mapping of well-known schemes to their default TCP ports, used by
     * {@link #normalizePort(Integer, String)} when {@link RFCNormalizationScheme#SCHEME} is enabled.
     * <p>
     * This table is not intended to be exhaustive; it only covers a set of
     * common schemes (HTTP(S), WS(S), FTP/SSH, mail protocols, LDAP, etc.).
     * Ports are expressed in their canonical form as defined by the relevant
     * RFCs. If the explicit port of an {@link IRIRef} matches the default
     * for its scheme, it may be removed during normalization.
     * </p>
     */
    private static final Map<String, Integer> DEFAULT_PORTS = Map.ofEntries(
                Map.entry("http", 80),
                Map.entry("https", 443),
                Map.entry("ws", 80),
                Map.entry("wss", 443),
                // File transfer / remote
                Map.entry("ftp", 21),
                Map.entry("sftp", 22),
                // SSH File Transfer Protocol
                Map.entry("ssh", 22),
                Map.entry("telnet", 23),
                // Mail
                Map.entry("smtp", 25),
                Map.entry("submission", 587), // RFC 6409
                Map.entry("imap", 143),
                Map.entry("imaps", 993),
                Map.entry("pop3", 110),
                Map.entry("pop3s", 995),
                // Directory / news / legacy
                Map.entry("ldap", 389),
                Map.entry("ldaps", 636),
                Map.entry("nntp", 119),
                Map.entry("news", 119),
                Map.entry("gopher", 70)
    );


    /**
     * Optional, scheme-specific case-insensitivity rules.
     * <p>
     * Maps a scheme (in lowercase) to the set of {@link IRIRef.PART}s that should be
     * treated as case-insensitive for that scheme when {@link RFCNormalizationScheme#CASE} is
     * enabled. By default, this map is empty: generic RFC rules are applied
     * (e.g. {@link IRIRef.PART#SCHEME} and ASCII-only {@link IRIRef.PART#HOST} are handled
     * globally in {@link #normalizeScheme(String)} and {@link #normalizeHost(String, String)}).
     * This map can be populated in the future if particular schemes require
     * additional case-insensitive components.
     * </p>
     */
    private static final Map<String, EnumSet<IRIRef.PART>> CASE_INSENSITIVE_PARTS;
    static {
        Map<String, EnumSet<IRIRef.PART>> m = new HashMap<>();
        // Example of a future, scheme-specific rule:
        // m.put("example-scheme", EnumSet.of(IriPart.PATH));
        CASE_INSENSITIVE_PARTS = Collections.unmodifiableMap(m);
    }

    /**
     * Pattern matching a single RFC&nbsp;3986 {@code unreserved} character,
     * as defined by {@link IRIRefParser#UNRESERVED}. Used by
     * {@link #normalizePCTHandler(CharSlice, StringBuilder)} to decide
     * whether a percent-encoded octet can be safely decoded.
     */
    private static final Pattern UNRESERVED_PATTERN = Pattern.compile("[" + IRIRefParser.UNRESERVED + "]");


    /**
     * Simple character-level transformation used for non-percent-encoded
     * characters during normalization.
     * <p>
     * Implementations receive a single {@code char} and return the transformed
     * {@code char}. Two common transformers are provided:
     * </p>
     * <ul>
     *   <li>{@link #IDENTITY}: returns the character unchanged;</li>
     *   <li>{@link #LOWERCASE}: applies {@link Character#toLowerCase(char)}.</li>
     * </ul>
     */
    @FunctionalInterface
    protected interface CharTransformer {
        char apply(char c);

        CharTransformer IDENTITY = c -> c;
        CharTransformer LOWERCASE = Character::toLowerCase;
    }

    /**
     * Active normalization modes for this {@code StandardComposableNormalizer} instance.
     * <p>
     * Populated at construction time from the requested {@link RFCNormalizationScheme}s and
     * then treated as immutable for the lifetime of the instance.
     * </p>
     */
    protected final EnumSet<RFCNormalizationScheme> normalizations =
            EnumSet.noneOf(RFCNormalizationScheme.class);

    /**
     * Constructs a {@code StandardComposableNormalizer} with the given set of normalization modes.
     * <p>
     * Each requested {@link RFCNormalizationScheme} is added to the internal {@code EnumSet}. The
     * composite mode {@link RFCNormalizationScheme#SYNTAX} is expanded into its components
     * {@link RFCNormalizationScheme#CASE}, {@link RFCNormalizationScheme#CHARACTER} and
     * {@link RFCNormalizationScheme#PCT}.
     * </p>
     * @param requested the normalization modes to enable (must not be {@code null})
     */
    public StandardComposableNormalizer(List<RFCNormalizationScheme> requested) {
        for (RFCNormalizationScheme n : requested) {
            if (n.equals(RFCNormalizationScheme.SYNTAX)) {
                normalizations.add(RFCNormalizationScheme.CASE);
                normalizations.add(RFCNormalizationScheme.CHARACTER);
                normalizations.add(RFCNormalizationScheme.PCT);
            } else {
                normalizations.add(n);
            }
        }
    }


    /**
     * Convenience constructor using varargs.
     *
     * <pre>
     * new StandardComposableNormalizer(RFCNormalizationScheme.CASE, RFCNormalizationScheme.PATH);
     * </pre>
     *
     * @param requested the normalization flags to enable
     */
    public StandardComposableNormalizer(RFCNormalizationScheme... requested) {
        this(List.of(requested));
    }

    /**
     * Returns {@code true} if the given normalization flag is enabled.
     *
     * @param mode the normalization to test
     * @return {@code true} if {@code mode} is enabled, {@code false} otherwise
     */
    public boolean has(RFCNormalizationScheme mode) {
        return normalizations.contains(mode);
    }

    /**
     * Normalizes the IRI scheme component.
     * <p>
     * When {@link RFCNormalizationScheme#CASE} is enabled and {@code scheme} is non-{@code null},
     * the scheme is converted to lowercase using {@link Locale#ROOT}, as
     * required by RFC&nbsp;3986/3987 (schemes are case-insensitive). Otherwise,
     * the input is returned unchanged.
     * </p>
     *
     * @param scheme the parsed scheme, or {@code null} if none
     * @return the normalized scheme, or {@code null} if {@code scheme} was {@code null}
     */
    public String normalizeScheme(String scheme) {
        if (scheme != null && has(RFCNormalizationScheme.CASE)) {
            return scheme.toLowerCase(Locale.ROOT);
        } else {
            return scheme;
        }
    }

    private String normalizeComponent(String component, IRIRef.PART part, String scheme) {
        if (component == null || component.isEmpty()) { return component; }
        String result = component;
        boolean safetransformation = ! part.equals(IRIRef.PART.QUERY) && ! part.equals(IRIRef.PART.FRAGMENT);
        if (has(RFCNormalizationScheme.PCT) && safetransformation) {
            result = applyPctTransformation(result);
        }
        if (has(RFCNormalizationScheme.CHARACTER)) {
            result = normalizeNFC(result);
        }
        if (has(RFCNormalizationScheme.CASE)) {
            if (safetransformation &&
                    (shouldLowercase(part, scheme) || (part.equals(IRIRef.PART.HOST) && isAsciiOnly(result)))) {
                result = transformPctAndOtherChars(result, StandardComposableNormalizer::uppercasePCTHandler,
                        CharTransformer.LOWERCASE );
            } else {
                result = transformPctAndOtherChars(result, StandardComposableNormalizer::uppercasePCTHandler,
                        CharTransformer.IDENTITY);
            }
        }
        return result;
    }

    /**
     * Normalizes the {@code userinfo} component of an IRI.
     * <p>
     * Depending on the active {@link RFCNormalizationScheme}s, this method can:
     * </p>
     * <ul>
     *   <li>apply percent-encoding normalization ({@link RFCNormalizationScheme#PCT}), decoding
     *       RFC&nbsp;3986 {@code unreserved} characters from {@code %HH} sequences;</li>
     *   <li>normalize Unicode characters to NFC when {@link RFCNormalizationScheme#CHARACTER} is enabled;</li>
     *   <li>apply case normalization when {@link RFCNormalizationScheme#CASE} is enabled:
     *     <ul>
     *       <li>if {@link #shouldLowercase(IRIRef.PART, String)} returns {@code true}
     *           for {@link IRIRef.PART#USERINFO} and the given scheme, all non
     *           percent-encoded characters are lowercased and percent-hex
     *           digits are uppercased;</li>
     *       <li>otherwise, only the hexadecimal digits of {@code %HH}
     *           sequences are uppercased.</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * If {@code user} is {@code null} or empty, it is returned unchanged.
     * The {@code scheme} parameter is used to determine scheme-specific
     * case rules via {@link #shouldLowercase(IRIRef.PART, String)}.
     * </p>
     *
     * @param user   the parsed {@code userinfo} component, or {@code null}
     * @param scheme the IRI scheme (lowercased or not), may be {@code null}
     * @return the normalized {@code userinfo}, or the original value if no
     *         relevant modes are enabled
     */
    public String normalizeUserInfo(String user, String scheme) {
        return normalizeComponent(user, IRIRef.PART.USERINFO, scheme);
    }

    /**
     * Normalizes the {@code host} component of an IRI.
     * <p>
     * Depending on the active {@link RFCNormalizationScheme}s, this method can:
     * </p>
     * <ul>
     *   <li>apply percent-encoding normalization ({@link RFCNormalizationScheme#PCT}) to decode
     *       RFC&nbsp;3986 {@code unreserved} characters from {@code %HH}
     *       sequences;</li>
     *   <li>normalize Unicode characters to NFC when {@link RFCNormalizationScheme#CHARACTER} is enabled;</li>
     *   <li>apply case normalization when {@link RFCNormalizationScheme#CASE} is enabled:
     *     <ul>
     *       <li>if {@link #shouldLowercase(IRIRef.PART, String)} returns {@code true}
     *           for {@link IRIRef.PART#HOST} and the given scheme, or if the host is
     *           ASCII-only (as per RFC&nbsp;3986/3987, ASCII hosts are
     *           case-insensitive), all non percent-encoded characters are
     *           lowercased and percent-hex digits are uppercased;</li>
     *       <li>otherwise, only the hexadecimal digits of {@code %HH}
     *           sequences are uppercased and the rest of the host is left
     *           unchanged.</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * If {@code host} is empty, it is returned unchanged. This method assumes
     * {@code host} is non-{@code null}; {@link IRIRef} is responsible for
     * enforcing this invariant.
     * </p>
     *
     * @param host   the parsed {@code host} component (never {@code null} here)
     * @param scheme the IRI scheme, used for scheme-specific case rules; may be {@code null}
     * @return the normalized {@code host}
     */
    public String normalizeHost(String host, String scheme) {
        return normalizeComponent(host, IRIRef.PART.HOST, scheme);
    }

    /**
     * Normalizes the {@code query} component of an IRI.
     * <p>
     * In this implementation, percent-encoding normalization
     * ({@link RFCNormalizationScheme#PCT}) is <em>not</em> applied to the query.
     * When {@link RFCNormalizationScheme#CHARACTER} is enabled, the query is put
     * into NFC, and when {@link RFCNormalizationScheme#CASE} is enabled, only
     * the hexadecimal digits of {@code %HH} sequences are uppercased; all other
     * characters are left unchanged.
     * </p>
     */
    public String normalizeQuery(String query, String scheme) {
        return normalizeComponent(query, IRIRef.PART.QUERY, scheme);
    }

    /**
     * Normalizes the {@code fragment} component of an IRI.
     * <p>
     * In this implementation, percent-encoding normalization
     * ({@link RFCNormalizationScheme#PCT}) is <em>not</em> applied to the
     * fragment: existing {@code %HH} triplets are preserved, except that
     * their hexadecimal digits may be uppercased when
     * {@link RFCNormalizationScheme#CASE} is enabled.
     * When {@link RFCNormalizationScheme#CHARACTER} is enabled, the fragment
     * is normalized to Unicode NFC.
     * </p>
     *
     * <p>
     * If {@code fragment} is {@code null} or empty, it is returned unchanged.
     * The {@code scheme} parameter is currently only used for possible
     * scheme-specific case-handling decisions.
     * </p>
     *
     * @param fragment the parsed fragment, or {@code null} if none
     * @param scheme   the IRI scheme, used for scheme-specific case rules; may be {@code null}
     * @return the normalized fragment, or the original value if no relevant
     *         modes are enabled
     */
    public String normalizeFragment(String fragment, String scheme) {
        return normalizeComponent(fragment, IRIRef.PART.FRAGMENT, scheme);
    }



    /**
     * Returns {@code true} if the given string consists only of US-ASCII
     * characters (code points {@code <= 0x7F}).
     *
     * @param s the string to test (must not be {@code null})
     * @return {@code true} if all characters are in the ASCII range, {@code false} otherwise
     */
    private static boolean isAsciiOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {  // au-delà de U+007F -> non ASCII
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether an empty path should be normalized to {@code "/"}
     * for the current IRI.
     * <p>
     * According to RFC&nbsp;3986/3987, when an IRI has an authority component
     * (i.e. starts with {@code "//host"}) and an empty path, the canonical
     * form uses {@code "/"} as the path. This method is used by
     * {@link IRIRef} to decide whether that rule should be applied.
     * </p>
     * <p>
     * In this implementation, the rule is enabled whenever {@link RFCNormalizationScheme#SCHEME}
     * is active and an authority is present, regardless of the specific
     * {@code scheme}. The {@code scheme} parameter is accepted for possible
     * future scheme-specific refinements.
     * </p>
     *
     * @param scheme       the IRI scheme (currently unused, may be {@code null})
     * @param hasAuthority {@code true} if the IRI has an authority component
     * @return {@code true} if an empty path should be replaced by {@code "/"},
     *         {@code false} otherwise
     */
    public boolean shouldNormalizeEmptyWithSlash(String scheme, boolean hasAuthority) {
        return has(RFCNormalizationScheme.SCHEME) && hasAuthority;
    }

    /**
     * Indicates whether dot-segment removal (path normalization) is allowed
     * for the current reference.
     * <p>
     * RFC&nbsp;3986/3987 specify that dot-segment normalization (removal of
     * {@code "."} and {@code ".."} segments) must <strong>not</strong> be
     * applied to relative references before resolution, because it can change
     * the result of resolution. In particular,
     * {@code relative.normalize().resolve(base)} is not guaranteed to be
     * equivalent to {@code relative.resolve(base).normalize()} if dot segments are
     * removed on the relative reference.
     * </p>
     * <p>
     * For this reason, this method only returns {@code true} when
     * {@link RFCNormalizationScheme#PATH} is enabled and the reference is a full IRI,
     * i.e. when {@code scheme} is non-{@code null}. Relative IRIs
     * (without a scheme) must first be resolved against a base
     * ({@link IRIRef#resolve(IRIRef)}) before any path normalization
     * is performed.
     * </p>
     *
     * @param scheme the scheme of the reference, or {@code null} if this is a relative IRI
     * @return {@code true} if dot-segment removal is allowed for this path,
     *         {@code false} otherwise
     */
    public boolean shouldRemoveDotsInPath(String scheme) {
        return has(RFCNormalizationScheme.PATH) && scheme != null;
    }

    /**
     * Normalizes a single path segment of an IRI.
     * <p>
     * This method is called by {@link IRIRef} on each segment of the path
     * (between {@code "/"} separators), and applies the same normalization
     * pipeline as other components, restricted to that segment:
     * </p>
     * <ul>
     *   <li>when {@link RFCNormalizationScheme#PCT} is enabled, percent-encodings {@code %HH}
     *       are normalized and decoded to RFC&nbsp;3986 {@code unreserved}
     *       characters via {@link #normalizePCTHandler(CharSlice, StringBuilder)};</li>
     *   <li>when {@link RFCNormalizationScheme#CHARACTER} is enabled, the resulting segment is
     *       normalized to NFC using {@link #normalizeNFC(String)};</li>
     *   <li>when {@link RFCNormalizationScheme#CASE} is enabled:
     *     <ul>
     *       <li>if {@link #shouldLowercase(IRIRef.PART, String)} returns {@code true}
     *           for {@link IRIRef.PART#SEGMENT} and the given {@code scheme}, all
     *           non-percent-encoded characters are lowercased and percent-hex
     *           digits are uppercased;</li>
     *       <li>otherwise, only the hexadecimal digits in {@code %HH}
     *           sequences are uppercased and the rest of the segment is left
     *           unchanged (the path remains case-sensitive by default).</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * If {@code segment} is empty, it is returned unchanged. This method
     * assumes {@code segment} is non-{@code null}; {@link IRIRef} is responsible
     * for enforcing that invariant.
     * </p>
     *
     * @param segment the path segment to normalize (never {@code null} here)
     * @param scheme  the IRI scheme, used for scheme-specific case rules; may be {@code null}
     * @return the normalized path segment
     */
    public String normalizeSegment(String segment, String scheme) {
        return normalizeComponent(segment, IRIRef.PART.SEGMENT, scheme);
    }

    /**
     * Normalizes the {@code port} component of an IRI.
     * <p>
     * When {@link RFCNormalizationScheme#SCHEME} is enabled, this method removes explicit ports
     * that match the well-known default for the given {@code scheme}. In
     * practice, if the supplied {@code port} equals
     * {@link #DEFAULT_PORTS the default port} for {@code scheme}, this method
     * returns {@code null} to indicate that the port can be omitted in the
     * normalized form.
     * </p>
     * <p>
     * If {@code port} is {@code null}, if {@code scheme} has no configured
     * default in {@link #DEFAULT_PORTS}, or if {@link RFCNormalizationScheme#SCHEME} is not
     * enabled, the {@code port} is returned unchanged.
     * </p>
     *
     * @param port   the explicit port number, or {@code null} if none was specified
     * @param scheme the IRI scheme (in lowercase), used to look up a default port;
     *               may be {@code null}
     * @return {@code null} if the port equals the default for this scheme and
     *         can be omitted; otherwise the original {@code port} value
     */
    public Integer normalizePort(Integer port, String scheme) {
        if (has(RFCNormalizationScheme.SCHEME) && port != null && DEFAULT_PORTS.containsKey(scheme) &&
                port.equals(DEFAULT_PORTS.get(scheme))) {
            return null;
        }
        return port;
    }

    /**
     * Indicates whether the given IRI {@link IRIRef.PART} should be treated as
     * case-insensitive for the specified {@code scheme}.
     * <p>
     * This looks up the scheme (in lowercase) in {@link #CASE_INSENSITIVE_PARTS}
     * and returns {@code true} if the corresponding set contains {@code part}.
     * </p>
     * <p>
     * In the current implementation, {@link #CASE_INSENSITIVE_PARTS} is empty,
     * so this method always returns {@code false}. Scheme-specific
     * case-insensitivity can be introduced later by populating that map.
     * </p>
     *
     * @param part   the IRI component to test
     * @param scheme the scheme name (expected lowercase), may be {@code null}
     * @return {@code true} if {@code part} is case-insensitive for this scheme,
     *         {@code false} otherwise (always {@code false} for now)
     */
    private static boolean shouldLowercase(IRIRef.PART part, String scheme) {
        EnumSet<IRIRef.PART> parts = CASE_INSENSITIVE_PARTS.get(scheme);
        return parts != null && parts.contains(part);
    }

    /**
     * Normalizes the given string to Unicode NFC (Normalization Form C).
     * <p>
     * If {@code input} is {@code null} or empty, it is returned unchanged.
     * Otherwise, {@link java.text.Normalizer#normalize(CharSequence,
     * java.text.Normalizer.Form)} is used with {@link java.text.Normalizer.Form#NFC}.
     * </p>
     *
     * @param input the string to normalize, may be {@code null}
     * @return the NFC-normalized string, or {@code input} if {@code null} or empty
     */
    private static String normalizeNFC(String input) {
        if (input == null || input.isEmpty()) { return input; }
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFC);
    }

    /**
     * Applies percent-encoding related normalization to a single IRI component.
     * <p>
     * This is a template method used by the various {@code normalizeX(...)}
     * methods when {@link RFCNormalizationScheme#PCT} (and/or case normalization)
     * is enabled. It delegates to
     * {@link #transformPctAndOtherChars(String,
     * java.util.function.BiConsumer, CharTransformer)} with the standard
     * percent-encoding handler and the given {@code transformer} for
     * non-percent-encoded characters.
     * </p>
     * <p>
     * The default implementation in {@code StandardComposableNormalizer}
     * performs RFC&nbsp;3986-style percent-encoding normalization: it decodes
     * {@code %HH} triplets only when they represent {@code unreserved}
     * characters, and re-emits all other percent-encoded octets unchanged.
     * </p>
     *
     * @param input       the component string to transform (may be {@code null})
     * @return the transformed string, or {@code input} if it is {@code null} or empty
     */
    protected String applyPctTransformation(String input) {
        return transformPctAndOtherChars(input, StandardComposableNormalizer::normalizePCTHandler, CharTransformer.IDENTITY);
    }


    /**
     * Transforms a string by applying a special handler to runs of
     * percent-encoded triplets and a simple transformer to all other
     * characters.
     * <p>
     * The method scans {@code input} left to right and:
     * </p>
     * <ul>
     *   <li>whenever it encounters a {@code '%'}, it assumes a run of
     *       {@code %HH} triplets (e.g. {@code "%41%42%43"}) and computes the
     *       maximal contiguous run. That run is exposed as a {@link CharSlice}
     *       to {@code handler}, which is responsible for appending the
     *       transformed representation to the supplied {@link StringBuilder};</li>
     *   <li>for any other character, it applies {@code transformer} and appends
     *       the result to the {@link StringBuilder}.</li>
     * </ul>
     * <p>
     * This utility is used to implement percent-encoding normalization
     * ({@code %HH} decoding, UTF-8 reconstruction, uppercasing hex digits)
     * while sharing a single traversal of the input string. If {@code input}
     * is {@code null} or empty, it is returned unchanged.
     * </p>
     *
     * @param input       the source string, may be {@code null}
     * @param handler     callback that processes a run of {@code %HH} triplets
     *                    and appends the result to the builder
     * @param transformer transformation applied to non-percent-encoded characters
     * @return the transformed string, or {@code input} if {@code null} or empty
     */
    protected static String transformPctAndOtherChars(String input, BiConsumer<CharSlice, StringBuilder> handler,
                                                    CharTransformer transformer) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length());
        final int len = input.length();

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == '%') {
                int start = i; // Start of a run of %HH triplets
                int end = start + 3;
                while (end < len && input.charAt(end) == '%') {
                    end = end + 3;
                }
                handler.accept(new CharSlice(input, start, end), sb);
                i = end - 1;
            } else {
                sb.append(transformer.apply(c));
            }
        }
        return sb.toString();
    }

    /**
     * Percent-run handler that uppercases the hexadecimal digits of
     * {@code %HH} triplets and re-emits them as percent-encoded.
     * <p>
     * The provided {@link CharSlice} is assumed to represent a contiguous
     * run of well-formed {@code %HH} triplets (e.g. {@code "%7e%3a"}).
     * For each triplet, this method:
     * </p>
     * <ul>
     *   <li>copies the leading {@code '%'} as-is, and</li>
     *   <li>uppercases both hex digits using {@link Character#toUpperCase(char)}.</li>
     * </ul>
     * <p>
     * No decoding is performed; this handler is used together with
     * {@link #transformPctAndOtherChars(String, java.util.function.BiConsumer, CharTransformer)}
     * to implement the {@link RFCNormalizationScheme#CASE} behaviour on percent-encoded
     * sequences.
     * </p>
     *
     * @param input   a {@link CharSlice} covering a run of {@code %HH} triplets
     * @param builder the target {@link StringBuilder} to append to
     */
    private static void uppercasePCTHandler(CharSlice input, StringBuilder builder) {
        for (int i = 0; i < input.length(); i+=3) {
            builder.append("%");
            builder.append(Character.toUpperCase(input.charAt(i + 1)));
            builder.append(Character.toUpperCase(input.charAt(i + 2)));
        }
    }

    /**
     * Percent-run handler for RFC&nbsp;3986-style percent-encoding normalization.
     * <p>
     * The provided {@link CharSlice} is assumed to represent a contiguous run
     * of well-formed {@code %HH} triplets. For each triplet, this handler:
     * </p>
     * <ul>
     *   <li>decodes {@code HH} to a single octet using {@link #decode(char, char)},</li>
     *   <li>checks whether the resulting character is RFC&nbsp;3986
     *       {@code unreserved} via {@link #UNRESERVED_PATTERN},</li>
     *   <li>if it is {@code unreserved}, appends the decoded character,</li>
     *   <li>otherwise, re-emits the original {@code %HH} sequence unchanged.</li>
     * </ul>
     * <p>
     * This corresponds to the standard percent-encoding normalization described
     * in RFC&nbsp;3987&nbsp;5.3.2.3 and is used when {@link RFCNormalizationScheme#PCT} is enabled,
     * via {@link #transformPctAndOtherChars(String, java.util.function.BiConsumer, CharTransformer)}.
     * </p>
     *
     * @param input   a {@link CharSlice} covering a run of {@code %HH} triplets
     * @param builder the target {@link StringBuilder} to append to
     */
    private static void normalizePCTHandler(CharSlice input, StringBuilder builder) {
        for (int i = 0; i < input.length(); i+=3) {
            char c = (char)decode(input.charAt(i+1), input.charAt(i+2));
            if (UNRESERVED_PATTERN.matcher(String.valueOf(c)).matches()) {
                builder.append(c);
            } else {
                builder.append("%").append(input.charAt(i + 1)).append(input.charAt(i + 2));
            }
        }
    }


    /**
     * Decodes two hexadecimal digits into a single byte value.
     * <p>
     * Each argument {@code h1} and {@code h2} is interpreted as a hex digit
     * ({@code 0–9}, {@code A–F}, {@code a–f}). The result is
     * {@code (high << 4) + low}, where {@code high} and {@code low} are
     * the numeric values of {@code h1} and {@code h2}, respectively.
     * </p>
     * <p>
     * If either character is not a valid hex digit, this method throws an
     * {@link AssertionError}, because such a situation should never occur
     * when called on already-validated percent-encodings.
     * </p>
     *
     * @param h1 the high hex digit
     * @param h2 the low hex digit
     * @return the decoded byte value in the range {@code 0..255}
     */
    protected static int decode(char h1, char h2) {
        int d1 = hexValue(h1);
        int d2 = hexValue(h2);
        if (d1 < 0 || d2 < 0) {
            throw new AssertionError("Internal error: invalid hex in % encoding: " + h1 + h2);
        }
        return (d1 << 4) + d2;
    }

    /**
     * Returns the numeric value of a hexadecimal digit.
     * <p>
     * Accepted digits are {@code '0'..'9'}, {@code 'A'..'F'} and
     * {@code 'a'..'f'}. For these, the method returns an integer in
     * the range {@code 0..15}. Any other character yields {@code -1}.
     * </p>
     *
     * @param c the character to interpret as a hex digit
     * @return the value {@code 0..15} if {@code c} is a valid hex digit,
     *         or {@code -1} otherwise
     */
    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return 10 + (c - 'A');
        }
        if (c >= 'a' && c <= 'f') {
            return 10 + (c - 'a');
        }
        return -1; // not a valid hex digit
    }



    public static void main(String... args) {
        String s = transformPctAndOtherChars("http://example.com:80/%7E%C3%A9%20%E2%82%AC%F0%9F%98%80%25",
                StandardComposableNormalizer::normalizePCTHandler, CharTransformer.IDENTITY);
        System.out.println(s);

    }
}
