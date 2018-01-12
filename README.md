# GlodMed2SKOS
A tool for converting a QV-GlodMed glossary CSV dump into a SKOS graph or an OWL ontology.

## Building from source
Nothing special. Just `mvn clean install`.

## Usage
`java de.simpleanno.glodmed.GlodmedConverter <format> <source-file> [<destination>]` where

`<format>:` defines the output format and can be one of
- `owl` for representing terms as OWL classes and subterms as OWL subclasses
- `skos-rdf`: for representing terms as instances of skos:Concept and super/subterm relations by skos:narrower/skos:broader and outputting the resulting SKOS concept scheme in the form of an RDF graph
- `skos-owl`: as `skos-rdf`, but outputting the resulting SKOS concept scheme in the form of an OWL ontology

`<source>`: path to the file containing the glodmed CSV dump

`<destination>`: path to the output file. Optional. If absent, the output will be written to the directory that contains the source file. The output file will have the same name as the source file plus an annex which depends on the `<format>` argument (.owl for `owl`, .skos.rdf for `skos-rdf` and .skos.owl for `skos-owl`).
