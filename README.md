# page4j

[![Maven Central](https://img.shields.io/maven-central/v/com.maxnth/page4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.maxnth/page4j)
[![Javadoc](https://javadoc.io/badge2/com.maxnth/page4j/javadoc.svg)](https://javadoc.io/doc/com.maxnth/page4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://openjdk.org/projects/jdk/25/)

A Java library for reading, writing and converting [PAGE XML](https://github.com/PRImA-Research-Lab/PAGE-XML)
documents, with readers for ALTO, ABBYY FineReader, hOCR and METS.

- An immutable model that covers every PAGE version, 2009-03-16 to 2024-07-15
- Strict IO: reading and writing validate against the bundled schemas, so nothing is lost or
  invented silently
- Conversion between PAGE versions that reports every change
- A named module with no runtime dependencies besides `java.xml` and JSpecify

page4j started as our internally used successor to PRImA's
[prima-core-libs](https://github.com/PRImA-Research-Lab/prima-core-libs). It has its own API and
implementation; [NOTICE](NOTICE) lists the attribution.

## Installation

Java 25 or later.

```kotlin
dependencies {
    implementation("com.maxnth:page4j:1.0.0")
}
```

```xml
<dependency>
    <groupId>com.maxnth</groupId>
    <artifactId>page4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

With the module system: `requires com.maxnth.page4j;`.

## Usage

Read, change and write a page:

```java
PageXml xml = PageXml.create();
PageXmlReadResult read = xml.read(Path.of("page.xml"));
Page page = read.page();

TextLine line = page.find("l1", TextLine.class).orElseThrow();
TextLine corrected =
    line.withTextEquivs(List.of(TextEquivBuilder.builder().unicode("Hello").build()));

xml.write(page.replace("l1", corrected), read.version(), Path.of("page.xml"));
```

A document that doesn't validate fails to read with a `PageReadException` that gives the position
of the problem. Writing fails with a `PageWriteException`, and leaves the file untouched, if the
page holds something the version can't express. Building a page with duplicate IDs or references
to unknown IDs throws `InvalidPageException`.

Convert to another version:

```java
Conversion conversion = PageConverter.convert(page, PageXmlVersion.V2019_07_15);
conversion.messages().forEach(System.out::println);
// TextLine 'l1': removed attribute orientation, not supported in PAGE 2019-07-15
```

Read other formats, which are mapped leniently and report what they couldn't map:

```java
byte[] data = Files.readAllBytes(path);
Page page = switch (Format.detect(data)) {
    case PAGE_XML -> PageXml.create().read(data).page();
    case ALTO -> AltoXml.create().read(data).page();
    case FINE_READER -> FineReaderXml.create().read(data).page();
    case HOCR -> Hocr.create().read(data).page();
    case METS -> throw new IllegalArgumentException("Not a page: " + MetsXml.create().files(data));
};
```

`AltoXml.write` writes a page as ALTO 4.1.

The [API documentation](https://javadoc.io/doc/com.maxnth/page4j) covers the model, the builders
and the details of each format.

| Package                         | Contents                                                     |
| ------------------------------- | ------------------------------------------------------------ |
| `com.maxnth.page4j.model`       | `Page` and one immutable record per PAGE element             |
| `com.maxnth.page4j.model.value` | Enums and values of PAGE attributes                          |
| `com.maxnth.page4j.pagexml`     | `PageXml`, which reads and writes PAGE XML                   |
| `com.maxnth.page4j.convert`     | `PageConverter`, which fits a page to another PAGE version   |
| `com.maxnth.page4j.formats`     | ALTO, FineReader, hOCR and METS                              |
| `com.maxnth.page4j.geometry`    | `Point`, `Polygon`, `Rect`                                   |

## Versioning

page4j follows [Semantic Versioning](https://semver.org). The exported packages above are the
public API; `*.internal` packages are not exported and can change in any release.

Snapshots of `main` are published to the Central snapshot repository:

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        mavenContent { snapshotsOnly() }
    }
}
```

## Building

```bash
./gradlew build
```

[Testing](docs/testing.md) describes the test suite and its fixtures; [CONTRIBUTING](CONTRIBUTING.md)
the conventions.

## License

[Apache License 2.0](LICENSE). The bundled PAGE XML schemas are by PRImA Research Lab.
