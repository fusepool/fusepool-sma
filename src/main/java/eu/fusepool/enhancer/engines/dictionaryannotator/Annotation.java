package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an entity and stores its foundText, URI, begin and end
 * position, its weight, whether it overlaps with other entities and the foundText
 * divided into tokens.
 * @author Gábor Reményi
 */
public class Annotation {
    String foundText;
    String label;
    String synonym;
    String uri;
    String type;
    private int begin;
    private int end;
    private int tokenizedBegin;
    private int tokenizedEnd;
    double score;
    boolean overlap;
    List<Token> tokens;
    
    /**
     * Simple constructor.
     */
    public Annotation() {
        tokens = new ArrayList<Token>();
        score = 1;
    }
    
    /**
     * Simple constructor.
     */
    public Annotation(String foundText, String uri) {
        this.foundText = foundText;
        this.uri = uri;
        tokens = new ArrayList<Token>();
        score = 1;
    }
    
    /**
     * Simple constructor.
     */
    public Annotation(String foundText, String uri, String type) {
        this.foundText = foundText;
        this.uri = uri;
        this.type = type;
        tokens = new ArrayList<Token>();
        score = 1;
    }
    
    public Boolean HasNoNull(){
        if(this.label != null && this.foundText != null && this.uri != null){
            return true;
        }
        return false;
    }
    
    /**
     * Returns the foundText of the entity.
     * @return 
     */
    public String getFoundText() {
        return foundText;
    }

    public String getUri() {
        return uri;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSynonym() {
        return synonym;
    }

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    /**
     * Returns the foundText of the entity stripping it from new line characters.
     * @return 
     */
    public String getDisplayText() {
        return foundText.replace("\\n", " ");
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

    public void setFoundText(String label) {
        this.foundText = label;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setOverlap(boolean overlap) {
        this.overlap = overlap;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Returns the foundText of the entity as a token list.
     * @return 
     */
    public List<Token> getTokens() {
        return tokens;
    }
    
    /**
     * Returns the length of the entity foundText.
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
    
    public int getTokenizedBegin() {
        return tokenizedBegin;
    }

    public void setTokenizedBegin(int tokenizedBegin) {
        this.tokenizedBegin = tokenizedBegin;
    }

    public int getTokenizedEnd() {
        return tokenizedEnd;
    }

    public void setTokenizedEnd(int tokenizedEnd) {
        this.tokenizedEnd = tokenizedEnd;
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
            foundText = originalText.substring(begin, end);
        }
        else if(tokenCount > 1){
            begin = this.tokens.get(0).originalBegin;
            end = this.tokens.get(tokenCount-1).originalEnd;
            foundText = originalText.substring(begin, end);
        }
        else{
            foundText = "";
        }
    }

    @Override
    public String toString() {
        return "Annotation{" + "label=\"" + label + "\", synonym=\"" + synonym + "\", foundText=\"" + foundText + "\", uri=\"" + uri + "\", type=\"" + type + "\", begin=" + begin + ", end=" + end + ", score=" + score + '}';
    }
}
