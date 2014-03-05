package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import org.apache.clerezza.rdf.core.UriRef;
import org.arabidopsis.ahocorasick.SearchResult;
import org.arabidopsis.ahocorasick.AhoCorasick;
import org.tartarus.snowball.SnowballStemmer;

/**
 * 
 * @author Gábor Reményi
 */
public class DictionaryAnnotator {
    // contains the search tree 
    private AhoCorasick tree;
    // OpenNLP tokenizer class
    private Tokenizer tokenizer;
    private TokenizedText tokenizedText;
    private List<TokenizedText> tokenizedTerms;
    private DictionaryStore originalDictionary;
    private DictionaryStore processedDictionary;
    private List<Entity> entities;
    
    private boolean caseSensitive;
    private int caseSensitiveLength;
    private boolean eliminateOverlapping;
    private String stemmingLanguage;
    private Boolean stemming;
    private Map<String,String> languages;
    
    /**
     * Initializes the dictionary annotator by reading the dictionary and building
     * the search tree which is the soul of the Aho-Corasic algorithm.
     * @param dictionary
     * @param _tokenizer
     * @param _stemmingLanguage
     * @param _caseSensitive
     * @param _caseSensitiveLength
     * @param _eliminateOverlapping 
     */
    public DictionaryAnnotator(DictionaryStore dictionary, Tokenizer _tokenizer, String _stemmingLanguage, boolean _caseSensitive, 
            int _caseSensitiveLength, boolean _eliminateOverlapping) {
        
        stemmingLanguage = _stemmingLanguage;
        caseSensitive = _caseSensitive;
        caseSensitiveLength = _caseSensitiveLength;
        eliminateOverlapping = _eliminateOverlapping;
        tokenizer = _tokenizer;

        // if no stemming language configuration is provided set stemming language to None
        if(stemmingLanguage == null)
        {
            stemmingLanguage = "None";
        }
        else if(stemmingLanguage.isEmpty())
        {
            stemmingLanguage = "None";
        }
        // create a mapping between the language and the name of the class
        // responsible for the stemming of the current language
        languages = new HashMap<String,String>();
        languages.put("None", "");
        languages.put("Danish", "danishStemmer");
        languages.put("Dutch", "dutchStemmer");
        languages.put("English", "englishStemmer");
        languages.put("Finnish", "finnishStemmer");
        languages.put("French", "frenchStemmer");
        languages.put("German", "germanStemmer");
        languages.put("Hungarian", "hungarianStemmer");
        languages.put("Italian", "italianStemmer");
        languages.put("Norwegian", "norwegianStemmer");
        //languages.put("english2", "porterStemmer");
        languages.put("Portuguese", "portugueseStemmer");
        languages.put("Romanian", "romanianStemmer");
        languages.put("Russian", "russianStemmer");
        languages.put("Spanish", "spanishStemmer");
        languages.put("Swedish", "swedishStemmer");
        languages.put("Turkish", "turkishStemmer");
        
        originalDictionary = new DictionaryStore();
        processedDictionary = new DictionaryStore();
        
        stemming = false;
        if(stemmingLanguage != null){
            if(!languages.get(stemmingLanguage).isEmpty()){
                stemmingLanguage = languages.get(stemmingLanguage);
                stemming = true;
            }
        }
        
        // read labels from the input dictionary
        String[] terms = ReadDictionary(dictionary.keywords);
        // tokenize terms in the dictionary
        tokenizedTerms = TokenizeTerms(terms);
        
        // if stemming language was set, perform stemming of terms in the dictionary
        if(stemming) {
            StemTerms();
        }
        
        tree = new AhoCorasick();
        // add each term to the seachtree
        for (TokenizedText e : tokenizedTerms) {
            tree.add(e.text, e.text);
	}
	tree.prepare();
    }
    
    /**
     * Processes the input text and returns the found entities.
     * @param text  Input text on which the dictionary matching is executed
     * @return 
     */
    public List<Entity> Annotate(String text){
        // tokenize text
        tokenizedText = TokenizeText(text);

        // if stemming language was set, perform stemming of the input text
        if(stemming) {
            StemText();
        }

        // perform the look-up
        FindEntities(tokenizedText);
        
        // eliminate overlapping entities
        EliminateOverlapping();
        
        List<Entity> entitiesToReturn = new ArrayList<Entity>(); 
        for(Entity e : entities){
            if(!e.overlap){
                entitiesToReturn.add(e);
            }
        }
        return entitiesToReturn;
    }
    
    /**
     * Creates the dictionary from the HashMap which contains label-URI pairs.
     * @param input The original dictionary as a HashMap (label-URI pairs)
     * @return 
     */
    private String[] ReadDictionary(Map<String,UriRef> dictionary) {
        String[] labels = new String[dictionary.size()];
        Set set = dictionary.entrySet();
        Iterator iterator = set.iterator();
        
        int index = 0;
        String label;
        UriRef uri;
        while(iterator.hasNext()){
            Map.Entry me = (Map.Entry)iterator.next();
            label = (String) me.getKey();
            uri = (UriRef) me.getValue();
            labels[index] = label;
            originalDictionary.AddElement(label, uri);       
            index++;
        }         
        return labels;
    }

    /**
     * Tokenizes the all the entities in the dictionary and returns the 
     * tokenized entities. 
     * If caseSensitive is true and caseSensitiveLength > 0 all tokens 
     * whose length is equal or bigger than the caseSensitiveLength are 
     * converted to lowercase.
     * If caseSensitive is true and caseSensitiveLength = 0 no conversion
     * is applied.
     * If caseSensitive is false all tokens are converted to lowercase.
     * 
     * @param originalTerms
     * @return 
     */
    public List<TokenizedText> TokenizeTerms(String[] originalTerms) {
        StringBuilder sb;
        Span[] spans;
        String[] terms = originalTerms;
        
        List<TokenizedText> tlist = new ArrayList<TokenizedText>();
        TokenizedText tokText;
        for (int i = 0; i < originalTerms.length; i++) {
            tokText = new TokenizedText(originalTerms[i]);

            spans = tokenizer.tokenizePos(terms[i]);
            sb = new StringBuilder();
            
            Token t;
            String word;
            int position = 1;
            int begin, end;
            sb.append(" ");
            if (caseSensitive) {
                if (caseSensitiveLength > 0) {
                    for (Span span : spans) {
                        word = terms[i].substring(span.getStart(), span.getEnd());
                        
                        if (word.length() > caseSensitiveLength) {
                            word = word.toLowerCase();
                        }
                        
                        t = new Token(word);
                        t.setOriginalBegin(span.getStart());
                        t.setOriginalEnd(span.getEnd());

                        begin = position + 1;
                        t.setBegin(begin);

                        end = begin + word.length();
                        t.setEnd(end);

                        position = end;

                        tokText.addToken(t);

                        sb.append(word);
                        sb.append(" ");
                    }
                }
                else{
                    for (Span span : spans) {
                        word = terms[i].substring(span.getStart(), span.getEnd());
                        
                        t = new Token(word);
                        t.setOriginalBegin(span.getStart());
                        t.setOriginalEnd(span.getEnd());

                        begin = position + 1;
                        t.setBegin(begin);

                        end = begin + word.length();
                        t.setEnd(end);

                        position = end;

                        tokText.addToken(t);

                        sb.append(word);
                        sb.append(" ");
                    }
                }
            } else {
                for (Span span : spans) {
                    word = terms[i].substring(span.getStart(), span.getEnd());

                    word = word.toLowerCase();

                    t = new Token(word);
                    t.setOriginalBegin(span.getStart());
                    t.setOriginalEnd(span.getEnd());

                    begin = position + 1;
                    t.setBegin(begin);

                    end = begin + word.length();
                    t.setEnd(end);

                    position = end;

                    tokText.addToken(t);

                    sb.append(word);
                    sb.append(" ");
                }
            }
              
            
            tokText.setText(sb.toString());
            tlist.add(tokText);
        }
        return tlist;
    }
    
    /**
     * Tokenizes the original text and returns the tokenized text. 
     * If caseSensitive is true and caseSensitiveLength > 0 all tokens 
     * whose length is equal or bigger than the caseSensitiveLength are 
     * converted to lowercase.
     * If caseSensitive is true and caseSensitiveLength = 0 no conversion
     * is applied.
     * If caseSensitive is false all tokens are converted to lowercase.
     * 
     * @param text
     * @return 
     */
    public TokenizedText TokenizeText(String text) {
        Span[] spans;
        StringBuilder sb;
        
        TokenizedText tokText = new TokenizedText(text);

        spans = tokenizer.tokenizePos(text);
        sb = new StringBuilder();

        Token t;
        String word;
        int position = 0;
        int begin, end;

        sb.append(" ");
        if(caseSensitive){
            if(caseSensitiveLength > 0){
                for (Span span : spans) {
                    word = text.substring(span.getStart(), span.getEnd());

                    if(word.length() > caseSensitiveLength){
                        word = word.toLowerCase();
                    }

                    t = new Token(word);
                    t.setOriginalBegin(span.getStart());
                    t.setOriginalEnd(span.getEnd());

                    begin = position + 1;
                    t.setBegin(begin);

                    end = begin + word.length();
                    t.setEnd(end);

                    position = end;

                    tokText.addToken(t);

                    sb.append(word);
                    sb.append(" ");
                }  
            }
            else{
                for (Span span : spans) {
                    word = text.substring(span.getStart(), span.getEnd());
                    System.out.println(word);
                    t = new Token(word);
                    t.setOriginalBegin(span.getStart());
                    t.setOriginalEnd(span.getEnd());

                    begin = position + 1;
                    t.setBegin(begin);

                    end = begin + word.length();
                    t.setEnd(end);

                    position = end;

                    tokText.addToken(t);

                    sb.append(word);
                    sb.append(" ");
                }          
            }
        }
        else{
            for (Span span : spans) {
                word = text.substring(span.getStart(), span.getEnd());

                word = word.toLowerCase();

                t = new Token(word);
                t.setOriginalBegin(span.getStart());
                t.setOriginalEnd(span.getEnd());

                begin = position + 1;
                t.setBegin(begin);

                end = begin + word.length();
                t.setEnd(end);

                position = end;

                tokText.addToken(t);

                sb.append(word);
                sb.append(" ");
            }
        }
        tokText.setText(sb.toString());
        return tokText;
    }
    
    /**
     * This function runs the Aho-Corasick string matching algorithm on the 
     * tokenized (and stemmed) text using the search tree built from the dictionary.
     * @param tokenizedText The tokenized text
     */
    private void FindEntities(TokenizedText tokenizedText){
        entities = new ArrayList<Entity>();
        Entity entity = null;
        String str = "";
        int length = 0, lastIndex = 0, maxlength = 0;
        for (Iterator iter = tree.search(tokenizedText.text.toCharArray()); iter.hasNext(); ) {
	    SearchResult result = (SearchResult) iter.next();
            maxlength = 0;
            for(Object e : result.getOutputs()){
                length = e.toString().length();
                if(maxlength < length){
                    str = e.toString();
                    maxlength = length;
                }
            }
            if(!str.equals("")){
            	str = str.substring(1, str.length() - 1);
                length = str.length();
                lastIndex = result.getLastIndex() - 1;
                
                entity = tokenizedText.FindMatch((lastIndex - length), lastIndex);
                //entity.text = str;
                entity.uri = stemming ? processedDictionary.GetURI(str) : originalDictionary.GetURI(entity.label);
                entities.add(entity);
            }
	}
    }
    
    /**
     * Eliminates the overlaps among all the entities found in the text. If 
     * we have two entities, Entity1 and Entity2, and Entity1 is within the 
     * boundaries of Entity2, then Entity1 is marked and is later discarded.
     * If the variable eliminateOverlapping is true, entities whose 
     * boundaries are overlapping are also marked and are later discarded.
     */
    public void EliminateOverlapping(){
        Entity e1, e2;
        for (int i = 0; i < entities.size(); i++) {
            e1 = entities.get(i);
            for (int j = 0; j < entities.size(); j++) {
                e2 = entities.get(j);
                if(i == j){
                    continue;
                }
                else{
                    if(e1.begin > e2.end){
                        continue;
                    }
                    else if(e1.end < e2.begin){
                        continue;
                    }
                    else{
                        if(e1.begin >= e2.begin && e1.end <= e2.end){
                            e1.overlap = true;
                            break;
                        }
                        else if(eliminateOverlapping){
                            if(e1.begin > e2.begin && e1.end > e2.end && e1.begin < e2.end){
                                e1.overlap = true;
                                break;
                            }
                            else if(e1.begin < e2.begin && e1.end < e2.end && e1.end < e2.begin){
                                e1.overlap = true;
                                break;
                            }
                            else{
                                continue;
                            }
                        }
                        else{
                            continue;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * The function is responsible for the stemming of each entity in the dictionary
     * based on the stemming language defined in the constructor.
     */
    private void StemTerms() {
        try {
            int offset = 0;
            int overallOffset = 0;
            String word = "";
            String name;
            UriRef uri;
            StringBuilder sb;
            Class stemClass = Class.forName("org.tartarus.snowball.ext." + stemmingLanguage);
            SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
            for (TokenizedText tokenizedText : tokenizedTerms) {
                sb = new StringBuilder();
                sb.append(" ");
                for (Token token : tokenizedText.tokens) {
                    stemmer.setCurrent(token.text);
                    stemmer.stem();
                    word = stemmer.getCurrent();  
                    
                    offset = token.text.length() - word.length();
                    
                    token.begin -= overallOffset;
                    overallOffset += offset;
                    token.end -= overallOffset;
                    
                    sb.append(word);
                    sb.append(" ");

                    token.text = word;
                }
                name = sb.toString();
                uri = originalDictionary.GetURI(tokenizedText.originalText);
                tokenizedText.setText(name);
                processedDictionary.AddElement(name.substring(1, name.length() - 1), uri);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * The function is responsible for the stemming of the main text based on
     * the stemming language defined in the constructor.
     */
    private void StemText() {
        try {
            int offset = 0;
            int overallOffset = 0;
            String word = "";
            StringBuilder sb = new StringBuilder();
            Class stemClass = Class.forName("org.tartarus.snowball.ext." + stemmingLanguage);
            SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();

            sb = new StringBuilder();
            sb.append(" ");
            for (Token token : tokenizedText.tokens) {
                stemmer.setCurrent(token.text);
                stemmer.stem();
                word = stemmer.getCurrent();

                offset = token.text.length() - word.length();

                token.begin -= overallOffset;
                overallOffset += offset;
                token.end -= overallOffset;

                sb.append(word);

                sb.append(" ");

                token.text = word;
            }
            tokenizedText.setText(sb.toString());
            
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
