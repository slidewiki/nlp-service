package services.nlp.tfidf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;

public class DocFrequencyCreatorForDecks {
	
	public static IDocFrequencyProviderTypeDependent createDocFrequencyProviderViaMapByRetrievingAllDataFromNLPStoreFirst(NLPStorageUtil nlpStorageUtil, int minDeckId, int maxDeckId){
		
		
		Map<String,String> mapSupportedTypeToJsonKeyname = initializeSupportedTypesMap();
		Set<String> supportedTypes = mapSupportedTypeToJsonKeyname.keySet();
		DocFrequencyProviderTypeDependentViaMap docFrequencyProvider = initializEmptyDocFreqencyProviderWithSupportedTypes(supportedTypes);

		for (int deckId = minDeckId; deckId <= maxDeckId; deckId++) {
			
			Response response = nlpStorageUtil.getNLPResultForDeckId(deckId+"");
			
			int status = response.getStatus();
			if(status == 404){ // not found
				// deck id doesn't exist, skip
				continue;
			}
			else if(status == 200){
				
				ObjectNode nlpResultNode = (ObjectNode) NLPStorageUtil.getJsonFromMessageBody(response);
				
				String language = NLPResultUtil.getLanguage(nlpResultNode);
				
				for (String supportedType : supportedTypes) {
					
					String jsonKeynameForFrequencyEntries = mapSupportedTypeToJsonKeyname.get(supportedType);
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
	
//	public static DocFrequencyProviderViaMap[] createDocFrequencyProvidersFromDecksOfDeckservice(String deckserviceURL, IHtmlToText htmlToText, int maxDeckId, NLPComponent nlpComponent, double spotlightConfidence){
//		
//		DocFrequencyProviderViaMap docFrequencyProviderPerLanguage = new DocFrequencyProviderViaMap();
//		DocFrequencyProviderViaMap docFrequencyProviderOneForAllLanguages = new DocFrequencyProviderViaMap();
//		
//		DeckServiceUtil deckServiceUtil = new DeckServiceUtil(deckserviceURL);
//		for (int deckId = 0; deckId <= maxDeckId; deckId++) {
//			
//			Logger.info(Timer.getDateAndTime() + "\tprocessing: " + deckId);
//			
//			// get slides from deckservice
//			Response response = deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId+"");
//			int responseStatus = response.getStatus();
//			if(responseStatus==404){// not found
//				continue;
//			}else if(responseStatus!=200){
//				throw new WebApplicationException("Problem occured for deck service while retrieving slides for deck with deckId " + deckId, response);
//			}
//			// 
//			Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceResponse(response);
//			Set<String> resourceURIsOfDeckRetrievedPerSlide = new HashSet<>();
//			StringBuilder sb = new StringBuilder();
//			while(slidesIterator.hasNext()){
//				
//				JsonNode slideNode = slidesIterator.next();
//				String slideTitleAndText = SlideContentUtil.retrieveSlideTitleAndTextWithoutHTML(htmlToText, slideNode, "\n");
//				sb.append("\n" + slideTitleAndText);
//				Response responseSpotlight = nlpComponent.performDBpediaSpotlight(slideTitleAndText, spotlightConfidence);
//				if(responseSpotlight.getStatus()!=200){
//					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + responseSpotlight.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", responseSpotlight);
//				}
//				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(responseSpotlight);
//
//				ArrayNode resources = (ArrayNode) spotlightresult.get("Resources");
//				if(resources!=null){
//					for (int i = 0; i < resources.size(); i++) {
//						JsonNode resourceNode = resources.get(i);
//						String URI = resourceNode.get("@URI").textValue();
//						resourceURIsOfDeckRetrievedPerSlide.add(URI);
//					}
//				}
//			
//			}
//			
//			String textOfWholeDeck = sb.toString();
//			String detectedLanguageWholeDeck = nlpComponent.detectLanguage(textOfWholeDeck);
//			docFrequencyProviderPerLanguage.addDocument(resourceURIsOfDeckRetrievedPerSlide, detectedLanguageWholeDeck);
//			docFrequencyProviderOneForAllLanguages.addDocument(resourceURIsOfDeckRetrievedPerSlide, "ALL");
//		}
//		
//		deckServiceUtil.close();
//		
//		return new DocFrequencyProviderViaMap[]{docFrequencyProviderOneForAllLanguages, docFrequencyProviderPerLanguage};
//	}

	private static Map<String,String> initializeSupportedTypesMap(){
		Map<String,String> map = new HashMap<>();
		map.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens, NLPResultUtil.propertyNameWordFrequenciesExclStopwords); 
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
