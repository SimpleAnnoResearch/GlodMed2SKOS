/**
 * 
 */
package de.simpleanno.glodmed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import de.simpleanno.glodmed.GlodmedLanguageSpecificEntryPart.CompoundKey;

/**
 * @author ralph
 */
public class Glodmed {
	
	public enum Glossary{GOMI, GOOT}
	
	public 	enum Language {EN, DE, FR, IT, PO, SP};

	private HashMap<Integer, GlodmedEntry> entriesByID = new HashMap<>();
	private HashMap<CompoundKey, ArrayList<GlodmedEntry>> entriesByLabel = new HashMap<>();
	
	private static Glodmed instance;
	
	public static Glodmed instance() {
		if (instance == null) {
			instance = new Glodmed();
		}
		return instance;
	}

	/**
	 * 
	 */
	private Glodmed() {
		// TODO Auto-generated constructor stub
	}
	
	public Optional<GlodmedEntry> getEntryByID(int id) {
		return Optional.ofNullable(entriesByID.get(id));
	}
	
	public List<GlodmedEntry> getEntriesByLabel(String term, Language language) {
		CompoundKey key = new CompoundKey(term, language);
		ArrayList<GlodmedEntry> entries = entriesByLabel.get(key);
		if (entries == null) {
			entries = new ArrayList<>();
			entriesByLabel.put(key, entries);
		}
		return entries;
	}
	
	public void addEntry(GlodmedEntry entry) {
		entriesByID.put(entry.getId(), entry);
		entry.languageSpecificParts().forEach(part -> getEntriesByLabel(part.getLabel(), part.getLanguage()).add(entry));
	}

}
