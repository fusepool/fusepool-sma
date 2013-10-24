/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.io.IOException;
import java.io.Reader;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 *
 * @author Gabor
 */
public class Html2Text extends HTMLEditorKit.ParserCallback {  
    StringBuffer s;

    public Html2Text() {
    }

    public void parse(Reader in) throws IOException {
        s = new StringBuffer();
        ParserDelegator delegator = new ParserDelegator();
        // the third parameter is TRUE to ignore charset directive  
        delegator.parse(in, this, Boolean.TRUE);
    }

    public void handleText(char[] text, int pos) {
        s.append(text);
    }

    public String getText() {
        return s.toString();
    } 
} 
