package de.simpleanno.glodmed.skos;

import de.simpleanno.glodmed.Glodmed;
import de.simpleanno.glodmed.GlodmedOutputGenerator;
import de.simpleanno.glodmed.Glossary;
import de.simpleanno.glodmed.Language;
import org.apache.commons.text.StringEscapeUtils;
import org.semanticweb.skos.*;
import org.semanticweb.skosapibinding.SKOSManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class GlodmedSkosOutputGenerator extends GlodmedOutputGenerator {

    HashSet<SKOSConcept> concepts = new HashSet<>();

    SKOSManager mgr;
    SKOSDataFactory df;
    SKOSDataset ds;

    HashMap<Glossary, SKOSConceptScheme> conceptSchemes = new HashMap<>();

    ArrayList<SKOSChange> changes = new ArrayList<>();

    private SKOSConcept currentConcept;

    public GlodmedSkosOutputGenerator(Glodmed glodmed) throws Exception {
        super(glodmed);

        mgr = new SKOSManager();
        df = mgr.getSKOSDataFactory();
        ds = mgr.createSKOSDataset(URI.create(BASE));

        conceptSchemes.put(Glossary.GOMI, df.getSKOSConceptScheme(uri("gomi")));
        conceptSchemes.put(Glossary.GOOT, df.getSKOSConceptScheme(uri("goot")));

        changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(conceptSchemes.get(Glossary.GOMI))));
        changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(conceptSchemes.get(Glossary.GOOT))));
    }

    @Override
    public void addTerm(String termID, Glossary glossary) {
        currentConcept = df.getSKOSConcept(uri(termID));
        changes.add(new AddAssertion(ds, df.getSKOSEntityAssertion(currentConcept)));
        changes.add(new AddAssertion(ds, df.getSKOSObjectRelationAssertion(currentConcept, df.getSKOSInSchemeProperty(), conceptSchemes.get(glossary))));
    }

    @Override
    public void addLabel(String label, Language language) {
        SKOSAnnotation prefLabelAnnotation = df.getSKOSAnnotation(df.getSKOSPrefLabelProperty().getURI(), label, language.isoCode);
        SKOSAnnotationAssertion prefLabelAnnotationAssertion = df.getSKOSAnnotationAssertion(currentConcept, prefLabelAnnotation);
        changes.add(new AddAssertion(ds, prefLabelAnnotationAssertion));
    }

    @Override
    public void addDefinition(String definition, Language language) {
        SKOSAnnotation definitionDP= df.getSKOSAnnotation(df.getSKOSDefinitionDataProperty().getURI(), StringEscapeUtils.escapeXml10(definition).trim(), language.isoCode);
        SKOSAnnotationAssertion definitionAnnotationAssertion = df.getSKOSAnnotationAssertion(currentConcept, definitionDP);
        changes.add(new AddAssertion(ds, definitionAnnotationAssertion));
    }

    @Override
    public void addSuperTermRelation(String superTermID) {
        SKOSObjectRelationAssertion broaderRelationAssertion = df.getSKOSObjectRelationAssertion(currentConcept, df.getSKOSBroaderProperty(), df.getSKOSConcept(uri(superTermID)));
        changes.add(new AddAssertion(ds, broaderRelationAssertion));
    }

    @Override
    public void addSeeRelation(String destinationID) {
        SKOSAnnotation definedByDP = df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"), df.getSKOSConcept(uri(destinationID)));
        SKOSAnnotationAssertion definedByAnnotationAssertion = df.getSKOSAnnotationAssertion(currentConcept, definedByDP);
        changes.add(new AddAssertion(ds, definedByAnnotationAssertion));
    }

    @Override
    public void addSeeAlsoRelation(String destinationID) {
        SKOSAnnotation seeAlsoAnnotation = df.getSKOSAnnotation(URI.create("http://www.w3.org/2000/01/rdf-schema#seeAlso"), df.getSKOSConcept(uri(destinationID)));
        SKOSAnnotationAssertion seeAlsoAnnotationAssertion = df.getSKOSAnnotationAssertion(currentConcept, seeAlsoAnnotation);
        changes.add(new AddAssertion(ds, seeAlsoAnnotationAssertion));
    }

    @Override
    public void addCompareRelation(String destinationID) {
        SKOSAnnotation relatedDP= df.getSKOSAnnotation(df.getSKOSRelatedProperty().getURI(), df.getSKOSConcept(uri(destinationID)));
        SKOSAnnotationAssertion relatedAnnotationAssertion = df.getSKOSAnnotationAssertion(currentConcept, relatedDP);
        changes.add(new AddAssertion(ds, relatedAnnotationAssertion));
    }

    protected URI uri (String localname) {
        return URI.create(BASE + "#" + localname);
    }

}
