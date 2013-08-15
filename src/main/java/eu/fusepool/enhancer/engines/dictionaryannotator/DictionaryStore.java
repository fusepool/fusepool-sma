/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.HashMap;
import java.util.Map;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * This class represents the dictionary using a HashMap to store the
 * entity - URI pairs for fast retrieval.
 * 
 * @author Gabor
 */
public class DictionaryStore {
    //HashMap representation of the keywords and matching URIs
    Map<String, UriRef> keywords;
    
    /**
     * Simple constructor.
     */
    public DictionaryStore() {
        keywords = new HashMap<String, UriRef>();
    }
    
    /**
     * Add new element to the dictionary.
     */
    public void AddElement(String text, UriRef uri){
        keywords.put(text.toLowerCase(), uri);
    }
    
    /**
     * Add new element to the dictionary without any change.
     */
    public void AddOriginalElement(String text, UriRef uri){
        keywords.put(text, uri);
    }
    
    /**
     * Get the URI of the matching entity.
     */
    public UriRef GetURI(String text){
        return keywords.get(text.toLowerCase());
    }
}
