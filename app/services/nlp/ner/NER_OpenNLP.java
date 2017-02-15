package services.nlp.ner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span; 

public class NER_OpenNLP implements INER{

    private NameFinderME nameFinder;
    private String name;
     
    
    public NER_OpenNLP(String filepathModel, String name){
       
    	this.name = name;
        loadModelFromFile(filepathModel);
        
    }
    
    @Override
    public Span[] getNEs(String[] tokens){
       Span[] nameSpans = nameFinder.find(tokens);
       return nameSpans;
    }
    
     
    private void loadModelFromFile(String filepath){
    
        
        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream(filepath);
            TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
            this.nameFinder = new NameFinderME(model);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
    
    public static String getTokenStringFromSpan(Span span, String[] tokens){
    	int startIndex = span.getStart();
        int endIndex = span.getEnd();
 
    	StringBuilder sb = new StringBuilder();
        sb.append(tokens[startIndex]);
        for (int i = startIndex+1; i < endIndex; i++) {
            sb.append(" ");
            sb.append(tokens[i]);
        }
        return sb.toString();
    }
    

    
    /**
     * Convenience method for getting a string representation for spans and tokens and span types, like e.g.:
     * "New York [location], Hans Müller [person]" 
     * @param tokens
     * @param spans
     * @param delimiterBetweenSpanResults
     * @param includeTypeInOutput
     * @return
     */
    public static String getStringRepresentationOfSpans(String[] tokens, Span[] spans, String delimiterBetweenSpanResults, boolean includeTypeInOutput){
        
        StringBuilder sb = new StringBuilder();
        for(Span nameSpan: spans){
           
            String namedEntity = getTokenStringFromSpan(nameSpan, tokens);
            sb.append(delimiterBetweenSpanResults);
            sb.append(namedEntity);
            if(includeTypeInOutput){
                sb.append(" [");
                sb.append(nameSpan.getType());
                sb.append("]");
            }
         }
        
        String result = sb.toString().trim();
        if(result.length() == 0){
            return "NO ENTITIES FOUND";
        }
        return result;
    }

	@Override
	public String getName() {
		
		return this.name;
	}

	
}
