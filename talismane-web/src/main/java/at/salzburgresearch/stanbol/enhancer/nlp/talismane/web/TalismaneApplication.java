package at.salzburgresearch.stanbol.enhancer.nlp.talismane.web;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.enhancer.nlp.json.writer.AnalyzedTextWriter;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.web.reader.BlobReader;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.web.resource.AnalysisResource;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.web.resource.MainResource;

public class TalismaneApplication extends Application {
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(
            AnalyzedTextWriter.class, BlobReader.class, MainResource.class,
            AnalysisResource.class));
    }

}
