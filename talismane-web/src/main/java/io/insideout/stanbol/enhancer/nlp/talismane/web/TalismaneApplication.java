package io.insideout.stanbol.enhancer.nlp.talismane.web;

import io.insideout.stanbol.enhancer.nlp.talismane.web.reader.BlobReader;
import io.insideout.stanbol.enhancer.nlp.talismane.web.resource.AnalysisResource;
import io.insideout.stanbol.enhancer.nlp.talismane.web.resource.MainResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.enhancer.nlp.json.writer.AnalyzedTextWriter;

public class TalismaneApplication extends Application {
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(
            AnalyzedTextWriter.class, BlobReader.class, MainResource.class,
            AnalysisResource.class));
    }

}
