package services.util;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;

public class NLPStorageUtil {

	private Client client;
	private String URL = "https://nlpstore.experimental.slidewiki.org";
	
	public NLPStorageUtil() {
		this.client = ClientBuilder.newClient();
	}
	
	public Response getNLPResultForDeckId(String deckId){
		
		String URL = this.URL + "/nlp/" + deckId;
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
		Logger.info("Response status for deck id " + deckId + ": " + response.getStatus());

		return response;
	}
	
	public Response getStatisticsDeckCount(String field, String value, String detectedLanguage){
				
        MultivaluedMap<String,String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("field", field);
        queryParams.add("value", value);
        queryParams.add("detectedLanguage", detectedLanguage);
        
   
        
        String URL = this.URL + "/statistics/deckCount";
		Response response = client.target(URL)
				.queryParam("field", field)
				.queryParam("value", value)
				.queryParam("detectedLanguage", detectedLanguage)
					.request().
					get();
		
		return response;
	}
	
	public Response updateNLPStoreByRecalculatingNLPResult(String deckId){
		
		Form form = new Form();
        form.param("deckid", deckId);
        
	    Response response =  client.target(URL)
	                .request()
	                .accept(MediaType.APPLICATION_JSON)
	                .post(Entity.entity(form, MediaType.APPLICATION_JSON));
	    return response;
	        
	}  

	public static JsonNode getJsonFromMessageBody(Response response) {

		String result = response.readEntity(String.class);
		JsonNode resultNode = Json.parse(result);
		return resultNode;
			
	}
	
	public static String getStringFromMessageBody(Response response) {

		String result = response.readEntity(String.class);	
		return result;
			
	}
	
	public static Integer getIntegerFromMessageBody(Response response) {

		String string = getStringFromMessageBody(response);
		Integer result = Integer.valueOf(string);
		return result;
			
	}
}
