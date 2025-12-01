package fr.inria.jfbaget.irirefs.manager.normalizer;

import fr.inria.jfbaget.irirefs.IRIRef;

public class Normalizer {

    public enum NORMALIZATION {
        /**
         * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.1">RFC 3987, Simple String Comparison</a>
         */
        STRING,
        /**
         * <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2">RFC 3987, Syntax-Based Normalization</a>
         * <p>
         * Regroups {@link #CASE}, {@link #CHARACTER}, {@link #PCT}
         * <p>
         * Though technically in Syntax-based Normalization, Path Segment Normalization {@link #PATH}
         * will not be called by {@link #SYNTAX}, since it should have already been done by a call to {@link IRIRef#resolve(IRIRef)}.
         * Path Segment Normalization should then be called explicitely if required.
         */
        SYNTAX,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.1">RFC 3987, Case Normalization</a>
         */
        CASE,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.2">RFC 3987, Character Normalization</a>
         */
        CHARACTER,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.3">RFC 3987, Percent-Encoding Normalization</a>
         */
        PCT,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.2.4">RFC 3987, Path Segment Normalization</a>
         */
        PATH,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.3">RFC 3987, Scheme-Based Normalization</a>
         */
        SCHEME,
        /**
         *  <a href="https://datatracker.ietf.org/doc/html/rfc3987#section-5.3.4">RFC 3987, Protocol-Based Normalization</a>
         */
        PROTOCOL
    }




}
