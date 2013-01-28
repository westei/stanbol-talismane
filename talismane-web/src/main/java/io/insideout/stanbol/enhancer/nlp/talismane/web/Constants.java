package io.insideout.stanbol.enhancer.nlp.talismane.web;

import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneAnalyzer;

public interface Constants {

    public static final String SERVLET_ATTRIBUTE_TALISMANE = TalismaneAnalyzer.class.getName();
    public static final String SERVLET_ATTRIBUTE_ANALYSERS_TREADS = 
            Constants.class.getPackage().getName()+".analysersThreads";
    public static final String SERVLET_ATTRIBUTE_CONTENT_ITEM_FACTORY = ContentItemFactory.class.getName();
        
}
