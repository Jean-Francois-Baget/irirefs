# Change Log

All notable changes to the **irirefs** library will be documented in this file.

This project follows the principles of [Keep a Changelog](https://keepachangelog.com/)
and aims to respect [Semantic Versioning](https://semver.org/).

## [0.1.4] - 2025-12-09

### Changed
- Generalized internal recomposition methods from `StringBuilder` to `Appendable`, 
allowing direct use with arbitrary `Appendable` implementations.
- The no-arg `recompose()` methods now delegate to the `Appendable`-based versions and 
wrap any unexpected `IOException` (which cannot occur with `StringBuilder`) in an `AssertionError`.

## [0.1.3] - 2025-12-08

### Added
- `IRIManager.getAllPrefixes()` to expose an unmodifiable view of all declared prefix keys, making it easy to iterate over all configured prefixes.


## [0.1.2] - 2025-12-08

### Added

- `IRIManager#setPrefix(String prefixKey, IRIRef base)`  
  Allows registering or updating a prefix mapping directly from an existing
  `IRIRef`. As with `setBase(IRIRef)`, no extra normalization is applied,
  and the method requires an absolute IRI. This is intended for
  performance-sensitive code that already manages `IRIRef` instances.


## [0.1.1] - 2025-12-08

### Added

- `IRIManager#setBase(IRIRef base)`  
  Allows setting the manager’s base IRI directly from an existing `IRIRef`
  instance, without re-running preparation or normalization. The method still
  enforces that the supplied IRI is absolute; callers are responsible for
  passing already-normalized bases when desired.

## [0.1.0] - 2025-11-07

### Added

- Initial public release of **irirefs**, an implementation of RFC 3987
  Internationalized Resource Identifiers (IRIs), including:
    - `IRIRef` for parsing, inspecting, resolving, relativizing, normalizing
      and recomposing IRIs and IRI references;
    - internal helpers for authority and path handling (`IRIAuthority`,
      `IRIPath`);
    - `IRIRefParser` / `IRIRefValidator` based on **NanoParse** for
      grammar-level parsing and validation;
    - the `IRINormalizer` SPI and the default implementations
      `StandardComposableNormalizer` and `ExtendedComposableNormalizer`,
      configurable via `RFCNormalizationScheme`;
    - optional string preparation via the `StringPreparator` interface and
      `BasicStringPreparator`;
    - a high-level `IRIManager` for handling bases, prefixes and
      relativization in client applications.

- Documentation: significantly expanded Javadoc across the `irirefs` packages
  (`IRIRef`, `IRIAuthority`, `IRIPath`, parser, normalizer, preparator,
  manager, and utility classes) to clarify:
    - internal invariants (canonical path segments, authority encoding, etc.),
    - resolution and relativization semantics,
    - normalization strategies and the role of `IRINormalizer`,
    - usage patterns and performance caveats (e.g. for `IRIManager`).
