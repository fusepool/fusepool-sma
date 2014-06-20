package eu.fusepool.enhancer.engines.dictionaryannotator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.arabidopsis.ahocorasick.SearchResult;
import org.arabidopsis.ahocorasick.AhoCorasick;
import org.tartarus.snowball.SnowballStemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Gábor Reményi
 */
public class DictionaryAnnotator {
    // contains the search tree 
    private AhoCorasick tree;
    // OpenNLP tokenizer class
    private TokenizerModel modelTok;
    private Tokenizer tokenizer;
    private ProcessedText processedText;
    private List<ProcessedText> processedTerms;
    private DictionaryStore dictionary;
    private DictionaryStore originalDictionary;
    private DictionaryStore processedDictionary;
    private List<Annotation> annotations;
    
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
    public DictionaryAnnotator(DictionaryStore _dictionary, Tokenizer _tokenizer, String _stemmingLanguage, boolean _caseSensitive, 
            int _caseSensitiveLength, boolean _eliminateOverlapping) throws Exception  {
        
        dictionary = _dictionary;
        tokenizer = _tokenizer;
        stemmingLanguage = _stemmingLanguage;
        caseSensitive = _caseSensitive;
        caseSensitiveLength = _caseSensitiveLength;
        eliminateOverlapping = _eliminateOverlapping;
        
        // if no stemming language configuration is provided set stemming language to None
        if(stemmingLanguage == null || stemmingLanguage.isEmpty())
        {
            stemmingLanguage = "None";
        }
        // create a mapping between the language and the name of the class
        // responsible for the stemming of the current language
        languages = new HashMap<String, String>();
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
        String[] terms = ReadDictionary();
        
        // tokenize terms in the dictionary
        TokenizeTerms(terms);
        
        tree = new AhoCorasick();
        
        // if stemming language was set, perform stemming of terms in the dictionary
        if(stemming) {
            StemTerms();
            // add each term to the seachtree
            for (ProcessedText e : processedTerms) {
                tree.add(e.stemmedText, e.stemmedText);
            }
        }
        else{
             // add each term to the seachtree
            for (ProcessedText e : processedTerms) {
                tree.add(e.tokenizedText, e.tokenizedText);
            }
        }

        // create search trie
	tree.prepare();
    }
    
    /**
     * Processes the input text and returns the found annotations.
     * @param text  Input text on which the dictionary matching is executed
     * @return 
     */
    public List<Annotation> GetAnnotations(String text) throws Exception {
        // tokenize text
        TokenizeText(text);

        // if stemming language was set, perform stemming of the input text
        if(stemming) {
            StemText();
        }

        // perform the look-up
        FindAnnotations();
        
        // eliminate overlapping annotations
        EliminateOverlapping();
        
        List<Annotation> entitiesToReturn = new ArrayList<Annotation>(); 
        for(Annotation e : annotations){
            if(!e.overlap){
                entitiesToReturn.add(e);
            }
        }
        return entitiesToReturn;
    }
    
    /**
     * Processes the input text and returns a tagged text.
     * @param text
     * @return 
     */
    public String GetURITaggedText(String text) throws Exception {
        String taggedText = "";
        String plain, tagged;
        
        // tokenize text
        TokenizeText(text);
        
        // if stemming language was set, perform stemming of the input text
        if(stemming) {
            StemText();
        }
        
        // perform the look-up
        FindAnnotations();
        
        // eliminate overlapping annotations
        EliminateOverlapping();
        
        int prevEnd = 0;
        for(Annotation e : annotations){
            if(!e.isOverlap()){
                if (e.getBegin() < prevEnd && prevEnd != 0) {
                    if (e.getEnd() > prevEnd) {
                        prevEnd = e.getEnd();
                    }
                } else {
                    plain = text.substring(prevEnd, e.getBegin());
                    tagged = "<entity uri=\"" + e.getUri() + "\">" + text.substring(e.getBegin(), e.getEnd()) + "</entity>";
                    taggedText += plain + tagged;
                    prevEnd = e.getEnd();
                }
            }
        }
        taggedText += text.substring(prevEnd, text.length());
        return taggedText;
    }
    
    /**
     * Processes the input text and returns a tagged text.
     * @param text
     * @return 
     */
    public String GetTypeTaggedText(String text) throws Exception {
        String taggedText = "";
        String plain, tagged;
        
        // tokenize text
        TokenizeText(text);

        // if stemming language was set, perform stemming of the input text
        if(stemming) {
            StemText();
        }
        
        // perform the look-up
        FindAnnotations();
        
        // eliminate overlapping annotations
        EliminateOverlapping();
        
        int prevEnd = 0;
        for(Annotation e : annotations){
            if(!e.isOverlap()){
                //e.setType(originalDictionary.GetType(e.getUri()));
                if (e.getBegin() < prevEnd && prevEnd != 0) {
                    if (e.getEnd() > prevEnd) {
                        prevEnd = e.getEnd();
                    }
                } else {
                    plain = text.substring(prevEnd, e.getBegin());
                    tagged = "<" + e.getType().toUpperCase() + ">" + text.substring(e.getBegin(), e.getEnd()) + "</" + e.getType().toUpperCase() + ">";
                    taggedText += plain + tagged;
                    prevEnd = e.getEnd();
                }
            }
        }
        taggedText += text.substring(prevEnd, text.length());
        return taggedText;
    }
    
    /**
     * Creates the dictionary from the HashMap which contains foundText-URI pairs.
     * @param input The original dictionary as a HashMap (foundText-URI pairs)
     * @return 
     */
    private String[] ReadDictionary() {
        String[] labels = new String[dictionary.keywords.size()];
        Set set = dictionary.keywords.entrySet();
        Iterator iterator = set.iterator();
        
        int index = 0;
        while(iterator.hasNext()){
            Map.Entry me = (Map.Entry)iterator.next();
            labels[index] = (String) me.getKey();
            index++;
        } 

        return labels;
    }

    /**
     * Tokenizes the all the annotations in the dictionary and returns the 
     * tokenized annotations. 
     * If caseSensitive is true and caseSensitiveLength > 0 all tokens 
     * whose length is equal or bigger than the caseSensitiveLength are 
     * converted to lowercase.
     * If caseSensitive is true and caseSensitiveLength = 0 no conversion
     * is applied.
     * If caseSensitive is false all tokens are converted to lowercase.
     * 
     * @param originalTerms
     */
    public void TokenizeTerms(String[] originalTerms) {
        StringBuilder sb;
        Span[] spans;
        String[] terms = originalTerms;
        
        processedTerms = new ArrayList<ProcessedText>();
        ProcessedText processedTerm;
        for (int i = 0; i < originalTerms.length; i++) {
            processedTerm = new ProcessedText(originalTerms[i]);

            spans = tokenizer.tokenizePos(terms[i]);
            sb = new StringBuilder();
            
            Token t;
            String word, name;
            Concept concept;
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

                        processedTerm.addToken(t);

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

                        processedTerm.addToken(t);

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

                    processedTerm.addToken(t);

                    sb.append(word);
                    sb.append(" ");
                }
            }
            name = sb.toString();
            concept = dictionary.GetConcept(processedTerm.originalText, false);
            
            processedTerm.setTokenizedText(sb.toString());
            processedTerms.add(processedTerm);
            
            originalDictionary.AddElement(name.substring(1, name.length() - 1), concept);
        }
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
     */
    public void TokenizeText(String text) {
        Span[] spans;
        StringBuilder sb;
        
        processedText = new ProcessedText(text);

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

                    processedText.addToken(t);

                    sb.append(word);
                    sb.append(" ");
                }  
            }
            else{
                for (Span span : spans) {
                    word = text.substring(span.getStart(), span.getEnd());
                    t = new Token(word);
                    t.setOriginalBegin(span.getStart());
                    t.setOriginalEnd(span.getEnd());

                    begin = position + 1;
                    t.setBegin(begin);

                    end = begin + word.length();
                    t.setEnd(end);

                    position = end;

                    processedText.addToken(t);

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

                processedText.addToken(t);

                sb.append(word);
                sb.append(" ");
            }
        }
        processedText.setTokenizedText(sb.toString());
    }
    
    /**
     * This function runs the Aho-Corasick string matching algorithm on the 
     * tokenized (and stemmed) text using the search tree built from the dictionary.
     * @param processedTerm The tokenized text
     */
    private void FindAnnotations(){
        annotations = new ArrayList<Annotation>();
        String text = stemming ? processedText.stemmedText : processedText.tokenizedText;
        Annotation annotation;
        String str = "";
        Concept concept;
        int begin, end, length, lastIndex, maxlength;
        for (Iterator iter = tree.search(text.toCharArray()); iter.hasNext(); ) {
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
                end = result.getLastIndex() - 1;
                begin = end - length;

                annotation = processedText.FindMatch(begin, end);
                annotation.setTokenizedBegin(begin);
                annotation.setTokenizedEnd(end);
                annotation.uri = stemming ? processedDictionary.GetURI(str, true) : originalDictionary.GetURI(str, true);
                
                if(annotation.getUri() != null){
                    concept = stemming ? processedDictionary.GetConcept(str, true) : originalDictionary.GetConcept(str, true);
                    
                    if(concept.IsLabel()){
                        annotation.label = concept.labelText;
                        annotation.synonym = null;
                    }
                    else{
                        annotation.label = dictionary.GetLabelByURI(annotation.getUri());
                        annotation.synonym = concept.labelText;
                    }
                    
                    if(stemming)
                    {
                        if(caseSensitive){
                            annotation.score = LevenshteinDistance.GetNormalizedDistance(concept.labelText, annotation.foundText);
                        }
                        else{
                            annotation.score = LevenshteinDistance.GetNormalizedDistance(concept.labelText.toLowerCase(), annotation.foundText.toLowerCase());
                            LevenshteinDistance.PrintNormalizedDistance(concept.labelText.toLowerCase(), annotation.foundText.toLowerCase());
                        }
                    }
                    
                    annotation.type = concept.type;
                    
                    annotations.add(annotation);
                }
            }
	}
    }
    
    /**
     * Eliminates the overlaps among all the annotations found in the text. If 
     * we have two annotations, Entity1 and Entity2, and Entity1 is within the 
     * boundaries of Entity2, then Entity1 is marked and is later discarded.
     * If the variable eliminateOverlapping is true, annotations whose 
     * boundaries are overlapping are also marked and are later discarded.
     */
    public void EliminateOverlapping(){
        Annotation e1, e2;
        for (int i = 0; i < annotations.size(); i++) {
            e1 = annotations.get(i);
            for (int j = 0; j < annotations.size(); j++) {
                e2 = annotations.get(j);
                if(i == j){
                    continue;
                }
                else{
                    if(e1.getBegin() > e2.getEnd()){
                        continue;
                    }
                    else if(e1.getEnd() < e2.getBegin()){
                        continue;
                    }
                    else{
                        if(e1.getBegin() >= e2.getBegin() && e1.getEnd() <= e2.getEnd()){
                            e1.overlap = true;
                            break;
                        }
                        else if(eliminateOverlapping){
                            if(e1.getBegin() > e2.getBegin() && e1.getEnd() > e2.getEnd() && e1.getBegin() < e2.getEnd()){
                                e1.overlap = true;
                                break;
                            }
                            else if(e1.getBegin() < e2.getBegin() && e1.getEnd() < e2.getEnd() && e1.getEnd() < e2.getBegin()){
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
     * The function is responsible for the stemming of each annotation in the dictionary
     * based on the stemming language defined in the constructor.
     */
    private void StemTerms() throws Exception {
            int offset, overallOffset = 0;
        String word, name;
        Concept concept;
        StringBuilder sb;

        Class stemClass = Class.forName("org.tartarus.snowball.ext." + stemmingLanguage);
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();

        for (ProcessedText processedTerm : processedTerms) {
            sb = new StringBuilder();
            sb.append(" ");
            for (Token token : processedTerm.tokens) {
                stemmer.setCurrent(token.text);
                stemmer.stem();
                word = stemmer.getCurrent();

                offset = token.text.length() - word.length();

                token.begin -= overallOffset;
                overallOffset += offset;
                token.end -= overallOffset;

                sb.append(word);
                sb.append(" ");

                token.stem = word;
            }
            name = sb.toString();
            concept = dictionary.GetConcept(processedTerm.originalText, false);
            processedTerm.setStemmedText(name);

            processedDictionary.AddElement(name.substring(1, name.length() - 1), concept);
        }
    }
    
    /**
     * The function is responsible for the stemming of the main text based on
     * the stemming language defined in the constructor.
     */
    private void StemText() throws Exception{
            int offset, overallOffset = 0;
        String word;
        StringBuilder sb;

        Class stemClass = Class.forName("org.tartarus.snowball.ext." + stemmingLanguage);
        SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();

        sb = new StringBuilder();
        sb.append(" ");
        for (Token token : processedText.tokens) {
            stemmer.setCurrent(token.text);
            stemmer.stem();
            word = stemmer.getCurrent();

            offset = token.text.length() - word.length();

            token.begin -= overallOffset;
            overallOffset += offset;
            token.end -= overallOffset;

            sb.append(word);

            sb.append(" ");

            token.stem = word;
        }
        processedText.setStemmedText(sb.toString());
    }
}
