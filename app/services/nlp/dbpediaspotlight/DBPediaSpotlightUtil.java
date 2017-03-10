package services.nlp.dbpediaspotlight;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

public class DBPediaSpotlightUtil {

	private Client client;
	private static final String URL = "http://api.dbpedia-spotlight.org/annotate";
	public static double dbpediaspotlightdefaultConfidence = 0.6;
	
	public DBPediaSpotlightUtil() {
		this.client = ClientBuilder.newClient();
	}
	
	
	public static JsonNode callDBPediaSpotlightOnce(String input, double confidence){
		try{
			Client client = ClientBuilder.newClient();
			input = removeSpecialChars(input);
			String spotlightResult = client.target(URL)
	        .queryParam("text", input)
	        .queryParam("confidence", confidence)
	        .request(MediaType.APPLICATION_JSON).get(String.class);
			JsonNode resultNode = Json.parse(spotlightResult);
			client.close();
			return resultNode;

		}catch(Exception e){
			System.err.println("Exception for following input: " +input + "\n" +e);
		}
		return Json.newObject();
	}
	
	public JsonNode callDBPediaSpotlight(String input, double confidence){
		try{
			input = removeSpecialChars(input);
			Form form = new Form();
	        form.param("confidence", String.valueOf(confidence));
	        form.param("text", input);
	        String spotlightResult =  client.target(URL)
	                .request()
	                .accept(MediaType.APPLICATION_JSON)
	                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), String.class);
	        JsonNode resultNode = Json.parse(spotlightResult);
	        return resultNode;
		
		}catch(Exception e){
			System.err.println("Exception for following input: " +input + "\n" +e);
		}
		return Json.newObject();
		}
	
	public static String removeSpecialChars(String input){
		String[] specialChars = new String[]{"{", "}", "", "", ""};
		for (String specialChar : specialChars) {
			input = input.replace(specialChar, "");
		}
		return input;
	}
	

	
	public void close(){
		this.client.close();
	}
}
