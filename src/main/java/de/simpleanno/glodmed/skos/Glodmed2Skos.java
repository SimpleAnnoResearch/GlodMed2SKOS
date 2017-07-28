/**
 * 
 */
package de.simpleanno.glodmed.skos;

import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.glossary;
import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.id;
import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.lang;
import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.mainterm;
import static de.simpleanno.glodmed.skos.Glodmed2Skos.CSVField.subterm;

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

import de.simpleanno.glodmed.GlodmedLanguageSpecificEntryPart;
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

import de.simpleanno.glodmed.Glodmed;
import de.simpleanno.glodmed.Glodmed.Glossary;
import de.simpleanno.glodmed.Glodmed.Language;
import de.simpleanno.glodmed.GlodmedEntry;

/**
 * @author ralph
 */
public class Glodmed2Skos {
	
	enum CSVField {id, lang, glossary, mainterm, subterm, definition, see, see_also, compare, figure_filename,
		figure_legend, figure_source, reference_literature, unique_test};
		
	private static final String BASE = "http://glodmed.simple-anno.de/";
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
	private int lastId;
	private GlodmedEntry currentEntry;

	private void transform(Reader in) throws IOException, SKOSException {

		CSVParser csvParser = CSVFormat.newFormat(',').withFirstRecordAsHeader().withQuote('"').parse(in);
		
//		csvParser.getHeaderMap().keySet().stream().forEach(header -> System.out.println(header));		
//		System.out.println("\n\n");
		
		Glodmed glodmed = Glodmed.instance();
		
//		HashMap<CompoundKey, SKOSConcept> conceptsByLabel = new HashMap<>();

		
		lastId = -1;

		csvParser.getRecords().stream().forEach(record -> {

			currentId = Integer.parseInt(record.get(id));
			
			if (currentId != lastId) {
				// new record

				currentEntry = new GlodmedEntry();
				glodmed.addEntry(currentEntry);

				lastId = currentId;

				Glossary glossary = Glossary.valueOf(record.get(CSVField.glossary));
				currentEntry.setGlossary(glossary);

			}


			String mainLabel = record.get(mainterm);
			
						
			// mainterm empty: bad record (should not happen)
			// mainterm set, subterm empty: regular concept
			// mainterm set, subterm set: subterm is concept name, mainterm is label of related concept
			
			if (!mainLabel.isEmpty()) {

				Language language = Language.valueOf(record.get(lang).toUpperCase());

				GlodmedLanguageSpecificEntryPart languageSpecificEntryPart = new GlodmedLanguageSpecificEntryPart(language);

				String subLabel = record.get(subterm);

				if (subLabel.isEmpty()) {
				} else {
					String properSubLabel = properSubtermLabel(mainLabel, subLabel);

					// for debugging

					System.out.format("%s, %s, %s\n", mainLabel, subLabel, properSubLabel);

//					List<GlodmedEntry> entriesWithSameMainLabel = glodmed.getEntriesByLabel(mainLabel, language);
//					if (entriesWithSameMainLabel.isEmpty()) {
//						System.out.format("Missing main term: %s\n", mainLabel);
//					}
					// end debugging

					glodmed.getEntriesByLabel(mainLabel, language).forEach(entry -> {
						if (entry.getGlossary() == currentEntry.getGlossary()) {

						}
					});
					
				}


				// strip markup
				mainLabel = stripMarkup(mainLabel);
				subLabel = stripMarkup(subLabel);
				

			}
			
		});
		
/*
		// construct SKOS ontology
		
		HashSet<SKOSConcept> concepts = new HashSet<>();		

		SKOSManager mgr = new SKOSManager();
		SKOSDataFactory df = mgr.getSKOSDataFactory();
		
		SKOSDataset ds = mgr.createSKOSDataset(URI.create(BASE));
		
        SKOSConceptScheme cs = df.getSKOSConceptScheme(uri("glodmed"));


		SKOSConcept concept = df.getSKOSConcept(uri(termID));

		if (!concepts.contains(concept)) {

			System.out.println("New: " + concept.getURI());

			// we encounter this id for the first time: create new concept with this id
			concepts.add(concept);
			changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(concept)));

			changes.add(new AddAssertion(ds, df.getSKOSObjectRelationAssertion(concept, df.getSKOSInSchemeProperty(), cs)));
		}




		if (conceptsByLabel.containsKey(key)) {
			String subTermLabel = record.get(subterm);
			if (subTermLabel.isEmpty()) {
//						System.out.println(termID + " Double label: " + mainLabel + " (" + conceptsByLabel.get(key).getURI() + ")");
			}
		}

		conceptsByLabel.put(key, concept);

		// preferred label
		SKOSAnnotation prefLabelAnnotation = df.getSKOSAnnotation(df.getSKOSPrefLabelProperty().getURI(), mainLabel, language);
		SKOSAnnotationAssertion prefLabelAnnotationAssertion = df.getSKOSAnnotationAssertion(concept, prefLabelAnnotation);
		changes.add(new AddAssertion(ds, prefLabelAnnotationAssertion));


		ArrayList<SKOSChange> changes = new ArrayList<>();
		changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(cs)));

		
		mgr.applyChanges(changes);
		
//		mgr.save(ds, SKOSFormatExt.RDFXML, URI.create("file:/tmp/glodmed-skos.rdf"));
*/
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
