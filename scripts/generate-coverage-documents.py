#!/usr/bin/env python3
"""Generates one PAGE document per schema version that uses every element and attribute.

The documents are test resources: reading them checks that the reader knows every element and
reads every attribute, and round-tripping them checks the writer. Run from the repository root:

    python3 scripts/generate-coverage-documents.py

Every optional attribute is set. Each choice of the schema is expanded fully (every alternative)
the first time it is met and minimally after that, so every element appears and recursion (nested
regions and groups) ends.
"""

import os
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape, quoteattr

XS = "{http://www.w3.org/2001/XMLSchema}"
SCHEMAS = "src/main/resources/com/maxnth/page4j/pagexml/schemas/{}_pagecontent.xsd"
TARGET = "src/test/resources/coverage"
VERSIONS = ["2009-03-16", "2010-01-12", "2010-03-19", "2013-07-15", "2016-07-15", "2017-07-15", "2018-07-15", "2019-07-15", "2024-07-15"]
NAMESPACE = "http://schema.primaresearch.org/PAGE/gts/pagecontent/"
REF_TARGET = "@REF@"

BUILTIN = {
    "string": "text",
    "int": "1",
    "integer": "1",
    "float": "0.5",
    "double": "0.5",
    "boolean": "true",
    "dateTime": "2024-01-02T03:04:05",
}


def local(qname):
    return qname.split(":")[-1] if qname else qname


class Generator:
    def __init__(self, version):
        root = ET.parse(SCHEMAS.format(version)).getroot()
        self.complex = {c.get("name"): c for c in root.findall(XS + "complexType")}
        self.simple = {s.get("name"): s for s in root.findall(XS + "simpleType")}
        self.ids = 0
        self.ref_target = None
        self.expanded = set()
        self.out = []

    # ---- values

    def new_id(self, element):
        self.ids += 1
        value = f"{element[0].lower()}{element[1:3].lower()}{self.ids}"
        if element == "TextRegion" and self.ref_target is None:
            self.ref_target = value
        return value

    def simple_value(self, simple, name):
        restriction = simple.find(XS + "restriction")
        if restriction is None:
            raise SystemExit(f"Unsupported simple type for {name}")
        enumerations = restriction.findall(XS + "enumeration")
        if enumerations:
            return enumerations[0].get("value")
        if restriction.find(XS + "pattern") is not None:  # PointsType
            return "0,0 10,0 10,10 0,10"
        return self.type_value(local(restriction.get("base")), name)

    def type_value(self, type_name, name):
        if type_name in BUILTIN:
            return BUILTIN[type_name]
        if type_name in self.simple:
            return self.simple_value(self.simple[type_name], name)
        raise SystemExit(f"Unknown type {type_name} of {name}")

    def attribute_value(self, attribute, element, counters):
        name = attribute.get("name")
        type_name = local(attribute.get("type"))
        if type_name == "ID":
            return self.new_id(element)
        if type_name == "IDREF":
            return REF_TARGET
        if name == "index":
            counters["index"] = counters.get("index", -1) + 1
            return str(counters["index"])
        if type_name:
            return self.type_value(type_name, name)
        return self.simple_value(attribute.find(XS + "simpleType"), name)

    # ---- structure

    def parts(self, type_name):
        """Returns (attributes, particles) of a complex type, base type first."""
        complex_type = self.complex[type_name]
        attributes, particles = [], []
        content = complex_type
        extension = complex_type.find(f"{XS}complexContent/{XS}extension")
        if extension is not None:
            base_attributes, base_particles = self.parts(local(extension.get("base")))
            attributes += base_attributes
            particles += base_particles
            content = extension
        for child in content:
            if child.tag == XS + "attribute":
                attributes.append(child)
            elif child.tag in (XS + "sequence", XS + "choice"):
                particles.append(child)
        return attributes, particles

    def element(self, name, type_name, counters, indent):
        pad = "  " * indent
        if type_name not in self.complex:  # text content
            text = self.type_value(type_name, name)
            self.out.append(f"{pad}<{name}>{escape(text)}</{name}>")
            return
        attributes, particles = self.parts(type_name)
        values = [
            f"{a.get('name')}={quoteattr(self.attribute_value(a, name, counters))}"
            for a in attributes
        ]
        start = f"{pad}<{name}" + "".join(" " + v for v in values)
        mark = len(self.out)
        child_counters = {}
        for particle in particles:
            self.particle(particle, child_counters, indent + 1)
        if len(self.out) == mark:
            self.out.append(start + "/>")
        else:
            self.out.insert(mark, start + ">")
            self.out.append(f"{pad}</{name}>")

    def particle(self, particle, counters, indent):
        if particle.tag == XS + "sequence":
            for _ in range(max(1, int(particle.get("minOccurs", "1")))):
                for child in particle:
                    self.particle(child, counters, indent)
        elif particle.tag == XS + "choice":
            alternatives = list(particle)
            if particle.get("maxOccurs") != "unbounded":
                alternatives = alternatives[:1]
            elif particle in self.expanded:
                if particle.get("minOccurs") == "0":
                    return
                alternatives = [self.leaf_alternative(alternatives)]
            self.expanded.add(particle)
            for child in alternatives:
                self.particle(child, counters, indent)
        elif particle.tag == XS + "element":
            occurs = max(1, int(particle.get("minOccurs", "1")))
            for _ in range(occurs):
                self.element(particle.get("name"), local(particle.get("type")), counters, indent)
        else:
            raise SystemExit(f"Unsupported particle {particle.tag}")

    def leaf_alternative(self, alternatives):
        names = [a.get("name") for a in alternatives]
        for leaf in ("RegionRefIndexed", "RegionRef", "Grapheme"):
            if leaf in names:
                return alternatives[names.index(leaf)]
        raise SystemExit(f"No leaf alternative among {names}")

    def document(self, version):
        self.element("PcGts", "PcGtsType", {}, 0)
        body = "\n".join(self.out).replace(REF_TARGET, self.ref_target)
        namespace = NAMESPACE + version
        root_attributes = (
            f' xmlns="{namespace}" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"'
            f' xsi:schemaLocation="{namespace} {namespace}/pagecontent.xsd"'
        )
        body = body.replace("<PcGts", "<PcGts" + root_attributes, 1)
        return '<?xml version="1.0" encoding="UTF-8"?>\n' + body + "\n"


def main():
    os.makedirs(TARGET, exist_ok=True)
    for version in VERSIONS:
        path = os.path.join(TARGET, version + ".xml")
        with open(path, "w", encoding="utf-8") as out:
            out.write(Generator(version).document(version))
        print(path)


if __name__ == "__main__":
    main()
