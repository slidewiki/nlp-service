package services.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;

public class TaggerComponent implements ITagger{

	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
    private IDocFrequencyProvider docFrequencyProvider;

    private int tfidfMaxTypesToReturn = 10;
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
		
		List<NlpTag> tags = new ArrayList<>();
		
		// detect language
		String detectedLanguage = this.languageDetector.getLanguage(input);	
		
		// tokenize
    	String[] tokens = this.tokenizer.tokenize(input, detectedLanguage);	
    	
    	// tags based on NER
    	List<NlpTag> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	
    	tags.addAll(ners);
    	
    	// tags based on tfidf and tokens
    	List<Entry<String, Double>> tfidfList = TFIDF.getTFIDFValuesTopX(tokens, true, detectedLanguage, docFrequencyProvider, tfidfMaxTypesToReturn);
    	for (Entry<String, Double> entry : tfidfList) {
    		String name = entry.getKey();
    		Double tfidf = entry.getValue(); 
			NlpTag nlptag = new NlpTag(name, null, "TFIDF_basedOnTokens_language-" + detectedLanguage, tfidf, null);
			tags.add(nlptag);
			// TODO: tfidf value is not a probability value but is stored in the field of probability of NLPTag -> discuss this
    	}
    	
    	return tags;

	}

    
   

    

}