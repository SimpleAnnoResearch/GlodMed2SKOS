package de.simpleanno.glodmed.skos;

import de.simpleanno.glodmed.Glodmed;
import org.semanticweb.skosapibinding.SKOSFormatExt;

import java.io.File;

public class GlodmedSkosRdfOutputGenerator extends GlodmedSkosOutputGenerator {

    public GlodmedSkosRdfOutputGenerator(Glodmed glodmed) throws Exception {
        super(glodmed);
    }

    @Override
    public void saveOutput(File destination) throws Exception {
        mgr.applyChanges(changes);
        mgr.save(ds, SKOSFormatExt.RDFXML, destination.toURI());
    }
}
