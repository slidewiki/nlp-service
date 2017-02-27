package services.nlp.dbpediaspotlight;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

public class DBPediaSpotlightUtil {

	public static JsonNode callDBPediaSpotlight(String input, double confidence){
		Client client = ClientBuilder.newClient();
		String spotlightResult = client.target("http://www.dbpedia-spotlight.com/en/annotate")
        .queryParam("text", input)
        .queryParam("confidence", confidence)
        .request(MediaType.APPLICATION_JSON).get(String.class);
		JsonNode resultNode = Json.parse(spotlightResult);
		client.close();
		return resultNode;
		
	}
}
