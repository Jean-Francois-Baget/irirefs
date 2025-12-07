package fr.inria.jfbaget.irirefs.exceptions;

/**
 * Unchecked exception thrown when an IRI or IRI reference cannot be parsed.
 * <p>
 * This exception typically indicates that an input string does not conform
 * to the syntax of RFC&nbsp;3986 / RFC&nbsp;3987, as interpreted by this
 * library's parser (see {@code IRIRefParser} and {@code IRIRef}).
 * </p>
 *
 * <p>
 * Because {@code IRIParseException} extends {@link RuntimeException}, callers
 * are not required to catch it explicitly, but may do so if they want to
 * distinguish parse failures from other errors.
 * </p>
 */
public class IRIParseException extends RuntimeException{


	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@code IRIParseException} with the given detail message.
	 *
	 * @param msg human-readable description of the parse error
	 */
	public IRIParseException(String msg) {
		super(msg);
	}

}
