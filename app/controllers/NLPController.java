package controllers;

import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.nlp.NLPComponent;
import services.nlp.dbpediaspotlight.DBPediaSpotlightUtil;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tokenization.ITokenizerLanguageDependent;


@Api(value = "/nlp")
public class NLPController extends Controller{
    
    private NLPComponent nlpComponent;

    
    @Inject
    public NLPController(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner,  Map<String,IDocFrequencyProvider> mapDocFrequencyProvider) {
		super();
		this.nlpComponent = new NLPComponent(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, mapDocFrequencyProvider);
	}

    @javax.ws.rs.Path(value = "/htmlToText")
    @ApiOperation(value = "html to text", notes = "")
    public Result htmlToText(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.getPlainTextFromHTML(inputText, result);
    	
        return ok(result);
       
    }
    
    @javax.ws.rs.Path(value = "/language")
    @ApiOperation(value = "returns language detection result for given input", notes = "language detection performed for given input")
    public Result detectLanguage(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.detectLanguage(inputText, result);
    	
        return ok(result);
       
    }

	@javax.ws.rs.Path(value = "/tokenize")
    @ApiOperation(value = "returns tokens for given input", notes = "tokens are calculated for the given input")
    public Result tokenize(
    		@ApiParam(value = "input text") String inputText, 
    		@ApiParam(value = "language") String language) {
    	
		ObjectNode result = Json.newObject();
		result = nlpComponent.tokenize(inputText, language, result);
    	
        return ok(result);
       
    }
    
    

    
    @javax.ws.rs.Path(value = "/nlp")
    @ApiOperation(value = "performs different available nlp steps", notes = "different nlp steps are performed, currently: language detection, tokenization, NER and tfidf (top 10)")
    public Result performNLP(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
    	double dbpediaSpotlightConfidence = DBPediaSpotlightUtil.dbpediaspotlightdefaultConfidence; // TODO: make this conigurable
		result = nlpComponent.performNLP(inputText, result, dbpediaSpotlightConfidence);
    	
        return ok(result);
       
    }
    
    @javax.ws.rs.Path(value = "/nlpForDeck")
    @ApiOperation(value = "performs different available nlp steps for content of deck", notes = "different nlp steps are performed, currently: language detection, tokenization, NER, DBPedia Spotlight, tfidf (top 10) for tokens and dbPediaSpotlight")
    public Result performNlpForDeck(
    		@ApiParam(value = "deckId") int deckId) {
    	
    	double dbpediaSpotlightConfidenceForSlide = DBPediaSpotlightUtil.dbpediaspotlightdefaultConfidence; // TODO: make this conigurable
    	double dbpediaSpotlightConfidenceForDeck = DBPediaSpotlightUtil.dbpediaspotlightdefaultConfidence; // TODO: make this conigurable

    	ObjectNode result = nlpComponent.processDeck(deckId, dbpediaSpotlightConfidenceForSlide, dbpediaSpotlightConfidenceForDeck);
    	
        return ok(result);
       
    }

    @javax.ws.rs.Path(value = "/dbpediaspotlight")
    @ApiOperation(value = "returns results for dbpedia spotlight", notes = "returns result of dbpedia spotlight for the given input")
    public Result dbpediaSpotlight(
    		@ApiParam(value = "input text") String inputText,
    		@ApiParam(value = "confidence") double confidence) {
    	
    	ObjectNode result = Json.newObject();
    	result = nlpComponent.performDBpediaSpotlight(inputText, confidence, result);
        return ok(result);
       
    }

    
}