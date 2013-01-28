/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.salzburgresearch.stanbol.enhancer.nlp.talismane.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneAnalyzer;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneStanbolConfig;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.mappings.TagSetRegistry;

public class TestTalismaneAnalyser {

    private static final Logger log = LoggerFactory.getLogger(TestTalismaneAnalyser.class);
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int ANALYZER_THREADS = 10;
    private static final ClassLoader cl = TestTalismaneAnalyser.class.getClassLoader();
    
    private static TalismaneAnalyzer analyzer;
    
    private static AnalysedTextFactory atf;
    private static ContentItemFactory cif;
    private static ExecutorService executorService;
    
    private static TagSet<PosTag> TAG_SET = TagSetRegistry.getInstance().getPosTagSet("fr");
    
    private static final Map<String, Blob> examples = new HashMap<String,Blob>();
    private static final List<String> testFileNames = Arrays.asList(
        "algerie.txt", "israel.txt", "lawrence-lessig.txt", "les-jeux.txt",
        "libye.txt", "mali.txt", "suisse.txt");
    
    @BeforeClass
    public static void initTalismane() throws Exception {
        executorService = Executors.newFixedThreadPool(ANALYZER_THREADS);
        analyzer = new TalismaneAnalyzer(new TalismaneStanbolConfig(
            AnalysedTextFactory.getDefaultInstance()), executorService);
        cif = InMemoryContentItemFactory.getInstance();
        //init the text eamples
        for(String name : testFileNames){
            examples.put(name, cif.createBlob(new StreamSource(
                cl.getResourceAsStream(name),"text/plain; charset="+UTF8.name())));
        }
    }


    @Test
    public void testAnalysis() throws IOException {
        for(Entry<String,Blob> example : examples.entrySet()){
            AnalysedText at = analyzer.analyse(example.getValue());
            validateAnalysedText(at.getSpan(), at);
        }
	}

    @Test
    public void testConcurrentAnalyses() throws IOException, InterruptedException, ExecutionException{
        //warm up
        log.info("Start concurrent analyses test");
        log.info("  ... warm up");
        for(Entry<String,Blob> example : examples.entrySet()){
            analyzer.analyse(example.getValue());
        }

        //performance test
        long start = System.currentTimeMillis();
        int concurrentRequests = 3;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        int iterations = 100;
        log.info("  ... start test with {} iterations", iterations);
        List<Future<?>> tasks = new ArrayList<Future<?>>(iterations);
        long[] times = new long[iterations];
        Iterator<Blob> texts = examples.values().iterator();
        for(int i=0; i < iterations;i++){
            if(!texts.hasNext()){
                texts = examples.values().iterator();
            }
            tasks.add(executor.submit(new AnalyzerRequest(i, times, analyzer, texts.next())));
        }
        for(Future<?> task : tasks){ //wait for completion of all tasks
            task.get();
        }
        long duration = System.currentTimeMillis()-start;
        log.info("Processed {} texts",iterations);
        log.info("  > time       : {}ms",duration);
        log.info("  > average    : {}ms",(duration)/(double)iterations);
        long sumTime = 0;
        for(int i=0;i<times.length;i++){
            sumTime = sumTime+times[i];
        }
        log.info("  > processing : {}ms",sumTime);
        float concurrency = sumTime/(float)duration;
        log.info("  > concurrency: {} / {}%",concurrency, concurrency*100/concurrentRequests);
    }
    
    private class AnalyzerRequest implements Runnable {

        private int index;
        private long[] times;
        private Blob blob;
        private TalismaneAnalyzer ta;

        private AnalyzerRequest(int index, long[] times, TalismaneAnalyzer ta, Blob blob){
            this.index = index;
            this.times = times;
            this.blob = blob;
            this.ta = ta;
        }
        
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                ta.analyse(blob);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            times[index] = System.currentTimeMillis()-start;
            log.info(" > finished task {} in {}ms",index+1,times[index]);
        }
        
    }
    
    
    private void validateAnalysedText(String text, AnalysedText at){
        Assert.assertNotNull(text);
        Assert.assertNotNull(at);
        //Assert the AnalysedText
        Assert.assertEquals(0, at.getStart());
        Assert.assertEquals(text.length(), at.getEnd());
        Iterator<Span> it = at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        while(it.hasNext()){
            //validate that the span|start|end corresponds with the Text
            Span span = it.next();
            Assert.assertNotNull(span);
            Assert.assertEquals(text.substring(span.getStart(), span.getEnd()), 
                span.getSpan());
            switch (span.getType()) {
                case Token:
                    double prevProb = -1;
                    List<Value<PosTag>> posTags = span.getAnnotations(POS_ANNOTATION);
                    Assert.assertTrue("All Tokens need to have a PosTag (missing for "
                        + span+ ")", posTags != null && !posTags.isEmpty());
                    for(Value<PosTag> posTag : posTags){
                        //assert Mapped PosTags
                        Assert.assertTrue("PosTag "+posTag+" used by "+span+" is not present in the PosTagSet",
                            TAG_SET.getTag(posTag.value().getTag()) != null);
                        //assert declining probabilities
                        Assert.assertTrue("Wrong order in "+posTags+" of "+span+"!",
                            prevProb < 0 || posTag.probability() <= prevProb);
                        prevProb = posTag.probability();
                    }
                    Assert.assertNull("Tokens MUST NOT have Phrase annotations!",
                        span.getAnnotation(PHRASE_ANNOTATION));
                    Assert.assertNull("Tokens MUST NOT have NER annotations!",
                        span.getAnnotation(NER_ANNOTATION));
                    break;
                case Chunk:
                    Assert.assertNull("Chunks MUST NOT have POS annotations!",
                        span.getAnnotation(POS_ANNOTATION));
                    List<Token> tokens = AnalysedTextUtils.asList(((Chunk)span).getTokens());
                    prevProb = -1;
                    List<Value<PhraseTag>> phraseTags = span.getAnnotations(PHRASE_ANNOTATION);
                    boolean hasPhraseTag = (phraseTags != null && !phraseTags.isEmpty());
                    List<Value<NerTag>> nerTags = span.getAnnotations(NER_ANNOTATION);
                    boolean hasNerTag = (nerTags != null && !nerTags.isEmpty());
                    Assert.assertTrue("All Chunks with several words need to have a PhraseTag (missing for "
                            + span+ ")",  hasPhraseTag || tokens.size() < 2);
                    Assert.assertTrue("All Chunks with a single word need to have a NerTag (missing for"
                            + span +")", hasNerTag || tokens.size() > 1);
                    for(Value<PhraseTag> phraseTag : phraseTags){
                        //assert Mapped PosTags
                        Assert.assertNotNull("PhraseTag "+phraseTag+" is not mapped",
                            phraseTag.value().getCategory());
                        //assert declining probabilities
                        Assert.assertTrue(prevProb < 0 || phraseTag.probability() < prevProb);
                        prevProb = phraseTag.probability();
                    }
                    for(Value<NerTag> nerTag : nerTags){
                        Assert.assertTrue("NER Tags need to have a probability",
                            nerTag.probability() > 0);
                    }
                    break;
                default:
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have POS annotations!",
                        span.getAnnotation(POS_ANNOTATION));
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have Phrase annotations!",
                        span.getAnnotation(PHRASE_ANNOTATION));
                    Assert.assertNull(span.getType()+" type Spans MUST NOT have NER annotations!",
                        span.getAnnotation(NER_ANNOTATION));
                    break;
            }
        }
    }
    
    
    @AfterClass
    public static final void cleanUp(){
        executorService.shutdown();
    }
}
