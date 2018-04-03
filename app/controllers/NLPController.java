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
import services.nlp.recommendation.TermFilterSettings;
import services.nlp.tfidf.TitleBoostSettings;


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
    		notes = "retrieves tag recommendations for a given deck id by calculating tfidf using frequency information stored nlp results of nlp store - alternative: via frequencies statistics of nlp store")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id  via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result tagRecommendations(
    		@ApiParam(required = true, value = "deckId") String deckId, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost: if true, title boost will be performed using the given title boost parameters below. If false no title boost will be performed and title boost parameters will be ignored.") boolean performTitleBoost, 
    		@ApiParam(required = true, defaultValue = "-1", value = "title boost parameter: if this value is set (bigger than 0), the title frequencies are multiplied with this given number as fixed factor. If not set (below or equal to 0), title boost is performed with factor equal to the number of slides with text of the given deck.") int titleBoostWithFixedFactor, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost parameter: if true, the result of title boost will be limited to the frequency of the most frequent word in the deck ") boolean titleBoostlimitToFrequencyOfMostFrequentWord, 
    		@ApiParam(required = true, defaultValue = "2", value = "term filter setting: the minimum frequency a term or entity must have to be considered in the processing.") int minFrequencyOfTermOrEntityToBeConsidered, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: if true, the min frequency set is only applied after title boost (if title boost is performed)") boolean applyMinFrequencyOfTermOnlyAfterTitleBoost, 
    		@ApiParam(required = true, defaultValue = "3", value = "term filter setting: the minimum character length for a recommended tag.") int minCharLength, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: maximum number of words in multi word unit if there is no URI available. NER tends to be greedy regarding multi word units and may create strange NEs. If there is no spotlight URI available for the multi word unit, only results up to the given number of words will be returned") int maxNumberOfWords, 
     		@ApiParam(required = true, defaultValue = "100", value = "the minimum number of documents of a certain language must exist in the platform to perform langauage dependent.") int tfidfMinDocsToPerformLanguageDependent,
    		@ApiParam(required = true, defaultValue = "20", value = "the maximum number of tag recommendations to return. Returns the top x.") int maxEntriesToReturn) {
    	
    	TitleBoostSettings titleBoostSettings = new TitleBoostSettings(performTitleBoost, titleBoostWithFixedFactor, titleBoostlimitToFrequencyOfMostFrequentWord);
    	TermFilterSettings termFilterSettings = new TermFilterSettings(minFrequencyOfTermOrEntityToBeConsidered, minCharLength, maxNumberOfWords, applyMinFrequencyOfTermOnlyAfterTitleBoost);
   	 	
    	ObjectNode resultNode = Json.newObject();
    	
    	try{

        	List<NlpTag> tags = nlpComponent.getTagRecommendations(deckId, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent, maxEntriesToReturn);
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
    
    @javax.ws.rs.Path(value = "/calculateTfidfValues")
    @ApiOperation(
    		tags = "deck",
    		value = "calculates tfidf values for a given deck id", 
    		notes = "calculates tfidf values for a given deck id (by calculating tfidf using frequency information of nlp store (GET /statistics/termFrequencies/{deckId}), so nlp result already needs to be stored")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id  via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result calculateTfidfValues(
    		@ApiParam(required = true, value = "deckId") String deckId, 
     		@ApiParam(required = true, defaultValue = "true", value = "title boost: if true, title boost will be performed using the given title boost parameters below. If false no title boost will be performed and title boost parameters will be ignored.") boolean performTitleBoost, 
    		@ApiParam(required = true, defaultValue = "-1", value = "title boost parameter: if this value is set (bigger than 0), the title frequencies are multiplied with this given number as fixed factor. If not set (below or equal to 0), title boost is performed with factor equal to the number of slides with text of the given deck.") int titleBoostWithFixedFactor, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost parameter: if true, the result of title boost will be limited to the frequency of the most frequent word in the deck ") boolean titleBoostlimitToFrequencyOfMostFrequentWord, 
    		@ApiParam(required = true, defaultValue = "2", value = "term filter setting: the minimum frequency a term or entity must have to be considered in the processing.") int minFrequencyOfTermOrEntityToBeConsidered, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: if true, the min frequency set is only applied after title boost (if title boost is performed)") boolean applyMinFrequencyOfTermOnlyAfterTitleBoost, 
    		@ApiParam(required = true, defaultValue = "3", value = "term filter setting: the minimum character length of a term to be considered") int minCharLength, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: maximum number of words in multi word unit. (NER tends to be greedy regarding multi word units and may create strange NEs)") int maxNumberOfWords, 
     		@ApiParam(required = true, defaultValue = "100", value = "the minimum number of documents of a certain language must exist in the platform to perform langauage dependent.") int tfidfMinDocsToPerformLanguageDependent

    		) {
    	
   	 	
    	
    	try{

    		TitleBoostSettings titleBoostSettings = new TitleBoostSettings(performTitleBoost, titleBoostWithFixedFactor, titleBoostlimitToFrequencyOfMostFrequentWord);
    		TermFilterSettings termFilterSettings = new TermFilterSettings(minCharLength, minFrequencyOfTermOrEntityToBeConsidered, maxNumberOfWords, applyMinFrequencyOfTermOnlyAfterTitleBoost);
        	ObjectNode resultNode = nlpComponent.calculateTfidfResultViaNLPStoreFrequenciesAndReturnAsJsonNode(deckId, tfidfMinDocsToPerformLanguageDependent, titleBoostSettings, termFilterSettings);
        
        	Result r = Results.ok(resultNode);        	
            return r;
    	}catch (WebApplicationException e) {

    		return createResultForExceptionalResponseCausedByWebApllicationException(e);
    	}catch(ProcessingException f){
    		String message = "Processing was interupted. Problem occured during Processing. For more information see details provided.";
    		
    		return createResultForProcessingException(500, f, message);
    	}
    	
       
    }
    

    
    @javax.ws.rs.Path(value = "/deckSimilarity")
    @ApiOperation(
    		tags = "deck",
    		value = "calculates cosine similarity between 2 given decks based on the content", 
    		notes = "calculates cosine similarity between 2 given decks based on the content by using top x tfidf terms (tokens, named entities & spotlight entities) calculated using frequency information stored nlp results of nlp store")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result deckSimilarity(
    		@ApiParam(required = true, value = "deckId1") String deckId1, 
    		@ApiParam(required = true, value = "deckId2") String deckId2,
     		@ApiParam(required = true, defaultValue = "true", value = "if true, tfidf for the given decks is freshly calculated by using frequency data from nlp store. If false, the precalculated tfdif result is used and the following parameters of title boost params, term filter params and tfidfMinDocsToPerformLanguageDependent are not used.") boolean performLiveTFIDFCalculation,
     		@ApiParam(required = true, defaultValue = "20", value = "the maximum number of top terms to consider. E.g. if set to 10, the top 10 tokens, top 10 named entities and top 10 spotlight resources are used") int maxTermsToConsider,
     		@ApiParam(required = true, defaultValue = "true", value = "title boost: if true, title boost will be performed using the given title boost parameters below. If false no title boost will be performed and title boost parameters will be ignored.") boolean performTitleBoost, 
    		@ApiParam(required = true, defaultValue = "-1", value = "title boost parameter: if this value is set (bigger than 0), the title frequencies are multiplied with this given number as fixed factor. If not set (below or equal to 0), title boost is performed with factor equal to the number of slides with text of the given deck.") int titleBoostWithFixedFactor, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost parameter: if true, the result of title boost will be limited to the frequency of the most frequent word in the deck ") boolean titleBoostlimitToFrequencyOfMostFrequentWord, 
    		@ApiParam(required = true, defaultValue = "2", value = "term filter setting: the minimum frequency a term or entity must have to be considered in the processing.") int minFrequencyOfTermOrEntityToBeConsidered, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: if true, the min frequency set is only applied after title boost (if title boost is performed)") boolean applyMinFrequencyOfTermOnlyAfterTitleBoost, 
    		@ApiParam(required = true, defaultValue = "3", value = "term filter setting: the minimum character length of a term to be considered") int minCharLength, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: maximum number of words in multi word unit. (NER tends to be greedy regarding multi word units and may create strange NEs)") int maxNumberOfWords, 
     		@ApiParam(required = true, defaultValue = "100", value = "the minimum number of documents of a certain language must exist in the platform to perform langauage dependent.") int tfidfMinDocsToPerformLanguageDependent
    		) {
    	
   	 	
    	
    	try{
    		TitleBoostSettings titleBoostSettings = new TitleBoostSettings(performTitleBoost, titleBoostWithFixedFactor, titleBoostlimitToFrequencyOfMostFrequentWord );
        	TermFilterSettings termFilterSettings = new TermFilterSettings(minCharLength, minFrequencyOfTermOrEntityToBeConsidered, maxNumberOfWords, applyMinFrequencyOfTermOnlyAfterTitleBoost);			
        	ObjectNode resultNode = nlpComponent.calculateCosineSimilarity(deckId1, deckId2, performLiveTFIDFCalculation, maxTermsToConsider, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent, true, false);
        	Result r = Results.ok(resultNode);        	
            return r;
    	}catch (WebApplicationException e) {

    		return createResultForExceptionalResponseCausedByWebApllicationException(e);
    	}catch(ProcessingException f){
    		String message = "Processing was interupted. Problem occured during Processing. For more information see details provided.";
    		
    		return createResultForProcessingException(500, f, message);
    	}
    	
       
    }

    @javax.ws.rs.Path(value = "/deckSimilarityExtendedInfo")
    @ApiOperation(
    		tags = "deck",
    		value = "calculates cosine similarity between 2 given decks based on the content - includes info about shared entries", 
    		notes = "calculates cosine similarity between 2 given decks based on the content by using top x tfidf terms (tokens, named entities & spotlight entities) calculated using frequency information stored nlp results of nlp store")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result deckSimilarityExtendedInfo(
    		@ApiParam(required = true, value = "deckId1") String deckId1, 
    		@ApiParam(required = true, value = "deckId2") String deckId2,
     		@ApiParam(required = true, defaultValue = "true", value = "if true, tfidf for the given decks is freshly calculated by using frequency data from nlp store. If false, the precalculated tfdif result is used and the following parameters of title boost params, term filter params and tfidfMinDocsToPerformLanguageDependent are not used.") boolean performLiveTFIDFCalculation,
     		@ApiParam(required = true, defaultValue = "20", value = "the maximum number of top terms to consider. E.g. if set to 10, the top 10 tokens, top 10 named entities and top 10 spotlight resources are used") int maxTermsToConsider,
     		@ApiParam(required = true, defaultValue = "true", value = "title boost: if true, title boost will be performed using the given title boost parameters below. If false no title boost will be performed and title boost parameters will be ignored.") boolean performTitleBoost, 
    		@ApiParam(required = true, defaultValue = "-1", value = "title boost parameter: if this value is set (bigger than 0), the title frequencies are multiplied with this given number as fixed factor. If not set (below or equal to 0), title boost is performed with factor equal to the number of slides with text of the given deck.") int titleBoostWithFixedFactor, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost parameter: if true, the result of title boost will be limited to the frequency of the most frequent word in the deck ") boolean titleBoostlimitToFrequencyOfMostFrequentWord, 
    		@ApiParam(required = true, defaultValue = "2", value = "term filter setting: the minimum frequency a term or entity must have to be considered in the processing.") int minFrequencyOfTermOrEntityToBeConsidered, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: if true, the min frequency set is only applied after title boost (if title boost is performed)") boolean applyMinFrequencyOfTermOnlyAfterTitleBoost, 
    		@ApiParam(required = true, defaultValue = "3", value = "term filter setting: the minimum character length of a term to be considered") int minCharLength, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: maximum number of words in multi word unit. (NER tends to be greedy regarding multi word units and may create strange NEs)") int maxNumberOfWords, 
     		@ApiParam(required = true, defaultValue = "100", value = "the minimum number of documents of a certain language must exist in the platform to perform langauage dependent.") int tfidfMinDocsToPerformLanguageDependent
    		) {
    	
   	 	
    	
    	try{
    		TitleBoostSettings titleBoostSettings = new TitleBoostSettings(performTitleBoost, titleBoostWithFixedFactor, titleBoostlimitToFrequencyOfMostFrequentWord );
        	TermFilterSettings termFilterSettings = new TermFilterSettings(minCharLength, minFrequencyOfTermOrEntityToBeConsidered, maxNumberOfWords, applyMinFrequencyOfTermOnlyAfterTitleBoost);
			ObjectNode resultNode = nlpComponent.calculateCosineSimilarity(deckId1, deckId2, performLiveTFIDFCalculation, maxTermsToConsider, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent, true, true);
        	Result r = Results.ok(resultNode);        	
            return r;
    	}catch (WebApplicationException e) {

    		return createResultForExceptionalResponseCausedByWebApllicationException(e);
    	}catch(ProcessingException f){
    		String message = "Processing was interupted. Problem occured during Processing. For more information see details provided.";
    		
    		return createResultForProcessingException(500, f, message);
    	}
    	
       
    }

    @javax.ws.rs.Path(value = "/deckRecommendationSimilarDecks")
    @ApiOperation(
    		tags = "deck",
    		value = "calculates similar decks to the given one based on the content of the deck", 
    		notes = "calculates similar decks to the given one based on the content of the deck ( = textual content of the slides of the deck)")
    @ApiResponses(
    		value = {
    				@ApiResponse(code = 404, message = "Problem while retrieving slides for given deck id via nlp storage service. Slides for given deck id not found. Probably this deck id does not exist."),
    				@ApiResponse(code = 500, message = "Problem occured. For more information see details provided.")
    				})

    public Result deckRecommendationSimilarDecks(
    		@ApiParam(required = true, value = "deckId") String deckId, 
     		@ApiParam(required = true, defaultValue = "10", value = "the maximum number of recommendations to return.") int maxRecommendationsToReturn,
     		@ApiParam(required = true, defaultValue = "30", value = "the maximum number of candidates to use for similarity calculation. This might increase number and quality of the results but also calcualtion time.") int maxCandidatesToUseForSimilarityCalculation,
     		@ApiParam(required = true, defaultValue = "10", value = "the maximum number of top terms to consider. E.g. if set to 10, the top 10 tokens, top 10 named entities and top 10 spotlight resources are used. You can try to increase this number if no recommendations are returned.") int maxTermsToConsider,
     		@ApiParam(required = true, defaultValue = "true", value = "if true, tfidf for the given deck is freshly calculated by using frequency data from nlp store. If false, the precalculated tfdif result is used and the following parameters of title boost params, term filter params and tfidfMinDocsToPerformLanguageDependent are not used. Use true until storage and API to precalcualted tfidf values is implemented in nlp store.") boolean performLiveTFIDFCalculationOfGivenDeck,
     		@ApiParam(required = true, defaultValue = "true", value = "if true, tfidf for the deck candidates is freshly calculated by using frequency data from nlp store. If false, the precalculated tfdif result is used and the following parameters of title boost params, term filter params and tfidfMinDocsToPerformLanguageDependent are not used.") boolean performLiveTFIDFCalculationOfDeckCandidates,
     		@ApiParam(required = true, defaultValue = "true", value = "title boost: if true, title boost will be performed using the given title boost parameters below. If false no title boost will be performed and title boost parameters will be ignored.") boolean performTitleBoost, 
    		@ApiParam(required = true, defaultValue = "-1", value = "title boost parameter: if this value is set (bigger than 0), the title frequencies are multiplied with this given number as fixed factor. If not set (below or equal to 0), title boost is performed with factor equal to the number of slides with text of the given deck.") int titleBoostWithFixedFactor, 
    		@ApiParam(required = true, defaultValue = "true", value = "title boost parameter: if true, the result of title boost will be limited to the frequency of the most frequent word in the deck ") boolean titleBoostlimitToFrequencyOfMostFrequentWord, 
    		@ApiParam(required = true, defaultValue = "2", value = "term filter setting: the minimum frequency a term or entity must have to be considered in the processing.") int minFrequencyOfTermOrEntityToBeConsidered, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: if true, the min frequency set is only applied after title boost (if title boost is performed)") boolean applyMinFrequencyOfTermOnlyAfterTitleBoost, 
    		@ApiParam(required = true, defaultValue = "3", value = "term filter setting: the minimum character length of a term to be considered") int minCharLength, 
    		@ApiParam(required = true, defaultValue = "4", value = "term filter setting: maximum number of words in multi word unit. (NER tends to be greedy regarding multi word units and may create strange NEs)") int maxNumberOfWords, 
     		@ApiParam(required = true, defaultValue = "100", value = "the minimum number of documents of a certain language must exist in the platform to perform langauage dependent.") int tfidfMinDocsToPerformLanguageDependent
    		) {
    	
   	 	
    	
    	try{
    		TitleBoostSettings titleBoostSettings = new TitleBoostSettings(performTitleBoost, titleBoostWithFixedFactor, titleBoostlimitToFrequencyOfMostFrequentWord );
        	TermFilterSettings termFilterSettings = new TermFilterSettings(minCharLength, minFrequencyOfTermOrEntityToBeConsidered, maxNumberOfWords, applyMinFrequencyOfTermOnlyAfterTitleBoost);

        	ObjectNode resultNode = nlpComponent.getDeckRecommendation(deckId, performLiveTFIDFCalculationOfGivenDeck, performLiveTFIDFCalculationOfDeckCandidates, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent, maxTermsToConsider, maxCandidatesToUseForSimilarityCalculation, maxRecommendationsToReturn);
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