/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.List;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * This class represents an entity and stores its label, URI, begin and end
 * position, its weight, whether it overlaps with other entities and the label
 * divided into tokens.
 * @author Gabor
 */
public class Entity {
    String label;
    UriRef uri;
    int begin;
    int end;
    double score;
    boolean overlap;
    List<Token> tokens;
    
    /**
     * Simple constructor.
     */
    public Entity() {
        tokens = new ArrayList<Token>();
        score = 1;
    }
    
    /**
     * Returns the label of the entity.
     * @return 
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Returns the label of the entity stripping it from new line characters.
     * @return 
     */
    public String getDisplayText() {
        return label.replace("\\n", " ");
    }
    
    /**
     * Returns the begin position of the entity.
     * @return 
     */
    public int getBegin() {
        return begin;
    }
    
    /**
     * Returns the end position of the entity.
     * @return 
     */
    public int getEnd() {
        return end;
    }
    
    /**
     * Returns the whether the entity overlaps with another.
     * @return 
     */
    public boolean isOverlap() {
        return overlap;
    }

    /**
     * Returns the label of the entity as a token list.
     * @return 
     */
    public List<Token> getTokens() {
        return tokens;
    }
    
    /**
     * Returns the length of the entity label.
     * @return 
     */
    public int getLength(){
        return end - begin;
    }
    
    /**
     * Adds a new token the to token list.
     * @param t 
     */
    public void addToken(Token t) {
        this.tokens.add(t);
    }
    
    /**
     * It is a lookup function to find the entity in the original text.
     * @param originalText 
     */
    public void FindEntityInOriginalText(String originalText) {
        int tokenCount = this.tokens.size();

        if(tokenCount == 1){
            begin = this.tokens.get(0).originalBegin;
            end = this.tokens.get(0).originalEnd;
            label = originalText.substring(begin, end);
        }
        else if(tokenCount > 1){
            begin = this.tokens.get(0).originalBegin;
            end = this.tokens.get(tokenCount-1).originalEnd;
            label = originalText.substring(begin, end);
        }
        else{
            label = "";
        }
    }
    
    @Override
    public String toString() {
        return "text\t-->\t\"" + label + "\"\nbegin\t-->\t" + begin + "\nend\t-->\t" + end;
    }
}
