package fr.inria.jfbaget.irirefs.preparator;

import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.function.Function;

/**
 * Basic {@link StringPreparator} implementation backed by a registry of named
 * {@code String -> String} transformers.
 * <p>
 * This class provides a convenient, configurable way to prepare input strings
 * before IRI parsing, using a small set of built-in transformers for the most
 * common cases (such as HTML4 and XML entity decoding). It is
 * <strong>not</strong> intended to be a definitive or exhaustive implementation
 * of all possible preparation rules. Instead, it is designed to be
 * <em>extensible</em>:
 * callers can register additional transformers or subclass
 * {@code BasicStringPreparator} to customise the preparation pipeline.
 * </p>
 *
 * <p>
 * Each instance of {@code BasicStringPreparator} is configured with an ordered
 * list of transformer names. When {@link #transform(String)} is called, the
 * corresponding functions are applied sequentially to the input string.
 * </p>
 *
 * <p>
 * The mapping from transformer name to function is stored in a static registry.
 * The default implementation registers:
 * </p>
 * <ul>
 *   <li>{@code "html4"} – delegates to
 *       {@link StringEscapeUtils#unescapeHtml4(String)}</li>
 *   <li>{@code "xml"} – delegates to
 *       {@link StringEscapeUtils#unescapeXml(String)}</li>
 * </ul>
 *
 * <p>
 * Clients may extend the registry at runtime using
 * {@link #addTransformer(String, Function)} or define their own subclass that
 * adds additional keys in a static initialiser block. Note that the registry
 * is mutable and shared across all instances in the JVM; it is therefore
 * recommended to configure it once at application startup and treat it as
 * effectively immutable thereafter.
 * </p>
 */
public class BasicStringPreparator implements StringPreparator {

    /**
     * Global registry of named transformers. The keys are symbolic names such as
     * {@code "html4"} or {@code "xml"}, the values are functions that map an
     * input string to a transformed string.
     * <p>
     * This map is mutable and shared across all {@code BasicStringPreparator}
     * instances. Callers may register additional transformers at runtime using
     * {@link #addTransformer(String, Function)}.
     * </p>
     */
    private static final Map<String, Function<String, String>> transformers = new HashMap<>();
    static {
        addTransformer("html4", StringEscapeUtils::unescapeHtml4);
        addTransformer("xml", StringEscapeUtils::unescapeXml);
    }

    /**
     * Ordered list of transformer names to apply when {@link #transform(String)}
     * is called. The order is significant: each transformer sees the output of
     * the previous one.
     */
    private final List<String> transformerNames = new ArrayList<>();

    /**
     * Constructs a new {@code BasicStringPreparator} that will apply the
     * transformers identified by the given names, in the given order.
     *
     * <p>
     * Each name must correspond to a key previously registered in the global
     * transformer registry. If an unknown name is encountered, this constructor
     * throws an {@link IllegalArgumentException}.
     * </p>
     *
     * <p>
     * This constructor does <strong>not</strong> modify the global registry:
     * it only selects which of the already-registered transformers will be used
     * by this instance.
     * </p>
     *
     * @param transformerNames an ordered list of transformer names to apply; must not
     *                  contain unknown names
     * @throws IllegalArgumentException if any of the provided names does not
     *                                  correspond to a registered transformer
     */
    public BasicStringPreparator(List<String> transformerNames) {
        for (String name : transformerNames) {
            if (transformers.containsKey(name)) {
                this.transformerNames.add(name);
            } else {
                throw new IllegalArgumentException("Unknown transformer: " + name +
                        ", must be part of " + getAvailableTransformers());
            }
        }
    }

    /**
     * Returns a snapshot of the currently registered transformer names.
     *
     * <p>
     * This is a read-only view that cannot be used to modify the underlying
     * registry. It is mainly intended for diagnostics, configuration, and error
     * messages. The contents may change over time if
     * {@link #addTransformer(String, Function)} is called.
     * </p>
     *
     * @return an unmodifiable set containing the keys of all registered
     *         transformers
     */
    public static Set<String> getAvailableTransformers() {
        return Collections.unmodifiableSet(new HashSet<>(transformers.keySet()));
    }

    /**
     * Registers or replaces a transformer in the global registry.
     *
     * <p>
     * If a transformer is already registered under the given key, it will be
     * replaced. This affects all existing and future {@code BasicStringPreparator}
     * instances that refer to that key.
     * </p>
     *
     * <p>
     * This method is provided to allow applications and subclasses to extend or
     * override the built-in behaviour. The built-in set
     * ({@code "html4"}, {@code "xml"}) is deliberately small and is
     * <strong>not</strong> meant to cover all use cases.
     * </p>
     *
     * @param key         the symbolic name of the transformer (for example
     *                    {@code "html4"})
     * @param transformer the function to associate with this key; must not be
     *                    {@code null}
     */
    public static void addTransformer(String key, Function<String, String> transformer) {
        transformers.put(key, transformer);
    }

    /**
     * Applies the configured sequence of transformers to the given input string.
     *
     * <p>
     * If {@code input} is {@code null}, this method returns {@code null}
     * immediately. Otherwise, each transformer referenced by this instance's
     * {@link #transformerNames} list is looked up in the global registry and applied in
     * order to the current intermediate result.
     * </p>
     *
     * <p>
     * The exact semantics therefore depend on:
     * </p>
     * <ul>
     *   <li>which transformer names were passed to the constructor; and</li>
     *   <li>which functions are currently registered under those names.</li>
     * </ul>
     *
     * @param input the raw input string to transform; may be {@code null}
     * @return the transformed string, or {@code null} if the input was
     *         {@code null}
     */
    @Override
    public String transform(String input) {
        if (input == null) { return null; }
        String result = input;
        for (String name : this.transformerNames) {
            result = transformers.get(name).apply(result);
        }
        return result;
    }

    public static void main(String[] args) {
        BasicStringPreparator preparator = new BasicStringPreparator(List.of("html4"));
        System.out.println(preparator.transform("Ros&eacute;"));
    }


}
