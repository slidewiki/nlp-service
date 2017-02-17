package controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.nlp.NLPComponent;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;


@Api(value = "/nlp")
public class NLPController extends Controller{
    
    private NLPComponent nlpComponent;

    
    @Inject
    public NLPController(ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer,
			INERLanguageDependent ner, TFIDF tfidf, IDocFrequencyProvider docFrequencyProvider) {
		super();
		this.nlpComponent = new NLPComponent(languageDetector, tokenizer, ner, tfidf, docFrequencyProvider);
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
    
    
    @javax.ws.rs.Path(value = "/language")
    @ApiOperation(value = "returns language detection result for given input", notes = "language detection performed for given input")
    public Result detectLanguage(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.detectLanguage(inputText, result);
    	
        return ok(result);
       
    }
    
    @javax.ws.rs.Path(value = "/nlp")
    @ApiOperation(value = "performs different available nlp steps", notes = "different nlp steps performed")
    public Result performNLP(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.performNLP(inputText, result);
    	
        return ok(result);
       
    }

    
}