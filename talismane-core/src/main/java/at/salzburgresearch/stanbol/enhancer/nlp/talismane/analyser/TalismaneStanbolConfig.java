package at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullWriter;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.fr.TalismaneFrench;
import com.joliciel.talismane.posTagger.PosTaggerLexicon;


/**
 * Special  {@link TalismaneStanbolConfig} that allows to reset content to be analysed
 * by the {@link #setAnalyzedText(AnalysedText)} method. The intension is to 
 * have multiple {@link TalismaneStanbolConfig} instances and manage them in an
 * {@link ResourcePool}.
 * 
 * @author Rupert Westenthaler
 *
 */
public class TalismaneStanbolConfig extends TalismaneFrench{

    private final Logger log = LoggerFactory.getLogger(TalismaneStanbolConfig.class);
    
    /**
     * Use a single lexicon for all threads
     */
    private final PosTaggerLexicon posTaggerLexicon;
    
    /**
     * The writer is not used
     */
    private Writer nullWriter = new NullWriter();

    private final AnalysedTextFactory analysedTextFactory;

    /**
     * This map does hold the command line arguments used by the 
     * {@link TalismaneStanbolConfig}. Sets the '<code>command</code>' to
     * '<code>analyse</code>' and '<code>newline</code>' to '<code>SPACE</code>'
     */
    private static final Map<String,String> config;
    static {
        config = new HashMap<String,String>();
        config.put("command", "analyse");
        config.put("newline", "SPACE");
    }
    
    /**
     * Create a new {@link TalismaneFrench} configuration customised for the use with
     * the Stanbol NLP processing module. As this constructor also initialises
     * the {@link PosTaggerLexicon} calling this will take several seconds.
     * @throws Exception On any error while initialising
     */
    public TalismaneStanbolConfig(AnalysedTextFactory atf) throws Exception {
        super(config);
        this.analysedTextFactory = atf;
        log.info("> init POS Tagger Lexicon ...");
        long start = System.currentTimeMillis();
        this.posTaggerLexicon = super.getDefaultLexiconService();
        log.info("  ... done in {}ms",System.currentTimeMillis()-start);
    }

    
    /**
     * Content is parsed by using the {@link TalismaneAnalyzer#analyse(AnalysedText)}
     * method and NOT via the configuration. Because of that this throws an
     * {@link UnsupportedOperationException}
     * @throws UnsupportedOperationException reading the content via the
     * configuration is not supported. Especially because a single config is
     * used by manny threads.
     */
    @Override
    public Reader getReader() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This Config does not support" +
        		"reading the data. It is intended to be used together with the" +
        		"TalismaneAnalyzer#analyze(..) method that processes the text" +
        		"parsd to this method.");
    }
    /**
     * This feature is not used. THerefore this returns a {@link NullWriter}
     */
    @Override
    public Writer getWriter() {
        return nullWriter;
    }
    
    /**
     * Always returns the same {@link PosTaggerLexicon} to avoid multiple
     * instantiation for different Threads. Basically a workaround for
     * {@link TalismaneSession#getLexicon()}
     */
    @Override
    public PosTaggerLexicon getDefaultLexiconService() {
        return posTaggerLexicon;
    }
    
    /**
     * Reads the rules of the parent implementations and adds two additional one
     * that converts multiple line breaks to a single space and also marks
     * multiple line breaks as sentence breaks
     */
    @Override
    public InputStream getDefaultTextMarkerFiltersFromStream() {
        try {
            StringBuilder config = new StringBuilder(IOUtils.toString(super.getDefaultTextMarkerFiltersFromStream(),"UTF-8"));
            config.append('\n');
            config.append("RegexMarkerFilter\tSKIP,SENTENCE_BREAK\t(\\r\\n|[\\r\\n]){2}\t0");
            //config.append("RegexMarkerFilter\tSPACE\t[^-\\r\\n](\\r\\n|[\\r\\n])\t1");
            //replace multiple spaces with SPACE
            //config.append("RegexMarkerFilter\tSPACE\t\\s{2,}\t0");
            return new ByteArrayInputStream(config.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            log.warn("Unable to apply additional RegexMarkerFilter",e);
            return super.getDefaultTextMarkerFiltersFromStream();
        }
    }


    /**
     * @return the analysedTextFactory
     */
    public AnalysedTextFactory getAnalysedTextFactory() {
        return analysedTextFactory;
    }
}
