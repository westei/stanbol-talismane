package at.salzburgresearch.stanbol.enhancer.nlp.talismane.server;


import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.wink.server.internal.servlet.RestServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneAnalyzer;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.analyser.TalismaneStanbolConfig;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.web.Constants;
import at.salzburgresearch.stanbol.enhancer.nlp.talismane.web.TalismaneApplication;


public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_ANALYSER_THREADS = 10;
    
    private static final Options options;
    static {
        options = new Options();
        options.addOption("h", "help", false, "display this help and exit");
        options.addOption("p","port",true, 
            "The port for the server (default: "+DEFAULT_PORT+")");
        options.addOption("t","analyser-threads",true,
            "The size of the thread pool used for Talismane to tokenize and "
            + "POS tag sentences (default: "+DEFAULT_ANALYSER_THREADS+")");
    }
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, args);
        args = line.getArgs();
        if(line.hasOption('h')){
            printHelp();
            System.exit(0);
        }
        log.info("Starting Stanbol Talismane Server ...");
        //create the Threadpool
        log.info(" > Initialise Talismane");
        ExecutorService executor = Executors.newFixedThreadPool(
            getInt(line, 't', DEFAULT_ANALYSER_THREADS));
        //and the Talismane Analyzer
        TalismaneAnalyzer talismane = new TalismaneAnalyzer(
            new TalismaneStanbolConfig(AnalysedTextFactory.getDefaultInstance()), 
            executor);
        
        
        //init the Jetty Server
        int port = getInt(line,'p',DEFAULT_PORT);
        log.info(" > Initialise Jetty Server on Port {}",port);
        Server server = new Server();
        Connector con = new SelectChannelConnector();
        //we need the port
        con.setPort(port);
        server.addConnector(con);

        log.info(" ... JAX-RS Application");
        //init the Servlet and the ServletContext
        Context context = new Context(server, "/", Context.SESSIONS);
        ServletHolder holder = new ServletHolder(RestServlet.class);
        holder.setInitParameter("javax.ws.rs.Application", TalismaneApplication.class.getName());
        context.addServlet(holder, "/*");
        
        log.info(" ... configure Servlet Context");
        //now initialise the servlet context
        context.setAttribute(Constants.SERVLET_ATTRIBUTE_CONTENT_ITEM_FACTORY, 
            lookupService(ContentItemFactory.class));
        context.setAttribute(Constants.SERVLET_ATTRIBUTE_TALISMANE, talismane);
        //Freeling
        
        log.info(" ... starting server");
        server.start();
        try {
            server.join();
        }catch (InterruptedException e) {
        }
        log.info("Shutting down Talismane");
        executor.shutdown();
    }
    
    private static int getInt(CommandLine line, char option, int defaultValue){
        String value = line.getOptionValue(option);
        if(value != null){
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }
    
    private static <T> T lookupService(Class<T> clazz){
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        Iterator<T> services = loader.iterator();
        if(services.hasNext()){
            return services.next();
        } else {
            throw new IllegalStateException("Unable to find implemetnation for service "+clazz);
        }
    }
    
    /**
     * 
     */
    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
            "java -Xmx{size} -jar at.salzburgresearch.stanbol.enhancer.nlp.talismane.server-*" +
            "-jar-with-dependencies.jar [options]",
            "Indexing Commandline Utility: \n"+
            "  size:  Heap requirements depend on the dataset and the configuration.\n"+
            "         1024m should be a reasonable default.\n",
            options,
            null);
    }

}
