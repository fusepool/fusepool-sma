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

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;

@Component(configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE,
    metatype = true, immediate = true, inherit = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME)
})
public class DictionaryAnnotatorEnhancerEngine
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    @Property
    public static final String DESCRIPTION = "eu.fusepool.enhancer.engines.dictionaryannotator.description";
    
    @Property
    public static final String GRAPH_URI = "eu.fusepool.enhancer.engines.dictionaryannotator.graphURI";
    
    @Property
    public static final String LABEL_FIELD = "eu.fusepool.enhancer.engines.dictionaryannotator.labelField";
    
    @Property
    public static final String URI_FIELD = "eu.fusepool.enhancer.engines.dictionaryannotator.URIField";   
    
    @Property(cardinality = 20)
    public static final String ENTITY_PREFIXES = "eu.fusepool.enhancer.engines.dictionaryannotator.entityPrefixes";  
    
    @Property
    public static final String TYPE = "eu.fusepool.enhancer.engines.dictionaryannotator.type";
    
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
    
    @Property(boolValue=false)
    public static final String CASE_SENSITIVE = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitive";

    @Property(intValue=3)
    public static final String CASE_SENSITIVE_LENGTH = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitiveLength";
    
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
     * This contains the only MIME type directly supported by this enhancement
     * engine.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(DictionaryAnnotatorEnhancerEngine.class);
    
    private DictionaryAnnotator annotator;
    private String description;
    private String graphURI;
    private String labelField;
    private String URIField;
    private String type;
    private List<String> entityPrefixes;
    private String stemmingLanguage;
    private Boolean caseSensitive;
    private Integer caseSensitiveLength;
    private Boolean eliminateOverlapping;
    
    @Activate
    @Override
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
        if (context != null) {
            Dictionary<String,Object> config = context.getProperties();
            
            Object gu = config.get(GRAPH_URI);
            graphURI = gu == null || gu.toString().isEmpty() ? null : gu.toString();

            Object lf = config.get(LABEL_FIELD);
            labelField = lf == null || lf.toString().isEmpty() ? null : lf.toString();
            
            Object uf = config.get(URI_FIELD);
            URIField = uf == null || uf.toString().isEmpty() ? null : uf.toString();
            
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
                  
            Object t = config.get(TYPE);
            type = t == null || t.toString().isEmpty() ? null : t.toString();
            
            Object sl = config.get(STEMMING_LANGUAGE);
            stemmingLanguage = sl == null ? "None" : sl.toString();
            
            Object cs = config.get(CASE_SENSITIVE);
            caseSensitive = cs == null || cs.toString().isEmpty() ? null : (Boolean) cs;
            
            Object csl = config.get(CASE_SENSITIVE_LENGTH);
            caseSensitiveLength = csl == null || csl.toString().isEmpty() ? null : (Integer) csl;

            Object eo = config.get(ELIMINATE_OVERLAPS);
            eliminateOverlapping = eo == null || eo.toString().isEmpty() ? null : (Boolean) eo;
        }

        String query = "";
        for (String prefix : entityPrefixes) {
            query += "PREFIX " + prefix + "\n";
        }
        query += "SELECT distinct ?uri ?label WHERE { "
                + "?uri a " + URIField + " ."
                + "?uri " + labelField + " ?label ."
                + " }";
        
        String sparqlEndPoint = "http://localhost:8080/sparql";

        HttpURLConnection connection = null;
        InputStream dictionaryStream = null;
        
        try {
            //Concating request parameters
            String urlParameters = "graphuri=" + URLEncoder.encode(graphURI, "UTF-8") +
                               "&query=" + URLEncoder.encode(query, "UTF-8");
            
            //Create connection
            URL url = new URL(sparqlEndPoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", ""
                    + Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response	
            dictionaryStream = connection.getInputStream();
            
            if(dictionaryStream != null) 
            {
                annotator = new DictionaryAnnotator(dictionaryStream, stemmingLanguage, caseSensitive, caseSensitiveLength, eliminateOverlapping);
            }
            else
            {
                log.error("Dictionary cannot be NULL");
            }
        } catch (IOException e) {
            log.error("Error occurred while querying the entityhub", e);
            e.printStackTrace();
        } finally {
            if (dictionaryStream != null) {
                try {
                    dictionaryStream.close();
                } catch (IOException ex) {
                    log.error("Unable to close inputstream", ex);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        annotator = null;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Map.Entry<UriRef, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
        if (contentPart == null) {
            throw new IllegalStateException("No ContentPart with Mimetype '"
                    + TEXT_PLAIN_MIMETYPE + "' found for ContentItem " + ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }
        String text = "";
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            log.info("No text contained in ContentPart {} of ContentItem {}",
                    contentPart.getKey(), ci.getUri());
            return;
        }

        List<Entity> entities = null;
        try {
            entities = annotator.Annotate(text);
            log.info("entities identified: {}", entities);
        } catch (Exception e) {
            log.warn("Could not recognize entities", e);
            return;
        }

        // add entities to metadata
        if (entities != null) {
            LiteralFactory literalFactory = LiteralFactory.getInstance();
            MGraph g = ci.getMetadata();
            ci.getLock().writeLock().lock();
            try {
                for (Entity e : entities) {
                    UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_REFERENCE, e.uri));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(e.weight)));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_TYPE, new PlainLiteralImpl(type)));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(e.text)));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_START, new PlainLiteralImpl(Integer.toString(e.begin))));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_END, new PlainLiteralImpl(Integer.toString(e.end))));
                }
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    } 
}
