package services.nlp.microserviceutil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

public class ActivitiesServiceUtil {

	private Client client;
	private String serviceURL;
	
	public ActivitiesServiceUtil(String activitiesServiceURL) {
		this.serviceURL = activitiesServiceURL;
		this.client = ClientBuilder.newClient();
	}

	public Response getActivitiesForDeckId(String deckId){
		
		String URL = serviceURL + "/activities/deck/" + deckId;
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
//		Logger.debug("Response status for deck id " + deckId + ": " + response.getStatus());

		return response;
	}
	
	public static JsonNode getJsonFromMessageBody(Response response) {

		String result = response.readEntity(String.class);
		JsonNode resultNode = Json.parse(result);
		return resultNode;
			
	}
}
