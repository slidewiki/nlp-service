package controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.nlp.ILanguageDetector;

@Api(value = "/nlp")
public class LanguageDetectionController extends Controller{

	ILanguageDetector languageDetector;
	
	@Inject
	public LanguageDetectionController(ILanguageDetector languageDetector) {
		this.languageDetector = languageDetector;
	}
	 
    @javax.ws.rs.Path(value = "/language")
    @ApiOperation(value = "returns language detection result for given input", notes = "language detection performed for given input")
    public Result detectLanguage(
    		@ApiParam(value = "input text") String inputText) {
    	
    	String language = this.languageDetector.getLanguage(inputText);	
    	ObjectNode result = Json.newObject();
        result.put("detectedlanguage", language);
        return ok(result);
       
    }

}
