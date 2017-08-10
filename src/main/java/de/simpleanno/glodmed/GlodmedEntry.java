/**
 * 
 */
package de.simpleanno.glodmed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author ralph
 */
public class GlodmedEntry {

	private int id;
	private HashMap<Language, GlodmedLanguageSpecificEntryPart> languageSpecificParts = new HashMap<>();
	
	private Glossary glossary;
	
	private ArrayList<Integer> seeRefs = new ArrayList<>();
	private ArrayList<Integer> seeAlsoRefs = new ArrayList<>();
	private ArrayList<Integer> compareRefs = new ArrayList<>();
	
	
	/**
	 * 
	 */
	public GlodmedEntry(int id, Glossary glossary) {
		this.id = id;
		this.glossary = glossary;
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}


	/**
	 * @return the languageSpecificParts
	 */
	public Optional<GlodmedLanguageSpecificEntryPart> getLanguageSpecificPart(Language language) {
		return Optional.ofNullable(languageSpecificParts.get(language));
	}
	
	public Stream<GlodmedLanguageSpecificEntryPart> languageSpecificParts() {
		return languageSpecificParts.values().stream();
	}


	/**
	 * @param languageSpecificPart the languageSpecificParts to set
	 */
	public void addLanguageSpecificPart(GlodmedLanguageSpecificEntryPart languageSpecificPart) {
		languageSpecificParts.put(languageSpecificPart.getLanguage(), languageSpecificPart);
	}


	/**
	 * @return the glossary
	 */
	public Glossary getGlossary() {
		return glossary;
	}


	/**
	 * @param glossary the glossary to set
	 */
	public void setGlossary(Glossary glossary) {
		this.glossary = glossary;
	}


	/**
	 * @return the seeRef
	 */
	public List<Integer> getSeeRefs() {
		return seeRefs;
	}


	/**
	 * @param seeRef the seeRef to set
	 */
	public void addSeeRef(int seeRef) {
		seeRefs.add(seeRef);
	}


	/**
	 * @return the seeAlsoRef
	 */
	public List<Integer> getSeeAlsoRefs() {
		return seeAlsoRefs;
	}


	/**
	 * @param seeAlsoRef the seeAlsoRef to set
	 */
	public void addSeeAlsoRef(int seeAlsoRef) {
		this.seeAlsoRefs.add(seeAlsoRef);
	}

	/**
	 * @return the compareRefs
	 */
	public List<Integer> getCompareRefs() {
		return compareRefs;
	}


	/**
	 * @param compareRef the compareRef to set
	 */
	public void addCompareRef(int compareRef) {
		this.compareRefs.add(compareRef);
	}

}
