package services.nlp.microserviceutil;

import java.util.Collections;
import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class DeckServiceUtil {
	
	private Client client;
	private String serviceURL;
	
	public DeckServiceUtil(String deckserviceURL) {
		this.serviceURL = deckserviceURL;
		this.client = ClientBuilder.newClient();
	}
	
	
	public Response getLatestDeckId(){
		
		String URL = serviceURL + "/allrecent/1/0";
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
		return response;
	}
	
	public Response getDeck(String deckId){
		
		String URL = serviceURL + "/deck/" + deckId;
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
		return response;
	}	
	
	public Response getSlidesForDeckIdFromDeckservice(String deckId){
		
		String URL = serviceURL + "/deck/" + deckId + "/slides";
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
//		Logger.debug("Response status for deck id " + deckId + ": " + response.getStatus());

		return response;
	}
	
	public Response getForksForGivenDeck(String deckId){
		
		String URL = serviceURL + "/deck/" + deckId + "/forkGroup";
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
		return response;
	}
	
	
	/**
	 * Returns iterator for slides for given deck service response. Expects a normal response object. Please check for error responses before.
	 * @param deckserviceResult
	 * @return
	 */
	public static Iterator<JsonNode> getSlidesIteratorFromDeckserviceResponse(Response response){

		JsonNode jsonNode = MicroserviceUtil.getJsonFromMessageBody(response);
		Iterator<JsonNode> iterator = getSlidesIteratorFromDeckserviceJsonResult(jsonNode);
		return iterator;
	}


	public static Iterator<JsonNode> getSlidesIteratorFromDeckserviceJsonResult(JsonNode deckserviceResult){
		Iterator<JsonNode> slidesIterator = Collections.<JsonNode>emptyList().iterator();
		if(deckserviceResultHasSlides(deckserviceResult)){
			ArrayNode slidesNode = (ArrayNode) deckserviceResult.get("children");
			slidesIterator = slidesNode.elements();
		}
		return slidesIterator;
	}
	
	private static boolean deckserviceResultHasSlides(JsonNode deckserviceResult){
		return deckserviceResult.has("children");
	}
	
	public static String getSlideTitle(JsonNode slideNode){
		if(slideNode.has("title")){
			return slideNode.get("title").textValue().trim();
		}
		return "";
	}
	
	public static String getSlideContent(JsonNode slideNode){
		if(slideNode.has("content")){
			return slideNode.get("content").textValue().trim();
		}
		return "";
	}
	
	public static String getDeckTitle(JsonNode deckNode){
		if(deckNode.has("title")){
			return deckNode.get("title").textValue().trim();
		}
		return "";
	}
	
	public void close(){
		this.client.close();
	}
}
