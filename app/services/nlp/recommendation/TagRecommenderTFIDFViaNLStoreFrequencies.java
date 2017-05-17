package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.tfidf.TFIDF;
import services.util.MapCounting;
import services.util.Sorter;

public class TagRecommenderTFIDFViaNLStoreFrequencies implements ITagRecommender{

	private NLPStorageUtil nlpStorageUtil;
	private int minDocsToPerformLanguageDependent;
	private boolean tagsToLowerCase;
	private int minCharLengthForTag;
	private int maxNumberOfWordsForNEsWhenNoLinkAvailable;
	private int maxEntriesToReturnTagRecommendation;


	
	
	public TagRecommenderTFIDFViaNLStoreFrequencies(NLPStorageUtil nlpStorageUtil,
			int minDocsToPerformLanguageDependent, boolean tagsToLowerCase, int minCharLengthForTag,
			int maxNumberOfWordsForNEsWhenNoLinkAvailable, int maxEntriesToReturnTagRecommendation) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.minDocsToPerformLanguageDependent = minDocsToPerformLanguageDependent;
		this.tagsToLowerCase = tagsToLowerCase;
		this.minCharLengthForTag = minCharLengthForTag;
		this.maxNumberOfWordsForNEsWhenNoLinkAvailable = maxNumberOfWordsForNEsWhenNoLinkAvailable;
		this.maxEntriesToReturnTagRecommendation = maxEntriesToReturnTagRecommendation;
	}




	@Override
	public List<NlpTag> getTagRecommendations(String deckId) {
		
		// calculate tfidf for tokens, NER and Spotlight
		Map<String,Map<String,Double>> tfidfMap = TFIDF.getTFIDFViaNLPStoreFrequencies(nlpStorageUtil, deckId, minDocsToPerformLanguageDependent);
	
		if(tfidfMap.size()==0){
			return new ArrayList<>();
		}
		
		Map<String,String> mapSpotlightNamesToSpotlightURIs = new HashMap<>();
		Set<String> providers = tfidfMap.keySet();
		
		// sum up tfidf values for same entries
		Map<String,Double> mapSummedTFIDF = new HashMap<>();
		for (String provider : providers) {
			
			boolean isSpotlightURIProvider = false;
			// specific treatment of spotlight
			if(provider.contains(NLPResultUtil.propertyNameTFIDFDBPediaSpotlightURIs)){
				isSpotlightURIProvider = true;
			}
			
			Set<Entry<String,Double>> entries =  tfidfMap.get(provider).entrySet();
			for (Entry<String, Double> entry : entries) {
				String key = entry.getKey();
				if(tagsToLowerCase){
					key = key.toLowerCase();
				}
				
				String keyToUse;
				if(isSpotlightURIProvider){
					String name = DBPediaSpotlightUtil.getSpotlightNameFromURI(key);
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
				
				
				MapCounting.addToCountingMapAddingDoubleValue(mapSummedTFIDF, keyToUse , entry.getValue());
			}
		}
		
		// sort
		List<Entry<String, Double>> sortedList = Sorter.sortByValueAndReturnAsList(mapSummedTFIDF, true);
		 
		// create NLP tags
		List<NlpTag> list = new ArrayList<>();
		int counterEntries = 0;
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
			counterEntries++;
			if(counterEntries>=maxEntriesToReturnTagRecommendation){
				break;
			}
		}
		return list;
	

	}

	
	
}
