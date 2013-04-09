/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.HashMap;
import java.util.Map;
import org.apache.clerezza.rdf.core.UriRef;

/**
 *
 * @author Gabor
 */
public class Dictionary {
    Map<String, UriRef> keywords;

    public Dictionary() {
        keywords = new HashMap<String, UriRef>();
    }
    
    public void AddElement(String text, UriRef uri){
        keywords.put(text.toLowerCase(), uri);
    }
    
    public UriRef GetURI(String text){
        return keywords.get(text.toLowerCase());
    }
}
