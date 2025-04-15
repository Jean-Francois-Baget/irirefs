# Relativization of an iriref against a base

## Tentative definition

We have already a function `IRI resolve(IRIRef relative, AbsoluteIRI base)`, implemented according to [RFC3987](https://www.ietf.org/rfc/rfc3987.txt). We want now an inverse function `IRIRef relativize(IRI input, AbsoluteIRI base)`, such that if `relativize(i, b) = r`, then `resolve(r, b) = i`. As stated, the problem is immediate: a function  `IRIRef relativize(IRI input, AbsoluteIRI base)` that returns `i` satisfies our condition. We must add the following constraint to our `relativize` function: it returns (the? a?) IRIRef with shortest string representation such that if `relativize(i, b) = r`, then `resolve(r, b) = i`.

## With Jena

* **No query fragment allowed in the base:** as soon as the base contains a query fragment, the relativize method returns null (no relativization found). So if we want to write something like that:

```
@computed foofunctions: <http://www.boreal.fr/integraal/api/json?file=foo.json>

p(foofunctions:#bar(1)).
```
Jena is unable to to relativization.


* cannot remove scheme
* fails when no authority
