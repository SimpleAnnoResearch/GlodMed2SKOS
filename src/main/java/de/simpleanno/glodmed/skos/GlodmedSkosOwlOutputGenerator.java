package de.simpleanno.glodmed.skos;

import de.simpleanno.glodmed.Glodmed;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.skosapibinding.SKOStoOWLConverter;

import java.io.File;

public class GlodmedSkosOwlOutputGenerator extends GlodmedSkosOutputGenerator {

    public GlodmedSkosOwlOutputGenerator(Glodmed glodmed) throws Exception {
        super(glodmed);
    }

    @Override
    public void saveOutput(File destination) throws Exception {
        mgr.applyChanges(changes);

        SKOStoOWLConverter converter = new SKOStoOWLConverter();
        OWLOntology onto = converter.getAsOWLOntology(ds);
        OWLOntologyManager man = onto.getOWLOntologyManager();
        try {
            man.saveOntology(onto, new OWLFunctionalSyntaxOntologyFormat(), IRI.create(destination));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }

    }
}
