/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.clerezza.platform.Constants;
import org.apache.clerezza.platform.graphprovider.content.ContentGraphProvider;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component(configurationFactory = true, 
    policy = ConfigurationPolicy.OPTIONAL,
    metatype = true, immediate = true, inherit = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME)
})
public class DictionaryAnnotatorEnhancerEngine
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    @Reference
    ContentGraphProvider cgp;
    
    /**
     * Configuration properties
     */
    
    /**
     * A short description about about the engine.
     */
    @Property
    public static final String DESCRIPTION = "eu.fusepool.enhancer.engines.dictionaryannotator.description";
    
    /**
     * The URI of the Clerezza MGraph/Graph used to store the data of the Yard that contains the dictionary.
     */
    @Property
    public static final String GRAPH_URI = "eu.fusepool.enhancer.engines.dictionaryannotator.graphURI";
    
    /**
     * The field used to retrieve the foundText of each entity in the dictionary.
     */
    @Property
    public static final String LABEL_FIELD = "eu.fusepool.enhancer.engines.dictionaryannotator.labelField";
    
    /**
     * The field used to retrieve the synonym labels of each entity in the dictionary.
     */
    @Property
    public static final String SYNONYM_FIELD = "eu.fusepool.enhancer.engines.dictionaryannotator.synonymField";
    
    /**
     * The field used to retrieve the URI value of each entity in the dictionary.
     */
    @Property
    public static final String URI_FIELD = "eu.fusepool.enhancer.engines.dictionaryannotator.URIField";    
    
    /**
     * Prefixes of URIs which are required to query the current dictionary.
     */
    @Property(cardinality = 10)
    public static final String ENTITY_PREFIXES = "eu.fusepool.enhancer.engines.dictionaryannotator.entityPrefixes";  
    
    /**
     * Defines which fields are processed from an RDF document. (Only when input MIME type is application/rdf+xml.
     */
    @Property(cardinality = 10)
    public static final String RDF_FIELDS = "eu.fusepool.enhancer.engines.dictionaryannotator.rdfFields"; 
    
    @Property
    public static final String TYPE = "eu.fusepool.enhancer.engines.dictionaryannotator_beer.type";
    
    /**
     * A list of the avaliable languages for stemming.
     */
    @Property(options={
        @PropertyOption(value="None",name="None"),
        @PropertyOption(value="Danish",name="Danish"),
        @PropertyOption(value="Dutch",name="Dutch"),
        @PropertyOption(value="English",name="English"),
        @PropertyOption(value="Finnish",name="Finnish"),
        @PropertyOption(value="French",name="French"),
        @PropertyOption(value="German",name="German"),
        @PropertyOption(value="Hungarian",name="Hungarian"),
        @PropertyOption(value="Italian",name="Italian"),
        @PropertyOption(value="Norwegian",name="Norwegian"),
        @PropertyOption(value="Portuguese",name="Portuguese"),
        @PropertyOption(value="Romanian",name="Romanian"),
        @PropertyOption(value="Russian",name="Russian"),
        @PropertyOption(value="Spanish",name="Spanish"),
        @PropertyOption(value="Swedish",name="Swedish"),
        @PropertyOption(value="Turkish",name="Turkish")
        },value="None")
    public static final String STEMMING_LANGUAGE = "eu.fusepool.enhancer.engines.dictionaryannotator.stemmingLanguage";
    
    /**
     * Option to set the annotator case sensitive or case insensitive.
     */
    @Property(boolValue=false)
    public static final String CASE_SENSITIVE = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitive";

    /**
     * This option offers a case sensitivity based on the length of each token.
     */
    @Property(intValue=3)
    public static final String CASE_SENSITIVE_LENGTH = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitiveLength";
    
    /**
     * Option to elimination overlapping matches in the result set.
     */
    @Property(boolValue=false)
    public static final String ELIMINATE_OVERLAPS = "eu.fusepool.enhancer.engines.dictionaryannotator.eliminateOverlapping";
    
    /**
     * Default value for the {@link Constants#SERVICE_RANKING} used by this engine.
     * This is a negative value to allow easy replacement by this engine depending
     * to a remote service with one that does not have this requirement
     */
    public static final int DEFAULT_SERVICE_RANKING = 100;
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;
    
    /**
     * This contains the MIME type text/plain.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * This contains the MIME type application/rdf+xml.
     */
    private static final String RDF_XML_MIMETYPE = "application/rdf+xml";
    /**
     * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    private static Set<String> SUPPORTED_MIMTYPES;
    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(DictionaryAnnotatorEnhancerEngine.class);
    
    private DictionaryAnnotator annotator;
    private DictionaryStore dictionary;
    private String description;
    private String graphURI;
    private String labelField;
    private String synonymField;
    private String URIField;
    private List<String> rdfFields;
    private List<String> entityPrefixes;
    private UriRef typeURI;
    private String stemmingLanguage;
    private Boolean caseSensitive;
    private Integer caseSensitiveLength;
    private Boolean eliminateOverlapping;
    private Tokenizer tokenizer;
    
    @Activate
    @Override
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        // the engine supports two mime types: text/plain and application/rdf+xml
        SUPPORTED_MIMTYPES = new HashSet();
        SUPPORTED_MIMTYPES.add(TEXT_PLAIN_MIMETYPE);
        SUPPORTED_MIMTYPES.add(RDF_XML_MIMETYPE);
        
        // read configuration
        if (context != null) {
            Dictionary<String,Object> config = context.getProperties();
            // reading graph uri property
            Object gu = config.get(GRAPH_URI);
            graphURI = gu == null || gu.toString().isEmpty() ? null : gu.toString();
            // reading foundText field property
            Object lf = config.get(LABEL_FIELD);
            labelField = lf == null || lf.toString().isEmpty() ? null : lf.toString();
            // reading synonym field property
            Object sf = config.get(SYNONYM_FIELD);
            synonymField = sf == null || sf.toString().isEmpty() ? null : sf.toString();
            // reading uri field property
            Object uf = config.get(URI_FIELD);
            URIField = uf == null || uf.toString().isEmpty() ? null : uf.toString();
            // reading entity prefixes property
            Object ep = config.get(ENTITY_PREFIXES);
            if (ep instanceof Iterable<?>) {
                entityPrefixes = new ArrayList<String>();
                for (Object o : (Iterable<Object>) ep) {
                    if (o != null && !o.toString().isEmpty()) {
                        entityPrefixes.add(o.toString());
                    } else {
                        log.warn("Entity prefixes configuration '{}' contained illegal value '{}' -> removed", ep, o);
                    }
                }
            } else if (ep.getClass().isArray()) {
                entityPrefixes = new ArrayList<String>();
                for (Object modelObj : (Object[]) ep) {
                    if (modelObj != null) {
                        entityPrefixes.add(modelObj.toString());
                    } else {
                        log.warn("Entity prefixes configuration '{}' contained illegal value '{}' -> removed",
                                Arrays.toString((Object[]) ep), ep);
                    }
                }
            } else {
                entityPrefixes = null;
            }
            // reading rdf fields property
            Object rf = config.get(RDF_FIELDS);         
            if (rf instanceof Iterable<?>) {
                rdfFields = new ArrayList<String>();
                for (Object o : (Iterable<Object>) rf) {
                    if (o != null && !o.toString().isEmpty()) {
                        rdfFields.add(o.toString());
                    } else {
                        log.warn("Entity prefixes configuration '{}' contained illegal value '{}' -> removed", rf, o);
                    }
                }
            } else if (rf.getClass().isArray()) {
                rdfFields = new ArrayList<String>();
                for (Object modelObj : (Object[]) rf) {
                    if (modelObj != null) {
                        rdfFields.add(modelObj.toString());
                    } else {
                        log.warn("Entity prefixes configuration '{}' contained illegal value '{}' -> removed",
                                Arrays.toString((Object[]) rf), rf);
                    }
                }
            } else {
                rdfFields = null;
            }
            // reading type property
            Object t = config.get(TYPE);        
            typeURI = t == null || t.toString().isEmpty() ? null : new UriRef(t.toString());
            // reading stemming language property
            Object sl = config.get(STEMMING_LANGUAGE);
            stemmingLanguage = sl == null ? "None" : sl.toString();
            // reading case sentitive property
            Object cs = config.get(CASE_SENSITIVE);
            caseSensitive = cs == null || cs.toString().isEmpty() ? null : (Boolean) cs;
            // reading case sentitive length property
            Object csl = config.get(CASE_SENSITIVE_LENGTH);
            caseSensitiveLength = csl == null || csl.toString().isEmpty() ? null : (Integer) csl;
            // reading eliminate overlaps property
            Object eo = config.get(ELIMINATE_OVERLAPS);
            eliminateOverlapping = eo == null || eo.toString().isEmpty() ? null : (Boolean) eo;
        }        

        // loading opennlp tokenizer model
        InputStream modelTokIn;
        TokenizerModel modelTok;
        try {
            modelTokIn = this.getClass().getResourceAsStream("/models/en-token.bin");
            modelTok = new TokenizerModel(modelTokIn);
            tokenizer = new TokenizerME(modelTok);
        } catch (FileNotFoundException ex) {
            log.error("Error while loading tokenizer model: {}", ex.getMessage());
        } catch (IOException ex) {
            log.error("Error while loading tokenizer model: {}", ex.getMessage());
        }
        if(tokenizer == null){
            log.error("Tokenizer cannot be NULL");
        }
        
        // concatenating SPARQL query
        String sparqlLabelQuery = "";
        for (String prefix : entityPrefixes) {
            sparqlLabelQuery += "PREFIX " + prefix + "\n";
        }
        sparqlLabelQuery += "SELECT distinct ?uri ?label WHERE { "
                + "?uri a " + URIField + " ."   
                + "?uri " + labelField + " ?label ."
                + " }";

        // concatenating second SPARQL query
        String sparqlSynonymQuery = "";
        if(synonymField != null){
            for (String prefix : entityPrefixes) {
                sparqlSynonymQuery += "PREFIX " + prefix + "\n";
            }
            sparqlSynonymQuery += "SELECT distinct ?uri ?label WHERE { "
                    + "?uri a " + URIField + " ."   
                    + "?uri " + synonymField + " ?label ."
                    + " }";
        }
        
        try {
            // get TcManager instance
            TcManager tcManager = TcManager.getInstance();
            TcAccessController tca;
            
            // get the graph by its URI
            LockableMGraph graph = null;
            try {
                graph = tcManager.getMGraph(new UriRef(graphURI));
                tca = tcManager.getTcAccessController();
                tca.setRequiredReadPermissions(new UriRef(graphURI),Collections.singleton((Permission)new TcPermission(
                    "urn:x-localinstance:/content.graph", "read"))
                );
                // add the graph as a temporary addition to the content graph
                cgp.addTemporaryAdditionGraph(new UriRef(graphURI));
            } catch (NoSuchEntityException e) {
                log.error("Enhancement Graph must be existing", e);
            }

            // parse the SPARQL query
            SelectQuery selectQuery = null;
            try {
                selectQuery = (SelectQuery) QueryParser.getInstance().parse(sparqlLabelQuery);
            } catch (ParseException e) {
                log.error("Cannot parse the SPARQL query", e);
            }

            if (graph != null) {
                // creating a read lock
                Lock l = graph.getLock().readLock();
                l.lock();
                try{

                    // execute the SPARQL query
                    ResultSet resultSet = tcManager.executeSparqlQuery(selectQuery, graph);
                    //ResultSet resultSet = (ResultSet) tcManager.executeSparqlQuery(sparqlLabelQuery, graph);
                    
                    dictionary = new DictionaryStore();
                    while (resultSet.hasNext()) {
                        SolutionMapping mapping = resultSet.next();
                        try{
                            Resource r = mapping.get("label");
                            // if the foundText is a TypedLiteral
                            if(r instanceof TypedLiteral){
                                TypedLiteral label = (TypedLiteral) r;
                                UriRef uri = (UriRef) mapping.get("uri");
                                // add elements to the dictionary
                                dictionary.AddOriginalElement(label.getLexicalForm(), "label", uri.getUnicodeString());
                            }
                            // else use PlainLiteralImpl
                            else{
                                PlainLiteralImpl label = (PlainLiteralImpl) r;
                                UriRef uri = (UriRef) mapping.get("uri");
                                // add elements to the dictionary
                                dictionary.AddOriginalElement(label.getLexicalForm(), "label", uri.getUnicodeString());
                            }
                        }catch(Exception e){
                            log.error("Cannot read resultset", e);
                            break;
                        }
                    }
                    
                    // get synonyms
                    if(!sparqlSynonymQuery.isEmpty()){
                        selectQuery = null;
                        try {
                            selectQuery = (SelectQuery) QueryParser.getInstance().parse(sparqlSynonymQuery);
                        } catch (ParseException e) {
                            log.error("Cannot parse the SPARQL query", e);
                        }
                        // execute the SPARQL query
                        resultSet = tcManager.executeSparqlQuery(selectQuery, graph);
                        
                        while (resultSet.hasNext()) {
                            SolutionMapping mapping = resultSet.next();
                            try {
                                Resource r = mapping.get("label");
                                // if the foundText is a TypedLiteral
                                if (r instanceof TypedLiteral) {
                                    TypedLiteral label = (TypedLiteral) r;
                                    UriRef uri = (UriRef) mapping.get("uri");
                                    // add elements to the dictionary
                                    dictionary.AddOriginalElement(label.getLexicalForm(), "synonym", uri.getUnicodeString());
                                } // else use PlainLiteralImpl
                                else {
                                    PlainLiteralImpl label = (PlainLiteralImpl) r;
                                    UriRef uri = (UriRef) mapping.get("uri");
                                    // add elements to the dictionary
                                    dictionary.AddOriginalElement(label.getLexicalForm(), "synonym", uri.getUnicodeString());
                                }
                            } catch (Exception e) {
                                log.error("Cannot read resultset", e);
                                break;
                            }
                        }
                    }

                    // creating a dictionary annotator instance
                    long start, end;
                    start = System.currentTimeMillis();
                    System.err.print("Loading dictionary from " + graphURI + " (" + dictionary.keywords.size() + ") and creating search trie ...");
                    annotator = new DictionaryAnnotator(dictionary, tokenizer, stemmingLanguage, caseSensitive, caseSensitiveLength, eliminateOverlapping);
                    end = System.currentTimeMillis();
                    System.err.println(" done [" + Double.toString((double)(end - start)/1000) + " sec] .");
                    log.info("Loading dictionary from " + graphURI + " (" + dictionary.keywords.size() + ") and creating search trie ... done [" + Double.toString((double)(end - start)/1000) + " sec] .");
                } finally {
                    l.unlock();
                }
            } else {
                log.error("There is no registered graph with given uri: " + graphURI);
                System.out.println("There is no registered graph with given uri: " + graphURI);
            }
        } catch(Exception e) {
            log.error("Error happened while creating the dictionary!",e);
            System.err.println("Error happened while creating the dictionary!");
        }
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        // remove the temporary addition from the content graph
        cgp.removeTemporaryAdditionGraph(new UriRef(graphURI));
        annotator = null;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ci.getBlob() == null)
                    || (ci.getBlob().getStream().read() == -1)) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for "
                    + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Map.Entry<UriRef, Blob> contentPart = null;
        List<Annotation> annotations = null;
        String text = "";
        List<String> texts = new ArrayList<String>();
        Document rdf;
        Element rootElement;
        
        // if MIME typs is text/plain
        if(TEXT_PLAIN_MIMETYPE.equals(ci.getMimeType())){
            contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
            try {
                // get input text content
                text = ContentItemHelper.getText(contentPart.getValue());
            } catch (IOException e) {
                throw new InvalidContentException(this, ci, e);
            }
            if (text.trim().length() == 0) {
                log.info("No text contained in ContentPart {} of ContentItem {}", contentPart.getKey(), ci.getUri());
                return;
            }

            try {
                // extract annotations from text input
                annotations = annotator.GetAnnotations(text);
            } catch (Exception e) {
                log.warn("Could not recognize entities", e);
                return;
            }
        }
        // if MIME type is application/rdf+xml
        else if(RDF_XML_MIMETYPE.equals(ci.getMimeType())){
            contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
            try {
                // get input text content
                text = ContentItemHelper.getText(contentPart.getValue());
                // create DOM object from string rdf
                rdf = this.stringToDom(text);
                rootElement = rdf.getDocumentElement();
                // look-up tags defined in rdf fields property
                for (String field : rdfFields) {
                    List<String> currentText = this.findTag(field, rootElement);
                    if(currentText != null){
                        texts.addAll(currentText);
                    }
                    else{
                        log.warn("RDF label '" + field + "' was not found in ContentItem {}",ci.getUri());
                    }
                }
            } catch (Exception e) {
                throw new InvalidContentException(this, ci, e);
            }

            try {
                annotations = new ArrayList<Annotation>();
                for (String textItem : texts) {
                    // extract annotations from each text part
                    List<Annotation> currentAnnotations = annotator.GetAnnotations(textItem);
                    if (currentAnnotations != null) {
                        // collect all annotations from the document
                        annotations.addAll(currentAnnotations);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not recognize entities", e);
                return;
            }
        }
        
        if (contentPart == null) {
            throw new IllegalStateException("No ContentPart with Mimetype '"
                    + TEXT_PLAIN_MIMETYPE + "' or '" + RDF_XML_MIMETYPE + "' found for ContentItem " + ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }

        // add annotations to metadata
        if (annotations != null) {
            LiteralFactory literalFactory = LiteralFactory.getInstance();
            MGraph g = ci.getMetadata();
            ci.getLock().writeLock().lock();
            try {
//                System.out.println("");
//                System.out.println("--- Annotations (" + annotations.size() + ") ---");
                // add enhancements to MGraph
                for (Annotation a : annotations) {
                    //System.out.println(a.toString());
                    if(a.HasNoNull()){
                        UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                        g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(a.getScore())));
                        g.add(new TripleImpl(textEnhancement, DC_TYPE, typeURI));
                        g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(a.getLabel())));
                        g.add(new TripleImpl(textEnhancement, ENHANCER_START, new PlainLiteralImpl(Integer.toString(a.getBegin()))));
                        g.add(new TripleImpl(textEnhancement, ENHANCER_END, new PlainLiteralImpl(Integer.toString(a.getEnd()))));
                        g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_REFERENCE, new UriRef(a.getUri())));
                        g.add(new TripleImpl(textEnhancement, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(a.getFoundText())));
                        g.add(new TripleImpl(new UriRef(a.getUri()), org.apache.clerezza.rdf.ontologies.RDFS.label, new PlainLiteralImpl(a.getLabel())));  
                    }
                }
//                System.out.println("------------------");
//                System.out.println("");
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }
    }
    
    /**
     * ServiceProperties are currently only used for automatic ordering of the 
     * execution of EnhancementEngines (a.g. by the WeightedChain implementation).
     * Default ordering means that the engine is called after all engines that
     * use a value < {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     * and >= {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}.
     */
    public Map getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, ORDERING_DEFAULT));
    }
    
    /**
     * Creates a DOM object from a string source.
     * @param xmlSource Contains an XML source.
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public Document stringToDom(String xmlSource) 
            throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        // parse XML string into a DOM object
        return builder.parse(new InputSource(new StringReader(xmlSource)));
    }
    
    /**
     * Finding and extracting specific tags from an element. All occurrences
     * of the specified tag are returned and the returned texts are striped from 
     * any HTML or XML tags.
     * @param tagName   Name of the tag we are looking for.
     * @param element   The element in which the look-up is performed.
     * @return 
     */
    public List<String> findTag(String tagName, Element element) {
        List<String> resultSet = null;
        // the list of nodes with the same name
        NodeList list = element.getElementsByTagName(tagName);
        try{
            if (list != null && list.getLength() > 0) {
                resultSet = new ArrayList<String>();
                // iterate through the nodes
                for (int i = 0; i < list.getLength(); i++) {
                    Element e = (Element) list.item(i);
                    if (e.getAttribute("xml:lang").equals("en") || e.getAttribute("xml:lang").equals("")) {
                        // extract and clean text from node
                        resultSet.add(innerXml(list.item(i)));
                    }
                }
            }
            return resultSet;
        }catch(IOException e){
            log.warn("Exception occured while extracting text.", e);
            return null;
        }
    }
    
    /**
     * Extracts all text from the node and strips it from all HTML
     * and XML tags.
     * @param node  The node that contains the HTML text.
     * @return  Clean plain text.
     * @throws IOException 
     */
    public String innerXml(Node node) throws IOException {
        DOMImplementationLS lsImpl = 
                (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        // create a LS serializer
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        // get all childnodes
        NodeList childNodes = node.getChildNodes();
        // concaterate all content from childnodes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        // create a HTML parser
        Html2Text parser = new Html2Text();
        // parse HTML text
        parser.parse(new StringReader(sb.toString()));
        // return cleaned text
        return parser.getText();
    }
}
