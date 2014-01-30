package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.io.IOException;
import java.io.Reader;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Parsing HTML to simple text. This is needed because the abstract and
 * the description in an RDF document sometimes contains HTML text.
 * @author Gábor Reményi
 */
public class Html2Text extends HTMLEditorKit.ParserCallback {  
    StringBuffer s;

    /**
     * Default constructor.
     */
    public Html2Text() {
    }

    /**
     * Parsing the input.
     * @param in
     * @throws IOException 
     */
    public void parse(Reader in) throws IOException {
        s = new StringBuffer();
        ParserDelegator delegator = new ParserDelegator();
        // the third parameter is TRUE to ignore charset directive  
        delegator.parse(in, this, Boolean.TRUE);
    }

    /**
     * Appends characters to the text.
     * @param text
     * @param pos
     */
    @Override
    public void handleText(char[] text, int pos) {
        s.append(text);
    }
    
    /**
     * Returns the parsed text.
     * @return 
     */
    public String getText() {
        return s.toString();
    } 
} 
