package services.nlp.nlpresultstorage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import services.util.MapCounting;

public class NLPResultUtil {
	
	public static String propertyNameOriginalInput = "input";
	public static String propertyNameHtmlToPlainText = "htmlToPlainText";
	public static String propertyNameSlideTitleAndText = "slideTitleAndText";
	public static String propertyNameLanguage = "detectedLanguage";
	public static String propertyNameTokens = "tokens";
	public static String propertyNameFrequencyOfMostFrequentWord = "frequencyOfMostFrequentWord";
	public static String propertyNameWordTypesAndFrequencies = "wordFrequenciesExclStopwords";
	public static String propertyNameNER = "NER";
	public static String propertyNameTFIDF = "TFIDF";
	public static String propertyNameDBPediaSpotlight = "DBPediaSpotlight";
	
	// key names for specific document frequency providers
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent";

	
	public static String getLanguage(ObjectNode nlpResult){
		if(!nlpResult.has(NLPResultUtil.propertyNameWordTypesAndFrequencies)){
			return null;
		}
		String language = nlpResult.get(NLPResultUtil.propertyNameLanguage).asText();
		return language;
	}
	public static Integer getFrequencyOfMostFrequentWordInDoc(ObjectNode nlpResult){
		if(!nlpResult.has(NLPResultUtil.propertyNameWordTypesAndFrequencies)){
			return null;
		}
		int frequencyOfMostFrequentWord = nlpResult.get(NLPResultUtil.propertyNameFrequencyOfMostFrequentWord).asInt();
		return frequencyOfMostFrequentWord;
	}
	
	public static Map<String,Integer> getWordFrequencies(ObjectNode nlpResult){
		
		ArrayNode wordFrequencyArray = (ArrayNode) nlpResult.get(propertyNameWordTypesAndFrequencies);
		Iterator<JsonNode> iteratorWordFrequencyArray= wordFrequencyArray.iterator();
		Map<String,Integer> mapWordFrequencies = new HashMap<>();
		while(iteratorWordFrequencyArray.hasNext()){
			JsonNode entry = iteratorWordFrequencyArray.next();
			String word = entry.get("word").asText();
			Integer frequency = entry.get("frequency").asInt();
			mapWordFrequencies.put(word, frequency);
		}
		
		return mapWordFrequencies;
	}
	
	
	public static Map<String,Integer> getSpotlightFrequenciesForURIs(ObjectNode nlpResult){
		return getSpotlightFrequencies(nlpResult, "@URI");
	}

	public static Map<String,Integer> getSpotlightFrequenciesForSurfaceForms(ObjectNode nlpResult){
		return getSpotlightFrequencies(nlpResult, "@surfaceForm");
	}
	
	/**
	 * Returns spotlight frequencies retrieved from spotlight resources per deck
	 * @param nlpResult
	 * @param keyname e.g. "@URI" for the URI or "surfaceForm" for the actual form used in text
	 * @return
	 */
	public static Map<String,Integer> getSpotlightFrequencies(ObjectNode nlpResult, String keyname){
		
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

		
}
