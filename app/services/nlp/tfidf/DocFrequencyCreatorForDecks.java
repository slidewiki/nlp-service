package services.nlp.tfidf;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.Logger;
import services.nlp.NLPComponent;
import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.DeckServiceUtil;
import services.util.Timer;

public class DocFrequencyCreatorForDecks {
	
	public static DocFrequencyProviderViaMap[] createDocFrequencyProvidersFromDecksOfDeckservice(int maxDeckId, NLPComponent nlpComponent, double spotlightConfidence){
		
		DocFrequencyProviderViaMap docFrequencyProviderPerLanguage = new DocFrequencyProviderViaMap();
		DocFrequencyProviderViaMap docFrequencyProviderOneForAllLanguages = new DocFrequencyProviderViaMap();
		
		DeckServiceUtil deckServiceUtil = new DeckServiceUtil();
		for (int deckId = 0; deckId <= maxDeckId; deckId++) {
			
			Logger.info(Timer.getDateAndTime() + "\tprocessing: " + deckId);
			
			// get slides from deckservice
			Response response = deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId+"");
			int responseStatus = response.getStatus();
			if(responseStatus==404){// not found
				continue;
			}else if(responseStatus!=200){
				throw new WebApplicationException("Problem occured for deck service while retrieving slides for deck with deckId " + deckId, response);
			}
			// 
			Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceResponse(response);
			Set<String> resourceURIsOfDeckRetrievedPerSlide = new HashSet<>();
			StringBuilder sb = new StringBuilder();
			while(slidesIterator.hasNext()){
				
				JsonNode slideNode = slidesIterator.next();
				String slideTitleAndText = nlpComponent.retrieveSlideTitleAndTextWithoutHTML(slideNode, "\n");
				sb.append("\n" + slideTitleAndText);
				Response responseSpotlight = nlpComponent.performDBpediaSpotlight(slideTitleAndText, spotlightConfidence);
				if(responseSpotlight.getStatus()!=200){
					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + responseSpotlight.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", responseSpotlight);
				}
				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(responseSpotlight);

				ArrayNode resources = (ArrayNode) spotlightresult.get("Resources");
				if(resources!=null){
					for (int i = 0; i < resources.size(); i++) {
						JsonNode resourceNode = resources.get(i);
						String URI = resourceNode.get("@URI").textValue();
						resourceURIsOfDeckRetrievedPerSlide.add(URI);
					}
				}
			
			}
			
			String textOfWholeDeck = sb.toString();
			String detectedLanguageWholeDeck = nlpComponent.detectLanguage(textOfWholeDeck);
			docFrequencyProviderPerLanguage.addDocument(resourceURIsOfDeckRetrievedPerSlide, detectedLanguageWholeDeck);
			docFrequencyProviderOneForAllLanguages.addDocument(resourceURIsOfDeckRetrievedPerSlide, "ALL");
		}
		
		deckServiceUtil.close();
		
		return new DocFrequencyProviderViaMap[]{docFrequencyProviderOneForAllLanguages, docFrequencyProviderPerLanguage};
	}


}
