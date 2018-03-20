package services.nlp.recommendation;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.tfidf.TFIDFResult;
import services.util.Sorter;

public class DeckRecommendation {


	public static JsonNode calculateLuceneValuesForTfidfMapAndReturnAsJsonNode(Map<String,Double> map, int maxEntriesToUse, String keynameForSolr){
		
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
			
			sbLuceneQuery.append(" " + keynameForSolr + ":");
			sbLuceneQuery.append("\""+ key + "\"");
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
	
	
	public static Map<String, Double> recreateMapFromJson(JsonNode jsonNode, String valueNameToUse){
		
		Map<String, Double> map = new HashMap<String, Double>();
		ArrayNode arrayNode = (ArrayNode) jsonNode.get("values");
		Iterator<JsonNode> iterator = arrayNode.iterator();
		while (iterator.hasNext()){
			JsonNode node = iterator.next();
			String entryKey = node.get("key").asText();
			Double value = node.get(valueNameToUse).asDouble();
			map.put(entryKey, value);
		}
		
		return map;
	}
	
	public static ObjectNode createDeckRecommendationBackgroundInfoNodeIncludingLuceneQueryFromTFIDFResult(TFIDFResult tfidfResult, int maxTermsToConsider){
		
		ObjectNode result = Json.newObject();
		
		result.put("language", tfidfResult.getLanguage());
		result.put("numberOfDecksWithGivenLanguage", tfidfResult.getNumberOfDecksInPlatformWithGivenLanguage());
		result.put("numberOfDecksOverall", tfidfResult.getNumberOfDecksInPlatformOverall());
		result.put("tfidfValuesWereCalculatedLanguageDependent", tfidfResult.isTfidfValuesWereCalculatedLanguageDependent());
		
		Map<String,Map<String,Double>> tfidfmap = tfidfResult.getTfidfMap();
		Set<String> providers= tfidfmap.keySet();

		StringBuilder sb = new StringBuilder();
		ArrayNode providersArrayNode = Json.newArray();
		for (String provider : providers) {
			
			Map<String,Double> tfidfMap = tfidfmap.get(provider);
			if(tfidfMap.isEmpty()){
				continue;
			}
			String keynameForSolr = NLPResultUtil.getSolrNameForProviderName(provider);
			
			// calculate & create lucene stuff
			JsonNode nodeForMap = DeckRecommendation.calculateLuceneValuesForTfidfMapAndReturnAsJsonNode(tfidfMap, maxTermsToConsider, keynameForSolr);
			
			// get lucene query for provider to create concatenated lucene query for all providers
			String luceneQueryStringForProvider = nodeForMap.get("luceneQuery").asText();
			sb.append(luceneQueryStringForProvider);
			
			ObjectNode providerNode = Json.newObject();
			providerNode.put("shortname", NLPResultUtil.getShortName(provider));
			providerNode.put("longname", provider);
			providerNode.set("topXEntries", nodeForMap);
			providersArrayNode.add(providerNode);
		}
		
		result.set("details", providersArrayNode);
		result.put("luceneQuery", sb.toString());
		
		return result;

	}
}
