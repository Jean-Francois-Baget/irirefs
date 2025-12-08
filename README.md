# IRIRefs

`irirefs` is a small Java 17 library implementing
[RFC 3987 – Internationalized Resource Identifiers (IRIs)](https://datatracker.ietf.org/doc/html/rfc3987).

It focuses on:

* **Parsing** IRIs and relative references (RFC 3987 / RFC 3986 grammar).
* **Component access**: scheme, authority (user / host / port), path, query, fragment.
* **Resolution** of relative references against an absolute base (RFC 3986 §5.2).
* **Relativization**: computing short relative references between two IRIs.
* **Normalization** (syntax-based, percent-encoding, path, scheme-based…).
* **String preparation** (e.g. HTML/XML entity decoding before parsing).
* A small **`IRIManager`** to manage a base IRI + prefixes and to resolve / relativize conveniently.

The library is intentionally low-level and RFC-oriented, with a strongly documented API.

---

## Author and maintainer

**Jean-François Baget** – [https://www.lirmm.fr/~baget/](https://www.lirmm.fr/~baget/)

---

## Installation

The artifact is published on Maven Central.

```xml
<dependency>
  <groupId>io.github.jean-francois-baget</groupId>
  <artifactId>irirefs</artifactId>
  <version>0.1.0</version>
</dependency>
```

Requirements:

* Java **17+**
* No extra configuration is needed beyond the dependency.

---

## Documentation

The javadoc can be accessed on [javadoc.io](https://javadoc.io/doc/io.github.jean-francois-baget/irirefs/latest/index.html).

---

## IRIRef – standalone usage

### Basic parsing and component access

```java
import fr.inria.jfbaget.irirefs.IRIRef;

public class BasicExample {
    public static void main(String[] args) {
        IRIRef iri = new IRIRef("https://user@example.org:8443/a/b/c?x=1#frag");

        System.out.println("Full IRI:  " + iri.recompose());
        System.out.println("Scheme:    " + iri.getScheme());

        if (iri.hasAuthority()) {
            System.out.println("User:      " + iri.getUser());
            System.out.println("Host:      " + iri.getHost());
            System.out.println("Port:      " + iri.getPort()); // may be null
        }

        System.out.println("Path:      " + iri.getPath());      // textual path
        System.out.println("Has query: " + iri.hasQuery());
        System.out.println("Query:     " + iri.getQuery());
        System.out.println("Fragment:  " + iri.getFragment());

        System.out.println("Empty path?   " + iri.hasEmptyPath());
        System.out.println("Rooted path?  " + iri.hasRootedPath());
    }
}
```

Notes:

* `hasXxx()` tells you if the component exists (even if empty, e.g. `foo?` has a query).
* `getHost()` may return `""` for an **empty authority** (e.g. `file:///C:`).

---

### Resolution (RFC 3986 §5.2)

```java
import fr.inria.jfbaget.irirefs.IRIRef;

public class ResolutionExample {
    public static void main(String[] args) {
        IRIRef base = new IRIRef("http://example.com/a/b/c/");
        IRIRef relative = new IRIRef("../d/e?x=1#frag");

        // Non-mutating: returns a new IRIRef
        IRIRef resolved = relative.resolve(base);
        System.out.println("Resolved: " + resolved.recompose());
        // -> http://example.com/a/d/e?x=1#frag

        // Mutating version (on this instance):
        relative.resolveInPlace(base);
        System.out.println("In-place: " + relative.recompose());
    }
}
```

* The `base` **must** be absolute (scheme present, no fragment), otherwise an `IllegalArgumentException` is thrown.
* Path dot-segments are removed as part of resolution, as required by the RFC.

---

### Relativization

Relativization finds a relative reference `r` such that:

> `r.resolve(base)` is equivalent to the original IRI.

```java
import fr.inria.jfbaget.irirefs.IRIRef;

public class RelativizeExample {
    public static void main(String[] args) {
        IRIRef base   = new IRIRef("http://example.com/a/b/c/");
        IRIRef target = new IRIRef("http://example.com/a/d/e");

        IRIRef relative = target.relativize(base);
        System.out.println("Relative: " + relative.recompose());
        // Example output: ../d/e
    }
}
```

* `relativize(base)` requires:

    * `base` to be absolute (with scheme and no fragment);
    * `this` (the target) to be a full IRI (has a scheme).
* The algorithm tries to return a *shortest* reasonable relative form; if no useful relative reference can be found, it falls back to keeping an absolute form.

---

### Normalization with `StandardComposableNormalizer`

The `IRINormalizer` interface lets you apply RFC 3987 normalization steps *after* parsing.

```java
import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;
import fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer;

public class NormalizationExample {
    public static void main(String[] args) {
        IRIRef iri = new IRIRef("HTTP://Example.COM:80/%7Euser/%e2%82%ac");

        StandardComposableNormalizer normalizer = new StandardComposableNormalizer(
                RFCNormalizationScheme.SYNTAX, // CASE + CHARACTER + PCT
                RFCNormalizationScheme.PATH,
                RFCNormalizationScheme.SCHEME
        );

        // Mutating version:
        iri.normalizeInPlace(normalizer);
        System.out.println("Normalized: " + iri.recompose());

        // Non-mutating style, if you prefer:
        // IRIRef normalized = new IRIRef(iri).normalizeInPlace(normalizer);
    }
}
```

Typical effects:

* Scheme lowercased (`HTTP` → `http`),
* Default port removed when it matches the scheme (e.g. `:80` for `http`),
* Percent-encoded unreserved characters decoded,
* Unicode NFC normalization,
* Optional dot-segment and scheme-based normalization, depending on the flags.

If you want a more aggressive percent-decoding using UTF-8 and `iunreserved`, you can use
`ExtendedComposableNormalizer` instead.

---

### String preparation (HTML / XML entities, etc.)

`StringPreparator` lets you transform the raw input *before* parsing, e.g. to decode HTML entities.

```java
import fr.inria.jfbaget.irirefs.IRIRef;
import fr.inria.jfbaget.irirefs.preparator.BasicStringPreparator;
import fr.inria.jfbaget.irirefs.preparator.StringPreparator;
import fr.inria.jfbaget.irirefs.normalizer.StandardComposableNormalizer;
import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;

import java.util.List;

public class PreparationExample {
    public static void main(String[] args) {
        // Decode HTML4 entities before parsing
        StringPreparator preparator = new BasicStringPreparator(List.of("html4"));

        StandardComposableNormalizer normalizer = new StandardComposableNormalizer(
                RFCNormalizationScheme.SYNTAX,
                RFCNormalizationScheme.SCHEME
        );

        IRIRef iri = new IRIRef("https://example.org/%7Euser?label=Ros&eacute;", preparator)
                .normalizeInPlace(normalizer);

        System.out.println(iri.recompose());
        // Query will use "Rosé" instead of "Ros&eacute;"
    }
}
```

You can register your own transformers in `BasicStringPreparator.addTransformer(...)` or subclass it.

---

## Using IRIManager

`IRIManager` is a small helper around `IRIRef` that manages:

* a mutable **base IRI** (always absolute and normalized),
* a map of **prefixes** (like `ex:` → absolute IRI),
* creation of IRIs relative to a base or prefix,
* relativization against base or prefixes, and selection of the “best” relative form.

### Creating a manager

```java
import fr.inria.jfbaget.irirefs.manager.IRIManager;
import fr.inria.jfbaget.irirefs.normalizer.ExtendedComposableNormalizer;
import fr.inria.jfbaget.irirefs.normalizer.IRINormalizer;
import fr.inria.jfbaget.irirefs.normalizer.RFCNormalizationScheme;
import fr.inria.jfbaget.irirefs.preparator.BasicStringPreparator;
import fr.inria.jfbaget.irirefs.preparator.StringPreparator;
import fr.inria.jfbaget.irirefs.IRIRef;

import java.util.List;

public class ManagerSetupExample {
    public static void main(String[] args) {
        StringPreparator preparator = new BasicStringPreparator(List.of("html4"));

        IRINormalizer normalizer = new ExtendedComposableNormalizer(
                RFCNormalizationScheme.SYNTAX,
                RFCNormalizationScheme.PATH,
                RFCNormalizationScheme.SCHEME
        );

        // Base is parsed, resolved against the internal DEFAULT_BASE,
        // normalized, and required to be absolute.
        IRIManager manager = new IRIManager(preparator, normalizer,
                "HTTP://www.BOREAL.inria.fr:80/");

        System.out.println("Base: " + manager.getBase());
        // e.g. http://www.boreal.inria.fr/
    }
}
```

There is also a convenience constructor:

```java
// No preparator, StandardComposableNormalizer with default (string-only) profile
IRIManager manager = new IRIManager("http://example.org/base/");
```

---

### Setting base and prefixes

```java
import fr.inria.jfbaget.irirefs.manager.IRIManager;
import fr.inria.jfbaget.irirefs.IRIRef;

public class PrefixExample {
    public static void main(String[] args) {
        IRIManager manager = new IRIManager("http://example.org/");

        // Change the base (must resolve to an absolute IRI)
        manager.setBase("http://example.org/base/");

        // Prefix without indirection
        manager.setPrefix("ex", "https://example.com/ns/");

        // Prefix defined relative to another prefix
        manager.setPrefix("data", "ex", "dataset/");

        System.out.println("Base:       " + manager.getBase());
        System.out.println("ex:         " + manager.getPrefix("ex"));
        System.out.println("data:       " + manager.getPrefix("data"));
    }
}
```

* `setBase(...)` and `setPrefix(...)` both use `IRIRef` internally:

    * parse → resolve against current base → normalize → require absolute.
* If the resulting IRI is not absolute, an `IllegalArgumentException` is thrown.

---

### Creating IRIs via the manager

```java
import fr.inria.jfbaget.irirefs.manager.IRIManager;
import fr.inria.jfbaget.irirefs.IRIRef;

public class ManagerCreateExample {
    public static void main(String[] args) {
        IRIManager manager = new IRIManager("http://example.org/base/");

        // Relative to the current base
        IRIRef iri1 = manager.createIRI("foo/bar#frag");
        System.out.println("IRI1: " + iri1.recompose());
        // -> http://example.org/base/foo/bar#frag

        // Using a prefix (prefix must have been declared)
        manager.setPrefix("ex", "https://example.com/ns/");
        IRIRef iri2 = manager.createIRI("ex", "Person");
        System.out.println("IRI2: " + iri2.recompose());
        // -> https://example.com/ns/Person
    }
}
```

All created IRIs are:

1. prepared (if a `StringPreparator` is configured),
2. resolved against the chosen base / prefix,
3. normalized with the configured `IRINormalizer`.

---

### Relativization via the manager

```java
import fr.inria.jfbaget.irirefs.manager.IRIManager;
import fr.inria.jfbaget.irirefs.IRIRef;

public class ManagerRelativizeExample {
    public static void main(String[] args) {
        IRIManager manager = new IRIManager("http://example.org/base/");

        manager.setPrefix("ex", "http://example.org/base/vocab/");
        manager.setPrefix("data", "http://example.org/data/");

        IRIRef iri = new IRIRef("http://example.org/base/vocab/Person");

        // Relativize w.r.t. base
        IRIRef relBase = manager.relativize(iri);
        System.out.println("Rel vs base:  " + relBase.recompose());
        // e.g. vocab/Person

        // Relativize w.r.t. prefix "ex"
        IRIRef relEx = manager.relativize("ex", iri);
        System.out.println("Rel vs ex:    " + relEx.recompose());
        // e.g. Person

        // Choose the “best” representation among base + all prefixes
        IRIManager.PrefixedIRIRef best = manager.relativizeBest(iri);
        System.out.println("Best prefix:  " + best.prefix());
        System.out.println("Best IRI:     " + best.iri().recompose());
        // Example:
        // Best prefix: ex
        // Best IRI:    Person
    }
}
```

`relativizeBest`:

* Tries `relativize` w.r.t. the current base and each registered prefix.
* Compares the length of `prefix + ":" + iri` against other possibilities.
* Returns a `PrefixedIRIRef(prefix, iri)` record:

    * `prefix` can be `null` if the best form is without prefix (base-relative).
    * `iri` is the chosen (possibly relative) `IRIRef`.

> ⚠️ **Performance note:**
> `relativizeBest` must run the relativization algorithm once per prefix.
> If you have many prefixes or call this very frequently on the **same** `IRIRef`,
> it can become non-trivial. In such cases, consider caching the result for that IRI.

---

## License

`irirefs` is licensed under the **Apache License 2.0**.
See the `LICENSE` file for details.
