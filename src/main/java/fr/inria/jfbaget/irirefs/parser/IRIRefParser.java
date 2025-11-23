package fr.inria.jfbaget.irirefs.parser;

import fr.inria.jfbaget.irirefs.exceptions.IRIParseException;

public interface IRIRefParser {

    record Result(
            String scheme,
            String user,
            String host,
            Integer port,
            String query,
            String fragment,
            java.util.List<String> segments
    ) {}

    Result parse(String iri) throws IRIParseException;
}
