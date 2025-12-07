package fr.inria.jfbaget.irirefs.parser;

import fr.inria.jfbaget.nanoparse.IMatch;

import java.util.List;

/**
 * Validator for individual IRI components and paths, built on top of
 * {@link IRIRefParser}.
 * <p>
 * This class exposes small, component-level validation methods such as
 * {@link #validateScheme(String)}, {@link #validateHost(String)} or
 * {@link #validatePath(boolean, java.util.List, boolean, boolean)}. Each method
 * checks that a given string (or list of segments) conforms to the
 * corresponding RFC&nbsp;3986 / RFC&nbsp;3987 production, using the grammar
 * rules defined in {@link IRIRefParser}.
 * </p>
 *
 * <p>
 * It is primarily intended for internal use and for future public
 * component-based constructors (e.g. {@code new IRIRef(scheme, host, path, ...)}
 * where each part can be validated independently). Library users who only need
 * to parse complete IRI references should use {@code IRIRef} instead.
 * </p>
 */
public class IRIRefValidator extends IRIRefParser {

    private boolean validate (String item, String readerName) {
        if (item == null) return true;
        IMatch match = this.read(item, 0, readerName);
        return match.success() && match.end() == item.length();
    }

    /**
     * Returns {@code true} if the given scheme is syntactically valid
     * according to the {@link IRIRefParser#SCHEME} rule.
     * <p>
     * A {@code null} value is considered valid.
     * </p>
     */
    public boolean validateScheme(String scheme){
       return this.validate(scheme, SCHEME);
    }

    /**
     * Returns {@code true} if the given userinfo is syntactically valid
     * according to the {@link IRIRefParser#USER} rule.
     * <p>
     * A {@code null} value is considered valid.
     * </p>
     */
    public boolean validateUser(String user){
        return this.validate(user, USER);
    }

    /**
     * Returns {@code true} if the given host is syntactically valid
     * according to the {@link IRIRefParser#HOST} rule.
     * <p>
     * A {@code null} value is considered valid.
     * </p>
     */
    public boolean validateHost(String host){
        return this.validate(host, HOST);
    }

    /**
     * Returns {@code true} if the given port number is either
     * {@code null} or in the valid range {@code [0, 65535]}.
     */
    public boolean validatePort(Integer port){
        if (port == null) return true;
        return port >= 0 && port <= 65535;
    }

    /**
     * Returns {@code true} if the given query is syntactically valid
     * according to the {@link IRIRefParser#QUERY} rule.
     * <p>
     * A {@code null} value is considered valid.
     * </p>
     */
    public boolean validateQuery(String query){
        return this.validate(query, QUERY);
    }

    /**
     * Returns {@code true} if the given fragment is syntactically valid
     * according to the {@link IRIRefParser#FRAGMENT} rule.
     * <p>
     * A {@code null} value is considered valid.
     * </p>
     */
    public boolean validateFragment(String fragment){
        return this.validate(fragment, FRAGMENT);
    }

    /**
     * Validates a path represented as a list of segments, given its
     * rootedness and the presence of a scheme and/or authority.
     * <p>
     * The first segment is checked against the appropriate non-terminal
     * ({@code isegment_nz} or {@code isegment_nz_nc}) and the remaining
     * segments against {@code isegment}, following RFC&nbsp;3986 / 3987
     * constraints for different IRI forms.
     * </p>
     *
     * @param rooted        whether the textual path starts with {@code "/"}
     * @param path          canonical segment list (may be empty)
     * @param hasScheme     {@code true} if the overall IRI has a scheme
     * @param hasAuthority  {@code true} if the overall IRI has an authority
     * @return {@code true} if the segment list is syntactically valid
     */
    public boolean validatePath(boolean rooted, List<String> path, boolean hasScheme, boolean hasAuthority){
        if (path.isEmpty()) return true;
        if (hasAuthority) {
            return rooted && validateSegments(path);
        }
        if (rooted || hasScheme) {
            return this.validate(path.get(0), "isegment_nz")
                    && validateSegments(path.subList(1, path.size()));
        }
        return this.validate(path.get(0), "isegment_nz_nc")
                    && validateSegments(path.subList(1, path.size()));
    }

    /**
     * Validates a textual path as a whole, given the presence of a scheme
     * and/or authority.
     * <p>
     * The path is checked against one of the grammar rules:
     * {@code ipath_abempty}, {@code ihier_part} or {@code irelative_part},
     * depending on {@code hasScheme} and {@code hasAuthority}.
     * </p>
     *
     * @param path          textual path (may be empty)
     * @param hasScheme     {@code true} if the overall IRI has a scheme
     * @param hasAuthority  {@code true} if the overall IRI has an authority
     * @return {@code true} if {@code path} is syntactically valid
     */
    public boolean validatePath(String path, boolean hasScheme, boolean hasAuthority){
        if (hasAuthority)  {
            return this.validate(path, "ipath_abempty");
        }
        if (hasScheme) {
            return this.validate(path, "ihier_part");
        }
        return this.validate(path, "irelative_part");
    }

    private boolean validateSegments(List<String> segments){
        for (String segment : segments) {
            if (! this.validate(segment, "isegment"))  return false;
        }
        return true;
    }



}
