package de.simpleanno.glodmed.owl;

import de.simpleanno.glodmed.Glodmed;
import de.simpleanno.glodmed.GlodmedOutputGenerator;
import de.simpleanno.glodmed.Glossary;
import de.simpleanno.glodmed.Language;
import org.apache.commons.text.StringEscapeUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GlodmedOwlOutputGenerator extends GlodmedOutputGenerator {

    private static final IRI GLODMED_CLASS_IRI = IRI.create("http://simple-anno.de/ontologies/dental_care_process#GLODMED");

    private HashSet<OWLClass> classes = new HashSet<>();

    private OWLOntologyManager mgr;
    private OWLDataFactory df;
    private OWLOntology ontology;

    private OWLClass glodmedClass;

    private HashMap<Glossary, OWLNamedIndividual> glossaries = new HashMap<>();

    private ArrayList<OWLOntologyChange> changes = new ArrayList<>();

    private OWLAnnotationProperty glossaryProp = df.getOWLAnnotationProperty(iri("glossary"));

    private OWLClass clazz;

    public GlodmedOwlOutputGenerator(Glodmed glodmed) throws Exception {
        super(glodmed);

        mgr = OWLManager.createOWLOntologyManager();
        df = mgr.getOWLDataFactory();
        ontology = mgr.createOntology(iri(""));

        glodmedClass = df.getOWLClass(GLODMED_CLASS_IRI);

        glossaries.put(Glossary.GOMI, df.getOWLNamedIndividual(iri( "GOMI")));
        glossaries.put(Glossary.GOOT, df.getOWLNamedIndividual(iri( "GOOT")));

        changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(glossaries.get(Glossary.GOMI))));
        changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(glossaries.get(Glossary.GOOT))));
    }

    @Override
    public void addTerm(String termID, Glossary glossary) {
        clazz = df.getOWLClass(iri(termID));

        changes.add(new AddAxiom(ontology, df.getOWLDeclarationAxiom(clazz)));
        changes.add(new AddAxiom(ontology, df.getOWLSubClassOfAxiom(clazz, glodmedClass)));
        changes.add(new AddAxiom(ontology, df.getOWLAnnotationAssertionAxiom(clazz.getIRI(), df.getOWLAnnotation(glossaryProp, glossaries.get(glossary).getIRI()))));
    }

    @Override
    public void addLabel(String label, Language language) {
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), clazz.getIRI(), df.getOWLLiteral(label, language.isoCode));
        changes.add(new AddAxiom(ontology, ax));
    }

    @Override
    public void addDefinition(String definition, Language language) {
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#definition")), clazz.getIRI(), df.getOWLLiteral(StringEscapeUtils.escapeXml10(definition), language.isoCode));
        changes.add(new AddAxiom(ontology, ax));
    }

    @Override
    public void addSuperTermRelation(String superTermID) {
        OWLSubClassOfAxiom subClassAxiom = df.getOWLSubClassOfAxiom(clazz, df.getOWLClass(iri(superTermID)));
        changes.add(new AddAxiom(ontology, subClassAxiom));
    }

    @Override
    public void addSeeRelation(String destinationID) {
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSIsDefinedBy(), clazz.getIRI(), iri(destinationID));
        changes.add(new AddAxiom(ontology, ax));
    }

    @Override
    public void addSeeAlsoRelation(String destinationID) {
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getRDFSSeeAlso(), clazz.getIRI(), iri(destinationID));
        changes.add(new AddAxiom(ontology, ax));
    }

    @Override
    public void addCompareRelation(String destinationID) {
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#related")), clazz.getIRI(), iri(destinationID));
        changes.add(new AddAxiom(ontology, ax));
    }

    @Override
    public void saveOutput(String destination) throws Exception {
        mgr.applyChanges(changes);
        mgr.saveOntology(ontology, new OWLFunctionalSyntaxOntologyFormat(), iri(destination));
    }

    private IRI iri(String localname) {
        return IRI.create(BASE + "#" + localname);
    }

}
