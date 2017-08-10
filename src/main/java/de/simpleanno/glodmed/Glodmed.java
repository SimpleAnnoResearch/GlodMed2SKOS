/**
 * 
 */
package de.simpleanno.glodmed;

import java.util.*;

import de.simpleanno.glodmed.GlodmedLanguageSpecificEntryPart.CompoundKey;

/**
 * @author ralph
 */
public class Glodmed implements Iterable<GlodmedEntry> {
	
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
	
	public List<GlodmedEntry> getEntriesByLabel(Glossary glossary, String term, Language language) {
		CompoundKey key = new CompoundKey(glossary, term, language);
		ArrayList<GlodmedEntry> entries = entriesByLabel.get(key);
		if (entries == null) {
			entries = new ArrayList<>();
			entriesByLabel.put(key, entries);
		}
		return entries;
	}
	
	public void addEntry(GlodmedEntry entry) {
		entriesByID.put(entry.getId(), entry);
		entry.languageSpecificParts().forEach(part -> getEntriesByLabel(entry.getGlossary(), part.getLabel(), part.getLanguage()).add(entry));
	}

    @Override
    public Iterator<GlodmedEntry> iterator() {
        return entriesByID.values().iterator();
    }
}
