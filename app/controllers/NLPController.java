package controllers;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import services.nlp.NLPComponent;
import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.recommendation.NlpTag;


@Api(value="nlp")
@SwaggerDefinition(	
        // Attention: "info"-part below is currently not supported by swagger-play2 and will be ignored. Configure instead in apllication.conf under swagger.api.info
        // but "tags" are supported and not ignored
		info = @Info(
        		description = "provides different nlp methods to be performed on decks.", 
        		title = "NLP service API", 
        		version = "0.1"),
        tags = {
                @Tag(name = "deck", description = "nlp for a given deck id"),
                @Tag(name = "sub", description = "some sub functions for convenience for processing input")
                
        }
        
		)
public class NLPController extends Controller{
    
	
    private NLPComponent nlpComponent;

    
    
    @Inject
    public NLPController(NLPComponent nlpComponent) {
		super();
		this.nlpComponent = nlpComponent;
	}

    @javax.ws.rs.Path(value = "/htmlToText")
    @ApiOperation(tags = "sub", value = "html to text", notes = "")
    public Result htmlToText(
    		@ApiParam(required = true, value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.getPlainTextFromHTML(inputText, result);
    	
        return ok(result);
       
    }
    
    @javax.ws.rs.Path(value = "/detectLanguage")
    @ApiOperation(tags = "sub", value = "returns language detection result for given input", notes = "language detection performed for given input")
    public Result detectLanguage(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
		result = nlpComponent.detectLanguage(inputText, result);
    	
        return ok(result);
       
    }

	@javax.ws.rs.Path(value = "/tokenize")
    @ApiOperation(tags = "sub", value = "returns tokens for given input", notes = "tokens are calculated for the given input")
    public Result tokenize(
    		@ApiParam(required = true, value = "input text") String inputText, 
    		@ApiParam(required = true, value = "language") String language) {
    	
		ObjectNode result = Json.newObject();
		result = nlpComponent.tokenize(inputText, language, result);
    	
        return ok(result);
       
    }
    
    

    
    @javax.ws.rs.Path(value = "/nlp")
    @ApiOperation(tags = "sub", value = "performs different available nlp steps", notes = "different nlp steps are performed, currently: language detection, tokenization, NER and tfidf (top 10)")
    public Result performNLP(
    		@ApiParam(value = "input text") String inputText) {
    	
    	ObjectNode result = Json.newObject();
    	double dbpediaSpotlightConfidence = DBPediaSpotlightUtil.dbpediaspotlightdefaultConfidence; // TODO: make this conigurable
		result = nlpComponent.performNLP(inputText, result, dbpediaSpotlightConfidence);
    	
        return ok(result);
       
    }
    
    @javax.ws.rs.Path(value = "/processDeck")
    @ApiOperation(
    		tags = "deck",
    		value = "performs different available nlp steps for content of deck", 
    		notes = "different nlp steps are performed, currently: language detection, tokenization, Stopword removal, NER, DBPedia Spotlight, frequencies, tfidf")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id  via deck service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured during calling spotlight service. For more information see details provided.")
    				})

    public Result performNlpForDeck(
    		@ApiParam(required = true, value = "deckId") String deckId, 
    		@ApiParam(required = true, defaultValue = "0.6", value = "dbpediaSpotlightConfidenceForSlide (use a value>1 to skip spotlight processing per slides)") double dbpediaSpotlightConfidenceForSlide, 
    		@ApiParam(required = true, defaultValue = "0.6", value = "dbpediaSpotlightConfidenceForDeck  (use a value>1 to skip spotlight processing per deck (=text of deck as input to spotlight). Spotlight per slide will be still processed.") double dbpediaSpotlightConfidenceForDeck) {
    	
    	
    	
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
    
    @javax.ws.rs.Path(value = "/tagRecommendations")
    @ApiOperation(
    		tags = "deck",
    		value = "retrieves tag recommendations for a given deck id", 
    		notes = "retrieves tag recommendations for a given deck id using tfidf from stored nlp results")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id  via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result tagRecommendations(
    		@ApiParam(required = true, value = "deckId") String deckId) {
    	
   	 	ObjectNode resultNode = Json.newObject();
    	
    	try{

        	List<NlpTag> tags = nlpComponent.getTagRecommendations(deckId);
        	JsonNode tagNode = Json.toJson(tags);
        	resultNode.set(NLPResultUtil.propertyNameTagRecommendations, tagNode);

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
    		tags = "sub",
    		value = "returns results for dbpedia spotlight", // displayed next to path
    		notes = "returns result of dbpedia spotlight for the given input",// displayed under "Implementation notes"
    	    nickname = "spotlight",
    	    httpMethod = "GET"
    	    )
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 400, message = "Please provide a confidence value in the range of 0 to 1"),
    				@ApiResponse(code = 500, message = "Problem occured during calling spotlight service. For more information see details provided.")}
    		)
    public Result dbpediaSpotlight(
    		@ApiParam(required = true, value = "input text") String inputText,
    		@ApiParam(required = true, defaultValue = "0.6", value = "confidence (in range of 0 to 1, e.g. 0.6)") double confidence,
    		@ApiParam(required = true, defaultValue = DBPediaSpotlightUtil.nameForNoTypeRestriction, value = "the types to restrict for. Use \"ALL\" for no restrictions on types") String types
    		) {
    	
    	Logger.debug("confidence set to " + confidence);
    	Logger.info("types set to\"" + types+"");
    	if(confidence>1 || confidence < 0 ){
    		return badRequest("Please provide a confidence value in the range of 0 to 1");
    	}
    	
    	Response response;
    	try{
    		response = nlpComponent.performDBpediaSpotlight(inputText, confidence, types);
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