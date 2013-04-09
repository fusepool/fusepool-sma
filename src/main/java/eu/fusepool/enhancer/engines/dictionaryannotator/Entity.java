/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.List;
import org.apache.clerezza.rdf.core.UriRef;

/**
 *
 * @author Gabor
 */
public class Entity {
    String text;
    UriRef uri;
    int begin;
    int end;
    float weight;
    boolean overlap;
    List<Token> tokens;

    public Entity() {
        tokens = new ArrayList<Token>();
        weight = 1;
    }

    public String getText() {
        return text;
    }
    
    public String getDisplayText() {
        return text.replace("\\n", " ");
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public boolean isOverlap() {
        return overlap;
    }

    public List<Token> getTokens() {
        return tokens;
    }
    
    public int getLength(){
        return end - begin;
    }
    
    public void addToken(Token t) {
        this.tokens.add(t);
    }
    
    public void FindEntityInOriginalText(String originalText) {
        int tokenCount = this.tokens.size();

        if(tokenCount == 1){
            begin = this.tokens.get(0).originalBegin;
            end = this.tokens.get(0).originalEnd;
            text = originalText.substring(begin, end);
//            System.out.println(this.tokens.get(0).toString());
        }
        else if(tokenCount > 1){
//            for (int i = 0; i < this.tokens.size(); i++) {
//                System.out.println(this.tokens.get(i).toString());
//            }
            begin = this.tokens.get(0).originalBegin;
            end = this.tokens.get(tokenCount-1).originalEnd;
            text = originalText.substring(begin, end);
        }
        else{
            text = "";
        }
    }
    
    @Override
    public String toString() {
        return "text\t-->\t\"" + text + "\"\nbegin\t-->\t" + begin + "\nend\t-->\t" + end;
    }
}
