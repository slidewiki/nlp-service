package controllers;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
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
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, Map<String,IDocFrequencyProvider> mapDocFrequencyProvider) {
		super();
		this.nlpComponent = new NLPComponent(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, mapDocFrequencyProvider);
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
    		@ApiParam(value = "deckId") String deckId, 
    		@ApiParam(value = "dbpediaSpotlightConfidenceForSlide (use a value >1 to skip spotlight processing per slides)") double dbpediaSpotlightConfidenceForSlide, 
    		@ApiParam(value = "dbpediaSpotlightConfidenceForDeck  (use a value >1 to skip spotlight processing per deck (text of whole deck as 1 input to spotlight). Spotlight per slide will be processed if <=1") double dbpediaSpotlightConfidenceForDeck) {
    	
    	
    	
    	try{
        	ObjectNode resultNode = nlpComponent.processDeck(deckId, dbpediaSpotlightConfidenceForSlide, dbpediaSpotlightConfidenceForDeck);
        	Result r = Results.ok(resultNode);        	
            return r;
    	}catch (WebApplicationException e) {

    		return createResultForExceptionalResponseCausedByWebApllicationException(e);
    	}catch(ProcessingException f){
    		String message = "Processing was interupted. Problem occured during Processing. For more information see details provided.";
    		
    		return createResultForProcessingException(500, f, message);
    	}
    	
       
    }

    @javax.ws.rs.Path(value = "/dbpediaspotlight")
    @ApiOperation(
    		value = "returns results for dbpedia spotlight", // displayed next to path
    		notes = "returns result of dbpedia spotlight for the given input"// displayed under "Implementation notes"
    		)
    public Result dbpediaSpotlight(
    		@ApiParam(value = "input text") String inputText,
    		@ApiParam(value = "confidence") double confidence) {
    	
    	Logger.debug("confidence set to " + confidence);
    	if(confidence>1 || confidence < 0 ){
    		return badRequest("Please provide a confidence value in the range of 0 to 1");
    	}
    	
    	Response response;
    	try{
    		response = nlpComponent.performDBpediaSpotlight(inputText, confidence);
    		if(response.getStatus()!=200){
        		
        		return createResultForResponseObject(response, "Problem occured during calling spotlight service. For more information see details provided.");
        	}else{
        		return createResultForResponseObject(response, "");
        	}
    	}catch(ProcessingException e){
    		return createResultForProcessingException(500, e, "Problem occured during calling spotlight service. For more information see details provided.");
    	}
    	
       
    }

    /**
     * Creates a Result for a {@link WebApplicationException} which is thrown when other called service do not return expected result.
     * Creates a Result with the same status code like returned by the called service.
     * @param e
     * @return
     */
    public static Result createResultForExceptionalResponseCausedByWebApllicationException(WebApplicationException e){
    	
    	ObjectNode responseContent = Json.newObject();
		String message = e.getMessage();
		responseContent.put("message", message);
		Response response = e.getResponse();
		int status = response.getStatus();
		responseContent.put("status", status);
		String responseAsString = response.readEntity(String.class);
		JsonNode responseAsJsonNode = Json.parse(responseAsString);
		responseContent.set("Response", responseAsJsonNode);

		Result result = Results.status(status, responseContent);
		return result;
    }
    
    public static Result createResultForResponseObject(Response response, String optionalMessage){
    	
    	ObjectNode responseContent = Json.newObject();
    	
    	if(optionalMessage!=null && optionalMessage.length()>0){
    		responseContent.put("message", optionalMessage);
    	}
    	
		int status = response.getStatus();
		responseContent.put("status", status);
		String responseAsString = response.readEntity(String.class);
		try{
			JsonNode responseAsJsonNode = Json.parse(responseAsString);
			responseContent.set("Response", responseAsJsonNode);
		}
		catch(RuntimeException e){// problem parsing json
			responseContent.put("Response", responseAsString);

		}
		Result result = Results.status(status, responseContent);
		return result;
    }
    
    public static Result createResultForProcessingException(int statusToReturn, Exception e, String optionalMessage){
    	ObjectNode responseContent = Json.newObject();
		responseContent.put("status", statusToReturn);

    	if(optionalMessage!=null && optionalMessage.length()>0){
    		responseContent.put("message", optionalMessage);
    	}
    	responseContent.put("detailsErrorMessage", e.getMessage());
    	if(e.getCause()!=null){
        	responseContent.put("detailsErrorCause", e.getCause().toString());
    	}

    	return Results.status(statusToReturn, responseContent);
    }
    
}