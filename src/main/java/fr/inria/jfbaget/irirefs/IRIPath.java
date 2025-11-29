package fr.inria.jfbaget.irirefs;


import java.util.*;

import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;
import fr.inria.jfbaget.nanoparse.matches.StringMatch;

public final class IRIPath {
	
	private static final String EMPTY_SEGMENT = "";
	private static final String DOT_SEGMENT = ".";
	private static final String DOUBLEDOT_SEGMENT = "..";
	private static final String SEGMENT_SEPARATOR = "/";
	
	//private boolean rooted;
    private final LinkedList<String> segments;
    
    private static final boolean DEBUG_DOT_SEGMENTS = false;


	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	IRIPath(IMatch pathMatch) {
		boolean rooted = false;
		this.segments = new LinkedList<>();
		switch (pathMatch.reader().getName()) {
			case "ipath_abempty" : {
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
				List<StringMatch> segMatches = (List<StringMatch>)((ListMatch)pathMatch).result().get(1).result();
				if (segMatches != null){
					for (StringMatch segmatch : segMatches) {
						this.segments.add((String)segmatch.result());
					}
				}
				break;
			}
			case "ipath_empty" : {
				break;
			}
			default:
				throw new AssertionError("No such rule for an IRI path, given " + pathMatch.reader().getName());
		}
		if (this.segments.isEmpty()) {
			this.segments.addFirst(EMPTY_SEGMENT);
		}
		if (rooted){
			this.segments.addFirst(EMPTY_SEGMENT);
		}
	}


    IRIPath(List<String> initialSegments) {
        this.segments = new LinkedList<>(initialSegments);
    }

	IRIPath(IRIPath other) {
		this.segments = new LinkedList<>(other.segments);
	}

	// =================================================================================================================
	// UTILS
	// =================================================================================================================

	IRIPath copyInPlace(IRIPath other) {
		this.segments.clear();
		this.segments.addAll(other.segments);
		return this;
	}

	// =================================================================================================================
	// GETTERS
	// =================================================================================================================


    boolean isRooted() {
    	return this.segments.size() >= 2 && this.segments.get(0).equals(EMPTY_SEGMENT) ;
    }

	boolean isEmpty() {
		return this.segments.size() == 1 && this.segments.get(0).equals(EMPTY_SEGMENT);
	}

	private static boolean isEmpty(List<String> segments) {
		return segments.size() == 1 && segments.get(0).equals(EMPTY_SEGMENT);
	}

    List<String> getSegments() {
    	return Collections.unmodifiableList(this.segments);	
    }

	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================
    
    String recompose() {
		return  String.join(SEGMENT_SEPARATOR, this.segments);
	}

    
    StringBuilder recompose(StringBuilder builder) {
    	ListIterator<String> iterator = this.segments.listIterator();
		builder.append(iterator.next()); // there is at least one
		while (iterator.hasNext()) {
			builder.append(SEGMENT_SEPARATOR).append(iterator.next());
		}
    	return builder;
    }

	 int recompositionLength() {
		return recompositionLength(this.segments);
	 }

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


	public IRIPath relativize(IRIPath basePath, boolean mustFindPath, int maxCost) {

		if (this.isRooted() && !basePath.isRooted())
			return this;

		if (!this.isRooted() && basePath.isRooted())
			return null;

		boolean rootedResult = false;

		int commonLength = computeCommonLength(this.segments, basePath.segments);
		List<String> baseRest =  basePath.segments.subList(commonLength, basePath.segments.size());
		List<String> thisRest = this.segments.subList(commonLength, this.segments.size());

		LinkedList<String> resultList = buildWithErasingPart(commonLength, basePath.segments.size());



		// Handling the special cases
		if (basePath.segments.size() == commonLength && this.segments.size() > commonLength) {
			System.out.println("Included base");
			if ((this.isRooted() && commonLength > 0) || commonLength > 0) {
				resultList.add(basePath.segments.getLast());
			} else {
				rootedResult = this.isRooted();
			}
		} else if (this.segments.size() == commonLength && basePath.segments.size() > commonLength) {
			System.out.println("Included target");
			if ((this.isRooted() && commonLength > 0) || commonLength > 0) {
				resultList.add("..");
				resultList.add(basePath.segments.get(commonLength - 1));
			} else{
				resultList.add("..");
			}
		}

		// Adding the REMAININGPART: segments of this that are not in common
		resultList.addAll(thisRest);
		System.out.println(resultList.size());


		// Fix for the base: /foo  this: /  mistake
		boolean effectivelyEmpty = resultList.isEmpty() || isEmpty(resultList);

		// If both are rooted, paths differ, and the candidate is effectively empty,
		// an empty relative path ("") would resolve back to the base path, not the target.
		// In this situation, use a rooted empty path ("/") instead.
		/*
		if (this.rooted && basePath.rooted && effectivelyEmpty
				&& !this.segments.equals(basePath.segments)) {

			rootedResult = true;
			resultList.clear();   // rooted=true + segments=[] ⇒ path "/"
		}

		 */

		// Handling the special case when we found an empty result (or [""]), but we are not allowed to
		if (!rootedResult && mustFindPath) {
			if (effectivelyEmpty) {
				// If target path itself is empty, there is no non-empty relative path
				// that keeps the same path and avoids query inheritance.
				if (this.segments.isEmpty()) {
					return null;
				}

				// Paths are identical but mustFindPath == true.
				// We need a non-empty relative path that resolves to the same location.

				String lastTargetSeg = this.segments.get(this.segments.size() - 1);

				resultList.clear();
				if (lastTargetSeg.equals(EMPTY_SEGMENT)) {
					// Target ends with "/", use "." so that merge+dot-removal yields same path
					resultList.add(DOT_SEGMENT);      // "."
				} else {
					// No trailing "/", use last segment (e.g. "b" in "/a/b")
					resultList.add(lastTargetSeg);
				}
			}
		}

		return this.selectBestRelativizedPath(rootedResult, resultList, maxCost);
	}

	private IRIPath selectBestRelativizedPath(boolean rootedResult, LinkedList<String> resultList, int maxCost) {
		if (rootedResult) {
			System.out.println("added empty for root");
			resultList.addFirst(EMPTY_SEGMENT);
		}
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
		System.out.println(resultList.size());
		return new IRIPath(resultList);
	}

	private static LinkedList<String> buildWithErasingPart(int commonLength, int baseLength) {
		int baseRestLength = baseLength - commonLength;
		LinkedList<String> resultList = new LinkedList<>();
		for (int i = 0; i < baseRestLength - 1; i++) {
			resultList.add(DOUBLEDOT_SEGMENT);
		}
		return resultList;
	}

	private static int computeCommonLength(List<String> a, List<String> b) {
		int len = Math.min(a.size(), b.size());
		int i = 0;
		while (i < len && a.get(i).equals(b.get(i))) {
			i++;
		}
		return i;
	}



	// =================================================================================================================
	// NORMALISATION
	// =================================================================================================================

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

	private static void removeDottedSegment(ListIterator<String> iterator) {
		if (iterator.hasNext()) {
			iterator.remove();
		} else {
			iterator.set(EMPTY_SEGMENT);
			iterator.previous();
		}
	}

	private static void erasePreviousSegment(ListIterator<String> iterator) {
		if (iterator.hasPrevious()) {
			iterator.previous();
			if (!iterator.next().equals(EMPTY_SEGMENT) || iterator.previousIndex() != 0) {
				iterator.remove();
			}
		}
	}



	public static void main(String[] args) {

		IRIPath path = new IRIPath(List.of("a", "b", "c", "..", ""));
		System.out.println(path.recompose());
		path.removeDotSegments();
		System.out.println(path.recompose());
	}

}
