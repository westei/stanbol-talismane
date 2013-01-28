package at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.mappings.TagSetRegistry;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;

public class TalismaneAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(TalismaneAnalyzer.class);

    private static final int MIN_BLOCK_SIZE = 1000;
    
    private static final String INIT_TEXT = "Fin de la crise suscitée par la prise d’otages dans le Sahara Algérien.";
    
    protected final FilterService filterService;
    protected final char endBlockCharacter;
    protected final Iterable<TextMarkerFilter> textMarkerFilters;
    protected final SentenceDetector sentenceDetector;
    protected final PosTagger posTagger;
    protected final Tokeniser tokenizer;
    protected final TagSet<PosTag> posTagSet;
    protected final Map<String,PosTag> adhocPosTags;
    protected final AnalysedTextFactory analysedTextFactory;
    
    private final ExecutorService executor;
    
    public TalismaneAnalyzer(TalismaneStanbolConfig config, ExecutorService executor) {
        this.analysedTextFactory = config.getAnalysedTextFactory();
        //this.config = config;
        this.filterService = config.getFilterService();
        this.posTagger = config.getPosTagger();
        this.tokenizer = config.getTokeniser();
        this.endBlockCharacter = config.getEndBlockCharacter();
        this.textMarkerFilters = config.getTextMarkerFilters();
        this.sentenceDetector = config.getSentenceDetector();
        this.executor = executor;
        TagSetRegistry tagSetRegistry = TagSetRegistry.getInstance();
        posTagSet = tagSetRegistry.getPosTagSet("fr");
        adhocPosTags = tagSetRegistry.getAdhocPosTagMap("fr");
        init();
    }
    /**
     * Because Talismane uses lazzy initialisation for all components and this
     * initialization is not thread save. Because of that we need to call
     * everything once during initialization.
     */
    private void init(){
        log.info("Initialise Talismane Analyzer Chain");
        Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
        for (TextMarkerFilter textMarkerFilter : textMarkerFilters) {
            Set<TextMarker> result = textMarkerFilter.apply("", INIT_TEXT, "");
            textMarkers.addAll(result);
        }
        log.info("  ... Text Marker Filters initialised");
        RollingSentenceProcessor rollingSentenceProcessor = 
                filterService.getRollingSentenceProcessor("",true);
        SentenceHolder sentenceHolder = rollingSentenceProcessor.addNextSegment(
            INIT_TEXT, textMarkers);
        List<Integer> sentenceBreaks = sentenceDetector.detectSentences(
            "", INIT_TEXT, "");
        log.info("  ... sentence detector initialised");
        for (int sentenceBreak : sentenceBreaks) {
            sentenceHolder.addSentenceBoundary(sentenceBreak);
        }
        List<Sentence> sentences = sentenceHolder.getDetectedSentences(null);
        //Tokenize the sentence
        TokenSequence ts = tokenizer.tokenise(sentences.get(0)).get(0);
        log.info("  ... tokenizer initialised");
        //POS Tag the tokens
        posTagger.tagSentence(ts);
        log.info("  ... POS Tagger initialised");
    }

    public AnalysedText analyse(Blob blob) throws IOException {
        AnalysedText at = analysedTextFactory.createAnalysedText(blob);
        CharSequence cs = at.getText();
        LinkedList<TextSegment> textSegments = new LinkedList<TextSegment>();
        
        Map<Sentence, Future<?>> sentences = new LinkedHashMap<Sentence,Future<?>>();
        // NOTE: This seams to be a light weight component created for each request
        Sentence leftover = null;
        int leftoverOffset = -1;
        // prime the sentence detector with two text segments, to ensure everything gets processed
        textSegments.addLast(new TextSegment(0, ""));
        textSegments.addLast(new TextSegment(0, ""));

        boolean finished = false;

        //String prevProcessedText = "";
        //String processedText = "";
        //String nextProcessedText = "";
        SentenceHolder prevSentenceHolder = null;
        int startIndex = 0;
        //we need to cut whitespace and line breaks at the end to avoid
        //java.lang.IndexOutOfBoundsException during POS tagging because of
        //an sentence containing an whitespace at the end
        int length = 0;
        for(int charIndex = cs.length()-1; charIndex >=0; charIndex--){
            char c = cs.charAt(charIndex);
            if(!Character.isWhitespace(c)){
                length = charIndex+1;
                break;
            }
        }
        for (int charIndex = 0; charIndex < length; charIndex++) {
            // read characters from the reader, one at a time
            char c = cs.charAt(charIndex);
            if(charIndex == length-1){
                finished = true;
            }
            
            if (finished || (Character.isWhitespace(c) && (charIndex-startIndex) > MIN_BLOCK_SIZE)
                    || c == endBlockCharacter) {
                if (startIndex < charIndex) {
                    textSegments.add(new TextSegment(startIndex, 
                        cs.subSequence(startIndex, charIndex+1).toString()));
                    startIndex = charIndex+1;
                } // is the current block > 0 characters?
                if (c == endBlockCharacter) {
                    textSegments.addLast(new TextSegment(charIndex+1, ""));
                }
                if (finished) {
                    TextSegment emptySegment = new TextSegment(charIndex+1, "");
                    textSegments.addLast(emptySegment);
                    textSegments.addLast(emptySegment);
                    textSegments.addLast(emptySegment);
                }
                
            } // is there a next block available?

            while (textSegments.size() >= 3) {
                TextSegment prevText = textSegments.removeFirst();
                TextSegment text = textSegments.removeFirst();
                TextSegment nextText = textSegments.removeFirst();
                log.trace("prevText: {}", prevText);
                log.trace("text: {}", text);
                log.trace("nextText: {}", nextText);

                Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
                for (TextMarkerFilter textMarkerFilter : textMarkerFilters) {
                    Set<TextMarker> result = textMarkerFilter.apply(prevText.segment, text.segment, nextText.segment);
                    textMarkers.addAll(result);
                }
                // push the text segments back onto the beginning of Deque
                textSegments.addFirst(nextText);
                textSegments.addFirst(text);
                RollingSentenceProcessor rollingSentenceProcessor = 
                        filterService.getRollingSentenceProcessor("",true);
                SentenceHolder sentenceHolder = rollingSentenceProcessor.addNextSegment(
                    text.segment, textMarkers);
                //prevProcessedText = processedText;
                //processedText = nextProcessedText;
                nextText.processed = sentenceHolder.getText();

                log.trace("prevProcessedText: {}", prevText.processed);
                log.trace("processedText: {}", text.processed);
                log.trace("nextProcessedText: {}", nextText.processed);

                boolean reallyFinished = finished && textSegments.size() == 3;

                if (prevSentenceHolder != null) {
                    List<Integer> sentenceBreaks = sentenceDetector.detectSentences(
                        prevText.processed, text.processed, nextText.processed);
                    for (int sentenceBreak : sentenceBreaks) {
                        prevSentenceHolder.addSentenceBoundary(sentenceBreak);
                    }

                    List<Sentence> theSentences = prevSentenceHolder.getDetectedSentences(leftover);
                    leftover = null;
                    for (Sentence sentence : theSentences) {
                        if (sentence.isComplete() || reallyFinished) {
                            final int startOffset;
                            if(leftoverOffset >= 0){
                                startOffset = leftoverOffset;
                                leftoverOffset = -1;
                            } else {
                                startOffset = prevText.offset;
                            }
                            int sentStart = sentence.getOriginalIndex(0);
                            //Talismane uses indexes, Stanbol uses length. Therefore the -1 & +1
                            int sentEnd = sentence.getOriginalIndex(sentence.getText().length()-1)+1;
                            log.trace("Sentence[{}+{}={},{}+{}={}]",new Object[]{
                                    startOffset, sentStart, startOffset+sentStart,
                                    prevText.offset, sentEnd,prevText.offset+sentEnd});
                            log.trace(" > Sentence  :{} ",sentence.getText());
                            log.trace(" > SubString :{}... ",cs.subSequence(startOffset+sentStart, prevText.offset+sentEnd));
                            if(!reallyFinished){
                                sentences.put(sentence, executor.submit(new SentenceProcessor(
                                    startOffset, prevText.offset, sentence, at)));
                            } else { //process the last sentence directly in this thread
                                processSentence(startOffset, prevText.offset, sentence, at);
                            }
                        } else {
                            log.trace("Setting leftover to: " + sentence.getText());
                            leftover = sentence;
                            leftoverOffset = prevText.offset;
                        }
                    }
                }
                prevSentenceHolder = sentenceHolder;
            } // we have at least 3 text segments (should always be the case once we get started)
        }
        int i=0;
        //waited for all sentenced to be processed -> analysis competed
        for(Entry<Sentence,Future<?>> sentence : sentences.entrySet()){
            i++;
            try {
                sentence.getValue().get();
            } catch (ExecutionException ee) {
                Throwable e = ee.getCause();
                if(e instanceof RuntimeException){
                    throw RuntimeException.class.cast(e);
                } else {
                    throw new IllegalStateException("Exception while processing the "
                        + i +"Sentence: '"+sentence.getKey()+"'!", e);
                }
            } catch (CancellationException e) {
                throw new IllegalStateException("Aanlyzer was shutdown while procesing "
                    + "the parsed Text", e);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Aanlyzer was interupped while procesing "
                        + "the parsed Text", e);
            }
        }
        return at;
    }
    /**
     * Tokenizes and POS tags the parsed {@link Sentence} and stores analysis
     * resuts in the {@link AnalysedText};
     * @param offset the offset for the indexes used by the sentence relative to 
     * the start of the whole text
     * @param sentence the sentence to analyse
     * @param at the {@link AnalysedText} to store the results of the analysis
     */
    protected void processSentence(final int startOffset, final int endOffset, Sentence sentence, AnalysedText at){
        log.debug("process Sentence [startOffset: {}, endOffset {}]'{}'", new Object[]{
                startOffset, endOffset,sentence});
        int sentStart = startOffset + sentence.getOriginalIndex(0);
        //Talismane uses indexes, Stanbol uses length. Therefore the -1 & +1
        int sentEnd = endOffset + sentence.getOriginalIndex(sentence.getText().length()-1)+1;
        log.trace("Sentence[{},{}]",sentStart,sentEnd);
        log.trace("sent     : {}",sentence.getText());
        //log.debug("subString: {}",cs.subSequence(start, end));
        synchronized (at) {
            at.addSentence(sentStart, sentEnd);
        }
        
        //Tokenize the sentence
        TokenSequence ts = tokenizer.tokenise(sentence).get(0);
        //POS Tag the tokens
        PosTagSequence pts;
        try {
            pts = posTagger.tagSentence(ts);
        } catch (Exception e) {
            log.warn("Exception while POS tagging Sentence: '"+sentence.getText()+"'!",e);
            //TODO write tokens without POS tags
            return;
        }
        //process the results
        int offset = startOffset;
        int lastIndex = -1;
        int index;
        StringBuilder m = null;
        for(int i=0;i<pts.size();i++){
            PosTaggedToken ptt = pts.get(i);
            if(log.isTraceEnabled()){
                m = new StringBuilder("> Token [");
            }
            Token token = ptt.getToken();
            //we need to check the indexes, because the offset may change within
            //the sentence :(
            index = sentence.getOriginalIndex(token.getStartIndex());
            if(index < lastIndex) { //border between start and endoffset
                offset = endOffset;
            }
            int tStart = offset + index;
            if(log.isTraceEnabled()){
                m.append(offset).append('+').append(index).append('=').append(tStart).append(',');
            }
            lastIndex = index;
            index = sentence.getOriginalIndex(token.getEndIndex()-1)+1;
            if(index < lastIndex){
                offset = endOffset;
            }
            int tEnd = offset + index; 
            if(log.isTraceEnabled()){
                m.append(offset).append('+').append(index).append('=').append(tEnd).append(']');
                m.append('\'').append(token.getText()).append('\'').append(": ");
            }
            lastIndex = index;
            org.apache.stanbol.enhancer.nlp.model.Token stanbolToken;
            synchronized (at) {
                stanbolToken = at.addToken(tStart, tEnd);
            }
            PosTag posTag = getPostTag(ptt.getDecision().getOutcome().getCode());
            double prob = ptt.getDecision().getProbability();
            Value<PosTag> posValue;
            if(prob >= 0){
                posValue = Value.value(posTag, prob);
            } else {
                posValue = Value.value(posTag);
            }
            if(log.isTraceEnabled()){
                m.append(posValue);
                log.trace(m.toString());
            }
            stanbolToken.addAnnotation(POS_ANNOTATION, posValue);
        }
    }
    
    /**
     * Getter for the {@link PosTag} based on the string tag provided by
     * Talismane
     * @param tag the string tag
     * @return the {@link PosTag} instance
     */
    protected PosTag getPostTag(String tag) {
        PosTag posTag = posTagSet != null ? posTagSet.getTag(tag) : null;
        if(posTag == null){
            posTag = adhocPosTags.get(tag);
            if(posTag == null) {
                log.warn("Unmapped POS tag '{}' for language 'fr' and Tagset '{}'",
                    new Object[]{tag, posTagSet != null ? posTagSet.getName() : "<<none>>"});
                posTag = new PosTag(tag);
                adhocPosTags.put(tag, posTag);
            }
        }
        return posTag;
    }
    /**
     * Allows to use an {@link ExecutorService} to Tokenize and POS tag sentences. 
     */
    protected class SentenceProcessor implements Runnable {

        
        private int startOffset;
        private int endOffset;
        private Sentence sentence;
        private AnalysedText at;

        protected SentenceProcessor(int startOffset, int offset, Sentence sentence, AnalysedText at){
            this.startOffset = startOffset;
            this.endOffset = offset;
            this.sentence = sentence;
            this.at = at;
        }
        
        @Override
        public void run () {
            processSentence(startOffset, endOffset, sentence, at);
        }
    }
}
