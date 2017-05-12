package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tfidf.TFIDF;
import services.util.MapCounting;
import services.util.Sorter;

public class TagRecommenderTFIDFCalculateViaDocFrequencyProvider implements ITagRecommender{

	private NLPStorageUtil nlpStorageUtil;
	private IDocFrequencyProviderTypeDependent docFrequencyProvider;
	private int minDocsToPerformLanguageDependent;
	private int maxEntriesToReturnForTFIDF;
	private boolean tagsToLowerCase;
	private int maxNumberOfWordsForNEsWhenNoLinkAvailable;
	private int minCharLengthForTag;
	
	
	
	
	public TagRecommenderTFIDFCalculateViaDocFrequencyProvider(NLPStorageUtil nlpStorageUtil,
			IDocFrequencyProviderTypeDependent docFrequencyProvider, int minDocsToPerformLanguageDependent,
			int maxEntriesToReturnForTFIDF, boolean tagsToLowerCase, int maxNumberOfWordsForNEsWhenNoLinkAvailable,
			int minCharLengthForTag) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.docFrequencyProvider = docFrequencyProvider;
		this.minDocsToPerformLanguageDependent = minDocsToPerformLanguageDependent;
		this.maxEntriesToReturnForTFIDF = maxEntriesToReturnForTFIDF;
		this.tagsToLowerCase = tagsToLowerCase;
		this.maxNumberOfWordsForNEsWhenNoLinkAvailable = maxNumberOfWordsForNEsWhenNoLinkAvailable;
		this.minCharLengthForTag = minCharLengthForTag;
	}

	@Override
	public List<NlpTag> getTagRecommendations(String deckId) {
		
		// get nlp result for deck id
		Response response = nlpStorageUtil.getNLPResultForDeckId(deckId);
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException("Problem while getting nlp result via nlp store service for deck id " + deckId + ". The nlp store service responded with status " + status + " (" + response.getStatusInfo() + ")", response);

		}
		ObjectNode nlpResult = (ObjectNode) NLPStorageUtil.getJsonFromMessageBody(response);
		
		
		// calc tfidf
		ObjectNode tfidf = TFIDF.getTFIDF(nlpResult, docFrequencyProvider, minDocsToPerformLanguageDependent, maxEntriesToReturnForTFIDF);
		Map<String, Map<String, Double>> mapProviderNameToTFIDFEnntries = NLPResultUtil.getTFIDFEntries(tfidf);
		
		
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
			if(provider.contains(NLPResultUtil.propertyNameTFIDFDBPediaSpotlightURIs)){
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
				if(StringUtils.isNumeric(keyToUse)){
					continue;
				}
				
				MapCounting.addToCountingMapAddingValue(mapSummedTFIDF, keyToUse , entry.getValue());
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
