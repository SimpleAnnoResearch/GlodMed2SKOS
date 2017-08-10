/**
 * 
 */
package de.simpleanno.glodmed;


/**
 * @author ralph
 */
public class GlodmedLanguageSpecificEntryPart {

	private String definition;
	private String see;
	private String seeAlso;
	private String compare;

    public String getSee() {
        return see;
    }

    public void setSee(String see) {
        this.see = see;
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        this.seeAlso = seeAlso;
    }

    public String getCompare() {
        return compare;
    }

    public void setCompare(String compare) {
        this.compare = compare;
    }

    public String getReferencedLiterature() {
        return referencedLiterature;
    }

    public void setReferencedLiterature(String referencedLiterature) {
        this.referencedLiterature = referencedLiterature;
    }

    private String referencedLiterature;

	private CompoundKey compoundKey;
	
	
    static class CompoundKey {
        Glossary glossary;
		private String term;
		Language lang;

		public CompoundKey(Glossary glossary, String term, Language lang) {
		    this.glossary = glossary;
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
			return o.glossary.equals(glossary) && o.term.equals(term) && o.lang.equals(lang);
		}
		
		@Override
		public int hashCode() {
			return 17 * term.hashCode() + 37 * lang.hashCode() + 53 * glossary.hashCode();
		}
	}

	public  GlodmedLanguageSpecificEntryPart(Glossary glossary, Language language) {
        this.compoundKey = new CompoundKey(glossary, null, language);
	}

	/**
	 * @deprecated We need to construct an empty entry part and fill it as we process the glodmed data.
	 * @param language
	 * @param label
	 * @param definition
	 */
	public GlodmedLanguageSpecificEntryPart(Glossary glossary, Language language, String label, String definition) {
		this.compoundKey = new CompoundKey(glossary, label, language);
		this.definition = definition;
	}


	/**
	 * @return the label
	 */
	public String getLabel() {
		return compoundKey.term;
	}

	public void setLabel(String label) {
        this.compoundKey.term = label;
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
		return compoundKey.lang;
	}


	/**
	 * @return the compoundKey
	 */
	public CompoundKey getCompoundKey() {
		return compoundKey;
	}
	
	

}
