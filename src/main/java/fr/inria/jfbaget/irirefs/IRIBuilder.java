package fr.inria.jfbaget.irirefs;

import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;
import fr.inria.jfbaget.irirefs.parser.IRIParser;
import fr.inria.jfbaget.nanoparse.IMatch;
import fr.inria.jfbaget.nanoparse.matches.ListMatch;
import fr.inria.jfbaget.nanoparse.matches.StringMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRIBuilder extends IRIParser {


    public static enum IRITYPE {
        ANY(IRIParser.IRIREF),
        IRI(IRIParser.IRI),
        REL(IRIParser.RELATIVE),
        ABS(IRIParser.ABSOLUTE);

        private final String ruleName;

        IRITYPE(String ruleName) {
            this.ruleName = ruleName;
        }

        private String getRuleName() {
            return this.ruleName;
        }

        private static final Map<String, IRIBuilder.IRITYPE> LOOKUP = new HashMap<>();

        static {
            for (IRIBuilder.IRITYPE t : IRIBuilder.IRITYPE.values()) {
                LOOKUP.put(t.getRuleName(), t);
            }
        }
    }

    ;


    public void parse(String iriString, IRIBuilder.IRITYPE type, IRIRef iriref) throws IRIParseException, IllegalArgumentException {
        IMatch iriMatch = this.read(iriString, 0, type.getRuleName());
        if (iriMatch.success() && iriMatch.end() == iriString.length()) {
            this.initializeFromMatch((ListMatch) iriMatch, iriref);
        } else {
            throw new IRIParseException(String.format(
                    "The string \"%s\" does not represent a valid %s, stopped parsing at position %d.",
                    iriString, type.getRuleName(), iriMatch.end())
            );
        }
    }

    private void initializeFromMatch(ListMatch match, IRIRef iriref) throws IllegalArgumentException {
        String iritype = match.reader().getName();

        switch (iritype) {
            case "IRI": {
                iriref.fragment = (String) match.result().get(3).result();
            }
            case "absolute_IRI": {
                iriref.scheme = (String) match.result().get(0).result();
                iriref.query = (String) match.result().get(2).result();
                this.initializeFromAuthorityPathMatch(match.result().get(1), iriref);
                break;
            }
            case "irelative_ref": {
                iriref.query = (String) match.result().get(1).result();
                iriref.fragment = (String) match.result().get(2).result();
                this.initializeFromAuthorityPathMatch(match.result().get(0), iriref);
                break;
            }
            default: {
                throw new IllegalArgumentException(String.format(
                        "Unsupported IRI type: \"%s\". Expected one of: IRI, absolute_IRI, irelative_ref.",
                        iritype
                ));
            }
        }
    }

    private void initializeFromAuthorityPathMatch(IMatch match, IRIRef iriref) throws IllegalArgumentException {
        String iritype = match.reader().getName();
        switch (iritype) {
            case "_seq_ihier_part": {
                iriref.authority = new IRIAuthority(null, null, null);
                initializeFromAuthorityMatch((ListMatch) ((ListMatch) match).result().get(0), iriref.authority);
                this.initializeFromPathMatch(((ListMatch) match).result().get(1), iriref.path);
                break;
            }
            default: {
                this.initializeFromPathMatch(match, iriref.path);
            }
        }
    }

    private void initializeFromAuthorityMatch(ListMatch authorityMatch, IRIAuthority authority) {
        authority.user = (String) authorityMatch.result().get(0).result();
        authority.host = (String) authorityMatch.result().get(1).result();
        String hostkind = authorityMatch.result().get(1).reader().getName();
        if (hostkind.equals("IPv6address") || hostkind.equals("IPvFuture")) {
            authority.host = "[" + authority.host + "]";
        }
        authority.port = (Integer) authorityMatch.result().get(2).result();
    }

    private void initializeFromPathMatch(IMatch match, IRIPath path) throws IllegalArgumentException{
        String pathType = match.reader().getName();
        switch (pathType) {
            case "ipath_abempty" : {
                List<StringMatch> args = (List<StringMatch>)match.result();
                if (args == null){
                    this.initializeFromSegmentListMatch(new ArrayList<>(), path);
                    break;
                }
                if (!args.isEmpty()) {
                    path.rooted = true;
                }
                this.initializeFromSegmentListMatch(args, path);
                break;
            }
            case "_opt_ipath_absolute" :{
                path.rooted = true;
                break;
            }
            case "_seq_ipath_absolute" : {
                path.rooted = true;
            }
            case "ipath_rootless" :
            case "ipath_noscheme" : {
                path.add((String)((ListMatch)match).result().get(0).result());
                List<StringMatch> rest = (List<StringMatch>)((ListMatch)match).result().get(1).result();
                if (rest != null) {
                    this.initializeFromSegmentListMatch(rest, path);
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

    private void initializeFromSegmentListMatch(List<StringMatch> segmentsMatch, IRIPath path) {
        for (StringMatch segmatch : segmentsMatch) {
            path.add((String)segmatch.result());
        }
    }

}