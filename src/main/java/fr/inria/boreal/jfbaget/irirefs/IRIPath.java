package fr.inria.boreal.jfbaget.irirefs;


import java.util.AbstractSequentialList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import fr.inria.boreal.jfbaget.nanoparse.IMatch;
import fr.inria.boreal.jfbaget.nanoparse.matches.ListMatch;
import fr.inria.boreal.jfbaget.nanoparse.matches.StringMatch;

public final class IRIPath extends AbstractSequentialList<String> {
	
	private static final String EMPTY_SEGMENT = "";
	private static final String DOT_SEGMENT = ".";
	private static final String DOUBLEDOT_SEGMENT = "..";
	private static final String SEGMENT_SEPARATOR = "/";
	
    private final LinkedList<String> segments;
    
    private static final boolean DEBUG_DOT_SEGMENTS = false;

    public IRIPath(List<String> initialSegments) {
        this.segments = new LinkedList<>(initialSegments);
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
    	return this.segments.isEmpty() || (this.segments.size() == 1 && this.segments.getFirst().equals(EMPTY_SEGMENT));
    }
    
    public List<String> getSegments() {
    	return Collections.unmodifiableList(this.segments);	
    }
    
    public String recompose() {
    	return String.join(SEGMENT_SEPARATOR, this.segments);
    }
    
    public StringBuilder recompose(StringBuilder builder) {
    	ListIterator<String> iterator = this.segments.listIterator();
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
    			this.add("");
				this.initializeFromSegmentListMatch((List<StringMatch>)match.result());
				break;
    		}
    		case "_opt_ipath_absolute" :{
    			this.add("");
    			this.add("");
    			break;
    		}
    		case "_seq_ipath_absolute" : {
    			this.add("");
    		}
    		case "ipath_rootless" :
    		case "ipath_noscheme" : {
    			this.add((String)((ListMatch)match).result().get(0).result());
    			this.initializeFromSegmentListMatch((List<StringMatch>)((ListMatch)match).result().get(1).result());
    			break;
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
		this.addAll(other);
    }
    
    // with otherHasAuthority = other.hasAuthority()
    
    protected void resolveNonEmptyPath(IRIPath other, boolean otherHasAuthority) {
    	if (!this.getFirst().equals(EMPTY_SEGMENT)) {
			if (otherHasAuthority && other.isEmpty()) {
				this.addFirst(EMPTY_SEGMENT);
			} else if (! other.segments.isEmpty()){
				 //return a string consisting of the reference's path component
			     // appended to all but the last segment of the base URI's path (i.e.,
			     // excluding any characters after the right-most "/" in the base URI
			     // path, or excluding the entire base URI path if it does not contain
			     //any "/" characters).
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
}
