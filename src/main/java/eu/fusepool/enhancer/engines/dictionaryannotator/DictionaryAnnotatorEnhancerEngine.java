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
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
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
    public static final String ONTOLOGY_NAME = "eu.fusepool.enhancer.engines.dictionaryannotator.name";

    @Property
    public static final String ONTOLOGY_DESCRIPTION = "eu.fusepool.enhancer.engines.dictionaryannotator.description";
    
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
    public static final String ONTOLOGY_STEMMING_LANGUAGE = "eu.fusepool.enhancer.engines.dictionaryannotator.stemmingLanguage";
    
    @Property(boolValue=false)
    public static final String ONTOLOGY_CASE_SENSITIVE = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitive";

    @Property(intValue=3)
    public static final String ONTOLOGY_CASE_SENSITIVE_LENGTH = "eu.fusepool.enhancer.engines.dictionaryannotator.caseSensitiveLength";
    
    @Property(boolValue=false)
    public static final String ONTOLOGY_ELIMINATE_OVERLAPS = "eu.fusepool.enhancer.engines.dictionaryannotator.eliminateOverlapping";
    
    @Property
    public static final String ONTOLOGY_TYPE = "eu.fusepool.enhancer.engines.dictionaryannotator.type";
    
    @Property
    public static final String ONTOLOGY_PATH = "eu.fusepool.enhancer.engines.dictionaryannotator.path";
    
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
    private String name;
    private String description;
    private String stemmingLanguage;
    private Boolean caseSensitive;
    private Integer caseSensitiveLength;
    private Boolean eliminateOverlapping;
    private String type;
    private String path;
    
    @Activate
    @Override
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
        if (context != null) {
            Dictionary<String,Object> config = context.getProperties();

            Object n = config.get(ONTOLOGY_NAME);
            name = n == null || n.toString().isEmpty() ? null : n.toString();
            
            Object d = config.get(ONTOLOGY_DESCRIPTION);
            description = d == null || d.toString().isEmpty() ? null : d.toString();

            Object s = config.get(ONTOLOGY_STEMMING_LANGUAGE);
            stemmingLanguage = s == null ? "None" : s.toString();
            
            Object c = config.get(ONTOLOGY_CASE_SENSITIVE);
            caseSensitive = c == null || c.toString().isEmpty() ? null : (Boolean) c;
            
            Object cl = config.get(ONTOLOGY_CASE_SENSITIVE_LENGTH);
            caseSensitiveLength = cl == null || cl.toString().isEmpty() ? null : (Integer) cl;

            Object e = config.get(ONTOLOGY_ELIMINATE_OVERLAPS);
            eliminateOverlapping = e == null || e.toString().isEmpty() ? null : (Boolean) e;
            
            Object t = config.get(ONTOLOGY_TYPE);
            type = t == null || t.toString().isEmpty() ? null : t.toString();
            
            Object p = config.get(ONTOLOGY_PATH);
            path = p == null || p.toString().isEmpty() ? null : p.toString();
        }
        InputStream dictionaryStream = this.getClass().getResourceAsStream("/dictionaries/" + path);
        if(dictionaryStream == null) 
        {
            log.warn("Invalid path for classifier file: " + path);
        }
        else
        {
            annotator = new DictionaryAnnotator(dictionaryStream, stemmingLanguage, caseSensitive, caseSensitiveLength, eliminateOverlapping);
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
//        LiteralFactory literalFactory = LiteralFactory.getInstance();
//        // Retrieve the existing text annotations (requires read lock)
//        Map<NER, List<UriRef>> textAnnotations = new HashMap<NER, List<UriRef>>();
//        // the language extracted for the parsed content or NULL if not
//        // available
//        String contentLangauge;
//        ci.getLock().readLock().lock();
//        try {
//            contentLangauge = EnhancementEngineHelper.getLanguage(ci);
//            for (Iterator<Triple> it = graph.filter(null, RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION); it.hasNext();) {
//                UriRef uri = (UriRef) it.next().getSubject();
//                if (graph.filter(uri, org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION, null).hasNext()) {
//                    // this is not the most specific occurrence of this name:
//                    // skip
//                    continue;
//                }
//                NamedEntity namedEntity = NamedEntity.createFromTextAnnotation(graph, uri);
//                if (namedEntity != null) {
//                    // This is a first occurrence, collect any subsumed
//                    // annotations
//                    List<UriRef> subsumed = new ArrayList<UriRef>();
//                    for (Iterator<Triple> it2 = graph.filter(null, org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION, uri); it2
//                            .hasNext();) {
//                        subsumed.add((UriRef) it2.next().getSubject());
//                    }
//                    textAnnotations.put(namedEntity, subsumed);
//                }
//            }
//        } finally {
//            ci.getLock().readLock().unlock();
//        }
//        // search the suggestions
//        Map<NER, List<Suggestion>> suggestions = new HashMap<NER, List<Suggestion>>(
//                textAnnotations.size());
//        for (Map.Entry<NER, List<UriRef>> entry : textAnnotations.entrySet()) {
//            try {
//                List<Suggestion> entitySuggestions = computeEntityRecommentations(site, entry.getKey(),
//                        entry.getValue(), contentLangauge);
//                if (entitySuggestions != null && !entitySuggestions.isEmpty()) {
//                    suggestions.put(entry.getKey(), entitySuggestions);
//                }
//            } catch (EntityhubException e) {
//                throw new EngineException(this, ci, e);
//            }
//        }
//        // now write the results (requires write lock)
//        ci.getLock().writeLock().lock();
//        try {
//            RdfValueFactory factory = RdfValueFactory.getInstance();
//            Map<String, Representation> entityData = new HashMap<String, Representation>();
//            for (Map.Entry<NamedEntity, List<Suggestion>> entitySuggestions : suggestions.entrySet()) {
//                List<UriRef> subsumed = textAnnotations.get(entitySuggestions.getKey());
//                List<NonLiteral> annotationsToRelate = new ArrayList<NonLiteral>(subsumed);
//                annotationsToRelate.add(entitySuggestions.getKey().getEntity());
//                for (Suggestion suggestion : entitySuggestions.getValue()) {
//                    log.debug("Add Suggestion {} for {}", suggestion.getEntity().getId(),
//                            entitySuggestions.getKey());
//                    EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory, graph, ci.getUri(),
//                            annotationsToRelate, suggestion, nameField,
//                            // TODO: maybe we want labels in a different
//                            // language than the
//                            // language of the content (e.g. Accept-Language
//                            // header)?!
//                            contentLangauge == null ? DEFAULT_LANGUAGE : contentLangauge);
//                    if (dereferenceEntities) {
//                        entityData.put(suggestion.getEntity().getId(), suggestion.getEntity()
//                                .getRepresentation());
//                    }
//                }
//            }
//            // if dereferneceEntities is true the entityData will also contain
//            // all
//            // Representations to add! If false entityData will be empty
//            for (Representation rep : entityData.values()) {
//                graph.addAll(factory.toRdfRepresentation(rep).getRdfGraph());
//            }
//        } finally {
//            ci.getLock().writeLock().unlock();
//        }
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    } 
}
