package services.nlp.microserviceutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class NLPResultUtil {
	
	public static String propertyNameOriginalInput = "input";
	public static String propertyNameHtmlToPlainText = "htmlToPlainText";
	public static String propertyNameDeckId = "deckId";
	public static String propertyNameDeckTitle = "deckTitle";
	public static String propertyNameNumberOfSlides = "numberOfSlides";
	public static String propertyNameNumberOfSlidesWithText = "numberOfSlidesWithText";

	public static String propertyNameSlidesNode = "children";
	public static String propertyNameSlideId = "slideId";
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
	public static String propertyNameInFrequencyEntriesForDeckTitleFrequency = "frequencyInDeckTitle";

	public static String propertyNameTFIDF = "TFIDF";
	public static String propertyNameTFIDFProviderName = "providerName";
	public static String propertyNameTFIDFResultArrayName = "results";
	public static String propertyNameTFIDFEntityName = "entry";
	public static String propertyNameTFIDFValueName = "value";
	public static String propertyNameTFIDFToken = propertyNameTFIDF + propertyNameTokens;
	public static String propertyNameTFIDFNER = propertyNameTFIDF + propertyNameNER;
	public static String propertyNameTFIDFDBPediaSpotlightURIs = propertyNameTFIDF + propertyNameDBPediaSpotlight + "_URI";
	public static String propertyNameTFIDFDBPediaSpotlightSurfaceForm = propertyNameTFIDF + propertyNameDBPediaSpotlight + "_SurfaceForm";
	


	
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
		
		Set<String> entries = new HashSet<String>();
		
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
	
	/**
	 * puts an extra field for title frequencies to the given frequencies.
	 * @param frequencyNode
	 * @param titleFrequencies
	 * @return
	 */
	public static JsonNode putTitleFrequenciesToFrequencyNode(JsonNode frequencyNode, Map<String,Integer> titleFrequencies){
		
		ArrayNode resultNode = Json.newArray();
		
		if(titleFrequencies.size()==0){
			return resultNode;
		}
		if(frequencyNode==null || frequencyNode.size()==0 || !frequencyNode.isArray()){
			return resultNode;
		}
		
		ArrayNode wordFrequencyArray = (ArrayNode) frequencyNode;
		Iterator<JsonNode> iteratorWordFrequencyArray= wordFrequencyArray.iterator();
		while(iteratorWordFrequencyArray.hasNext()){
			
			JsonNode frequencyEntry = iteratorWordFrequencyArray.next();
			String word = frequencyEntry.get(NLPResultUtil.propertyNameInFrequencyEntriesForWord).asText();
			if(titleFrequencies.containsKey(word)){
				ObjectNode node = Json.newObject();
				Integer frequency = frequencyEntry.get(NLPResultUtil.propertyNameInFrequencyEntriesForFrequency).asInt();
				Integer frequencyDeckTitle = titleFrequencies.get(word);
				node.put(NLPResultUtil.propertyNameInFrequencyEntriesForWord, word);
				node.put(NLPResultUtil.propertyNameInFrequencyEntriesForFrequency, frequency);
				node.put(NLPResultUtil.propertyNameInFrequencyEntriesForDeckTitleFrequency, frequencyDeckTitle);
				resultNode.add(node);
			}else{
				// add it to result as is
				resultNode.add(frequencyEntry);
			}
		}
		return resultNode;
		
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
	
	
	public static ArrayNode getSlidesNode(JsonNode nlpResult){
		
		if(nlpResult.has(NLPResultUtil.propertyNameSlidesNode)){
			ArrayNode slidesNode = (ArrayNode) nlpResult.get(NLPResultUtil.propertyNameSlidesNode);
			return slidesNode;
		}
		else{
			return Json.newArray();
		}
	}
	
	public static String getSlideTitleAndTextFromSlideNode(JsonNode slideNode){
		return slideNode.get(NLPResultUtil.propertyNameSlideTitleAndText).asText();
	}
	
	public static List<String> getSlidesText(JsonNode nlpResult){
	
		List<String> result = new ArrayList<>();
		ArrayNode slidesNode = NLPResultUtil.getSlidesNode(nlpResult);
		Iterator<JsonNode> iterator = slidesNode.iterator();
		while(iterator.hasNext()){
			JsonNode slideNode = iterator.next();
			String slideText = NLPResultUtil.getSlideTitleAndTextFromSlideNode(slideNode);
			result.add(slideText);
		}
		return result;
	}
	
	public static String geSlidesTextAsOneString(JsonNode nlpResult, String separatorToUseBetweenSlides, boolean skipSlidesWithoutText){
		
		ArrayNode slidesNode = NLPResultUtil.getSlidesNode(nlpResult);
		Iterator<JsonNode> iterator = slidesNode.iterator();
		StringBuilder sb = new StringBuilder();
		boolean firstSlideText = true;
		while(iterator.hasNext()){
			JsonNode slideNode = iterator.next();
			String slideText = NLPResultUtil.getSlideTitleAndTextFromSlideNode(slideNode);
			if(skipSlidesWithoutText && slideText.isEmpty()){
				continue;
			}
			if(!firstSlideText){
				sb.append(separatorToUseBetweenSlides);
			}
			sb.append(slideText);
			firstSlideText = false;
		}
		return sb.toString();
	}
}
