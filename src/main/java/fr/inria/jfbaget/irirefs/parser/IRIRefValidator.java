package fr.inria.jfbaget.irirefs.parser;

import fr.inria.jfbaget.nanoparse.IMatch;

import java.util.List;

public class IRIRefValidator extends IRIRefParser {

    private boolean validate (String item, String readerName) {
        if (item == null) return true;
        IMatch match = this.read(item, 0, readerName);
        return match.success() && match.end() == item.length();
    }

    public boolean validateScheme(String scheme){
       return this.validate(scheme, SCHEME);
    }

    public boolean validateUser(String user){
        return this.validate(user, USER);
    }

    public boolean validateHost(String host){
        return this.validate(host, HOST);
    }

    public boolean validatePort(Integer port){
        if (port == null) return true;
        return port >= 0 && port <= 65535;
    }

    public boolean validateQuery(String query){
        return this.validate(query, QUERY);
    }

    public boolean validateFragment(String fragment){
        return this.validate(fragment, FRAGMENT);
    }

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
