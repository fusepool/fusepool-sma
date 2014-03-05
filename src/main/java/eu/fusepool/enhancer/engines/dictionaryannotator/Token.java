package eu.fusepool.enhancer.engines.dictionaryannotator;

/**
 * This class represents a single token created during tokenization. 
 * Tokenization is the process of breaking a stream of text up into words, 
 * phrases, symbols, or other meaningful elements called tokens. 
 * 
 * @author Gábor Reményi
 */
public class Token {
    // the token
    String text;
    // the begining of the token in the tokenized text
    int begin;
    // the end of the token in the tokenized text
    int end;
    // the begining of the token in the original text
    int originalBegin;
    // the end of the token in the original text
    int originalEnd;
    
    /**
     * Simple constructor. 
     * @param text The token
     */
    public Token(String text) {
        this.text = text;
    }
    
    /**
     * Sets the begin position of the token in the text.
     * @param begin 
     */
    public void setBegin(int begin) {
        this.begin = begin;
    }
    
    /**
     * Sets the end position of the token in the text.
     * @param end 
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * Sets the begin position of the token in the original text.
     * @param originalBegin 
     */
    public void setOriginalBegin(int originalBegin) {
        this.originalBegin = originalBegin;
    }
    
    /**
     * Sets the end position of the token in the original text.
     * @param originalEnd 
     */
    public void setOriginalEnd(int originalEnd) {
        this.originalEnd = originalEnd;
    }

    @Override
    public String toString() {
        return "Token{" + "text=" + text + ", begin=" + begin + ", end=" + end + '}';
    }
}
