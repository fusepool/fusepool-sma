/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gabor
 */
public class TokenizedText {
    String originalText;
    String text;
    List<Token> tokens;

    public TokenizedText(String originalText) {
        this.originalText = originalText;
        this.tokens = new ArrayList<Token>();
    }
    
    public void setText(String text) {
        this.text = text;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    public void addToken(Token t) {
        this.tokens.add(t);
    }
    
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
