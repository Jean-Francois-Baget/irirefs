package fr.inria.jfbaget.irirefs.utils;

/**
 * A lightweight, immutable view over a substring of a {@link String},
 * implementing {@link CharSequence} without allocating a new {@link String}
 * for the slice.
 * <p>
 * The underlying {@code orig} string is shared; this class only stores
 * start and end indices. It is therefore cheap to create and suitable for
 * use in tight loops or parsing code.
 * </p>
 * <p>
 * All indices are in the coordinate system of the original string:
 * {@code start} is inclusive and {@code end} is exclusive, just like
 * {@link String#substring(int, int)}.
 * </p>
 */
public class CharSlice implements CharSequence {

    private final String orig;
    private final int start;
    private final int end;

    /**
     * Creates a new {@code CharSlice} representing the range
     * {@code [start, end)} of the given {@code orig} string.
     *
     * @param orig  the original string; must not be {@code null}
     * @param start the start index (inclusive), in {@code orig}
     * @param end   the end index (exclusive), in {@code orig}
     * @throws NullPointerException      if {@code orig} is {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of range
     *                                   ({@code start < 0}, {@code end < start},
     *                                   or {@code end > orig.length()})
     */
    public CharSlice(String orig, int start, int end) {
        if (orig == null) {
            throw new NullPointerException("orig must not be null");
        }
        if (start < 0 || end < start || end > orig.length()) {
            throw new IndexOutOfBoundsException(
                    "Invalid slice [" + start + ", " + end + ") for string of length " + orig.length()
            );
        }
        this.orig = orig;
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the length of this slice, in characters.
     * <p>
     * This is simply {@code end - start} in the coordinate system of the
     * original string.
     * </p>
     *
     * @return the number of characters in this slice
     */
    @Override
    public int length() {
        return this.end - this.start;
    }

    /**
     * Returns the character at the given index within this slice.
     * <p>
     * The index is relative to the slice, not to the original string:
     * {@code index == 0} corresponds to {@code orig.charAt(start)}.
     * </p>
     *
     * @param index the index of the character to return, between
     *              {@code 0} (inclusive) and {@code length()} (exclusive)
     * @return the character at the given index in this slice
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    @Override
    public char charAt(int index) {
        return this.orig.charAt(this.start + index);
    }

    /**
     * Returns a new {@code CharSlice} that is a subsequence of this slice.
     * <p>
     * The coordinates {@code start} and {@code end} are interpreted relative
     * to this slice (from {@code 0} to {@code length()}), and are translated
     * back into the coordinate system of the original string.
     * </p>
     *
     * @param start the start index (inclusive) within this slice
     * @param end   the end index (exclusive) within this slice
     * @return a new {@code CharSlice} representing {@code [start, end)} of this slice
     * @throws IndexOutOfBoundsException if {@code start < 0}, {@code end < start},
     *                                   or {@code end > length()}
     */
    @Override
    public CharSlice subSequence(int start, int end) {
        return new CharSlice(this.orig, this.start + start, this.start + end);
    }

    /**
     * Returns the sliced region as a {@link String}.
     * <p>
     * This allocates a new {@code String} instance. No allocation occurs as long
     * as you keep working with {@code CharSlice} / {@link CharSequence}; conversion
     * to {@code String} is done only when explicitly requested.
     * </p>
     *
     * @return a new {@code String} containing the characters of this slice
     */
    @Override
    public String toString() {
        // This allocates a String only when you explicitly ask for it.
        return orig.substring(start, end);
    }
}
