package fr.inria.jfbaget.irirefs;


import java.util.*;

import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;
import fr.inria.jfbaget.nanoparse.matches.StringMatch;

/**
 * Internal representation of the path component of an IRI, as defined by RFC 3986 / RFC 3987.
 * <p>
 * The path is modeled as the unique non-empty list of string segments uch that joining them with
 * {@code "/"} yields the textual representation of the path:
 * <pre>
 *   recompose().equals(String.join("/", segments))
 * </pre>
 *
 * <h2>Canonical encoding</h2>
 * The following invariants are enforced:
 * <ul>
 *   <li>The list {@code segments} is never {@code null} and never empty.</li>
 *   <li>The empty path is encoded as a single empty segment: {@code [""]}.</li>
 *   <li>A rooted path (starting with {@code "/"}) has at least two segments and the first one is empty:
 *     <ul>
 *       <li>{@code "/"}          → {@code ["", ""]}</li>
 *       <li>{@code "/a/b"}       → {@code ["", "a", "b"]}</li>
 *       <li>{@code "/a/b/"}      → {@code ["", "a", "b", ""]}</li>
 *     </ul>
 *   </li>
 *   <li>A non-rooted path has a non-empty first segment:
 *     <ul>
 *       <li>{@code "a/b"}        → {@code ["a", "b"]}</li>
 *       <li>{@code "a/b/"}      → {@code ["a", "b", ""]}</li>
 *       <li>{@code "a//b"}      → {@code ["a", "", "b"]}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Operations</h2>
 * This class provides:
 * <ul>
 *   <li>Recomposition of the path into its string form ({@link #recompose()}).</li>
 *   <li>Dot-segment normalization according to RFC 3986 section&nbsp;5.2.4
 *       ({@link #removeDotSegments()}).</li>
 *   <li>Resolution of a relative path against a base path (used by {@code IRIRef.resolve}).</li>
 *   <li>Relativization of one path against another
 *       ({@link #relativize(IRIPath, boolean, int)}), used to construct the shortest
 *       “reasonable” relative reference between two IRIs.</li>
 * </ul>
 *
 * <h2>Scope and mutability</h2>
 * <ul>
 *   <li>{@code IRIPath} is package-private and is not part of the public API.</li>
 *   <li>Instances are mutable and are intended to be manipulated only by {@code IRIRef}
 *       and related classes within this package.</li>
 *   <li>No thread-safety guarantees are provided.</li>
 * </ul>
 */
final class IRIPath {
	
	private static final String EMPTY_SEGMENT = "";
	private static final String DOT_SEGMENT = ".";
	private static final String DOUBLEDOT_SEGMENT = "..";
	private static final String SEGMENT_SEPARATOR = "/";

    private final LinkedList<String> segments;


	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	/**
	 * Builds a canonical {@code IRIPath} from a parser match of one of the
	 * {@code ipath_*} rules (RFC 3986 / 3987).
	 * <p>
	 * This constructor interprets the parse tree and populates {@link #segments}
	 * according to the internal encoding invariants described in the class Javadoc:
	 * <ul>
	 *   <li>For {@code ipath_abempty}, the path is rooted if at least one segment
	 *       is present (parsed leading {@code "/"}), and the segments are taken as-is.</li>
	 *   <li>For {@code ipath_absolute}, {@code ipath_rootless} and {@code ipath_noscheme},
	 *       the first segment is extracted separately, then the remaining segments
	 *       are appended from the list match.</li>
	 *   <li>For {@code ipath_empty}, no segment is produced by the grammar; the
	 *       canonical encoding will be added below.</li>
	 * </ul>
	 * After the switch:
	 * <ul>
	 *   <li>If no segment was produced, the empty path is encoded as {@code [""]}.</li>
	 *   <li>If the path is rooted (a leading {@code "/"} was present in the syntax),
	 *       an empty segment is inserted at the beginning to encode the leading slash.</li>
	 * </ul>
	 *
	 * @param pathMatch a successful match of an {@code ipath_*} rule from the parser
	 * @throws AssertionError if the match does not correspond to a supported path rule
	 */
	IRIPath(IMatch pathMatch) {
		boolean rooted = false;  // whether the parsed syntax had a leading "/"
		this.segments = new LinkedList<>();
		switch (pathMatch.reader().getName()) {
			case "ipath_abempty" : {
				@SuppressWarnings("unchecked")
				List<StringMatch> segMatches = (List<StringMatch>)pathMatch.result();
				if (segMatches != null){
					rooted = true;
					for (StringMatch segmatch : segMatches) {
						this.segments.add((String)segmatch.result());
					}
				}
				break;
			}
			case "_opt_ipath_absolute": {
				rooted = true;
				break;
			}
			case "_seq_ipath_absolute" : {
				rooted = true;
				// fallthrough: same segments structure as rootless and noscheme
			}
			case "ipath_rootless": {}
			case "ipath_noscheme": {
				this.segments.add((String)((ListMatch)pathMatch).result().get(0).result());
				@SuppressWarnings("unchecked")
				List<StringMatch> segMatches = (List<StringMatch>)((ListMatch)pathMatch).result().get(1).result();
				if (segMatches != null){
					for (StringMatch segmatch : segMatches) {
						this.segments.add((String)segmatch.result());
					}
				}
				break;
			}
			case "ipath_empty" : {
				// no segments; handled by canonicalization below
				break;
			}
			default:
				throw new AssertionError("No such rule for an IRI path, given " + pathMatch.reader().getName());
		}
		// Canonicalize internal representation
		if (this.segments.isEmpty()) {
			// Empty path => [""].
			this.segments.addFirst(EMPTY_SEGMENT);
		}
		if (rooted){
			// Rooted path => insert leading "" segment to encode the '/'.
			this.segments.addFirst(EMPTY_SEGMENT);
		}
	}

	/**
	 * Low-level constructor from an already canonical list of segments.
	 * <p>
	 * This constructor assumes that {@code initialSegments} already satisfies
	 * the internal encoding invariants of {@link IRIPath}.
	 * <p>
	 * No additional validation is performed. It is intended for internal use
	 * by other methods in this package that manipulate paths in canonical form.
	 *
	 * @param initialSegments canonical segment list to copy into this path
	 */
    private IRIPath(List<String> initialSegments) {
        this.segments = new LinkedList<>(initialSegments);
    }

	/**
	 * Copy constructor.
	 * <p>
	 * Creates a new {@code IRIPath} with the same canonical segments as
	 * {@code other}. The list of segments is copied, so subsequent mutations
	 * to this path do not affect {@code other}, and vice versa. The individual
	 * segment strings are not cloned (strings are immutable).
	 *
	 * @param other path to copy
	 */
	IRIPath(IRIPath other) {
		this.segments = new LinkedList<>(other.segments);
	}

	// =================================================================================================================
	// UTILS
	// =================================================================================================================

	/**
	 * Overwrites this path's segments with those of {@code other}, keeping
	 * the same {@code IRIPath} instance. Intended for in-place operations
	 * such as {@code IRIRef.resolveInPlace}.
	 */
	IRIPath copyInPlace(IRIPath other) {
		this.segments.clear();
		this.segments.addAll(other.segments);
		return this;
	}

	// =================================================================================================================
	// GETTERS
	// =================================================================================================================

	/**
	 * Returns {@code true} if this path is rooted (i.e. its textual form
	 * starts with {@code "/"}).
	 * <p>
	 * With the internal canonical encoding:
	 * <ul>
	 *   <li>rooted paths have at least two segments and the first one is empty:
	 *     <ul>
	 *       <li>{@code "/"}       → {@code ["", ""]}</li>
	 *       <li>{@code "/a/b"}    → {@code ["", "a", "b"]}</li>
	 *     </ul>
	 *   </li>
	 *   <li>the empty path is encoded as {@code [""]} and is <strong>not</strong> rooted.</li>
	 * </ul>
	 *
	 * @return {@code true} if the path is rooted, {@code false} otherwise
	 */
    boolean isRooted() {
    	return this.segments.size() >= 2 && this.segments.get(0).equals(EMPTY_SEGMENT) ;
    }

	/**
	 * Returns {@code true} if this path is the empty path.
	 * <p>
	 * In the internal canonical encoding, the empty path is represented as
	 * a single empty segment:
	 * <ul>
	 *   <li>empty path {@code ""} → {@code [""]}</li>
	 * </ul>
	 * This is distinct from the root path {@code "/"},
	 * which is encoded as {@code ["", ""]} and for which {@link #isRooted()}
	 * returns {@code true} while this method returns {@code false}.
	 *
	 * @return {@code true} if this path is empty, {@code false} otherwise
	 */
	boolean isEmptyPath() {
		return this.segments.size() == 1 && this.segments.get(0).equals(EMPTY_SEGMENT);
	}

	/**
	 * Returns {@code true} if the given segment list represents the empty path
	 * in the internal canonical encoding.
	 * <p>
	 * By convention, the empty path is encoded as a single empty segment:
	 * <ul>
	 *   <li>empty path {@code ""} → {@code [""]}</li>
	 * </ul>
	 * This helper does not check for {@code null} and assumes that
	 * {@code segments} follows the same invariants as {@link IRIPath#segments}.
	 *
	 * @param segments segment list to test
	 * @return {@code true} if {@code segments} encodes the empty path
	 */
	private static boolean isEmptyPath(List<String> segments) {
		return segments.size() == 1 && segments.get(0).equals(EMPTY_SEGMENT);
	}

	/**
	 * Returns an unmodifiable view of the canonical segment list for this path.
	 * <p>
	 * The returned list reflects the internal representation used by {@code IRIPath},
	 * where joining the segments with {@code "/"} yields the textual path and the
	 * first segment encodes rootedness / emptiness (see class Javadoc).
	 * <p>
	 * The view itself cannot be modified (any attempt will throw
	 * {@link UnsupportedOperationException}), but it reflects subsequent mutations
	 * of this {@code IRIPath}.
	 *
	 * @return an unmodifiable view of this path's segments
	 */
    List<String> getSegments() {
    	return Collections.unmodifiableList(this.segments);	
    }

	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================

	/**
	 * Reconstructs the textual form of this path from its segments.
	 * <p>
	 * This is the inverse of the internal encoding: joining all segments with
	 * {@code "/"} yields exactly the path string as it appears in the IRI:
	 * <pre>
	 *   recompose().equals(String.join("/", segments))
	 * </pre>
	 * Rootedness and emptiness are already encoded in {@link #segments}
	 * (see class Javadoc), so no extra {@code "/"} is added or removed here.
	 *
	 * @return the string representation of this path
	 */
    String recompose() {
		return  String.join(SEGMENT_SEPARATOR, this.segments);
	}

	/**
	 * Appends the textual form of this path to the given {@link StringBuilder}.
	 * <p>
	 * This is equivalent to {@link #recompose()} but writes directly into an
	 * existing builder to avoid creating an intermediate string:
	 * <pre>
	 *   StringBuilder sb = new StringBuilder();
	 *   path.recompose(sb);
	 *   // sb now contains the path text
	 * </pre>
	 * The method assumes that {@link #segments} is non-empty (as guaranteed
	 * by the {@code IRIPath} invariants), writes the first segment as-is,
	 * then prefixes each subsequent segment with {@code "/"}.
	 *
	 * @param builder target {@code StringBuilder} to append to
	 * @return the same {@code builder}, for call chaining
	 */
    StringBuilder recompose(StringBuilder builder) {
    	ListIterator<String> iterator = this.segments.listIterator();
		builder.append(iterator.next()); // there is at least one
		while (iterator.hasNext()) {
			builder.append(SEGMENT_SEPARATOR).append(iterator.next());
		}
    	return builder;
    }

	/**
	 * Computes the length, in characters, of this path's textual representation,
	 * i.e. of {@link #recompose()}, without actually allocating the string.
	 * <p>
	 * This is used internally to compare the cost of different relative forms
	 * (e.g. when deciding whether a relativized path is shorter than the
	 * original absolute one).
	 *
	 * @return the character length of {@code recompose()}
	 */
	 int recompositionLength() {
		return recompositionLength(this.segments);
	 }

	/**
	 * Computes the length, in characters, of the textual path obtained by
	 * joining {@code segments} with {@code "/"}.
	 * <p>
	 * This is the static counterpart of {@link #recompositionLength()} and assumes
	 * that {@code segments} follows the same invariants as {@link IRIPath#segments}:
	 * in particular, the list must be non-empty.
	 * <p>
	 * The length is computed as the sum of the segment lengths plus one
	 * {@code "/"} separator between each pair of consecutive segments:
	 * <pre>
	 *   len = Σ seg.length() + (segments.size() - 1)
	 * </pre>
	 *
	 * @param segments canonical segment list
	 * @return number of characters in {@code String.join("/", segments)}
	 * @throws AssertionError if {@code segments} is empty (invariant violation)
	 */
	private static int recompositionLength(List<String> segments) {
		assert !segments.isEmpty() : "IRIPath invariant violated: segments must be non-empty";
		int len = 0;
		for (String seg : segments) {
			len += seg.length();
		}
		// separators between segments
		return len + segments.size() - 1;
	}

	// =================================================================================================================
	// RESOLUTION
	// =================================================================================================================

	/**
	 * Resolves this non-empty reference path against a base path, following
	 * the merge rule from RFC 3986 section 5.2.3.
	 * <p>
	 * This method assumes that:
	 * <ul>
	 *   <li>{@code this} is the path of the reference IRI (already parsed
	 *       into canonical segments),</li>
	 *   <li>{@code basePath} is the canonical path of the base IRI,</li>
	 *   <li>the reference path is <strong>non-empty</strong>
	 *       (the empty-path case is handled separately in {@code IRIRef}).</li>
	 * </ul>
	 *
	 * <p>Behaviour:
	 * <ul>
	 *   <li>If this path is <em>not rooted</em>, it is merged with the base path by
	 *       prepending all segments of {@code basePath} except its last segment.
	 *       This matches the “merge” step of RFC 3986:
	 *       take {@code basePath} without the last segment, then append the
	 *       reference path.</li>
	 *   <li>If the base IRI has an authority (host), and the merged path is
	 *       still not rooted, a leading empty segment is inserted to encode a
	 *       leading {@code "/"} and obtain an absolute path.</li>
	 * </ul>
	 *
	 * @param basePath        canonical path for the base IRI
	 * @param baseHasAuthority {@code true} if the base IRI has an authority component
	 */
	void resolveNonEmptyPath(IRIPath basePath, boolean baseHasAuthority) {
		// only merge when ref path is not absolute
		if (! this.isRooted()) {
			// prepend base path without its last segment
			ListIterator<String> it = basePath.segments.listIterator(basePath.segments.size() - 1);
			while (it.hasPrevious()) {
				this.segments.addFirst(it.previous());
			}
		}
		// If base has authority and we *still* don't start with "/",
		// add a leading "" segment to get a rooted path.
		if (baseHasAuthority && ! this.isRooted()) {
			this.segments.addFirst(EMPTY_SEGMENT);
		}
	}

	// =================================================================================================================
	// RELATIVISATION
	// =================================================================================================================

	/**
	 * Computes a relative path for this path with respect to {@code basePath},
	 * if such a relative representation is useful.
	 * <p>
	 * Intuitively, relativization answers the question:
	 * <em>“Given a base IRI and a target IRI, how can I write the target as a
	 * relative reference from the base, so that resolving it against the base
	 * recovers the original target?”</em>
	 * <br/>
	 * At the path level, this method tries to build a relative path {@code R}
	 * such that:
	 * <pre>
	 *   resolve(R, basePath) ≡ this
	 * </pre>
	 * and such that {@code R} is “reasonably short” compared to the original path.
	 *
	 * <h2>Compatibility of rootedness</h2>
	 * Not every pair of paths admits a meaningful relative form. Before doing
	 * any work, we apply two simple rules:
	 * <ul>
	 *   <li>If this path is rooted (starts with {@code "/"}) and {@code basePath}
	 *       is not rooted, then {@code basePath} cannot serve as a container for
	 *       this path. In that case, this method returns {@code this} unchanged
	 *       at the path level (caller will typically keep an absolute IRI).</li>
	 *   <li>If this path is not rooted and {@code basePath} is rooted, there is
	 *       no sensible relative representation, and the method returns
	 *       {@code null} to signal “no path relativization available”.</li>
	 * </ul>
	 *
	 * <h2>Algorithm (high level)</h2>
	 * Once rootedness is compatible:
	 * <ol>
	 *   <li>We compute the length (in segments) of the common prefix between
	 *       {@code this.segments} and {@code basePath.segments}.</li>
	 *   <li>We build an “erasing part” consisting of zero or more {@code ".."}
	 *       segments, which conceptually climbs from the base path up to the
	 *       common ancestor ({@link #buildWithErasingPart(int, int)}).</li>
	 *   <li>We adjust this erasing part in {@link #joinResultList} for special
	 *       cases: identical paths, one strictly containing the other, or the
	 *       need to preserve/avoid a trailing slash.</li>
	 *   <li>We append the remaining suffix of this path (the part after the
	 *       common prefix) to obtain a candidate relative path.</li>
	 *   <li>We clean up useless trailing slashes such as {@code "./"} or
	 *       {@code "../"} via {@link #removeUselessTrailingSlash}.</li>
	 *   <li>If {@code mustFindPath} is {@code true} and the result would be the
	 *       empty path (canonically {@code [""]}), we turn it into a minimal
	 *       non-empty relative path that still resolves to the same location
	 *       ({@link #makeNonEmpty(List, java.util.LinkedList)}).</li>
	 *   <li>Finally, {@link #selectBestRelativizedPath(LinkedList, int)} compares
	 *       the cost of this candidate relative path with the original path and
	 *       either keeps the relativized form, returns {@code this}, or returns
	 *       {@code null} depending on {@code maxCost} and rootedness.</li>
	 * </ol>
	 *
	 * <h2>{@code mustFindPath} and {@code maxCost}</h2>
	 * <ul>
	 *   <li>{@code mustFindPath}:
	 *     <ul>
	 *       <li>If {@code false}, the method is allowed to return an empty path
	 *           (meaning “no path component”) when that still resolves correctly.</li>
	 *       <li>If {@code true}, the result must not be the empty path; in
	 *           ambiguous cases, a minimal non-empty relative path is constructed
	 *           instead (e.g. {@code "."} or the last segment of the base).</li>
	 *     </ul>
	 *   </li>
	 *   <li>{@code maxCost}:
	 *     <ul>
	 *       <li>Represents how many extra characters you are willing to “pay”
	 *           to obtain a relative path instead of keeping the original one.</li>
	 *       <li>If the relativized path would be too long compared to the
	 *           original, {@link #selectBestRelativizedPath(LinkedList, int)}
	 *           either returns {@code this} (for rooted paths) or {@code null}
	 *           (for non-rooted paths), so that callers can fall back to an
	 *           absolute form or to another base IRI.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * @param basePath     the base path against which this path is relativized
	 * @param mustFindPath if {@code true}, forces the result to be a non-empty
	 *                     path that still resolves back to the same location
	 * @param maxCost      maximum tolerated overhead (in characters) for the
	 *                     relativized path compared to the original
	 * @return a new {@code IRIPath} representing a relative form of this path,
	 *         {@code this} if keeping the original rooted path is preferable,
	 *         or {@code null} if no useful relative representation can be found
	 *         under the given constraints
	 */
	public IRIPath relativize(IRIPath basePath, boolean mustFindPath, int maxCost) {

		if (this.isRooted() && !basePath.isRooted())
			return this;

		if (!this.isRooted() && basePath.isRooted())
			return null;

		int commonLength = computeCommonLength(this.segments, basePath.segments);
		List<String> thisRest = this.segments.subList(commonLength, this.segments.size());

		LinkedList<String> resultList = buildWithErasingPart(commonLength, basePath.segments.size());

		joinResultList(resultList, thisRest, basePath.segments, commonLength);
		resultList.addAll(thisRest);

		removeUselessTrailingSlash(resultList);

		if (mustFindPath && isEmptyPath(resultList)) {
			makeNonEmpty(resultList, basePath.segments);
		}
		return this.selectBestRelativizedPath(resultList, maxCost);
	}

	/**
	 * Computes the length (in segments) of the common prefix between two
	 * canonical segment lists.
	 * <p>
	 * More formally, this returns the largest {@code i} such that for all
	 * {@code 0 <= k < i}, {@code a.get(k).equals(b.get(k))}. The result is in
	 * the range {@code [0, min(a.size(), b.size())]}.
	 *
	 * @param a first segment list
	 * @param b second segment list
	 * @return number of leading segments shared by both lists
	 */
	private static int computeCommonLength(List<String> a, List<String> b) {
		int len = Math.min(a.size(), b.size());
		int i = 0;
		while (i < len && a.get(i).equals(b.get(i))) {
			i++;
		}
		return i;
	}

	/**
	 * Builds the initial “erasing part” of a relative path, consisting of
	 * zero or more {@code ".."} segments.
	 * <p>
	 * Given:
	 * <ul>
	 *   <li>{@code commonLength}: number of leading segments shared by the
	 *       target path and the base path,</li>
	 *   <li>{@code baseLength}: total number of segments in the base path,</li>
	 * </ul>
	 * the remaining suffix of the base has length:
	 * <pre>
	 *   baseRestLength = baseLength - commonLength
	 * </pre>
	 * To construct a relative path from the base to the target, we conceptually
	 * want to:
	 * <ul>
	 *   <li>“climb up” from the end of {@code base} back to the common prefix,</li>
	 *   <li>then descend along the suffix of the target.</li>
	 * </ul>
	 *
	 * This method encodes the “climb up” part as {@code baseRestLength - 1}
	 * repetitions of {@code ".."}:
	 * <ul>
	 *   <li>If {@code baseRestLength <= 1}, no {@code ".."} is needed and the
	 *       returned list is empty.</li>
	 *   <li>If {@code baseRestLength > 1}, each {@code ".."} removes one segment
	 *       of the base beyond the last common segment when resolving.</li>
	 * </ul>
	 *
	 * @param commonLength number of common leading segments between base and target
	 * @param baseLength   total number of segments in the base path
	 * @return a new {@link LinkedList} initialized with zero or more {@code ".."} segments
	 */
	private static LinkedList<String> buildWithErasingPart(int commonLength, int baseLength) {
		int baseRestLength = baseLength - commonLength;
		LinkedList<String> resultList = new LinkedList<>();
		for (int i = 0; i < baseRestLength - 1; i++) {
			resultList.add(DOUBLEDOT_SEGMENT);
		}
		return resultList;
	}

	/**
	 * Adjusts the initial "erasing part" of a relative path (the {@code ".."}
	 * chain in {@code result}) with a few special cases, before appending the
	 * remaining suffix of the target path.
	 * <p>
	 * Parameters:
	 * <ul>
	 *   <li>{@code result} – current prefix of the candidate relative path
	 *       (already initialized by {@link #buildWithErasingPart(int, int)});</li>
	 *   <li>{@code rest} – the suffix of this path after the common prefix
	 *       with {@code base}: {@code this.segments.subList(common, this.size)};</li>
	 *   <li>{@code base} – the full segment list of the base path;</li>
	 *   <li>{@code common} – length of the common prefix between this path
	 *       and the base path.</li>
	 * </ul>
	 *
	 * <p>This method tweaks {@code result} in four situations:
	 * <ol>
	 *   <li><b>Identical paths</b>:
	 *     <pre>{@code
	 *     rest.isEmpty() && base.size() == common
	 *     }</pre>
	 *     In this case, both paths are the same. A purely structural relative
	 *     path would be empty, which we encode as a single empty segment
	 *     ({@code [""]}) to match the canonical representation of the empty path.
	 *   </li>
	 *
	 *   <li><b>Target is strictly above the base</b> (shorter path):
	 *     <pre>{@code
	 *     rest.isEmpty() && base.size() > common
	 *     }</pre>
	 *     Here, the target path is a prefix of the base path. The erasing part
	 *     (already in {@code result}) climbs up from the base, and we then append
	 *     {@code ".."} plus the last common segment of the base. This yields
	 *     a relative form like {@code "../b"} rather than an empty or confusing
	 *     sequence of {@code ".."} only.
	 *   </li>
	 *
	 *   <li><b>Base is a strict prefix of the target</b>:
	 *     <pre>{@code
	 *     base.size() == common && !base.get(common - 1).equals("")
	 *     }</pre>
	 *     Here, the base path is a prefix of the target, and the last common
	 *     segment is not the empty root segment. We start the relative path with
	 *     that last common segment (e.g. {@code "c/d/e"} instead of
	 *     {@code "../c/d/e"}), which is shorter and more readable while still
	 *     resolving correctly.
	 *   </li>
	 *
	 *   <li><b>Root-like {@code rest} without an erasing part</b>:
	 *     <pre>{@code
	 *     result.isEmpty() && !rest.isEmpty() && rest.get(0).equals("")
	 *     }</pre>
	 *     If no {@code ".."} were needed and the remaining suffix {@code rest}
	 *     would start with an empty segment, directly using {@code rest} would
	 *     encode a rooted path (starting with {@code "/"}), which would ignore
	 *     the base path on resolution. To avoid that, we prefix the relative
	 *     path with {@code "."} so that patterns like {@code ".//b/"} are used
	 *     instead of {@code "//b/"} and still resolve back to the same target.
	 *   </li>
	 * </ol>
	 *
	 * After this adjustment, the caller appends {@code rest} to {@code result}
	 * to obtain the full candidate relative path.
	 */
	private void joinResultList(List<String> result, List<String> rest, List<String> base, int common) {
		if (rest.isEmpty() && base.size() == common) {
			result.add(EMPTY_SEGMENT);

		} else if (rest.isEmpty()) {
			result.add(DOUBLEDOT_SEGMENT);
			result.add(base.get(common-1));

		} else if (base.size() == common && !base.get(common-1).equals(EMPTY_SEGMENT)) {
			result.add(base.get(common-1));

		} else if (result.isEmpty() && rest.get(0).equals(EMPTY_SEGMENT)) {
			result.add(DOT_SEGMENT);
		}
	}

	/**
	 * Normalizes away useless trailing slashes in the candidate relative path.
	 * <p>
	 * In the internal encoding, a trailing {@code ""} segment means the
	 * textual path ends with {@code "/"}. For some relative patterns this is
	 * redundant or misleading:
	 * <ul>
	 *   <li>{@code "./"} and {@code "..//"} are textually possible, but
	 *       semantically equivalent to {@code "."} and {@code ".."} as
	 *       relative references.</li>
	 * </ul>
	 * To keep relative paths as short and conventional as possible, we drop a
	 * trailing empty segment when it follows a single-dot or double-dot segment:
	 * <pre>
	 *   ["."      , ""] → ["."]
	 *   [".."     , ""] → [".."]
	 *   ["..", "a", ""] → unchanged ("/a/" is significant)
	 * </pre>
	 *
	 * @param resultList mutable list of segments for the candidate relative path
	 */
	private void removeUselessTrailingSlash(LinkedList<String> resultList) {
		if (resultList.size() > 1 && resultList.getLast().equals(EMPTY_SEGMENT) ) {
			if (resultList.get(resultList.size() - 2).equals(DOT_SEGMENT)) {
				resultList.removeLast();
			}
			else if (resultList.get(resultList.size() - 2).equals(DOUBLEDOT_SEGMENT)) {
				resultList.removeLast();
			}
		}
	}

	/**
	 * Ensures that a candidate relative path is non-empty while still
	 * resolving back to the same location as the original target.
	 * <p>
	 * This helper is used when {@code mustFindPath == true} and the
	 * relativization logic has produced an "empty" path – i.e. a single
	 * empty segment {@code [""]}, which encodes {@code ""} as the path.
	 * In many contexts, an explicitly non-empty relative reference is
	 * preferred for display or disambiguation.
	 * <p>
	 * Policy:
	 * <ul>
	 *   <li>If the base path ends with an empty segment (i.e. the base path
	 *       is rooted and ends with {@code "/"}), we turn the relative path
	 *       into {@code "."}. This keeps us “at the same directory level”
	 *       and still resolves to the same location.</li>
	 *   <li>Otherwise, we replace the first segment with the last segment of
	 *       the base path. For example, if both base and target are
	 *       {@code "http:/a/b"}, we use {@code "b"} as a non-empty relative
	 *       path that still resolves to {@code "http:/a/b"}.</li>
	 * </ul>
	 *
	 * <p>Precondition: {@code resultList} currently encodes the empty path
	 * as a single-element list, so {@code resultList.set(0, ...)} is always
	 * valid.
	 *
	 * @param resultList mutable segment list for the relative path candidate
	 * @param basePath   canonical segment list of the base path
	 */
	private void makeNonEmpty(List<String> resultList, LinkedList<String> basePath) {
		if (basePath.getLast().equals(EMPTY_SEGMENT)) {
			resultList.set(0, DOT_SEGMENT);
		} else {
			resultList.set(0, basePath.getLast());
		}
	}

	/**
	 * Chooses between the original path and the candidate relative path,
	 * based on their textual length and a configurable {@code maxCost}.
	 * <p>
	 * At this point, {@code resultList} encodes a candidate relative path
	 * (as canonical segments), and we need to decide whether it is worth
	 * using instead of the original {@code this} path.
	 *
	 * <h2>Decision rules</h2>
	 * Let:
	 * <ul>
	 *   <li>{@code oldLen} = length of {@code this.recompose()},</li>
	 *   <li>{@code newLen} = length of the relative candidate obtained from
	 *       {@code resultList}.</li>
	 * </ul>
	 *
	 * <ul>
	 *   <li><b>If this path is rooted</b>:
	 *     <ul>
	 *       <li>We keep the original path whenever it is shorter or equal:
	 *           {@code oldLen <= newLen} ⇒ return {@code new IRIPath(this)}.</li>
	 *       <li>Otherwise we accept the shorter relative candidate.</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>If this path is not rooted</b>:
	 *     <ul>
	 *       <li>We interpret {@code maxCost} as the maximum number of extra
	 *           characters we are willing to pay for a relative form. If
	 *           {@code newLen >= oldLen + maxCost}, we reject the candidate
	 *           and return {@code null}, letting the caller keep an absolute
	 *           or differently-based representation.</li>
	 *       <li>Otherwise, the relative form is considered acceptable and
	 *           we return it.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * <p>This asymmetry reflects typical usage: for rooted paths, we only
	 * switch to a relative form when it is strictly beneficial; for
	 * non-rooted paths (e.g. when multiple bases are available), we are
	 * more tolerant but still avoid excessively verbose {@code ../..}
	 * chains via {@code maxCost}.
	 *
	 * @param resultList canonical segment list for the candidate relative path
	 * @param maxCost    maximum tolerated overhead (in characters) for the
	 *                   relative path compared to the original, when this
	 *                   path is non-rooted
	 * @return a new {@code IRIPath} using either the original path or the
	 *         relative candidate, or {@code null} if the candidate is deemed
	 *         too expensive for a non-rooted path
	 */
	private IRIPath selectBestRelativizedPath(LinkedList<String> resultList, int maxCost) {
		int newLen = recompositionLength(resultList);
		int oldLen = this.recompositionLength();
		if (this.isRooted()) {
			if (oldLen <= newLen) {
				return new IRIPath(this);
			}
		} else {
			if (oldLen + maxCost <= newLen) {
				return null;
			}
		}
		return new IRIPath(resultList);
	}

	// =================================================================================================================
	// NORMALISATION
	// =================================================================================================================

	/**
	 * Normalizes this path by removing dot segments, according to
	 * RFC 3986, section 5.2.4 ("Remove Dot Segments").
	 * <p>
	 * The algorithm is applied directly on the canonical segment list.
	 * For each segment:
	 * <ul>
	 *   <li>If the segment is {@code "."}, it is removed or turned into an
	 *       empty segment at the end (via {@link #removeDottedSegment(ListIterator)}),
	 *       so that it no longer contributes to the logical path.</li>
	 *   <li>If the segment is {@code ".."}, the segment itself is removed or
	 *       turned into an empty trailing segment, and then the previous
	 *       non-root segment (if any) is erased via
	 *       {@link #erasePreviousSegment(ListIterator)}.</li>
	 *   <li>All other segments are left unchanged.</li>
	 * </ul>
	 * The net effect matches the standard URL/URI dot-segment removal:
	 * sequences such as {@code "./"}, {@code "../"}, {@code "a/./b"},
	 * {@code "a/../b"} are normalized in place, but the overall canonical
	 * encoding of rooted vs non-rooted paths is preserved.
	 */
	void removeDotSegments() {
		ListIterator<String> iterator = this.segments.listIterator();
		while (iterator.hasNext()) {
			switch (iterator.next()) {
				case DOT_SEGMENT -> {
					removeDottedSegment(iterator);
				}
				case DOUBLEDOT_SEGMENT -> {
					removeDottedSegment(iterator);
					erasePreviousSegment(iterator);
				}
				default -> { }
			}
		}
	}

	/**
	 * Handles the first step of dot / double-dot removal for the current segment.
	 * <p>
	 * Precondition: this method is called immediately after {@code iterator.next()}
	 * has returned either {@code "."} or {@code ".."}. In both cases, the iterator
	 * is positioned <em>after</em> that segment, and {@link ListIterator#remove()}
	 * or {@link ListIterator#set(Object)} will affect the last element returned
	 * by {@code next()}.
	 *
	 * <ul>
	 *   <li>If there is another segment after the current one
	 *       ({@code iterator.hasNext() == true}), the dotted segment is simply
	 *       removed. This corresponds to the RFC cases like {@code "/./"} or
	 *       {@code "/../"} in the middle of the path, where the trailing slash
	 *       is effectively preserved by the following segment.</li>
	 *
	 *   <li>If there is no following segment, the dotted segment occurs at the
	 *       end of the path. In this case, we rewrite it as an empty segment
	 *       ({@code ""}) so that the trailing slash is preserved in the canonical
	 *       encoding, then move the iterator one position back so that subsequent
	 *       logic (e.g. {@code erasePreviousSegment}) can inspect the preceding
	 *       segment if needed.</li>
	 * </ul>
	 *
	 * This helper does not erase the previous segment for {@code ".."}; that is
	 * handled separately by {@link #erasePreviousSegment(ListIterator)}.
	 */
	private static void removeDottedSegment(ListIterator<String> iterator) {
		if (iterator.hasNext()) {
			iterator.remove();
		} else {
			iterator.set(EMPTY_SEGMENT);
			iterator.previous();
		}
	}

	/**
	 * Erases the segment immediately preceding the current position, if appropriate,
	 * as part of handling a {@code ".."} segment (RFC 3986 §5.2.4).
	 * <p>
	 * Precondition: this method is called <em>after</em>
	 * {@link #removeDottedSegment(ListIterator)} has processed a {@code ".."}:
	 * <ul>
	 *   <li>If {@code ".."} was in the middle of the path, it has just been removed,
	 *       and the iterator is positioned after the segment that followed it.</li>
	 *   <li>If {@code ".."} was at the end of the path, it has been rewritten as an
	 *       empty segment {@code ""} and the iterator has been moved back to stand
	 *       <em>before</em> that empty segment.</li>
	 * </ul>
	 *
	 * This method then:
	 * <ol>
	 *   <li>Moves one segment backwards with {@code previous()}, to reach the
	 *       candidate segment to erase (the one before the original {@code ".."}).</li>
	 *   <li>Peeks at that segment via {@code next()} and checks its index via
	 *       {@code previousIndex()}.</li>
	 *   <li>Removes the segment <strong>unless</strong> it is the leading empty
	 *       segment at index {@code 0}, which encodes the root {@code "/"} in the
	 *       canonical representation.</li>
	 * </ol>
	 *
	 * In other words, this removes the "directory" that {@code ".."} should cancel,
	 * but never erases the root marker {@code ""} at position 0, so paths like
	 * {@code "/.."} normalize to {@code "/"} rather than to the empty path.
	 *
	 * @param iterator list iterator positioned as described above
	 */
	private static void erasePreviousSegment(ListIterator<String> iterator) {
		if (iterator.hasPrevious()) {
			iterator.previous();
			if (!iterator.next().equals(EMPTY_SEGMENT) || iterator.previousIndex() != 0) {
				iterator.remove();
			}
		}
	}
}
