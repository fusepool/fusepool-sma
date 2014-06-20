package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the dictionary using a HashMap to store the
 * entity - URI pairs for fast retrieval.
 * @author Gábor Reményi
 */
public class DictionaryStore {
    // HashMap representation of the keywords and matching URIs
    Map<String, Concept> keywords;
    Map<String, String> labels;
    
    /**
     * Simple constructor.
     */
    public DictionaryStore() {
        keywords = new HashMap<String, Concept>();
        labels = new HashMap<String, String>();
    }
    
    /**
     * Add new element to the dictionary, label is transformed to lower case.
     * @param text  The label
     * @param uri   The URI
     */
    public void AddElement(String label, Concept concept){
        keywords.put(label.toLowerCase(), concept);
    }
    
    /**
     * Add new element to the dictionary without any change.
     * @param text  The label
     * @param uri   The URI
     */
    public void AddOriginalElement(String labelText, String labelType, String uri){
        Concept concept;
        
        if(keywords.containsKey(labelText)){
            concept = keywords.get(labelText);
        }
        else{
            concept = new Concept(labelText, labelType, uri);
        }
        
        keywords.put(labelText, concept);
        
        if(concept.IsLabel()){
            labels.put(uri, labelText);
        }
    }
    
    /**
     * Add new element to the dictionary.
     */
    public void AddOriginalElement(String labelText, String labelType, String uri, String type){
        Concept concept;

        if(keywords.containsKey(labelText)){
            concept = keywords.get(labelText);
        }
        else{
            concept = new Concept(labelText, labelType, uri, type);
        }
        
        keywords.put(labelText, concept);
        
        if(concept.IsLabel()){
            labels.put(uri, labelText);
        }
    }
    
    /**
     * Get the URI of the matching entity, label is transformed to lower case.
     * @param text The label
     * @return 
     */
    public String GetURI(String label, Boolean toLowerCase){
        if(toLowerCase){
            label = label.toLowerCase();
        }
        return keywords.get(label).uri;
    }
        
    /**
     * Get the type of the matching entity.
     */
    public String GetType(String label, Boolean toLowerCase){
        if(toLowerCase){
            label = label.toLowerCase();
        }
        return keywords.get(label).type;
    }
    
    /**
     * Get concept object
     */   
    public Concept GetConcept(String label, Boolean toLowerCase){ 
        if(toLowerCase){
            label = label.toLowerCase();
        }
        return keywords.get(label);
    }
    
    /**
     * Get label by URI
     */   
    public String GetLabelByURI(String uri){
        return labels.get(uri);
    }

    @Override
    public String toString() {
        String result = "";
        
        Set set = keywords.entrySet();
        Iterator iterator = set.iterator();
        
        int index = 0;
        String label;
        Concept concept;
        while(iterator.hasNext()){
            Map.Entry me = (Map.Entry)iterator.next();
            label = (String) me.getKey();
            concept = (Concept) me.getValue(); 
            if(concept != null){
                result += "\t\"" + label + "\", " + concept.toString() + "\r\n";
            }
            else{
                result += "\t\"" + label + "\", " + null + "\r\n";
            }
            index++;
        }         
        
        return "DictionaryStore{\r\n" + result + '}';
    }
}