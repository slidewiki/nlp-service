package services.nlp;

import java.util.List;

import javax.inject.Inject;

public class TaggerComponent implements ITagger{

	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
//  private ITagResultSelector tagresultSelector; // maybe implement in future
    
    
    @Inject
    public TaggerComponent(ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer,
    		INERLanguageDependent ner) {
    	
    	this.languageDetector = languageDetector;
    	this.tokenizer = tokenizer;
    	this.ner = ner;
    }
    
    
	@Override
	public List<NlpTag> getTags(String input) {
		
		// detect language
		String detectedLanguage = this.languageDetector.getLanguage(input);	
		
		// tokenize
    	String[] tokens = this.tokenizer.tokenize(input, detectedLanguage);	
    	
    	// tags based on NER
    	List<NlpTag> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	
    	return ners;

	}

    
   

    

}