/**
 * 
 */
package de.simpleanno.glodmed;

import de.simpleanno.glodmed.Glodmed.Language;

/**
 * @author ralph
 */
public class GlodmedLanguageSpecificEntryPart {

	private final Language language;
	private String label;
	private String definition;
	private CompoundKey compoundKey;
	
	
    static class CompoundKey {
		private String term;
		Language lang;

		public CompoundKey(String term, Language lang) {
			this.term = term;
			this.lang = lang;
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof CompoundKey))
				return false;
			if (other == this)
				return true;
			CompoundKey o = (CompoundKey)other;
			return o.term.equals(term) && o.lang.equals(lang);
		}
		
		@Override
		public int hashCode() {
			return 17 * term.hashCode() + 37 * lang.hashCode(); 
		}
	}

	public  GlodmedLanguageSpecificEntryPart(Language language) {
    	this.language = language;
	}

	/**
	 * @deprecated We need to construct an empty entry part and fill it as we process the glodmed data.
	 * @param language
	 * @param label
	 * @param definition
	 */
	public GlodmedLanguageSpecificEntryPart(Language language, String label, String definition) {
		this.language = language;
		this.label = label;
		this.compoundKey = new CompoundKey(label, language);
		this.definition = definition;
	}


	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
        this.label = label;
        this.compoundKey = new CompoundKey(label, language);
	}

	/**
	 * @return the definition
	 */
	public String getDefinition() {
		return definition;
	}

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
	 * @return the language
	 */
	public Language getLanguage() {
		return language;
	}


	/**
	 * @return the compoundKey
	 */
	public CompoundKey getCompoundKey() {
		return compoundKey;
	}
	
	

}
