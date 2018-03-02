package services.nlp.recommendation;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.util.Sorter;

public class DeckRecommendation {


	public static JsonNode createJsonNodeForResponseFromMap(Map<String,Double> map, int maxEntriesToUse){
		
		ObjectNode result = Json.newObject();
		
		if(map.isEmpty()){
			return result;
		}
		
		List<Entry<String, Double>> sorted = Sorter.sortByValueAndReturnAsList(map, true);
		StringBuilder sbLuceneQuery = new StringBuilder();
		Double highestValue = sorted.get(0).getValue();
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.HALF_UP);

		ArrayNode arrayNode = Json.newArray();
		int counter = 0;
		
		for (Entry<String, Double> entry : sorted) {
			
			counter++;
			if(maxEntriesToUse>=0 && counter > maxEntriesToUse){
				break;
			}
			String key = entry.getKey();
			Double value = entry.getValue();
			Double valueNormalized = value / highestValue;
			String valueRounded = df.format(valueNormalized).replace(',', '.');
			
			String valueForLuceneQuery = valueRounded;
			// remove leading zero (normalized can be only start with 0 or 1
			if(valueRounded.startsWith("0")){
				valueForLuceneQuery = valueRounded.substring(1);
			}
			
			sbLuceneQuery.append(" body:");
			sbLuceneQuery.append(key);
			sbLuceneQuery.append("^");
			sbLuceneQuery.append(valueForLuceneQuery);
			
			ObjectNode entryAsJson = Json.newObject();
			entryAsJson.put("key", key);
			entryAsJson.put("value", value);
			entryAsJson.put("valueNormalized", valueNormalized);
			entryAsJson.put("valueRounded", valueRounded);
			entryAsJson.put("valueForLuceneQuery", valueForLuceneQuery);

			arrayNode.add(entryAsJson);
		}
		String luceneQuery = sbLuceneQuery.toString();
		
		
		result.put("luceneQuery", luceneQuery);
		result.set("values", arrayNode);
		
		return result;
	}
}
