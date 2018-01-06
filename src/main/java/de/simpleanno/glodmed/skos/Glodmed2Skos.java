/**
 * 
 */
package de.simpleanno.glodmed.skos;

import de.simpleanno.glodmed.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.text.StringEscapeUtils;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.skos.*;
import org.semanticweb.skosapibinding.SKOSFormatExt;
import org.semanticweb.skosapibinding.SKOSManager;
import org.semanticweb.skosapibinding.SKOStoOWLConverter;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.*;

/**
 * @author ralph
 */
public class Glodmed2Skos {
	
	enum CSVField {id, lang, glossary, mainterm, subterm, definition, see, see_also, compare, figure_filename,
		figure_legend, figure_source, reference_literature, unique_test};
		
	private static final String BASE = "http://glodmed.simple-anno.de/glodmed";
	private static final String PREFIX = BASE + "#";

	private final Pattern markupPattern = Pattern.compile("\\[.*?\\]|<.*?>"); 

	/**
	 * 
	 */
	public Glodmed2Skos() {
		// TODO Auto-generated constructor stub
	}

	private int currentId;
	private int lastId = -1;
	private GlodmedEntry currentEntry;

	private void transform(Reader in) throws IOException, SKOSException {

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


			String mainLabel = record.get(mainterm).trim();
			
						
			// mainterm empty: bad record (should not happen)
			// mainterm set, subterm empty: regular concept
			// mainterm set, subterm set: subterm is concept name, mainterm is label of related concept
			
			if (!mainLabel.isEmpty()) {

				Language language = Language.valueOf(record.get(lang).trim().toUpperCase());

				GlodmedLanguageSpecificEntryPart languageSpecificEntryPart = new GlodmedLanguageSpecificEntryPart(currentEntry.getGlossary(), language);
                currentEntry.addLanguageSpecificPart(languageSpecificEntryPart);

                mainLabel = stripMarkup(mainLabel);

                String subLabel = record.get(subterm).trim();

				if (!subLabel.isEmpty()) {
                    subLabel = stripMarkup(subLabel);

					String properSubLabel = properSubtermLabel(mainLabel, subLabel);
					subLabel = mainLabel;
					mainLabel = properSubLabel;

                    languageSpecificEntryPart.setSuperTerm(subLabel);
				}

                languageSpecificEntryPart.setLabel(mainLabel);
                languageSpecificEntryPart.setDefinition(record.get(definition).trim());
                languageSpecificEntryPart.setSee(record.get(see).trim());
                languageSpecificEntryPart.setSeeAlso(stripMarkup(record.get(see_also).trim()));
                languageSpecificEntryPart.setCompare(record.get(compare).trim());
                languageSpecificEntryPart.setReferencedLiterature(record.get(reference_literature).trim());
			}
			
		});

		// add the last one
        glodmed.addEntry(currentEntry);

        // second pass
		// construct SKOS ontology
		
		HashSet<SKOSConcept> concepts = new HashSet<>();		

		SKOSManager mgr = new SKOSManager();
		SKOSDataFactory df = mgr.getSKOSDataFactory();
		
		SKOSDataset ds = mgr.createSKOSDataset(URI.create(BASE));
		
        HashMap<Glossary, SKOSConceptScheme> conceptSchemes = new HashMap<>();

        conceptSchemes.put(Glossary.GOMI, df.getSKOSConceptScheme(uri("gomi")));
        conceptSchemes.put(Glossary.GOOT, df.getSKOSConceptScheme(uri("goot")));

        ArrayList<SKOSChange> changes = new ArrayList<>();
        changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(conceptSchemes.get(Glossary.GOMI))));
        changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(conceptSchemes.get(Glossary.GOOT))));

        glodmed.forEach(entry -> {
            SKOSConcept concept = df.getSKOSConcept(uri("" + entry.getId()));

            changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(concept)));

            changes.add(new AddAssertion(ds, df.getSKOSObjectRelationAssertion(concept, df.getSKOSInSchemeProperty(), conceptSchemes.get(entry.getGlossary()))));

            BooleanFlag superTermHandled = new BooleanFlag();

            entry.languageSpecificParts().forEach(part -> {

                Language language = part.getLanguage();
                String languageCode = language.isoCode;

                if (!superTermHandled.value) {
                    String superLabel = part.getSuperTerm();
                    if (superLabel != null) {
                        List<GlodmedEntry> superEntries = glodmed.getEntriesByLabel(entry.getGlossary(), part.getSuperTerm(), language);
                        superEntries.forEach(superEntry -> {
                            SKOSObjectRelationAssertion broaderRelationAssertion = df.getSKOSObjectRelationAssertion(concept, df.getSKOSBroaderProperty(), df.getSKOSConcept(uri("" + superEntry.getId())));
                            changes.add(new AddAssertion(ds, broaderRelationAssertion));
                        });
                    }
                }

                // Super term references are stored redundantly for each language in glodmed. We only need to take care
                // of them once per entry.
                superTermHandled.value = true;

				Optional.of(part.getLabel()).filter(s -> !s.isEmpty()).ifPresent(label -> {
					SKOSAnnotation prefLabelAnnotation = df.getSKOSAnnotation(df.getSKOSPrefLabelProperty().getURI(), label, languageCode);
					SKOSAnnotationAssertion prefLabelAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, prefLabelAnnotation);
					changes.add(new AddAssertion(ds, prefLabelAnnotationAssertion));
				});

				Optional.of(part.getDefinition()).filter(s -> !s.isEmpty()).ifPresent(definition -> {
					SKOSAnnotation definitionDP= df.getSKOSAnnotation(df.getSKOSDefinitionDataProperty().getURI(), StringEscapeUtils.escapeXml10(definition).trim(), languageCode);
					SKOSAnnotationAssertion definitionAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, definitionDP);
					changes.add(new AddAssertion(ds, definitionAnnotationAssertion));
				});

				Optional.of(part.getSee()).filter(s -> !s.isEmpty()).ifPresent(see -> {
					Arrays.stream(see.split(";")).forEach(seeTerm -> {
						glodmed.getEntriesByLabel(entry.getGlossary(), seeTerm.trim(), language).forEach(referencedEntry -> {
							SKOSAnnotation definedByDP = df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"), df.getSKOSConcept(uri(""+referencedEntry.getId())));
							SKOSAnnotationAssertion definedByAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, definedByDP);
							changes.add(new AddAssertion(ds, definedByAnnotationAssertion));
						});
					});
				});

				Optional.of(part.getSeeAlso()).filter(s -> !s.isEmpty()).ifPresent(seeAlso -> {
					Arrays.stream(seeAlso.split(";")).forEach(seeAlsoTerm -> {
						glodmed.getEntriesByLabel(entry.getGlossary(), seeAlsoTerm.trim(), language).forEach(referencedEntry -> {
							SKOSAnnotation seeAlsoAnnotation = df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#seeAlso"), df.getSKOSConcept(uri("" + referencedEntry.getId())));
							SKOSAnnotationAssertion seeAlsoAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, seeAlsoAnnotation);
							changes.add(new AddAssertion(ds, seeAlsoAnnotationAssertion));
						});
					});
				});

				Optional.of(part.getCompare()).filter(s -> !s.isEmpty()).ifPresent(compare -> {
					Arrays.stream(compare.split(";")).forEach(compareTerm -> {
						glodmed.getEntriesByLabel(entry.getGlossary(), compareTerm.trim(), language).forEach(referencedEntry -> {
							SKOSAnnotation relatedDP= df.getSKOSAnnotation(df.getSKOSRelatedProperty().getURI(), df.getSKOSConcept(uri(""+referencedEntry.getId())));
							SKOSAnnotationAssertion relatedAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, relatedDP);
							changes.add(new AddAssertion(ds, relatedAnnotationAssertion));
						});
					});
				});



            });

        });





		
		mgr.applyChanges(changes);
		
		mgr.save(ds, SKOSFormatExt.RDFXML, URI.create("file:/tmp/glodmed-skos.rdf"));

        SKOStoOWLConverter converter = new SKOStoOWLConverter();
        OWLOntology onto = converter.getAsOWLOntology(ds);
        OWLOntologyManager man = onto.getOWLOntologyManager();
        try {
            man.saveOntology(onto, new OWLFunctionalSyntaxOntologyFormat(), new FileOutputStream("/tmp/glodmed-skos.ofn"));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

    }
	
	private URI uri (String localname) {
		return URI.create(BASE + "#" + localname);
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
		Glodmed2Skos g2s = new Glodmed2Skos();
		try {
			g2s.transform(new FileReader("/Users/ralph/Documents/Arbeit/SimpleAnno/Daten/GlodMed/qe-glodmed-glodmed-Wed_19_Oct_2016-09_33_52.csv"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SKOSException e) {
			e.printStackTrace();
		}
	}

}
