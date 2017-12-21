/**
 *
 */
package de.simpleanno.glodmed.owl;

import de.simpleanno.glodmed.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.simpleanno.glodmed.owl.Glodmed2OWL.CSVField.*;

/**
 * @author ralph
 */
public class Glodmed2OWL {

    enum CSVField {id, lang, glossary, mainterm, subterm, definition, see, see_also, compare, figure_filename,
        figure_legend, figure_source, reference_literature, unique_test};

    private static final String BASE = "http://glodmed.simple-anno.de/glodmed";
    private static final String PREFIX = BASE + "#";

    private final Pattern markupPattern = Pattern.compile("\\[.*?\\]|<.*?>");

    private static final IRI GLODMED_CLASS_IRI = IRI.create("http://simple-anno.de/ontologies/dental_care_process#GLODMED");

    /**
     *
     */
    public Glodmed2OWL() {
        // TODO Auto-generated constructor stub
    }

    private int currentId;
    private int lastId = -1;
    private GlodmedEntry currentEntry;

    private void transform(Reader in) throws IOException, OWLException {

        CSVParser csvParser = CSVFormat.newFormat(',').withFirstRecordAsHeader().withQuote('"').parse(in);

        Glodmed glodmed = Glodmed.instance();

        // We need two passes: With the first pass we get all entries, with the second pass we add the references
        // (cannot be done in one pass because there can be references to objects that have not been created yet)

        // first pass

        csvParser.getRecords().stream().forEach(record -> {

            currentId = Integer.parseInt(record.get(id));

            if (currentId != lastId) {
                // new record

                if (currentEntry != null) {
                    glodmed.addEntry(currentEntry);
                }

                currentEntry = new GlodmedEntry(currentId, Glossary.valueOf(record.get(glossary)));

                lastId = currentId;
            }


            String mainLabel = record.get(mainterm);


            // mainterm empty: bad record (should not happen)
            // mainterm set, subterm empty: regular concept
            // mainterm set, subterm set: subterm is concept name, mainterm is label of related concept

            if (!mainLabel.isEmpty()) {

                Language language = Language.valueOf(record.get(lang).toUpperCase());

                GlodmedLanguageSpecificEntryPart languageSpecificEntryPart = new GlodmedLanguageSpecificEntryPart(currentEntry.getGlossary(), language);
                currentEntry.addLanguageSpecificPart(languageSpecificEntryPart);

                mainLabel = stripMarkup(mainLabel);

                String subLabel = record.get(subterm);

                if (!subLabel.isEmpty()) {
                    subLabel = stripMarkup(subLabel);

                    String properSubLabel = properSubtermLabel(mainLabel, subLabel);
                    subLabel = mainLabel;
                    mainLabel = properSubLabel;

                    languageSpecificEntryPart.setSuperTerm(subLabel);
                }

                languageSpecificEntryPart.setLabel(mainLabel);
                languageSpecificEntryPart.setDefinition(record.get(definition));
                languageSpecificEntryPart.setSee(record.get(see));
                languageSpecificEntryPart.setSeeAlso(record.get(see_also));
                languageSpecificEntryPart.setCompare(record.get(compare));
                languageSpecificEntryPart.setReferencedLiterature(record.get(reference_literature));
            }

        });

        // add the last one
        glodmed.addEntry(currentEntry);

        // second pass
        // construct SKOS ontology

        HashSet<OWLClass> classes = new HashSet<>();

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();

        OWLDataFactory df = mgr.getOWLDataFactory();

        OWLOntology ontology = mgr.createOntology(iri(""));

        OWLClass glodmedClass = df.getOWLClass(GLODMED_CLASS_IRI);

        HashMap<Glossary, OWLNamedIndividual> glossaries = new HashMap<>();

        glossaries.put(Glossary.GOMI, df.getOWLNamedIndividual(iri( "GOMI")));
        glossaries.put(Glossary.GOOT, df.getOWLNamedIndividual(iri( "GOOT")));

        ArrayList<OWLOntologyChange> changes = new ArrayList<>();
        changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(glossaries.get(Glossary.GOMI))));
        changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(glossaries.get(Glossary.GOOT))));

        OWLAnnotationProperty glossaryProp = df.getOWLAnnotationProperty(iri("glossary"));

        glodmed.forEach(entry -> {
            OWLClass clazz = df.getOWLClass(iri("" + entry.getId()));

            changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(clazz)));

            changes.add(new AddAxiom(ontology, df.getOWLSubClassOfAxiom(clazz, glodmedClass)));

            changes.add(new AddAxiom(ontology, df.getOWLAnnotationAssertionAxiom(clazz.getIRI(), df.getOWLAnnotation(glossaryProp, glossaries.get(entry.getGlossary()).getIRI()))));

            BooleanFlag superTermHandled = new BooleanFlag();

            entry.languageSpecificParts().forEach(part -> {

                Language language = part.getLanguage();
                String languageCode = language.isoCode;

                if (!superTermHandled.value) {
                    String superLabel = part.getSuperTerm();
                    if (superLabel != null) {
                        List<GlodmedEntry> superEntries = glodmed.getEntriesByLabel(entry.getGlossary(), part.getSuperTerm(), language);
                        superEntries.forEach(superEntry -> {
                            OWLSubClassOfAxiom subClassAxiom = df.getOWLSubClassOfAxiom(clazz, df.getOWLClass(
                                    iri("" + superEntry.getId())));
                            changes.add(new AddAxiom(ontology, subClassAxiom));
                        });
                    }
                }

                // Super term references are stored redundantly for each language in glodmed. We only need to take care
                // of them once per entry.
                superTermHandled.value = true;

                Optional.of(part.getLabel()).filter(s -> !s.isEmpty()).ifPresent(label -> {
                    OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), clazz.getIRI(), df.getOWLLiteral(label, languageCode));
                    changes.add(new AddAxiom(ontology, ax));
                });

                Optional.of(part.getDefinition()).filter(s -> !s.isEmpty()).ifPresent(definition -> {
                    OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#definition")), clazz.getIRI(), df.getOWLLiteral(definition, languageCode));
                    changes.add(new AddAxiom(ontology, ax));
                });

                Optional.of(part.getSee()).filter(s -> !s.isEmpty()).ifPresent(see -> {
                    OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSIsDefinedBy(), clazz.getIRI(), df.getOWLLiteral(see, languageCode));
                    changes.add(new AddAxiom(ontology, ax));
                });

                Optional.of(part.getSeeAlso()).filter(s -> !s.isEmpty()).ifPresent(seeAlso -> {
                    OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSSeeAlso(), clazz.getIRI(), df.getOWLLiteral(seeAlso, languageCode));
                    changes.add(new AddAxiom(ontology, ax));
                });

                Optional.of(part.getCompare()).filter(s -> !s.isEmpty()).ifPresent(compare -> {
                    OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#related")), clazz.getIRI(), df.getOWLLiteral(compare, languageCode));
                    changes.add(new AddAxiom(ontology, ax));
                });


            });

        });






        mgr.applyChanges(changes);

        mgr.saveOntology(ontology, new OWLFunctionalSyntaxOntologyFormat(), new FileOutputStream("/tmp/qv_glodmed-2016.owl"));

    }

    private IRI iri(String localname) {
        return IRI.create(BASE + "#" + localname);
    }

    /*
     * pre-condition: mainterm.length > 0
     */
    private String properSubtermLabel(String mainterm, String subterm) {
        char firstChar = mainterm.toUpperCase().charAt(0);
        Pattern pattern = Pattern.compile("(^|[\\W])" + firstChar + "\\.");

        Matcher matcher = pattern.matcher(subterm);

        return matcher.replaceAll("$1" + mainterm);
    }

    private String stripMarkup(String orig) {
        return markupPattern.matcher(orig).replaceAll("");
    }

    private GlodmedEntry getPropertGlodmedEntry(String mainterm, String subterm) {
        return null;
    }

    // boolean flag for use inside lambdas
    private class BooleanFlag {
        public boolean value = false;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Glodmed2OWL g2o = new Glodmed2OWL();
        try {
            g2o.transform(new FileReader("/Users/ralph/Documents/Arbeit/SimpleAnno/Daten/GlodMed/qe-glodmed-glodmed-Wed_19_Oct_2016-09_33_52.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OWLException e) {
            e.printStackTrace();
        }
    }

}
