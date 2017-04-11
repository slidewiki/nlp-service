package services.nlp.nlpresultstorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.NlpTag;
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
	
	public static String propertyNameInFrequencyEntriesForWord = "entry";
	public static String propertyNameInFrequencyEntriesForFrequency = "frequency";
	
	public static String propertyNameTFIDF = "TFIDF";
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
	
	
	
	public static Map<String,Integer> getFrequenciesStoredInNLPResult(ObjectNode nlpResult, String propertyName, String nameForWord, String nameForFrequency){
		
		ArrayNode wordFrequencyArray = (ArrayNode) nlpResult.get(propertyName);
		Iterator<JsonNode> iteratorWordFrequencyArray= wordFrequencyArray.iterator();
		Map<String,Integer> mapWordFrequencies = new HashMap<>();
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
		if(spotlightResourcesNode!=null && !spotlightResourcesNode.isNull()){
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
			NlpTag nerEntity = Json.fromJson(neEntry, NlpTag.class);
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
	
	
		
}
