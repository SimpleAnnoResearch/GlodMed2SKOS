package de.simpleanno.glodmed.skos;

import de.simpleanno.glodmed.Glodmed;
import org.semanticweb.skosapibinding.SKOSFormatExt;

import java.io.OutputStream;
import java.net.URI;

public class GlodmedSkosRdfOutputGenerator extends GlodmedSkosOutputGenerator {

    public GlodmedSkosRdfOutputGenerator(Glodmed glodmed) throws Exception {
        super(glodmed);
    }

    @Override
    public void saveOutput(String destination) throws Exception {
        mgr.applyChanges(changes);
        mgr.save(ds, SKOSFormatExt.RDFXML, URI.create("file:"+ destination));
    }
}
