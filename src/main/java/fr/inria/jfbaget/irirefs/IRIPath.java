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
	
	private boolean rooted;
    private final LinkedList<String> segments;
    
    private static final boolean DEBUG_DOT_SEGMENTS = false;


	// =================================================================================================================
	// CONSTRUCTORS
	// =================================================================================================================

	IRIPath(IMatch pathMatch) {
		this.rooted = false;
		this.segments = new LinkedList<>();
		switch (pathMatch.reader().getName()) {
			case "ipath_abempty" : {
				List<StringMatch> segMatches = (List<StringMatch>)pathMatch.result();
				if (segMatches != null){
					this.rooted = true;
					for (StringMatch segmatch : segMatches) {
						this.segments.add((String)segmatch.result());
					}
				}
				break;
			}
			case "_opt_ipath_absolute": {
				this.rooted = true;
				break;
			}
			case "_seq_ipath_absolute" : {
				this.rooted = true;
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
	}

    IRIPath(boolean rooted, List<String> initialSegments) {
    	this.rooted = rooted;
		if (initialSegments.size() == 1 && initialSegments.get(0).equals(EMPTY_SEGMENT)) {
			initialSegments.remove(0);
			System.out.println("REDONDANCE");
		}
        this.segments = new LinkedList<>(initialSegments);
    }

	IRIPath(IRIPath other) {
		this.rooted = other.rooted;
		this.segments = new LinkedList<>(other.segments);
	}

	// =================================================================================================================
	// UTILS
	// =================================================================================================================


	// =================================================================================================================
	// GETTERS
	// =================================================================================================================


    boolean isRooted() {
    	return this.rooted;
    }

	boolean isEmpty() {
		return this.segments.isEmpty();
	}

    List<String> getSegments() {
    	return Collections.unmodifiableList(this.segments);	
    }

	// =================================================================================================================
	// RECOMPOSITION
	// =================================================================================================================
    
    String recompose() {
    	if (this.rooted) {
    		return SEGMENT_SEPARATOR + String.join(SEGMENT_SEPARATOR, this.segments);
    	} else {
    		return  String.join(SEGMENT_SEPARATOR, this.segments);
    	}
    }
    
    StringBuilder recompose(StringBuilder builder) {
    	ListIterator<String> iterator = this.segments.listIterator();
    	if (this.rooted) { 
    		builder.append(SEGMENT_SEPARATOR);
    	}
    	if (iterator.hasNext()) {
    		builder.append(iterator.next());
    		while (iterator.hasNext()) {
    			builder.append(SEGMENT_SEPARATOR).append(iterator.next());
    		}
    	}
    	return builder;
    }

	 int recompositionLength() {
		return recompositionLength(rooted, segments);
	 }

	private static int recompositionLength(boolean rooted, List<String> segments) {
		if (segments.isEmpty()) {
			return rooted ? 1 : 0; // "/" or ""
		}
		int len = rooted ? 1 : 0; // leading "/"
		for (String seg : segments) {
			len += seg.length();
		}
		// separators between segments
		len += Math.max(0, segments.size() - 1);
		return len;
	}

	// =================================================================================================================
	// RESOLUTION
	// =================================================================================================================


    
    void resolveEmptyPath(IRIPath other) {
    	this.segments.clear();
    	this.rooted = other.rooted;
		this.segments.addAll(other.segments);
    }


    
    void resolveNonEmptyPath(IRIPath other, boolean otherHasAuthority) {
    	//if (!this.getFirst().equals(EMPTY_SEGMENT)) {
    	if (!this.rooted) {	
			if (otherHasAuthority && other.segments.isEmpty()) {
			//if (otherHasAuthority) {
				//this.addFirst(EMPTY_SEGMENT);
				this.rooted = true;
			} else if (! other.segments.isEmpty()){
				 //return a string consisting of the reference's path component
			     // appended to all but the last segment of the base URI's path (i.e.,
			     // excluding any characters after the right-most "/" in the base URI
			     // path, or excluding the entire base URI path if it does not contain
			     //any "/" characters).
				this.rooted = other.rooted;
				ListIterator<String> it = other.segments.listIterator(other.segments.size() - 1);
				while (it.hasPrevious()) {
				    this.segments.addFirst(it.previous());
				}
				
			}
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

		// Now here are sure that they are both rootedResult or both unrooted

		int commonLength = computeCommonLength(this.segments, basePath.segments);
		int baseRestLength = basePath.segments.size() - commonLength;
		boolean rootedResult = false;

		// Building the ERASINGPART: enough ".." to erase non common suffix of base
		LinkedList<String> resultList = new LinkedList<>();
		for (int i = 0; i < baseRestLength - 1; i++) {
			resultList.add("..");
		}

		// Handling the special cases
		if (basePath.segments.size() == commonLength && this.segments.size() > commonLength) {
			if (commonLength > 0) {
				resultList.add(basePath.segments.getLast());
			} else {
				rootedResult = this.isRooted();
			}
		} else if (this.segments.size() == commonLength && basePath.segments.size() > commonLength) {
			if (commonLength > 0) {
				resultList.add("..");
				resultList.add(basePath.segments.get(commonLength - 1));
			} else{
				resultList.add("..");
			}
		}

		// Adding the REMAININGPART: segments of this that are not in common
		for (int i = commonLength; i < this.segments.size(); i++) {
			resultList.add(this.segments.get(i));
		}


		// Fix for the base: /foo  this: /  mistake
		boolean effectivelyEmpty =
				resultList.isEmpty()
						|| (resultList.size() == 1 && resultList.getFirst().equals(EMPTY_SEGMENT));

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
		int newLen = recompositionLength(rootedResult, resultList);
		int oldLen = this.recompositionLength();
		if (this.rooted) {
			System.out.println("A");
			System.out.println(oldLen);
			System.out.println(newLen);
			if (oldLen <= newLen) {
				return new IRIPath(this);
			}
		} else {
			if (oldLen + maxCost <= newLen) {
				return null;
			}
		}
		return new IRIPath(rootedResult, resultList);
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

	protected void removeDotSegments() {
		if (DEBUG_DOT_SEGMENTS) System.out.printf("Removing dot segments on path \"%s\".\n", this.recompose());
		ListIterator<String> iterator = this.segments.listIterator();
		String current;
		boolean currentRoot = this.rooted;
		while(iterator.hasNext()) {
			current = iterator.next();
			if (DEBUG_DOT_SEGMENTS) System.out.printf("Examining \"%s\" in %s list %s\n",
					current, this.rooted ? "rooted" : "unrooted", this.segments);
			if (current.equals(DOT_SEGMENT)) {
				if (currentRoot) {
					if (iterator.hasNext()) {
						iterator.remove();
					} else {
						iterator.set(EMPTY_SEGMENT);
					}
				} else {
					iterator.remove();
					currentRoot = true;
				}
			} else if (current.equals(DOUBLEDOT_SEGMENT)) {
				if (currentRoot) {
					if (iterator.hasNext()) {
						iterator.remove();
					} else {
						iterator.set(EMPTY_SEGMENT);
						iterator.previous();
					}
					if (iterator.hasPrevious()) {
						iterator.previous();
						iterator.remove();
					}

				} else {

					iterator.remove();
					currentRoot = true;
				}



			} else if (current.equals(EMPTY_SEGMENT) && !this.rooted && iterator.previousIndex() == 0)  {
				if (DEBUG_DOT_SEGMENTS) System.out.printf("Rare case when the unrooted list is [\"\", ...]\n");
				this.rooted = true;
				iterator.remove();
			} else {
				currentRoot = true;
			}

		}
	}


}
