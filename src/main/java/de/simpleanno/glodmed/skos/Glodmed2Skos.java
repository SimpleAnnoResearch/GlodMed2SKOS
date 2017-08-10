/**
 * 
 */
package de.simpleanno.glodmed.skos;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.GenericArrayType;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.simpleanno.glodmed.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.semanticweb.skos.AddAssertion;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSAnnotationAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSDataFactory;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSException;
import org.semanticweb.skosapibinding.SKOSFormatExt;
import org.semanticweb.skosapibinding.SKOSManager;
import uk.ac.manchester.cs.skos.SKOSDataFactoryImpl;

import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.*;

/**
 * @author ralph
 */
public class Glodmed2Skos {
	
	enum CSVField {id, lang, glossary, mainterm, subterm, definition, see, see_also, compare, figure_filename,
		figure_legend, figure_source, reference_literature, unique_test};
		
	private static final String BASE = "http://glodmed.simple-anno.de/glodmed";
	private static final String PREFIX = BASE + "#";
	
//	enum Glossary {
//		
//		GOMI, GOOT;
//		
//		String getIRI() {
//			return PREFIX + name();
//		}
//	};

//	private static class CompoundKey {
//		private String term, lang, glossary;
//
//		public CompoundKey(String term, String lang, String glossary) {
//			this.term = term;
//			this.lang = lang;
//			this.glossary = glossary;
//		}
//		
//		@Override
//		public boolean equals(Object other) {
//			if (!(other instanceof CompoundKey))
//				return false;
//			if (other == this)
//				return true;
//			CompoundKey o = (CompoundKey)other;
//			return o.term.equals(term) && o.lang.equals(lang) && o.glossary.equals(glossary);
//		}
//		
//		@Override
//		public int hashCode() {
//			return 13 * term.hashCode() + 37 * lang.hashCode() + 41 * glossary.hashCode(); 
//		}
//	}
	
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
		
//		csvParser.getHeaderMap().keySet().stream().forEach(header -> System.out.println(header));		
//		System.out.println("\n\n");
		
		Glodmed glodmed = Glodmed.instance();
		
//		HashMap<CompoundKey, SKOSConcept> conceptsByLabel = new HashMap<>();

		// We need two passes: With the first pass we get all entries, with the second pass we add the references
		// (cannot be done in one pass because there can be references to objects that have not been created yet)

		// first pass

		csvParser.getRecords().stream().forEach(record -> {

			currentId = Integer.parseInt(record.get(id));
			
			if (currentId != lastId) {
				// new record

				currentEntry = new GlodmedEntry(currentId, Glossary.valueOf(record.get(glossary)));
				glodmed.addEntry(currentEntry);

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

				String subLabel = record.get(subterm);

				if (subLabel.isEmpty()) {
				} else {
					String properSubLabel = properSubtermLabel(mainLabel, subLabel);
					subLabel = mainLabel;
					mainLabel = properSubLabel;


					// for debugging

//					System.out.format("%s, %s, %s\n", mainLabel, subLabel, properSubLabel);

//					List<GlodmedEntry> entriesWithSameMainLabel = glodmed.getEntriesByLabel(mainLabel, language);
//					if (entriesWithSameMainLabel.isEmpty()) {
//						System.out.format("Missing main term: %s\n", mainLabel);
//					}
					// end debugging

//					glodmed.getEntriesByLabel(mainLabel, language).forEach(entry -> {
//						if (entry.getGlossary() == currentEntry.getGlossary()) {
//
//						}
//					});

				}


				// strip markup
				mainLabel = stripMarkup(mainLabel);
				subLabel = stripMarkup(subLabel);
				
                languageSpecificEntryPart.setLabel(mainLabel);
                languageSpecificEntryPart.setDefinition(record.get(definition));
                languageSpecificEntryPart.setSee(record.get(see));
                languageSpecificEntryPart.setSeeAlso(record.get(see_also));
                languageSpecificEntryPart.setCompare(record.get(compare));
                languageSpecificEntryPart.setReferencedLiterature(record.get(reference_literature));
			}
			
		});
		

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

            entry.languageSpecificParts().forEach(part -> {

                String language = part.getLanguage().name().toLowerCase();

                SKOSAnnotation prefLabelAnnotation = df.getSKOSAnnotation(df.getSKOSPrefLabelProperty().getURI(), part.getLabel(), language);
                SKOSAnnotationAssertion prefLabelAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, prefLabelAnnotation);
                changes.add(new AddAssertion(ds, prefLabelAnnotationAssertion));

                SKOSAnnotation definitionDP= df.getSKOSAnnotation(df.getSKOSDefinitionDataProperty().getURI(), part.getDefinition(), language);
                SKOSAnnotationAssertion definitionAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, definitionDP);
                changes.add(new AddAssertion(ds, definitionAnnotationAssertion));

                SKOSAnnotation definedByDP= df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"), part.getSee());
                SKOSAnnotationAssertion definedByAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, definedByDP);
                changes.add(new AddAssertion(ds, definedByAnnotationAssertion));

                SKOSAnnotation seeAlsoAnnotation = df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#seeAlso"), part.getSeeAlso());
                SKOSAnnotationAssertion seeAlsoAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, seeAlsoAnnotation);
                changes.add(new AddAssertion(ds, seeAlsoAnnotationAssertion));

                SKOSAnnotation relatedDP= df.getSKOSAnnotation(df.getSKOSRelatedProperty().getURI(), part.getCompare(), language);
                SKOSAnnotationAssertion relatedAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, relatedDP);
                changes.add(new AddAssertion(ds, relatedAnnotationAssertion));

            });


//            if (conceptsByLabel.containsKey(key)) {
//                String subTermLabel = record.get(subterm);
//                if (subTermLabel.isEmpty()) {
////						System.out.println(termID + " Double label: " + mainLabel + " (" + conceptsByLabel.get(key).getURI() + ")");
//                }
//            }
//
//            conceptsByLabel.put(key, concept);

        });





		
		mgr.applyChanges(changes);
		
		mgr.save(ds, SKOSFormatExt.RDFXML, URI.create("file:/tmp/glodmed-skos.rdf"));

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
