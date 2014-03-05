package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.HashMap;
import java.util.Map;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * This class represents the dictionary using a HashMap to store the
 * entity - URI pairs for fast retrieval.
 * @author Gábor Reményi
 */
public class DictionaryStore {
    // HashMap representation of the keywords and matching URIs
    Map<String, UriRef> keywords;
    
    /**
     * Simple constructor.
     */
    public DictionaryStore() {
        keywords = new HashMap<String, UriRef>();
    }
    
    /**
     * Add new element to the dictionary, label is transformed to lower case.
     * @param text  The label
     * @param uri   The URI
     */
    public void AddElement(String text, UriRef uri){
        keywords.put(text.toLowerCase(), uri);
    }

    /**
     * Get the URI of the matching entity, label is transformed to lower case.
     * @param text The label
     * @return 
     */
    public UriRef GetURI(String text){
        return keywords.get(text.toLowerCase());
    }
    
    /**
     * Add new element to the dictionary without any change.
     * @param text  The label
     * @param uri   The URI
     */
    public void AddOriginalElement(String text, UriRef uri){
        keywords.put(text, uri);
    }
    
    /**
     * Get the URI of the matching entity.
     * @param text The label
     * @return 
     */
    public UriRef GetOriginalURI(String text){
        return keywords.get(text.toLowerCase());
    }
}
