package controllers;

import java.util.List;

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
import services.nlp.ITagger;
import services.nlp.NlpTag;


@Api(value = "/getTag")
//@javax.ws.rs.Path
public class TagController extends Controller{
    
    
    private ITagger tagger;
    
    @Inject
    public TagController(ITagger tagger) {
       this.tagger = tagger;
    }
    
    @ApiOperation(value = "returns tag suggestions for given input", notes = "tags are calculated for the given input")
    public Result getTag(@ApiParam(value = "Input text the tags should be calculated for")String input) {
    	
    	
    	List<NlpTag> tags = this.tagger.getTags(input);
    	ArrayNode jsonArray = Json.newArray();
    	for (NlpTag tag : tags) {		
    		JsonNode node = Json.toJson(tag);
        	jsonArray.add(node);
		}
    	
    	ObjectNode result = Json.newObject();
        result.set("tags", jsonArray);
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