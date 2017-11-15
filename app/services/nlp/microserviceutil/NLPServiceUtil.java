package services.nlp.microserviceutil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.libs.Json;
import services.nlp.recommendation.NlpTag;

public class NLPServiceUtil {

	private Client client;
	private String serviceURL;
	
	private boolean DEFAULT_VALUE_performTitleBoost = true;
	private int DEFAULT_VALUE_titleBoostWithFixedFactor = -1;
	private boolean DEFAULT_VALUE_titleBoostlimitToFrequencyOfMostFrequentWord = true;
	private int DEFAULT_VALUE_minCharLengthForTag = 3;
	private int DEFAULT_VALUE_maxNumberOfWordsForNEsWhenNoLinkAvailable = 4;
	
	
	public NLPServiceUtil(String serviceURL) {
		this.serviceURL = serviceURL;
		this.client = ClientBuilder.newClient();
	}
	
	
	public Response getTagRecommendations(String deckId, int maxEntriesToReturn){
		
		String URL = serviceURL + "/nlp/tagRecommendations/" + deckId;
		
        Response response = client.target(URL)
				.queryParam("performTitleBoost", DEFAULT_VALUE_performTitleBoost)
				.queryParam("titleBoostWithFixedFactor", DEFAULT_VALUE_titleBoostWithFixedFactor)
				.queryParam("titleBoostlimitToFrequencyOfMostFrequentWord", DEFAULT_VALUE_titleBoostlimitToFrequencyOfMostFrequentWord)
				.queryParam("maxNumberOfWordsForNEsWhenNoLinkAvailable", DEFAULT_VALUE_maxNumberOfWordsForNEsWhenNoLinkAvailable)
				.queryParam("minCharLengthForTag", DEFAULT_VALUE_minCharLengthForTag)
				.queryParam("maxEntriesToReturnTagRecommendation", maxEntriesToReturn)
				
					.request().
					get();
                
	    return response;
		
	}

	
	public static List<NlpTag> getTagRecommendations(JsonNode recommendationsNode){
		
		List<NlpTag> result = new ArrayList<>();
		ArrayNode recommendations = (ArrayNode) recommendationsNode.get("TagRecommendations");
		Iterator<JsonNode> iterator = recommendations.elements();
		while(iterator.hasNext()){
			JsonNode recommendationNode = iterator.next();
			NlpTag nlpTag = Json.fromJson(recommendationNode, NlpTag.class);
			result.add(nlpTag);
		}
		return result;
	}
	
	public static List<String> getTagRecommendationsAsStringList(JsonNode recommendationsNode){
		
		List<String> result = new ArrayList<>();
		ArrayNode recommendations = (ArrayNode) recommendationsNode.get("TagRecommendations");
		Iterator<JsonNode> iterator = recommendations.elements();
		while(iterator.hasNext()){
			JsonNode recommendationNode = iterator.next();
			String word = recommendationNode.get("name").asText();
			result.add(word);
		}
		return result;
	}
}
