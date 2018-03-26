package services.nlp.microserviceutil;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.util.StringRepresentation;

public class NLPStorageUtil {

	private Client client;
	private String URL;
	
	public NLPStorageUtil(String baseURL) {
		this.URL = baseURL;
		this.client = ClientBuilder.newClient();
	}
	
	public Response getNLPResultForDeckId(String deckId){
		
		String URL = this.URL + "/nlp/" + deckId;
		Response response = client.target(URL)
        .request(MediaType.APPLICATION_JSON).get();
		
//		Logger.debug("Response status for deck id " + deckId + ": " + response.getStatus());

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
	
	public Response getStatisticsDeckFrequencies(String deckId){
		
               
        String URL = this.URL + "/statistics/termFrequencies/" + deckId;
		Response response = client.target(URL)
					.request().
					get();
		
		return response;
	}
	
	/***
	 * Returns TFIDFResult which was precalculated and stored
	 * Has the same structure like TFIDF Result result resturned from NLP service GET /nlp/calculateTfidfValues/{deckId}
	 * @param deckId
	 * @return
	 */
	public Response getPrecalculatedTFIDFResult(String deckId){
		
        String URL = this.URL + "/nlp/precalculatedTFIDFResult/" + deckId;
		Response response = client.target(URL)
					.request().
					get();
		
		return response;
	}
	 
	public Response updateNLPStoreByRecalculatingNLPResult(String deckId){
		
		String URL = this.URL + "/init/";
		Form form = new Form();
        form.param("deckid", deckId);
        
	    Response response =  client.target(URL)
	                .request()
	                .accept(MediaType.APPLICATION_JSON)
	                .post(Entity.entity(form, MediaType.APPLICATION_JSON));
	    return response;
	        
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

	public Response queryIndex(String query, String language, List<String> excludeDeckIds, int maxResultsToReturn) {
		
		ObjectNode jsonObject = getJsonObjectToQueryNLPStoreIndex(query, language, excludeDeckIds, maxResultsToReturn);
		String indexQuery = jsonObject.toString();
		
		String URL = this.URL + "/nlp/query";
		
		Response response =  client.target(URL)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(indexQuery, MediaType.APPLICATION_JSON));
        
        return response;
	}
	
	public static ObjectNode getJsonObjectToQueryNLPStoreIndex(String query, String language, List<String> excludeDeckIds, int maxResultsToReturn){
		
		ObjectNode result = Json.newObject();
		result.put("query", query);
		if(language!=null && language.length()>0){
			result.put("language", language);
		}
		if(excludeDeckIds!=null && excludeDeckIds.size()>0){
			result.put("excludeDeckIds", StringRepresentation.fromList(excludeDeckIds, ","));
		}
		result.put("pageSize", maxResultsToReturn);
		
		return result;
	}
	
}
