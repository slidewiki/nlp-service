package services.nlp.tfidf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import services.nlp.microserviceutil.DeckServiceUtil;
import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;

public class DocFrequencyCreatorForDecks {
	
	public static IDocFrequencyProviderTypeDependent createDocFrequencyProviderViaMapByRetrievingAllDataFromNLPStoreFirst(DeckServiceUtil deckserviceUtil, NLPStorageUtil nlpStorageUtil){
		
		Response responseLatestDeck = deckserviceUtil.getLatestDeckId();
		int statusLatestDeck = responseLatestDeck.getStatus();
		if(statusLatestDeck!=200){
			throw new WebApplicationException("Problem while latest deck id via deck service. See deck service response for details.", responseLatestDeck);
		}
		JsonNode node = ((ArrayNode) MicroserviceUtil.getJsonFromMessageBody(responseLatestDeck)).get(0);
		Integer latestDeckId = node.get("_id").asInt();
		
		
		Logger.info("Creating DocFrequencyProvider. Latest deck id " + latestDeckId);
		
		Map<String,String> mapSupportedTypeToJsonKeynameForFrequencyEntries = initializeSupportedTypesMap();
		Set<String> supportedTypes = mapSupportedTypeToJsonKeynameForFrequencyEntries.keySet();
		DocFrequencyProviderTypeDependentViaMap docFrequencyProvider = initializEmptyDocFreqencyProviderWithSupportedTypes(supportedTypes);

		for (int deckId = 0; deckId <= latestDeckId; deckId++) {
			
			if(deckId % 100==0){
				Logger.info("... reading deck id " + deckId);
			}
			Response response = nlpStorageUtil.getNLPResultForDeckId(deckId+"");
			
			int status = response.getStatus();
			if(status == 404){ // not found
				// deck id doesn't exist, skip
				continue;
			}
			else if(status == 200){
				
				ObjectNode nlpResultNode = (ObjectNode) MicroserviceUtil.getJsonFromMessageBody(response);
				
				String language = NLPResultUtil.getLanguage(nlpResultNode);
				
				for (String supportedType : supportedTypes) {
					
					String jsonKeynameForFrequencyEntries = mapSupportedTypeToJsonKeynameForFrequencyEntries.get(supportedType);
					Set<String> entries = NLPResultUtil.getDistinctEntriesFromFrequencies(nlpResultNode, jsonKeynameForFrequencyEntries, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
					
					// add entries for language dependent
					docFrequencyProvider.add(supportedType, language, entries);
					// add entries for not language dependent
					docFrequencyProvider.add(supportedType, DocFrequencyProviderViaMap.specialNameForLanguageIndependent, entries);			
					
				}
				
				
			}else{
				// any other problem
				throw new WebApplicationException("Problem while getting nlp result from nlp store for deck id " + deckId + ".", response);
			}
			
		}
		
		return docFrequencyProvider;
	}
	

	private static Map<String,String> initializeSupportedTypesMap(){
		Map<String,String> map = new HashMap<>();
		map.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens, NLPResultUtil.propertyNameWordFrequenciesExclStopwords); 
		map.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_TITLEBOOST", NLPResultUtil.propertyNameWordFrequenciesExclStopwords + "_TITLEBOOST"); 
		map.put(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, NLPResultUtil.propertyNameNERFrequencies); 
		map.put(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies);
//		map.put(NLPResultUtil.propertyNameDocFreqProvider_SpotlightSurfaceForm, NLPResultUtil.propertyNameDBPediaSpotlight);
		return map;
	}

	private static DocFrequencyProviderTypeDependentViaMap initializEmptyDocFreqencyProviderWithSupportedTypes(Set<String> supportedTypes){

		
		Map<String, DocFrequencyProviderViaMap> mapTypeToDocFrequencyProvider = new HashMap<>();
		for (String supportedType : supportedTypes) {
			mapTypeToDocFrequencyProvider.put(supportedType, new DocFrequencyProviderViaMap());
		}
		return new DocFrequencyProviderTypeDependentViaMap(mapTypeToDocFrequencyProvider);
	}
}
