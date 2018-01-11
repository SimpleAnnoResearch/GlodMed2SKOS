package de.simpleanno.glodmed;


import java.io.File;

public abstract class GlodmedOutputGenerator {

    protected Glodmed glodmed;

    public GlodmedOutputGenerator(Glodmed glodmed) throws Exception {
        this.glodmed = glodmed;
    }

    protected static final String BASE = "http://glodmed.simple-anno.de/glodmed";

    public abstract void addTerm(String termID, Glossary glossary);

    public abstract void addLabel(String label, Language language);

    public abstract void addDefinition(String definition, Language language);


    public abstract void addSuperTermRelation(String superTermID);

    public abstract void addSeeRelation(String destinationID);

    public abstract void addSeeAlsoRelation(String destinationID);

    public abstract void addCompareRelation(String destinationID);


    public abstract void saveOutput(File destination) throws Exception;

}
