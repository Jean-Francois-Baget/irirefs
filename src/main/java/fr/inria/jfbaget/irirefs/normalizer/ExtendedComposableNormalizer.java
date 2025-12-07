package fr.inria.jfbaget.irirefs.normalizer;

import fr.inria.jfbaget.irirefs.parser.IRIRefParser;
import fr.inria.jfbaget.irirefs.utils.CharSlice;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Extended normalizer that interprets {@link RFCNormalizationScheme#PCT}
 * using UTF-8 and {@code iunreserved} instead of only RFC&nbsp;3986
 * {@code unreserved} characters.
 * <p>
 * This is a non-standard, more aggressive normalization profile, intended
 * for applications that use IRIs as UTF-8-based canonical identifiers,
 * and that are willing to treat percent-encodings of
 * RFC&nbsp;3987 {@code iunreserved} characters as equivalent to their
 * decoded form.
 * </p>
 */
public class ExtendedComposableNormalizer extends StandardComposableNormalizer {

    /**
     * Pattern matching a single RFC&nbsp;3987 {@code iunreserved} character,
     * as defined by {@link IRIRefParser#IUNRESERVED}. Used by the private
     * percent-run handler in this class to decide whether the decoded code
     * point is an RFC&nbsp;3987 {@code iunreserved} character.
     */
    private static final Pattern IUNRESERVED_PATTERN = Pattern.compile("[" + IRIRefParser.IUNRESERVED + "]");

    /**
     * Constructs an {@code ExtendedComposableNormalizer} with the given set
     * of normalization modes.
     * <p>
     * Each requested {@link RFCNormalizationScheme} is forwarded to the
     * superclass constructor. As with
     * {@link StandardComposableNormalizer#StandardComposableNormalizer(List)},
     * the composite mode {@link RFCNormalizationScheme#SYNTAX} is expanded
     * into its components {@link RFCNormalizationScheme#CASE},
     * {@link RFCNormalizationScheme#CHARACTER} and
     * {@link RFCNormalizationScheme#PCT}.
     * </p>
     *
     * @param requested the normalization modes to enable (must not be {@code null})
     */
    public ExtendedComposableNormalizer(List<RFCNormalizationScheme> requested) {
        super(requested);
    }


    /**
     * Convenience constructor using varargs.
     *
     * <pre>
     * new ExtendedComposableNormalizer(RFCNormalizationScheme.CASE,
     *                                  RFCNormalizationScheme.PATH,
     *                                  RFCNormalizationScheme.PCT);
     * </pre>
     *
     * @param requested the normalization flags to enable
     */
    public ExtendedComposableNormalizer(RFCNormalizationScheme... requested) {
        super(List.of(requested));
    }

    /** {@inheritDoc}
     * <p>
     * In {@code ExtendedComposableNormalizer}, this uses a UTF-8-based handler
     * that can decode runs of {@code %HH} into RFC&nbsp;3987 {@code iunreserved}
     * characters (when valid), providing a more aggressive, non-standard
     * interpretation of {@link RFCNormalizationScheme#PCT} than the base
     * {@link StandardComposableNormalizer} implementation.
     * </p>
     */
    @Override
    protected String applyPctTransformation(String input) {
        return transformPctAndOtherChars(input, ExtendedComposableNormalizer::normalizePCTHandler, CharTransformer.IDENTITY);
    }

    /**
     * Percent-run handler for the extended {@link RFCNormalizationScheme#PCT} normalization,
     * that decodes PCTs when they encode RFC&nbsp;3987 {@code iunreserved} characters instead
     * of only RFC&nbsp;3986 {@code unreserved} characters.
     * <p>
     * The provided {@link CharSlice} is assumed to represent a contiguous run
     * of well-formed {@code %HH} triplets. This handler interprets that run
     * as a sequence of bytes and tries to decode it as UTF-8:
     * </p>
     * <ul>
     *   <li>The first byte determines how many continuation bytes are expected,
     *       using {@link #extraPCTChunks(int)} (0, 1, 2, or 3 extra bytes,
     *       or {@code -1} if the leading byte is not valid as a UTF-8 starter).</li>
     *   <li>If the leading byte is invalid ({@code extra == -1}), the original
     *       {@code %HH} for that byte is re-emitted unchanged.</li>
     *   <li>If there are not enough {@code %HH} triplets left in the run to
     *       form the full sequence, all remaining triplets are re-emitted
     *       unchanged.</li>
     *   <li>Otherwise, the expected continuation bytes ({@code 10xxxxxx})
     *       are decoded and combined with the leading byte into a Unicode code
     *       point.</li>
     *   <li>If the UTF-8 sequence is valid <em>and</em> the resulting code point
     *       matches {@link #IUNRESERVED_PATTERN} (i.e. is allowed as an
     *       {@code iunreserved} character), the corresponding character(s) are
     *       appended to the output.</li>
     *   <li>If the sequence is invalid or the resulting code point is not
     *       {@code iunreserved}, all {@code %HH} triplets involved in that
     *       sequence are re-emitted unchanged.</li>
     * </ul>
     * <p>
     * This behaviour goes beyond RFC&nbsp;3987, which only requires decoding
     * to RFC&nbsp;3986 {@code unreserved}. In {@link ExtendedComposableNormalizer},
     * it implements a more aggressive, non-standard interpretation of
     * {@link RFCNormalizationScheme#PCT}, intended for applications that
     * treat IRIs as UTF-8-based canonical identifiers.
     * </p>
     *
     * @param input   a {@link CharSlice} covering a run of {@code %HH} triplets
     * @param builder the target {@link StringBuilder} to append to
     */
    private static void normalizePCTHandler(CharSlice input, StringBuilder builder) {
        for (int i = 0; i < input.length(); i+=3) {
            int c = decode(input.charAt(i + 1), input.charAt(i + 2));
            int extra = extraPCTChunks(c);
            if (extra == -1) {
                builder.append("%").append(input.charAt(i + 1)).append(input.charAt(i + 2));
            } else if (i + (extra + 1) * 3 > input.length()) {
                while (i < input.length()) {
                    builder.append("%").append(input.charAt(i + 1)).append(input.charAt(i + 2));
                    i += 3;
                }
            } else { // we know how many extra chunks, and we have them
                int acc = initializeFirstChunk(c, extra);
                int j = i;
                int k = 0;
                boolean ok = true;
                while (ok && k < extra) {
                    j += 3;
                    int b = decode(input.charAt(j + 1), input.charAt(j + 2));
                    if ((b & 0xC0) != 0x80) {
                        ok = false;
                    } else {
                        acc = (acc << 6) | (b & 0x3F);
                    }
                    k += 1;
                }
                if (ok && IUNRESERVED_PATTERN.matcher(new String(Character.toChars(acc))).matches()) {
                    builder.append(Character.toChars(acc));
                } else {
                    for (int r = 0; r <= extra; r++) {
                        builder.append("%")
                                .append(input.charAt(i + (3 * r) + 1))
                                .append(input.charAt(i + (3 * r) + 2));
                    }
                }
                i += (extra*3);
            }
        }
    }

    /**
     * Initializes the accumulator for the first byte of a UTF-8 sequence.
     * <p>
     * Given the leading byte value and the number of expected continuation
     * bytes ({@code extra}), this method masks out the UTF-8 prefix bits
     * and returns the initial payload:
     * </p>
     * <ul>
     *   <li>{@code extra == 0}: single-byte ASCII, payload is {@code value};</li>
     *   <li>{@code extra == 1}: 2-byte sequence, keep {@code 0b00011111};</li>
     *   <li>{@code extra == 2}: 3-byte sequence, keep {@code 0b00001111};</li>
     *   <li>{@code extra == 3}: 4-byte sequence, keep {@code 0b00000111}.</li>
     * </ul>
     * <p>
     * Any other {@code extra} value is considered an internal logic error
     * and results in an {@link AssertionError}.
     * </p>
     *
     * @param value the first byte of the UTF-8 sequence (0–255)
     * @param extra the number of expected continuation bytes (0–3)
     * @return the initial accumulator value for the code point
     */
    private static int initializeFirstChunk(int value, int extra) {
        switch (extra) {
            case 0 -> { return value; }
            case 1 -> { return value & 0x1F; }
            case 2 -> { return value & 0x0F; }
            case 3 -> { return value & 0x07; }
            default -> { throw new AssertionError("Impossible extra value: " + extra); }
        }
    }



    /**
     * Determines how many additional UTF-8 bytes are required after a leading byte.
     * <p>
     * The input {@code c} is the numeric value of a single byte (0–255),
     * typically obtained from decoding a {@code %HH} sequence. This method
     * inspects the high-order bits to classify it as:
     * </p>
     * <ul>
     *   <li>{@code 0xxxxxxx} → ASCII, 1-byte sequence → returns {@code 0};</li>
     *   <li>{@code 110xxxxx} → 2-byte sequence       → returns {@code 1};</li>
     *   <li>{@code 1110xxxx} → 3-byte sequence       → returns {@code 2};</li>
     *   <li>{@code 11110xxx} → 4-byte sequence      → returns {@code 3};</li>
     *   <li>any other pattern → not a valid UTF-8 leading byte → returns {@code -1}.</li>
     * </ul>
     *
     * @param c the leading byte value (0–255)
     * @return the number of expected continuation bytes (0–3), or {@code -1}
     *         if {@code c} cannot start a valid UTF-8 sequence
     */
    private static int extraPCTChunks(int c) {
        if ((c & 0x80) == 0) {
            // 0xxxxxxx -> ASCII (1 byte total)
            return 0;
        } else if ((c & 0xE0) == 0xC0) {
            // 110xxxxx -> 2-byte sequence (1 extra byte)
            return 1;
        } else if ((c & 0xF0) == 0xE0) {
            // 1110xxxx -> 3-byte sequence (2 extra bytes)
            return 2;
        } else if ((c & 0xF8) == 0xF0) {
            // 11110xxx -> 4-byte sequence (3 extra bytes)
            return 3;
        } else {
            // Invalid leading byte for UTF-8
            return -1;
        }
    }
}
