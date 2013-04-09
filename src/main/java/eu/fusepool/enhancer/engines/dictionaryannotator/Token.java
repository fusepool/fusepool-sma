/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

/**
 *
 * @author Gabor
 */
public class Token {
    String text;
    int begin;
    int end;
    int originalBegin;
    int originalEnd;

    public Token(String text) {
        this.text = text;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setOriginalBegin(int originalBegin) {
        this.originalBegin = originalBegin;
    }

    public void setOriginalEnd(int originalEnd) {
        this.originalEnd = originalEnd;
    }

    @Override
    public String toString() {
        return "Token{" + "text=" + text + ", begin=" + begin + ", end=" + end + '}';
    }
}
