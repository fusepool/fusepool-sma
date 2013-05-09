/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.List;

/**
 * This object is used to store the results of the tokenization process. 
 * During tokenization the original text is divided into tokens.
 * 
 * @author Gabor
 */
public class TokenizedText {
    //original text, before tokenization
    String originalText;
    //tokenized text
    String text;
    //token list created from the original text
    List<Token> tokens;
    
    /**
     * Simple constructor.
     * @param originalText 
     */
    public TokenizedText(String originalText) {
        this.originalText = originalText;
        this.tokens = new ArrayList<Token>();
    }
    
    /**
     * Sets the tokenized text.
     * @param text 
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Sets the token list.
     * @param tokens 
     */
    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    /**
     * Adds a new token to the list.
     * @param t 
     */
    public void addToken(Token t) {
        this.tokens.add(t);
    }
    
    /**
     * Creates an entity object using its begin and end position in the 
     * tokenized text.
     * @param begin
     * @param end
     * @return 
     */
    public Entity FindMatch(int begin, int end){
        Entity e = new Entity();
        for (Token t : this.tokens) {
            if(t.begin >= begin && t.end <= end){
                e.addToken(t);
            }
        }
        e.FindEntityInOriginalText(originalText);
        return e;
    }
}
