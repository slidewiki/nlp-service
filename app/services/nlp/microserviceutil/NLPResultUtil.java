package services.nlp.microserviceutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.ner.NerAnnotation;
import services.util.MapCounting;

public class NLPResultUtil {
	
	public static String propertyNameOriginalInput = "input";
	public static String propertyNameHtmlToPlainText = "htmlToPlainText";
	public static String propertyNameSlideTitleAndText = "slideTitleAndText";
	public static String propertyNameLanguage = "detectedLanguage";
	public static String propertyNameTokens = "tokens";
	public static String propertyNameFrequencyOfMostFrequentWord = "frequencyOfMostFrequentWord";
	public static String propertyNameWordFrequenciesExclStopwords = "wordFrequenciesExclStopwords";
	public static String propertyNameNER = "NER";
	public static String propertyNameNERFrequencies = "NERFrequencies";
	public static String propertyNameDBPediaSpotlight = "DBPediaSpotlight";
	public static String propertyNameDBPediaSpotlightURIFrequencies = "DBPediaSpotlightURIFrequencies";
	public static String propertyNameTagRecommendations = "TagRecommendations";

	public static String propertyNameInFrequencyEntriesForWord = "entry";
	public static String propertyNameInFrequencyEntriesForFrequency = "frequency";
	
	public static String propertyNameTFIDF = "TFIDF";
	public static String propertyNameTFIDFProviderName = "providerName";
	public static String propertyNameTFIDFResultArrayName = "results";
	public static String propertyNameTFIDFEntityName = "entry";
	public static String propertyNameTFIDFValueName = "value";

	// key names for specific document frequency providers
	public static String propertyNameDocFreqProvider_Tokens = "docFreqProvider_Tokens";
	public static String propertyNameDocFreqProvider_NamedEntities = "docFreqProvider_NamedEntities";
	public static String propertyNameDocFreqProvider_SpotlightURI = "docFreqProvider_SpotlightURI";
	public static String propertyNameDocFreqProvider_SpotlightSurfaceForm = "docFreqProvider_SpotlightSurfaceForm";

	
	public static String getLanguage(ObjectNode nlpResult){
		if(!nlpResult.has(NLPResultUtil.propertyNameWordFrequenciesExclStopwords)){
			return null;
		}
		String language = nlpResult.get(NLPResultUtil.propertyNameLanguage).asText();
		return language;
	}
	
	public static Integer getFrequencyOfMostFrequentWordInDoc(ObjectNode nlpResult){
		if(!nlpResult.has(NLPResultUtil.propertyNameWordFrequenciesExclStopwords)){
			return null;
		}
		int frequencyOfMostFrequentWord = nlpResult.get(NLPResultUtil.propertyNameFrequencyOfMostFrequentWord).asInt();
		return frequencyOfMostFrequentWord;
	}
	
	public static Set<String> getDistinctEntriesFromFrequencies(ObjectNode nlpResult, String propertyName, String nameForWord, String nameForFrequency){
		
		Set<String> entries = new HashSet();
		
		JsonNode node = nlpResult.get(propertyName);
		if(node==null){
			return entries;
		}
		if(!node.isArray()){
			return entries;
		}
		ArrayNode wordFrequencyArray = (ArrayNode) node;
		Iterator<JsonNode> iteratorWordFrequencyArray= wordFrequencyArray.iterator();
		while(iteratorWordFrequencyArray.hasNext()){
			JsonNode entry = iteratorWordFrequencyArray.next();
			String word = entry.get(nameForWord).asText();
			entries.add(word);
		}
		
		return entries;
	}
	
	
	public static Map<String,Integer> getFrequenciesStoredInNLPResult(ObjectNode nlpResult, String propertyName, String nameForWord, String nameForFrequency){
		
		Map<String,Integer> mapWordFrequencies = new HashMap<>();

		JsonNode node = nlpResult.get(propertyName);
		if(node==null){
			return mapWordFrequencies;
		}
		if(!node.isArray()){
			return mapWordFrequencies;
		}
		ArrayNode wordFrequencyArray = (ArrayNode) node;
		Iterator<JsonNode> iteratorWordFrequencyArray= wordFrequencyArray.iterator();
		while(iteratorWordFrequencyArray.hasNext()){
			JsonNode entry = iteratorWordFrequencyArray.next();
			String word = entry.get(nameForWord).asText();
			Integer frequency = entry.get(nameForFrequency).asInt();
			mapWordFrequencies.put(word, frequency);
		}
		
		return mapWordFrequencies;
	}
	
	public static Map<String,Integer> getSpotlightFrequenciesForURIsByAnalyzingSpotlightResults(ObjectNode nlpResult){
		return getSpotlightFrequenciesByAnalyzingSpotlightResults(nlpResult, "@URI");
	}

	public static Map<String,Integer> getSpotlightFrequenciesForSurfaceFormsByAnalyzingSpotlightResults(ObjectNode nlpResult){
		return getSpotlightFrequenciesByAnalyzingSpotlightResults(nlpResult, "@surfaceForm");
	}
	
	/**
	 * Returns spotlight frequencies retrieved from spotlight resources per deck
	 * @param nlpResult
	 * @param keyname e.g. "@URI" for the URI or "surfaceForm" for the actual form used in text
	 * @return
	 */
	public static Map<String,Integer> getSpotlightFrequenciesByAnalyzingSpotlightResults(ObjectNode nlpResult, String keyname){
		
		Map<String,Integer> result = new HashMap<>();
		ArrayNode spotlightResources = getSpotlightResources(nlpResult);
		if(spotlightResources==null){
			return result;
		}
		for (int i = 0; i < spotlightResources.size(); i++) {
			JsonNode resourceNode = spotlightResources.get(i);
			String URI = resourceNode.get(keyname).textValue();
			MapCounting.addToCountingMap(result, URI);
		}
		
		return result;
	
	}
	
	
	private static ArrayNode getSpotlightResources(ObjectNode nlpResult){
		
		if(!nlpResult.has(propertyNameDBPediaSpotlight)){
			return null;
		}
		JsonNode spotlight= nlpResult.get(NLPResultUtil.propertyNameDBPediaSpotlight);
		if(!spotlight.has("Resources")){
			return null;
		}
		JsonNode spotlightResourcesNode = spotlight.get("Resources");
		if(spotlightResourcesNode==null || spotlightResourcesNode.isNull()){
			return null;
		}
		
		ArrayNode resources = (ArrayNode) spotlightResourcesNode;
		return resources;
	}
	

	/**
	 * Retrieves the Named Entity frequencies.
	 * If several NER methods were used, the same entity might be recognized more than once.
	 * To count these entities only once, the process includes identity check via spans. If the same span was already counted, it will be skipped.
	 * @param nlpResult
	 * @return
	 */
	public static Map<String,Integer> getNERFrequenciesByAnalyzingNEs(ObjectNode nlpResult){
		
		Map<String,Integer> result = new HashMap<>(); 
		
		ArrayNode namedEntityArray = (ArrayNode) nlpResult.get(NLPResultUtil.propertyNameNER);
		Iterator<JsonNode> iterator = namedEntityArray.iterator();
		Set<String> tokenSpans= new HashSet<>(); // tracks tokenSpans (as String begin_end for counting NEs detected by several sources only once (identity is defined here by the token spans))
		while(iterator.hasNext()){
			JsonNode neEntry = iterator.next();
			NerAnnotation nerEntity = Json.fromJson(neEntry, NerAnnotation.class);
			int tokenspanBegin = nerEntity.getTokenSpanBegin();
			if(tokenspanBegin>=0){// only do tracking if token spans available
				int tokenSpanEnd = nerEntity.getTokenSpanEnd();
				String tokenSpan = tokenspanBegin + "_" + tokenSpanEnd;
				if(tokenSpans.contains(tokenSpan)){
					continue;
				}
				tokenSpans.add(tokenSpan);
				String ne = neEntry.get("name").textValue();
				MapCounting.addToCountingMap(result, ne);				
			}
			
		}
		return result;
	}
	
	
	public static ArrayNode getTFIDFArrayNode(JsonNode nlpResult){
		if(!nlpResult.has(NLPResultUtil.propertyNameTFIDF)){
			return null;
		}
		JsonNode node = nlpResult.get(NLPResultUtil.propertyNameTFIDF);
		if(!node.isArray()){
			return null;
		}
		ArrayNode arraynode = (ArrayNode) node;
		return arraynode;		
	}
	
	public static Map<String,Map<String,Double>> getTFIDFEntries(JsonNode nlpResult){
		
		Map<String,Map<String,Double>> mapProviderNameToTFIDFMap = new HashMap<>();
		ArrayNode arrayNode = getTFIDFArrayNode(nlpResult);
		if(arrayNode==null){
			return null;
		}
		Iterator<JsonNode> iteratorTFIDFProviderResults = arrayNode.iterator();
		while(iteratorTFIDFProviderResults.hasNext()){
			JsonNode tfidfNode = iteratorTFIDFProviderResults.next();
			String providerName = tfidfNode.get(NLPResultUtil.propertyNameTFIDFProviderName).asText();
			ArrayNode tfidfResultsArrayNode = (ArrayNode) tfidfNode.get(NLPResultUtil.propertyNameTFIDFResultArrayName);
			Iterator<JsonNode> iteratorTFIDFResults = tfidfResultsArrayNode.iterator();
			Map<String,Double> mapEntryToTFIDF = new HashMap<>();
			while(iteratorTFIDFResults.hasNext()){
				JsonNode tfidfResult = iteratorTFIDFResults.next();
				String entityName = tfidfResult.get(NLPResultUtil.propertyNameTFIDFEntityName).textValue();
				Double tfidfValue = tfidfResult.get(NLPResultUtil.propertyNameTFIDFValueName).asDouble();
				mapEntryToTFIDF.put(entityName, tfidfValue);
			}
			mapProviderNameToTFIDFMap.put(providerName, mapEntryToTFIDF);
		}
		
		return mapProviderNameToTFIDFMap;
	}
}
