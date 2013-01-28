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
package at.salzburgresearch.stanbol.enhancer.nlp.talismane.mappings;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

public class TagSetRegistry {

    private static TagSetRegistry instance = new TagSetRegistry();
    
    private TagSetRegistry(){}
    
    private final Map<String, TagSet<PosTag>> posModels = new HashMap<String,TagSet<PosTag>>();
    /**
     * Adhoc {@link PosTag}s created for string tags missing in the {@link #posModels}
     */
    private Map<String,Map<String,PosTag>> adhocPosTagMap = new HashMap<String,Map<String,PosTag>>();
    
    public static TagSetRegistry getInstance(){
        return instance;
    }
    
    private void addPosTagSet(TagSet<PosTag> model) {
        for(String lang : model.getLanguages()){
            if(posModels.put(lang, model) != null){
                throw new IllegalStateException("Multiple Pos Models for Language '"
                    + lang+"'! This is an error in the static confituration of "
                    + "this class!");
            }
        }
    }

    /**
     * Getter for the {@link PosTag} {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<PosTag> getPosTagSet(String language){
        return posModels.get(language);
    }
    
    /**
     * Getter for the map holding the adhoc {@link PosTag} for the given language
     * @param language the language
     * @return the map with the adhoc {@link PosTag}s
     */
    public Map<String,PosTag> getAdhocPosTagMap(String language){
        Map<String,PosTag> adhocMap =  adhocPosTagMap.get(language);
        if(adhocMap == null){
            adhocMap = new HashMap<String,PosTag>();
            adhocPosTagMap.put(language, adhocMap);
        }
        return adhocMap;
    }

    /**
     * The Talismane Tagset for French as described on
     * <a href="https://www.joli-ciel.com/talismane/talismane_doc.htm#tagset">here</a>.
     */
    private static final TagSet<PosTag> TALISMANE_FR = new TagSet<PosTag>(
            "Talismane TagSet for French", "fr");
    static {
        TALISMANE_FR.addTag(new PosTag("ADJ", LexicalCategory.Adjective));
        TALISMANE_FR.addTag(new PosTag("ADJWH", LexicalCategory.Adjective, 
            //adding InterrogativeParticle because Interrogative adjective does not exist
            Pos.InterrogativeParticle)); 
        TALISMANE_FR.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        TALISMANE_FR.addTag(new PosTag("ADVWH", Pos.InterrogativeAdverb));
        TALISMANE_FR.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        TALISMANE_FR.addTag(new PosTag("CLO")); //Clitic (object)
        TALISMANE_FR.addTag(new PosTag("CLR")); //Clitic (reflexive)
        TALISMANE_FR.addTag(new PosTag("CLS")); // Clitic (subject)
        TALISMANE_FR.addTag(new PosTag("CS", Pos.SubordinatingConjunction));
        TALISMANE_FR.addTag(new PosTag("DET", Pos.Determiner)); // Determinent
        TALISMANE_FR.addTag(new PosTag("DETWH", Pos.InterrogativeDeterminer));
        TALISMANE_FR.addTag(new PosTag("ET", Pos.Foreign)); //  Foreign word
        TALISMANE_FR.addTag(new PosTag("I", Pos.Interjection));
        TALISMANE_FR.addTag(new PosTag("NC", Pos.CommonNoun));
        TALISMANE_FR.addTag(new PosTag("NPP", Pos.ProperNoun));
        TALISMANE_FR.addTag(new PosTag("P", Pos.Preposition));
        TALISMANE_FR.addTag(new PosTag("PONCT", LexicalCategory.Punctuation));
        TALISMANE_FR.addTag(new PosTag("PRO" , Pos.Pronoun));
        TALISMANE_FR.addTag(new PosTag("PROREL", Pos.RelativePronoun));
        TALISMANE_FR.addTag(new PosTag("PROWH", Pos.InterrogativePronoun));
        TALISMANE_FR.addTag(new PosTag("V", Pos.IndicativeVerb));
        TALISMANE_FR.addTag(new PosTag("VIMP", Pos.ImperativeVerb));
        TALISMANE_FR.addTag(new PosTag("VINF", Pos.Infinitive));
        TALISMANE_FR.addTag(new PosTag("VPP", Pos.PastParticiple));
        TALISMANE_FR.addTag(new PosTag("VPR", Pos.PresentParticiple));
        TALISMANE_FR.addTag(new PosTag("VS", Pos.SubjunctiveVerb));
        TALISMANE_FR.addTag(new PosTag("null")); //unknown ??
        getInstance().addPosTagSet(TALISMANE_FR);
    }
}
