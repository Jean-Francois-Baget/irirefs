# 🧬  irirefs

This project aims at a java implementation of [RFC 3987 Internationalized Resource Identifiers (IRIs)](https://www.ietf.org/rfc/rfc3987.txt). It relies upon a parser written in [nanoparse](https://gitlab.inria.fr/jfbaget/nanoparse), and offers the possibility to build irirefs (relative or not) from strings, recompose (display) them, resolve a relative against a base or normalize a full IRI, all according to the specifications in [RFC 3987](https://www.ietf.org/rfc/rfc3987.txt). A relativization mechanism is also offered to display short versions of a full IRI.

⚠️ This project is a work in progress. Its API and internal structure may evolve in future versions.

---

## 🚀 Installation

Since this project is not yet available on Maven Central, you can build and install it locally, by first installing the required project [nanoparse](https://gitlab.inria.fr/jfbaget/nanoparse).

```bash
git clone https://gitlab.inria.fr/jfbaget/nanoparse.git
cd nanoparse
mvn clean install
cd ..
git clone https://gitlab.inria.fr/jfbaget/irirefs.git
cd irirefs
mvn clean install
```
---