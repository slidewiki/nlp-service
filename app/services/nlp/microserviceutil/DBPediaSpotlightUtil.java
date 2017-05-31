package services.nlp.microserviceutil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import services.util.MapCounting;

public class DBPediaSpotlightUtil {

	
	public static final String nameForNoTypeRestriction = "ALL";
	private Client client;
	private String URL;												
	private String[] fallbackURLs;
	
	
	public static double dbpediaspotlightdefaultConfidence = 0.6;
	
	
	
	/**
	 * 	Constructor for util class for calling DBPedia Spotlight web service
	 * @param urlOfDBPediaWebService The URL of the DBPedia Spotlight service (has changed several times, it might be worth to re-check from time to time)
	 * 
	 */
	public DBPediaSpotlightUtil(String urlOfDBPediaWebService) {
		this.client = ClientBuilder.newClient();
		this.URL = urlOfDBPediaWebService;
		this.fallbackURLs = null;
	}
	
	/**
	 * Constructor for util class for calling DBPedia Spotlight web service
	 * @param urlOfDBPediaWebService The URL of the DBPedia Spotlight service (has changed several times, it might be worth to re-check from time to time)
	 * @param fallbackURLs A string array of URLs to be used as fall back when given URL returns a 5xx server error. 
	 * Tries 1st fallback URL, if this gives an 5xx, the next fallback URL is tried and so on. As soon a non 5xx is returned, the Response is resturned. 
	 * For not using this fall back mechanism, set to null, empty array or use other constructor.
	 * Possible fallback URLs might be (because these were URLs previously published as URL of Spotlight web service): 
	 * http://model.dbpedia-spotlight.org/en/annotate
	 * http://www.dbpedia-spotlight.com/en/annotate
	 * http://api.dbpedia-spotlight.org/rest/annotate
	 * http://spotlight.dbpedia.org/rest/annotate
	 */
	public DBPediaSpotlightUtil(String urlOfDBPediaWebService, String[] fallbackURLs) {
		this.client = ClientBuilder.newClient();
		this.URL = urlOfDBPediaWebService;
		this.fallbackURLs = fallbackURLs;
	}
	
	/**
	 * Calls web service of DBPedia Spotlight and returns the response.  
	 * If default URL of web service returns a server error or a {@link ProcessingException} while trying to call the service, tries the fallback URL next and returns response from there. If all fallback URLs also return server error or {@link ProcessingException}, a {@link ProcessingException} is thrown. If a fallback URL returns response code != 5xx, warning message is returned.
	 * @param input
	 * @param confidence
	 * @return
	 * @throws ProcessingException if Spotlight service returns server error (5xx) or {@link ProcessingException}. Tries first URL and then fallbackURLs.
	 */
	public Response performDBPediaSpotlight(String input, double confidence, String types) throws ProcessingException{
		
		input = removeSpecialChars(input);
		Logger.info("call DBPedia Spotlight via URL " + URL + "...");
		StringBuilder errorMessages = new StringBuilder();
		errorMessages.append("ErrorMessages collected while trying to call DBPedia Spotlight:");
		
		Response response;
		try{		
			response = getSpotlightResponse(input, confidence, types, URL);
			int status = response.getStatus();	 
			if(status/100 == 5){
	    		String message = "DBPediaSpotlight: Server error occured while calling DBPedia spotlight via main URL " + URL + ". Returned status " + status +".";
	    		Logger.warn(message);
				
				throw new ProcessingException(message); // will be catched below and fallback URLs are tried if available
			}
    
			return response;
		
		}catch( ProcessingException e){
			String message = "DBPediaSpotlight: ProcessingException occured while calling DBPedia spotlight via main URL " + URL + ". Details: " + e.getMessage();
			Logger.warn(message);
			errorMessages.append("\n" + message);
			
			if(fallbackURLs!= null && fallbackURLs.length>0){
				// try fallback URLs
				String messageFallbacks = "DBPediaSpotlight: Trying fallback URLs...";
				errorMessages.append("\n" + messageFallbacks);
				Logger.warn(messageFallbacks);
				
				for (String fallbackURL : fallbackURLs) {
					
					String messageFallback = "DBPediaSpotlight: Trying fallback URL " + fallbackURL + "...";
					errorMessages.append(messageFallback);
					Logger.warn(messageFallback);

					try{
						Response response2 = getSpotlightResponse(input, confidence, types, fallbackURL);
				        int status2 = response2.getStatus();
				        if(status2/100==5){
				    		String messageFallbackServerError = "DBPediaSpotlight: Server error occured when calling DBPedia spotlight via fallback URL " + fallbackURL + ". Returned status " + status2 +". (" + response2.getStatusInfo() + ")";
				    		Logger.warn(messageFallbackServerError);
							throw new ProcessingException(messageFallbackServerError); // will be catched below and fallback URLs are tried if available
				        }
				        
				        errorMessages.append("\nDBPediaSpotlight: success (no server error and no processing exception) for calling service via fallback URL: " + fallbackURL + ". Returned status " + response2.getStatus() + " (" + response2.getStatusInfo() + ")");
				        Logger.warn(errorMessages.toString());
				        return response2; // no server error for this fallback URL, return result of this fallback URL // TODO: add a errorMessages to returned Response telling the user that the original URL disn't work, which fallback URLs were tried and which one finally worked.
				        
			        }catch (ProcessingException e2){
			        	
						String messageFallbackException = "DBPediaSpotlight: ProcessingException occured while calling DBPedia spotlight via fallback URL " + fallbackURL + ". Details: " + e.getMessage();
						errorMessages.append("\n" + messageFallbackException);
						Logger.warn(messageFallbackException);
			        	// try next 
			        	continue;
			        }
				}
				
				// none of fallback URLs worked:
				String messageFallbacksError = "DBPediaSpotlight: Error when processing DBPediaSpotlight. Neither calling URL nor calling fallback URLs was sucessful.";
				errorMessages.append("\n" + messageFallbacksError);
				Logger.warn(messageFallbacksError);
				throw new ProcessingException(errorMessages.toString());
			}
			else{ // no fallback URLs available
				String errorMessage = "DBPediaSpotlight: Processing Exception when calling DBPedia Spotlight: " + e.getMessage();
				Logger.warn(errorMessage);
				throw new ProcessingException(errorMessage, e);
				
			}
			
		}
		
        
	}
	
	
	/**
	 * 
	 * @param input
	 * @param confidence
	 * @param url
	 * @param types
	 * @return
	 * @throws ResponseProcessingException - in case processing of a received HTTP response fails (e.g. in a filter or during conversion of the response entity data to an instance of a particular Java type).
	 * @throws ProcessingException - in case the request processing or subsequent I/O operation fails
	*/
	private Response getSpotlightResponse(String input, double confidence, String types, String url) throws ResponseProcessingException, ProcessingException {
		Form form = new Form();
        form.param("confidence", String.valueOf(confidence));
        form.param("text", input);
        
        
        if(types!=null && types.trim().length()>0 && !types.equals(DBPediaSpotlightUtil.nameForNoTypeRestriction)){
            form.param("types", types);
        }
                
	    Response response =  client.target(url)
	                .request()
	                .accept(MediaType.APPLICATION_JSON)
	                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
	
	    return response;
        
	}
	
	public static JsonNode getJsonFromMessageBody(Response response) {

		String result = response.readEntity(String.class);
		JsonNode resultNode = Json.parse(result);
		return resultNode;
			
	}
	public static String removeSpecialChars(String input){
		String[] specialChars = new String[]{"{", "}", "", "", ""};
		for (String specialChar : specialChars) {
			input = input.replace(specialChar, "");
		}
		return input;
	}
	
	public static String getSpotlightNameFromURI(String URI){
		String urlSequenceToRemove = "http://dbpedia.org/resource/";
		int lengthOfUrlSequenceToRemove = urlSequenceToRemove.length();
		String name = URI.substring(lengthOfUrlSequenceToRemove);
		if(name.contains("_")){
			name = name.replace("_", " ");
		}
		return name;
	}
	
	public static ArrayNode getSpotlightResources(JsonNode spotlightNode){
				
		if(!spotlightNode.has("Resources")){
			return null;
		}
		JsonNode spotlightResourcesNode = spotlightNode.get("Resources");
		if(spotlightResourcesNode==null || spotlightResourcesNode.isNull()){
			return null;
		}
		
		ArrayNode resources = (ArrayNode) spotlightResourcesNode;
		return resources;
	}
	
	public static Map<String,Integer> getSpotlightFrequenciesForURIsByAnalyzingSpotlightResults(JsonNode spotlightNode){
		return getSpotlightFrequenciesByAnalyzingSpotlightResults(spotlightNode, "@URI");
	}

	public static Map<String,Integer> getSpotlightFrequenciesForSurfaceFormsByAnalyzingSpotlightResults(JsonNode spotlightNode){
		return getSpotlightFrequenciesByAnalyzingSpotlightResults(spotlightNode, "@surfaceForm");
	}

	
	/**
	 * Returns spotlight frequencies retrieved from spotlight resources per deck
	 * @param nlpResult
	 * @param keyname e.g. "@URI" for the URI or "@surfaceForm" for the actual form used in text
	 * @return
	 */
	public static Map<String,Integer> getSpotlightFrequenciesByAnalyzingSpotlightResults(JsonNode spotlightNode, String keyname){
		
		ArrayNode spotlightResources = getSpotlightResources(spotlightNode);		
		return getSpotlightFrequenciesByAnalyzingSpotlightResources(spotlightResources, keyname);
	
	}

	public static Map<String,Integer> getSpotlightURIFrequenciesByAnalyzingSpotlightResources(ArrayNode spotlightResources){
		return getSpotlightFrequenciesByAnalyzingSpotlightResources(spotlightResources, "@URI");
	}
	
	public static Map<String,Integer> getSpotlightSurfaceformFrequenciesByAnalyzingSpotlightResources(ArrayNode spotlightResources){
		return getSpotlightFrequenciesByAnalyzingSpotlightResources(spotlightResources, "@surfaceForm");
	}
	/**
	 * Returns spotlight frequencies retrieved from spotlight resources per deck
	 * @param nlpResult
	 * @param keyname e.g. "@URI" for the URI or "@surfaceForm" for the actual form used in text
	 * @return
	 */
	public static Map<String,Integer> getSpotlightFrequenciesByAnalyzingSpotlightResources(ArrayNode spotlightResources, String keyname){
		Map<String,Integer> result = new HashMap<>();
		if(spotlightResources==null){
			return result;
		}
		for (int i = 0; i < spotlightResources.size(); i++) {
			JsonNode resourceNode = spotlightResources.get(i);
			String value = resourceNode.get(keyname).textValue();
			MapCounting.addToCountingMap(result, value);
		}
		return result;
	}
	
	public static ArrayNode filterResourcesForTextSpan(ArrayNode spotlightResources, int minTextSpan, int maxTextSpan){
		ArrayNode resultArrayNode = Json.newArray();
		if(spotlightResources == null){
			return resultArrayNode;
		}
		Iterator<JsonNode> iteratorResources = spotlightResources.iterator();
		while(iteratorResources.hasNext()){
			JsonNode spotlightResourceNode = iteratorResources.next();
			int begin = spotlightResourceNode.get("@offset").asInt();
			String surfaceForm = spotlightResourceNode.get("@surfaceForm").asText();
			int end = begin + surfaceForm.length()-1;
			
			if(begin < minTextSpan){
				continue;
			}
			if(end > maxTextSpan){
				continue;
			}
		
			resultArrayNode.add(spotlightResourceNode);
		}
		return resultArrayNode;
	}
	
	public void close(){
		this.client.close();
	}
}
