package fr.inria.jfbaget.irirefs.parser;


import java.util.List;

import fr.inria.jfbaget.nanoparse.IReader;
import fr.inria.jfbaget.nanoparse.Parser;
import fr.inria.jfbaget.nanoparse.readers.ChoiceReader;
import fr.inria.jfbaget.nanoparse.readers.OptionalReader;
import fr.inria.jfbaget.nanoparse.readers.RegexReader;
import fr.inria.jfbaget.nanoparse.readers.RepetitionReader;
import fr.inria.jfbaget.nanoparse.readers.SequenceReader;
import fr.inria.jfbaget.nanoparse.readers.StringReader;

public class IRIRefParser extends Parser {
	
	private static final String REGEXP__SCHEME = "[a-zA-Z][a-zA-Z0-9+-.]*";
	
	private static final String CHARS__SUB_DELIMS = "!$&'()*+,;=";
	private static final String CHARS__GEN_DELIMS = ":/?#\\[\\]@";
	private static final String CHARS__RESERVED = CHARS__GEN_DELIMS + CHARS__SUB_DELIMS;
	private static final String CHARS__UNRESERVED = "a-zA-Z0-9-._~";
	private static final String CHARS__UCSCHAR = "\\u00A0-\\uD7FF" + "\\uF900-\\uFDCF" + "\\uFDF0-\\uFFEF" +
	                           "\\x{10000}-\\x{1FFFD}" + "\\x{20000}-\\x{2FFFD}" + "\\x{30000}-\\x{3FFFD}" +
                               "\\x{40000}-\\x{4FFFD}" + "\\x{50000}-\\x{5FFFD}" + "\\x{60000}-\\x{6FFFD}" +
                               "\\x{70000}-\\x{7FFFD}" + "\\x{80000}-\\x{8FFFD}" + "\\x{90000}-\\x{9FFFD}" +
                               "\\x{A0000}-\\x{AFFFD}" + "\\x{B0000}-\\x{BFFFD}" + "\\x{C0000}-\\x{CFFFD}" +
                               "\\x{D0000}-\\x{DFFFD}" + "\\x{E1000}-\\x{EFFFD}";
	private static final String CHARS__IUNRESERVED = CHARS__UNRESERVED + CHARS__UCSCHAR;
	private static final String REGEXP__PCT_ENCODED = "%[0-9a-fA-F]{2}"; 
	private static final String REGEXP__IPCHAR = "[" + CHARS__IUNRESERVED + CHARS__SUB_DELIMS + ":@" + "]|" 
	                                             + REGEXP__PCT_ENCODED;
	private static final String REGEXP__IFRAGMENT = "(" + REGEXP__IPCHAR + "|[/?])*";
	private static final String CHARS__IPRIVATE = "\\uE000-\\uF8FF" + "\\x{F0000}-\\x{FFFFD}" +
			                                      "\\x{100000}-\\x{10FFFD}";
	private static final String REGEXP__IQUERY = "(" + REGEXP__IPCHAR + "|[" + CHARS__IPRIVATE + "/?])*";
	
	private static final String REGEXP__IUSERINFO = "([" + CHARS__IUNRESERVED + CHARS__SUB_DELIMS + ":]|" 
	                                                 + REGEXP__PCT_ENCODED + ")*";
	private static final String REGEXP__ISEGMENT = "(" + REGEXP__IPCHAR + ")*";
	private static final String REGEXP__ISEGMENT_NZ = "(" + REGEXP__IPCHAR + ")+";
	
	private static final String REGEXP__ISEGMENT_NZ_NC = "([" + CHARS__IUNRESERVED + CHARS__SUB_DELIMS + "@]|" +
			                                             REGEXP__PCT_ENCODED + ")+";
	
	private static final String REGEXP__IREG_NAME = "([" + CHARS__IUNRESERVED + CHARS__SUB_DELIMS + "]|" +
			                                        REGEXP__PCT_ENCODED + ")*";
	
	//private static final String REGEXP__DEC_OCTET = "[0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5]";
	// ERREUR DANS LE STANDARD
	private static final String REGEXP__DEC_OCTET = "2[0-4][0-9]|25[0-5]|1[0-9][0-9]|[1-9][0-9]|[0-9]";
	
	private static final String REGEXP__IPV4ADDRESS = "(d)\\.(d)\\.(d)\\.(d)".replaceAll("d", REGEXP__DEC_OCTET);
	
	private static final String REGEXP__IPVFUTURE = "\\[(v[0-9A-Fa-f]+\\.([" + CHARS__UNRESERVED +
			                                        CHARS__SUB_DELIMS + ":])+)\\]";
	
	private static final String REGEXP__H16 = "[a-fA-F0-9]{1,4}";
	private static final String REGEXP__LS32 = REGEXP__H16 + ":" + REGEXP__H16 + "|" + REGEXP__IPV4ADDRESS ;
	private static final String PATTERN__IPV6ADDRESS = "\\[(" +
			                                           "(H:){6}(L)|::(H:){5}(L)|(H)?::(H:){4}(L)|" +
	                                                   "((H:){0,1}H)?::(H:){3}(L)|" +
	                                                   "((H:){0,2}H)?::(H:){2}(L)|" +
	                                                   "((H:){0,3}H)?::H:(L)|" +
	                                                   "((H:){0,4}H)?::(L)|" + 
	                                                   "((H:){0,5}H)?::H|" +
	                                                   "((H:){0,6}H)?::" + ")\\]";
	
	private static final String REGEXP__IPV6ADDRESS = PATTERN__IPV6ADDRESS.replaceAll("H", REGEXP__H16)
			                                                              .replaceAll("L", REGEXP__LS32);
	
	public static final String IRIREF = "IRI_Reference";
	public static final String IRI = "IRI";
	public static final String RELATIVE = "irelative_ref";
	public static final String ABSOLUTE = "absolute_IRI";

	public static final String SCHEME = "scheme";
	public static final String USER = "iuserinfo";
	public static final String HOST = "ihost";
	public static final String QUERY = "iquery";
	public static final String FRAGMENT = "ifragment";

	public static final String HIERARCHICAL = "_seq_ihier_part";
	
	
	
	private static List<IReader> readers() {
		return List.of(
			new ChoiceReader(IRIREF, List.of(IRI, RELATIVE), false),
			new SequenceReader(IRI, List.of(SCHEME, ":", "ihier_part", "_opt_iquery", "_opt_ifragment"),
					false, List.of(0, 2, 3, 4)),
			new SequenceReader(ABSOLUTE, List.of(SCHEME, ":", "ihier_part", "_opt_iquery"),
					false, List.of(0, 2, 3)),
			new SequenceReader(RELATIVE, List.of("irelative_part", "_opt_iquery", "_opt_ifragment"), false),
			new OptionalReader("_opt_iquery", "_seq_iquery", false),
			new SequenceReader("_seq_iquery", List.of("?", "iquery"), false, 1),
			new OptionalReader("_opt_ifragment", "_seq_ifragment", false),
			new SequenceReader("_seq_ifragment", List.of("#", "ifragment"), false, 1),
			new StringReader(":", ":", false),
			new StringReader("?", "?", false),
			new StringReader("#", "#", false),
			new RegexReader(SCHEME, REGEXP__SCHEME, false),
			new RegexReader(FRAGMENT, REGEXP__IFRAGMENT, false),
			new RegexReader(QUERY, REGEXP__IQUERY, false),
			new ChoiceReader("ihier_part",
					List.of(HIERARCHICAL, "ipath_absolute", "ipath_rootless", "ipath_empty"), false),
			new SequenceReader(HIERARCHICAL, List.of("//", "iauthority", "ipath_abempty"),
					false, List.of(1, 2)),
			new StringReader("ipath_empty", "", false),
			new ChoiceReader("irelative_part",
					List.of(HIERARCHICAL, "ipath_absolute", "ipath_noscheme", "ipath_empty"), false),
			new StringReader("//", "//", false),
			new SequenceReader("iauthority", List.of("_opt_iuserinfo", "ihost", "_opt_port"), false),
			new OptionalReader("_opt_iuserinfo", "_seq_iuserinfo", false),
			new SequenceReader("_seq_iuserinfo", List.of("iuserinfo", "@"), false, 0),
			new StringReader("@", "@", false),
			new OptionalReader("_opt_port", "_seq_port", false),
			new SequenceReader("_seq_port", List.of(":", "_opt_int"), false, 1), 
			new OptionalReader("_opt_int", "INT", false),
			new RegexReader("iuserinfo", REGEXP__IUSERINFO, false),
			new RepetitionReader("ipath_abempty", "_seq_ipath_abempty",
					null, 0, Integer.MAX_VALUE, false),
			new SequenceReader("_seq_ipath_abempty", List.of("/", "isegment"), false, 1),
			new StringReader("/", "/", false),
			new RegexReader("isegment", REGEXP__ISEGMENT, false),
			new SequenceReader("ipath_absolute", List.of("/", "_opt_ipath_absolute"), false, 1),
			new OptionalReader("_opt_ipath_absolute", "_seq_ipath_absolute", false),
			new SequenceReader("_seq_ipath_absolute", List.of("isegment_nz", "ipath_abempty"), false),
			new RegexReader("isegment_nz", REGEXP__ISEGMENT_NZ, false),
			new SequenceReader("ipath_noscheme", List.of("isegment_nz_nc", "ipath_abempty"), false),
			new RegexReader("isegment_nz_nc", REGEXP__ISEGMENT_NZ_NC, false),
			new SequenceReader("ipath_rootless", List.of("isegment_nz", "ipath_abempty"), false),
			new ChoiceReader("ihost", List.of("IP_literal", "IPv4address", "ireg_name"), false),
			new RegexReader("ireg_name", REGEXP__IREG_NAME, false),
			new RegexReader("IPv4address", REGEXP__IPV4ADDRESS, false),
			new ChoiceReader("IP_literal", List.of("IPv6address", "IPvFuture"), false),
			new RegexReader("IPvFuture", REGEXP__IPVFUTURE, false),
			new RegexReader("IPv6address", REGEXP__IPV6ADDRESS, false)
			);
	}
	

	
	public IRIRefParser() {
		super(readers());
	}

}