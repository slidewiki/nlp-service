package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.util.MapCounting;
import services.util.Sorter;

public class TagRecommenderTFIDFStoredInNLPResult implements ITagRecommender {

	private NLPStorageUtil nlpStorageUtil;

	private boolean tagsToLowerCase;
	private int minCharLengthForTag;
	private int maxNumberOfWordsForNEsWhenNoLinkAvailable;
	
	
	
	public TagRecommenderTFIDFStoredInNLPResult(NLPStorageUtil nlpStorageUtil, boolean tagsToLowerCase, int minCharLengthForTag,
			int maxNumberOfWordsForNEsWhenNoLinkAvailable) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.tagsToLowerCase = tagsToLowerCase;
		this.minCharLengthForTag = minCharLengthForTag;
		this.maxNumberOfWordsForNEsWhenNoLinkAvailable = maxNumberOfWordsForNEsWhenNoLinkAvailable;
	}

	

	@Override
	public List<NlpTag> getTagRecommendations(String deckId) {
		
		Response response = nlpStorageUtil.getNLPResultForDeckId(deckId);
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException("Problem while getting nlp result via nlp store service for deck id " + deckId + ". The nlp store service responded with status " + status + " (" + response.getStatusInfo() + ")", response);

		}
		JsonNode nlpResult = NLPStorageUtil.getJsonFromMessageBody(response);
		
		
		
		Map<String,Map<String,Double>> mapProviderNameToTFIDFEnntries = NLPResultUtil.getTFIDFEntries(nlpResult);
		
		if(mapProviderNameToTFIDFEnntries == null){
			throw new ProcessingException("No TFIDF results could be retrieved for deck id " + deckId + " from nlp result. Either tfidf wasn't performed or deck has no content. You can recheck via nlp store service.");

		}
		
		if(mapProviderNameToTFIDFEnntries.size()==0){
			return new ArrayList<>();
		}
		
		Map<String,String> mapSpotlightNamesToSpotlightURIs = new HashMap<>();
		Set<String> providers = mapProviderNameToTFIDFEnntries.keySet();
		
		// sum up tfidf values for same entries
		Map<String,Double> mapSummedTFIDF = new HashMap<>();
		for (String provider : providers) {
			
			boolean isSpotlightURIProvider = false;
			// specific treatment of spotlight
			if(provider.contains("Spotlight") && provider.contains("URI")){
				isSpotlightURIProvider = true;
			}
			
			Set<Entry<String,Double>> entries =  mapProviderNameToTFIDFEnntries.get(provider).entrySet();
			for (Entry<String, Double> entry : entries) {
				String key = entry.getKey();
				if(tagsToLowerCase){
					key = key.toLowerCase();
				}
				
				String keyToUse;
				if(isSpotlightURIProvider){
					String name = getSpotlightNameFromURI(key);
					mapSpotlightNamesToSpotlightURIs.put(name, key);
					keyToUse = name;
				}else{
					keyToUse = key;
				}
				
				if(keyToUse.length()<minCharLengthForTag){
					continue;
				}
				
				MapCounting.addToCountingMapAddingDoubleValue(mapSummedTFIDF, keyToUse , entry.getValue());
			}
		}
		
		// sort
		List<Entry<String, Double>> sortedList = Sorter.sortByValueAndReturnAsList(mapSummedTFIDF, true);
		 
		// create NLP tags
		List<NlpTag> list = new ArrayList<>();
		for (Entry<String, Double> entry : sortedList) {
			String name = entry.getKey();
			Double tfidfValue = entry.getValue();
			String link = null;
			if(mapSpotlightNamesToSpotlightURIs.containsKey(name)){
				link = mapSpotlightNamesToSpotlightURIs.get(name);
			}else{
				// no link available: check length of name (NER tends to be greedy and creates long strange names)
				// exclude these long names (but only if there is no dbpedia link
				if(name.split(" ").length>maxNumberOfWordsForNEsWhenNoLinkAvailable){
					continue;
				}
			}
			Set<String> sources = new HashSet<>();
			sources.add(NLPResultUtil.propertyNameTFIDF);
			NlpTag nlpTag = new NlpTag(name, link, tfidfValue);

			
			list.add(nlpTag);
		}
		return list;
	}

	private static String getSpotlightNameFromURI(String URI){
		String urlSequenceToRemove = "http://dbpedia.org/resource/";
		int lengthOfUrlSequenceToRemove = urlSequenceToRemove.length();
		String name = URI.substring(lengthOfUrlSequenceToRemove);
		if(name.contains("_")){
			name = name.replace("_", " ");
		}
		return name;
	}
	
}
