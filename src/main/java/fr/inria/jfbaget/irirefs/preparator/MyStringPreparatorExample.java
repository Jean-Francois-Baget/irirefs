package fr.inria.jfbaget.irirefs.preparator;


import java.util.List;

/**
 * Example subclass of {@link BasicStringPreparator} illustrating how to extend
 * the basic preparation mechanism with custom transformers.
 *
 * <p>
 * This class registers a new transformer under the key {@code "killroy"},
 * which ignores its input and always returns the literal string
 * {@code "Killroy was here"}. It is meant purely as a demonstration of how to:
 * </p>
 * <ul>
 *   <li>register a new named transformer in the global registry; and</li>
 *   <li>configure a {@code BasicStringPreparator} (via {@code super(...)})
 *       to use that transformer.</li>
 * </ul>
 *
 * <p>
 * In real applications, you would typically replace {@link #killroyTransformation(String)}
 * with more useful logic (for example, custom entity decoding, additional
 * Unicode normalisation, or application-specific cleanup). The overall design
 * of the string preparation API is intentionally extensible rather than
 * definitive: the built-in transformers in {@link BasicStringPreparator}
 * cover common cases, but callers are expected to plug in their own rules
 * when needed.
 * </p>
 */
public class MyStringPreparatorExample extends BasicStringPreparator {

    /**
     * Static initialiser that registers the custom transformer {@code "killroy"}
     * once per JVM.
     *
     * <p>
     * The transformer is added to the global registry maintained by
     * {@link BasicStringPreparator}. After this block has executed,
     * any {@code BasicStringPreparator} (or subclass) can refer to
     * {@code "killroy"} in its constructor.
     * </p>
     */
    static {
        addTransformer("killroy", MyStringPreparatorExample::killroyTransformation);
    }

    /**
     * Creates a {@code MyStringPreparatorExample} that always applies the
     * {@code "killroy"} transformer.
     *
     * <p>
     * Calling {@link #transform(String)} on an instance created with this
     * constructor will always return {@code "Killroy was here"}, regardless of
     * the input.
     * </p>
     */
    public MyStringPreparatorExample() {
        super(List.of("killroy"));
    }

    /**
     * Creates a {@code MyStringPreparatorExample} that applies the transformers
     * identified by the given names, in the given order.
     *
     * <p>
     * This constructor behaves exactly like
     * {@link BasicStringPreparator#BasicStringPreparator(List)}, but is provided
     * here to show that subclasses can mix built-in and custom transformers
     * freely. For example:
     * </p>
     *
     * <pre>{@code
     * // First decode HTML, then apply the "killroy" transformation.
     * MyStringPreparatorExample prep =
     *     new MyStringPreparatorExample(List.of("html4", "killroy"));
     * }</pre>
     *
     * @param transformers the ordered list of transformer names to apply;
     *                     each name must be registered in the global registry
     * @throws IllegalArgumentException if any name is unknown
     */
    public MyStringPreparatorExample(List<String> transformers) {
        super(transformers);
    }

    /**
     * Custom transformer implementation used by the {@code "killroy"} key.
     *
     * <p>
     * This implementation deliberately ignores its input and always returns
     * the fixed string {@code "Killroy was here"}. It serves purely as a
     * simple, recognisable example of a {@code String -> String} transformer.
     * </p>
     *
     * @param input the original input string (ignored)
     * @return the literal string {@code "Killroy was here"}
     */
    private static  String killroyTransformation(String input) {
        return "Killroy was here";
    }

    /**
     * Simple demonstration program showing how this example preparator behaves.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        MyStringPreparatorExample prep = new MyStringPreparatorExample(List.of("killroy"));
        System.out.println(prep.transform("Hello, world")); // -> "Killroy was here"
    }
}