package controllers;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.nlp.ITokenizer;
import services.nlp.NlpTag;


@Api(value = "/nlp")
//@javax.ws.rs.Path
public class NLPController extends Controller{
    
	private static String defaultLanguageToUseIfLanguageNotAvailale = "en";
    private Map<String,ITokenizer> tokenizerMap;

    
    @Inject
    public NLPController(Map<String,ITokenizer> tokenizerMap) {
       this.tokenizerMap = tokenizerMap;
    }

    @ApiOperation(value = "returns tokens for given input", notes = "tokens are calculated for the given input")
    public Result getTag(
    		@ApiParam(value = "Input text the tags should be calculated for") String inputText, 
    		@ApiParam(value = "language") String language) {
    	
    	ITokenizer tokenizerToUse;
    	if(this.tokenizerMap.containsKey(language)){
    		tokenizerToUse = tokenizerMap.get(language);
    	}else{
    		tokenizerToUse = tokenizerMap.get(defaultLanguageToUseIfLanguageNotAvailale);
    	}
    	
    	String[] tokens = tokenizerToUse.tokenize(inputText);
    	ArrayNode jsonArray = Json.newArray();
    	for (String token : tokens) {		
    		JsonNode node = Json.toJson(token);
        	jsonArray.add(node);
		}
    	
    	ObjectNode result = Json.newObject();
        result.set("tokens", jsonArray);
        return ok(result);
       
    }
    
//    @BodyParser.Of(BodyParser.Json.class)
//    public Result sayHello() {
//        JsonNode json = request().body().asJson();
//        String name = json.findPath("name").textValue();
//        if(name == null) {
//            return badRequest("Missing parameter [name]");
//        } else {
//            return ok("Hello " + name);
//        }
//    }
}