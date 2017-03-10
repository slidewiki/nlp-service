package services.util;

import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;

public class DeckServiceUtil {
	
	private Client client;

	public DeckServiceUtil() {
		this.client = ClientBuilder.newClient();
	}
	
	public JsonNode getSlidesForDeckIdFromDeckservice(int deckId){
		try{
			Response response = client.target("https://deckservice.experimental.slidewiki.org/deck/" + deckId + "/slides")
	        .request(MediaType.APPLICATION_JSON).get();
			int status = response.getStatus();
			
			Logger.info("Response status: " + status);
			
			if(status == 200){
				String result = response.readEntity(String.class);
				JsonNode resultNode = Json.parse(result);
				return resultNode;
			}else{
				ObjectNode errorNode = Json.newObject();
				String statusInfo = response.getStatusInfo().toString();
				errorNode.put("statusInfo", statusInfo);
				return errorNode;
			}
	       

		}catch(Exception e){
			System.err.println("Exception for following input: " +deckId + "\n" +e);
		}
		return Json.newObject();
	}

	public static Iterator<JsonNode> getSlidesIteratorForDeckserviceResultDeckSlides(JsonNode deckserviceResult){
		ArrayNode slidesNode = (ArrayNode) deckserviceResult.get("children");
		Iterator<JsonNode> slidesIterator = slidesNode.elements();
		return slidesIterator;
	}
	
	public static String getSlideTitle(JsonNode slideNode){
		return slideNode.get("title").textValue().trim();
	}
	
	public static String getSlideContent(JsonNode slideNode){
		return slideNode.get("content").textValue().trim();
	}
	
	
	public void close(){
		this.client.close();
	}
}
