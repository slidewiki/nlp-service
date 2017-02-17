package services.nlp;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.util.Sorter;

public class NLPComponent implements INLPComponent{
	
	public String propertyNameLanguage = "detectedLanguage";
	public String propertyNameTokens = "tokens";
	public String propertyNameNER = "NER";
	public String propertyNameTFIDF = "TFIDF";
	public int maxEntriesForTFIDFResult = 10;

	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
    private TFIDF tfidf;
    private IDocFrequencyProvider docFrequencyProvider;
    private boolean tfidfCalculationWithToLowerCase = true;

	@Inject
	public NLPComponent(ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer,
			INERLanguageDependent ner, TFIDF tfidf, IDocFrequencyProvider docFrequencyProvider) {
		super();
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.ner = ner;
		this.tfidf = tfidf;
		this.docFrequencyProvider = docFrequencyProvider;
	}
	
	public ObjectNode detectLanguage(String input, ObjectNode node){
    	String language = this.languageDetector.getLanguage(input);	
    	return node.put(propertyNameLanguage, language);
	}
	
	public ObjectNode tokenize(String input, String language, ObjectNode node){
    	String[] tokens = this.tokenizer.tokenize(input, language);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);
    	return node;
	}
	
	public ObjectNode ner(String[] tokens, String language, ObjectNode node){
    	List<NlpTag> ners = this.ner.getNEs(tokens, language);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);
    	return node;
	}
	
	public ObjectNode tfidf(String[] tokens, String language, ObjectNode node){
		Map<String,Double> tfidf = this.tfidf.getTFIDFValues(tokens, this.tfidfCalculationWithToLowerCase, language, this.docFrequencyProvider);
    	tfidf = Sorter.sortByValue(tfidf, true);
		JsonNode tfidfNode = Json.toJson(tfidf);
		node.set(propertyNameTFIDF, tfidfNode);
		return node;
	}
	
	public ObjectNode performNLP(String input, ObjectNode node){
		
		String detectedLanguage = this.languageDetector.getLanguage(input);	
		node.put(propertyNameLanguage, detectedLanguage);
		
    	String[] tokens = this.tokenizer.tokenize(input, detectedLanguage);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);

    	List<NlpTag> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);
    	
    	// TFIDF all token tyoes (regardless NER)
    	Map<String,Double> tfidfTypes = this.tfidf.getTFIDFValues(tokens, this.tfidfCalculationWithToLowerCase, detectedLanguage, this.docFrequencyProvider);
    	List<Entry<String,Double>> entries = Sorter.sortByValueAndReturnAsList(tfidfTypes, true);
     	
		// output as array for top x entries
		ArrayNode arrayNode = Json.newArray();
		int countEntries = 0;
		for (Entry<String,Double> entry : entries) {
			countEntries++;
			if(countEntries>maxEntriesForTFIDFResult){
				break;
			}
			ObjectNode singleNode = Json.newObject();
			singleNode.put("term", entry.getKey());
			singleNode.put("tfidf", entry.getValue());
			arrayNode.add(singleNode);
		}
		node.set(propertyNameTFIDF, arrayNode);
		
    	return node;
	}



	public String getPropertyNameLanguage() {
		return propertyNameLanguage;
	}



	public void setPropertyNameLanguage(String propertyNameLanguage) {
		this.propertyNameLanguage = propertyNameLanguage;
	}



	public String getPropertyNameTokens() {
		return propertyNameTokens;
	}



	public void setPropertyNameTokens(String propertyNameTokens) {
		this.propertyNameTokens = propertyNameTokens;
	}



	public String getPropertyNameNER() {
		return propertyNameNER;
	}



	public void setPropertyNameNER(String propertyNameNER) {
		this.propertyNameNER = propertyNameNER;
	}



	public ILanguageDetector getLanguageDetector() {
		return languageDetector;
	}



	public void setLanguageDetector(ILanguageDetector languageDetector) {
		this.languageDetector = languageDetector;
	}



	public ITokenizerLanguageDependent getTokenizer() {
		return tokenizer;
	}



	public void setTokenizer(ITokenizerLanguageDependent tokenizer) {
		this.tokenizer = tokenizer;
	}



	public INERLanguageDependent getNer() {
		return ner;
	}



	public void setNer(INERLanguageDependent ner) {
		this.ner = ner;
	}






	

	
}
