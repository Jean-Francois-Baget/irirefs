package fr.inria.jfbaget.irirefs;


import java.util.*;

import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;
import fr.inria.jfbaget.nanoparse.matches.StringMatch;

public final class IRIPath extends AbstractSequentialList<String> {
	
	private static final String EMPTY_SEGMENT = "";
	private static final String DOT_SEGMENT = ".";
	private static final String DOUBLEDOT_SEGMENT = "..";
	private static final String SEGMENT_SEPARATOR = "/";
	
	boolean rooted;
    final LinkedList<String> segments;
    
    private static final boolean DEBUG_DOT_SEGMENTS = false;

    public IRIPath(boolean rooted, List<String> initialSegments) {
    	this.rooted = rooted;
        this.segments = new LinkedList<>(initialSegments);
    }
    
    public boolean isRooted() {
    	return this.rooted;
    }
    
    void setRooted(boolean rooted) {
    	this.rooted = rooted;
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        return segments.listIterator(index);
    }

    @Override
    public int size() {
        return segments.size();
    }
    
    @Override
    public boolean isEmpty() {
    	//return this.segments.isEmpty() || (this.segments.size() == 1 && this.segments.getFirst().equals(EMPTY_SEGMENT));
    	return this.segments.isEmpty();
    }
    
    public List<String> getSegments() {
    	return Collections.unmodifiableList(this.segments);	
    }
    
    public String recompose() {
    	if (this.rooted) {
    		return SEGMENT_SEPARATOR + String.join(SEGMENT_SEPARATOR, this.segments);
    	} else {
    		return  String.join(SEGMENT_SEPARATOR, this.segments);
    	}
    }
    
    public StringBuilder recompose(StringBuilder builder) {
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
    
    /**
     * Returns the first segment in this path.
     * 
     * @return the first segment (head) of the path
     * @throws NoSuchElementException if the path is empty
     */
    public String getFirst() {
        return segments.getFirst();
    }


    /**
     * Inserts the specified segment at the beginning of the path.
     *
     * <p>This is functionally equivalent to {@code segments.addFirst(segment)} and
     * allows the path to behave like a deque or a rooted IRI path starting with a new segment.</p>
     *
     * @param segment the segment to add at the head of the path
     * @throws NullPointerException if the segment is {@code null}
     */
    public void addFirst(String segment) {
        segments.addFirst(segment);
    }

    
    protected void addFromMatch(IMatch match) throws IllegalArgumentException{
    	String pathType = match.reader().getName();
    	switch (pathType) {
    		case "ipath_abempty" : {
    			List<StringMatch> args = (List<StringMatch>)match.result();
				if (args == null){
					this.initializeFromSegmentListMatch(new ArrayList<>());
					break;
				}
    			if (!args.isEmpty()) {
    				this.rooted = true;
    			}
				this.initializeFromSegmentListMatch(args);
				break;
    		}
    		case "_opt_ipath_absolute" :{
    			this.rooted = true;
    			break;
    		}
    		case "_seq_ipath_absolute" : {
    			this.rooted = true;
    		}
    		case "ipath_rootless" :
    		case "ipath_noscheme" : {
    			this.add((String)((ListMatch)match).result().get(0).result());
				List<StringMatch> rest = (List<StringMatch>)((ListMatch)match).result().get(1).result();
				if (rest != null) {
    				this.initializeFromSegmentListMatch(rest);
    				break;
				}
    		}
    		case "ipath_empty" : {
    			break;
    		}
    		default:
    			throw new IllegalArgumentException("No such rule for an IRI path, given " + pathType);
    	}
    }
    
    private void initializeFromSegmentListMatch(List<StringMatch> segmentsMatch) {
		for (StringMatch segmatch : segmentsMatch) {
			this.add((String)segmatch.result());
		}
	}
    
    protected void resolveEmptyPath(IRIPath other) {
    	this.clear();
    	this.rooted = other.rooted;
		this.addAll(other);
    }
    
    // with otherHasAuthority = other.hasAuthority()
    
    protected void resolveNonEmptyPath(IRIPath other, boolean otherHasAuthority) {
    	//if (!this.getFirst().equals(EMPTY_SEGMENT)) {
    	if (!this.rooted) {	
			if (otherHasAuthority && other.isEmpty()) {
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
				ListIterator<String> it = other.listIterator(other.size() - 1); 
				while (it.hasPrevious()) {
				    this.addFirst(it.previous());
				}
				
			}
		}
    	
    }
    
    protected void removeDotSegments() {
    	if (DEBUG_DOT_SEGMENTS) System.out.printf("Removing dot segments on path \"%s\".\n", this.recompose());
    	ListIterator<String> iterator = this.listIterator();
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
    
    
    
    
    protected void removeDotSegments2() {
    	if (DEBUG_DOT_SEGMENTS) System.out.printf("Removing dot segments on path \"%s\".\n", this.recompose());
		ListIterator<String> iterator = this.listIterator();
		String current;
		while (iterator.hasNext()) {
			current = iterator.next();
			if (DEBUG_DOT_SEGMENTS) System.out.printf("Examining \"%s\"\n", current);
			if (current.equals(DOT_SEGMENT)) {
				if (!iterator.hasPrevious() || iterator.hasNext()) {
					if (DEBUG_DOT_SEGMENTS) System.out.println("     Either it has no previous, or it has a next: we remove it.");
					iterator.remove();
				} else {
					if (DEBUG_DOT_SEGMENTS) System.out.println("     It has a previous, but no next: we set it to empty.");
					iterator.set(EMPTY_SEGMENT);
				}
				if (DEBUG_DOT_SEGMENTS) System.out.printf("     We obtain \"%s\".\n", this.recompose());
			} else if (current.equals(DOUBLEDOT_SEGMENT)) { 
				if (!iterator.hasPrevious()) { 
					if (DEBUG_DOT_SEGMENTS) System.out.println("     It has no previous, so we just remove it.");
					iterator.remove();
				} else { 
					if (iterator.hasNext()) {
						if (DEBUG_DOT_SEGMENTS) System.out.println("     It has a previous and a next, so we remove it.");
						iterator.remove();
						current = iterator.previous();
						if (DEBUG_DOT_SEGMENTS) System.out.printf("     We should now remove the previous segment \"%s\".\n", current);
						if (!current.equals(EMPTY_SEGMENT) || iterator.hasPrevious()) {
							iterator.remove();
						} else if (DEBUG_DOT_SEGMENTS) {
							System.out.println("     But we don't do it because we are in the first segment after the leading \\");
						}
					}
					else {
						if (DEBUG_DOT_SEGMENTS) System.out.println("     It has a previous but no next next, so we remove it and preserve the trailing \\.");
						iterator.set(EMPTY_SEGMENT);
						iterator.previous();
						current = iterator.previous();
						if (DEBUG_DOT_SEGMENTS) System.out.printf("     We should now remove the previous segment \"%s\".\n", current);
						if (!current.equals(EMPTY_SEGMENT) || iterator.hasPrevious()) {
							iterator.remove();
						} else if (DEBUG_DOT_SEGMENTS) {
							System.out.println("     But we don't do it because we are in the first segment after the leading \\");
						}
					}
				}
				if (DEBUG_DOT_SEGMENTS) System.out.printf("     We obtain \"%s\".\n", this.recompose());
			}
		}
	}

	public IRIPath relativize(IRIPath basePath, boolean mustFindPath, int maxCost) {

		if (this.isRooted() && ! basePath.isRooted())
			return this;

		if (! this.isRooted() && basePath.isRooted())
			return null;

		int commonLength = computeCommonLength(this.segments, basePath.segments);

		int baseRestLength = basePath.segments.size() - commonLength;
		boolean rooted = false;
		LinkedList<String> resultList = new LinkedList<>();

		if (commonLength == basePath.segments.size()) {
			// Les chemins sont identiques

			if (commonLength == 0) {
				// Les deux paths sont vides
				// On ne fabrique rien ici : resultList reste vide.
				// Le cas mustFindPath=true sera géré plus bas (ton bloc avec ".").
				rooted = this.isRooted();
			} else {
				// Chemins identiques et non vides
				if (!mustFindPath) {
					// Meilleure solution : chemin relatif vide
					// => on laisse resultList vide, rooted reste false
					// (un path vide relatif hérite du path de la base).
				} else {
					// Il FAUT un path non vide : on garde ton fallback "b"
					resultList.add(basePath.segments.get(commonLength - 1));
				}
			}
		}
		else if (this.segments.size() == commonLength) {
			// "this" est un préfixe strict de basePath
			if (commonLength == 0) {
				// cas rare: this = "" et base non vide, ex: base = "a/b/c", this = ""
				// un relatif correct est simplement ".." répété baseRestLength fois
				for (int i = 0; i < baseRestLength; i++) {
					resultList.add("..");
				}
			} else {
				// this non vide: ancêtre strict classique
				// ex: base = a/b/c/d/e/f, this = a/b/c  → ../../../c
				// ex: base = a/b/c,       this = a/b    → ../b
				for (int i = 0; i < baseRestLength; i++) {
					resultList.add("..");
				}
				resultList.add(this.segments.get(commonLength - 1));
			}
		}
		else {
			// cas général
			for (int i = 0; i < baseRestLength - 1; i++) {
				resultList.add("..");
			}
		}

		for (int i = commonLength; i < this.segments.size(); i++) {
			resultList.add(this.segments.get(i));
		}

		if (resultList.isEmpty() && ! rooted && mustFindPath) {
			resultList.add(".");
		}


		return new IRIPath(rooted, resultList);
 	}

	private static int computeCommonLength(List<String> a, List<String> b) {
		int len = Math.min(a.size(), b.size());
		int i = 0;
		while (i < len && a.get(i).equals(b.get(i))) {
			i++;
		}
		return i;
	}

	public IRIPath relativize2(IRIPath basePath, boolean mustFindPath, int maxCost) {


		// 1) Rooted / non-rooted incompatibilities at path level
		if (this.isRooted() && !basePath.isRooted())
			return this;      // best we can do is this path itself

		if (!this.isRooted() && basePath.isRooted())
			return null;      // impossible to get a non-rooted result

		List<String> baseSeg = basePath.segments;
		List<String> thisSeg = this.segments;

		int baseLen = baseSeg.size();
		int thisLen = thisSeg.size();

		// 2) Determine "directory" of base (Bdir)
		// If base ends with "", it's a directory (e.g. "a/b/c/").
		// Otherwise it's a document (e.g. "a/b/c") and Bdir excludes the last segment.
		boolean baseEndsWithEmpty =
				(baseLen > 0 && baseSeg.get(baseLen - 1).isEmpty());
		int baseDirLen = baseEndsWithEmpty ? baseLen : Math.max(0, baseLen - 1);

		// 3) Common prefix on FULL paths (for the "same path" case)
		int fullCommonLen = commonPrefixLength(baseSeg, thisSeg,
				Math.min(baseLen, thisLen));
		boolean samePath = (fullCommonLen == baseLen && baseLen == thisLen);

		// 4) Common prefix between Bdir and target (for the general case)
		int commonDirLen = commonPrefixLength(baseSeg, thisSeg, baseDirLen);
		int baseDirRestLen = baseDirLen - commonDirLen;

		// 5) Target equals Bdir ?
		boolean targetIsBaseDir =
				(thisLen == baseDirLen && commonDirLen == baseDirLen);

		boolean rooted = false;   // result path is never rooted in our relativization
		LinkedList<String> resultList = new LinkedList<>();

		// ---------- Case 1: same full path ----------
		if (samePath) {
			if (baseLen == 0) {
				// both paths empty
				rooted = this.isRooted();  // probably false
				// resultList remains empty; mustFindPath handled below
			} else {
				if (!mustFindPath) {
					// Best is empty path: ""
					// resultList stays empty
				} else {
					// Need a non-empty path that resolves to the same path
					// Simple choice: last segment of this
					resultList.add(thisSeg.get(thisLen - 1));
				}
			}
		}
		// ---------- Case 2: target is exactly the base directory ----------
		else if (targetIsBaseDir) {
			if (baseDirLen == 0) {
				// base has no directory (e.g. "a", "" ...)
				// Strictly speaking, this is rare/awkward. Let's fall back:
				// We can't use empty path here (it would resolve to base, not target),
				// so approximate with "." and let resolve() handle it.
				resultList.add(".");
			} else {
				// General pattern: "../last(Bdir)"
				// Example: base = "a/b/c", Bdir = "a/b", target = "a/b" → "../b"
				// Example: base = "a/b/c/d/e/f", Bdir = "a/b/c/d/e", target = "a/b/c/d/e"
				//          → "../e"
				resultList.add("..");
				String lastDirSeg = baseSeg.get(baseDirLen - 1);
				resultList.add(lastDirSeg);
			}
		}
		// ---------- Case 3: general case ----------
		else {
			// ".." for each extra segment in Bdir after the common prefix
			for (int i = 0; i < baseDirRestLen; i++) {
				resultList.add("..");
			}
			// then the tail of target after the common prefix
			for (int i = commonDirLen; i < thisLen; i++) {
				resultList.add(thisSeg.get(i));
			}
		}

		// ---------- Final mustFindPath corner-case ----------
		if (resultList.isEmpty() && !rooted && mustFindPath) {
			// Only possible when both paths are empty & unrooted.
			// A correct non-empty relative is ".".
			resultList.add(".");
		}

		return new IRIPath(rooted, resultList);
	}

	private static int commonPrefixLength(List<String> baseSeg, List<String> thisSeg, int maxBase) {
		int max = Math.min(maxBase, thisSeg.size());
		int i = 0;
		while (i < max && baseSeg.get(i).equals(thisSeg.get(i))) {
			i++;
		}
		return i;
	}

	public IRIPath relativize3(IRIPath basePath, boolean mustFindPath, int maxCost) {


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

		// Handling the special case when we found an empty result, but we are not allowed to
		if (resultList.isEmpty() && !rootedResult && mustFindPath) {
			if (this.segments.isEmpty()) {
				resultList.add(".");
			} else {
				resultList.add(basePath.segments.getLast());
			}
		}


		return new IRIPath(rootedResult, resultList);

	}

}
