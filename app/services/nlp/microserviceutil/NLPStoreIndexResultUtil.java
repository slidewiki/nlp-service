package services.nlp.microserviceutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.libs.Json;

public class NLPStoreIndexResultUtil {

	public static ArrayNode getArrayNodeWithItems(JsonNode nlpStoreIndexResult) {
		
		if(!nlpStoreIndexResult.has("items")){
			return Json.newArray(); // no items found, return empty arraynode
		}
		ArrayNode itemArrayNode = (ArrayNode) nlpStoreIndexResult.get("items");
		
		return itemArrayNode;

	}

	public static String getDeckIdFromSingleItemEntry(JsonNode nlpStoreIndexResultEntry) {

		return nlpStoreIndexResultEntry.get("_id").asText();
	}

	public static double getValueFromSingleItemEntry(JsonNode nlpStoreIndexResultEntry) {
		
		return nlpStoreIndexResultEntry.get("score").doubleValue();
	}

}
