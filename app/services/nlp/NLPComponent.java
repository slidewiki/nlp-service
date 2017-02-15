package services.nlp;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.tokenization.ITokenizerLanguageDependent;

public class NLPComponent implements INLPComponent{
	
	public String propertyNameLanguage = "detectedLanguage";
	public String propertyNameTokens = "tokens";
	public String propertyNameNER = "NER";

	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;

	@Inject
	public NLPComponent(ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer,
			INERLanguageDependent ner) {
		super();
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.ner = ner;
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
	
	public ObjectNode performNLP(String input, ObjectNode node){
		
		String detectedLanguage = this.languageDetector.getLanguage(input);	
		node.put(propertyNameLanguage, detectedLanguage);
		
    	String[] tokens = this.tokenizer.tokenize(input, detectedLanguage);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);

    	List<NlpTag> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);
    	
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
