package io.insideout.stanbol.enhancer.nlp.talismane.web.resource;

import static io.insideout.stanbol.enhancer.nlp.talismane.web.Constants.SERVLET_ATTRIBUTE_TALISMANE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import io.insideout.stanbol.enhancer.nlp.talismane.web.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneAnalyzer;

@Path("/analysis")
public class AnalysisResource {

    private final Logger log = LoggerFactory.getLogger(AnalysisResource.class);
    
    @Context
    ServletContext servletContext;


    private TalismaneAnalyzer analyser;
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getHomepage(){
        InputStream in = AnalysisResource.class.getClassLoader().getResourceAsStream("analysis.html");
        if(in == null){
            return Response.status(Status.NOT_FOUND).build();
        } else {
            return Response.ok(in,MediaType.TEXT_HTML_TYPE).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> supported(){
        return Collections.singleton("fr");
    }
    
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyse(Blob blob, @Context HttpHeaders headers){
        TalismaneAnalyzer analyser = getTalismane();
        List<String> contentLanguages = headers.getRequestHeader(HttpHeaders.CONTENT_LANGUAGE);
        //Now retrieve/detect the language of the text
        if(contentLanguages == null || contentLanguages.isEmpty()){
            //language identification is not supported
            return Response.status(BAD_REQUEST).entity("Language Identification"
                    + "is not supported. Please explicitly parse the "
                    + "Language by setting the '"+HttpHeaders.CONTENT_LANGUAGE
                    + "' in the Request").build();
        } else if(contentLanguages.size() > 1){
            return Response.status(BAD_REQUEST).entity("The " + CONTENT_LANGUAGE
                + "Header MUST only have a single value (parsed: "+
                    contentLanguages.toString()+")!").build();
        } else {
            String clString = contentLanguages.get(0);
            if(clString.length() != 2){
                return Response.status(BAD_REQUEST).entity("The " + CONTENT_LANGUAGE
                    + "Header MUST use two digit (ISO 639-1) language codes (parsed: "+
                    clString+")!").build();
            }
            if(!"fr".equalsIgnoreCase(clString)){
                return Response.status(BAD_REQUEST).entity("The language '" + clString
                        + "' of the parsed text is not supported (supported: [\"fr\"])")
                    .header(HttpHeaders.CONTENT_LANGUAGE, clString)
                    .build();
            }
        }
        AnalysedText at; 
        try {
            at = analyser.analyse(blob);
        } catch (IOException e) {
            log.error("Unable to read data from Blob",e);
            throw new WebApplicationException(e);
        } catch (RuntimeException e) {
            log.error("Exception while analysing Blob",e);
            throw new WebApplicationException(e);
        }
        return Response.ok(at)
                .header(HttpHeaders.CONTENT_LANGUAGE, "fr")
                .build();
    }
    
    private TalismaneAnalyzer getTalismane(){
        if(analyser == null){
            analyser = Utils.getResource(TalismaneAnalyzer.class, 
                servletContext, SERVLET_ATTRIBUTE_TALISMANE);
        }
        return analyser;
    }
    
}
