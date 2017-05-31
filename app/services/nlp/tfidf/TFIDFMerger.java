package services.nlp.tfidf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.recommendation.NlpTag;
import services.util.MapCounting;
import services.util.Sorter;

/**
 * Merges tfidf values provided by diffrent providers (tokens/NEs/Spotlight entity URIs) and returns as list of {@link NlpTag}s sorted by their tfidf value (reverse)
 * Spotlight entity names for an URI are retrieved via {@link DBPediaSpotlightUtil}.getSpotlightNameFromURI(..)
 * If this name equals a name of an entity of another provider they get merged.
 * Merging of tfidf values is performed by adding values.
 * {@link NlpTag}.source is set to {@link NLPResultUtil}.propertyNameTFIDF
 * @author aschlaf
 *
 */
public class TFIDFMerger implements ITFIDFMerger {

	private boolean toLowerCase;

	
	
	public TFIDFMerger(boolean toLowerCase) {
		super();
		this.toLowerCase = toLowerCase;
	}



	@Override
	public List<NlpTag> mergeTFIDFValuesOfDifferentProviders(Map<String, Map<String, Double>> mapProviderToTFIDFValues) {
		
		Map<String,String> mapSpotlightNamesToSpotlightURIs = new HashMap<>();
		Set<String> providers = mapProviderToTFIDFValues.keySet();
		
		// sum up tfidf values for same entries
		Map<String,Double> mapSummedTFIDF = new HashMap<>();
		for (String provider : providers) {
			
			boolean isSpotlightURIProvider = false;
			// specific treatment of spotlight
			if(provider.contains(NLPResultUtil.propertyNameTFIDFDBPediaSpotlightURIs)){
				isSpotlightURIProvider = true;
			}
			
			Set<Entry<String,Double>> entries =  mapProviderToTFIDFValues.get(provider).entrySet();
			for (Entry<String, Double> entry : entries) {
				String key = entry.getKey();
				if(toLowerCase){
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
				
				
				MapCounting.addToCountingMapAddingDoubleValue(mapSummedTFIDF, keyToUse , entry.getValue());
			}
		}

		List<Entry<String, Double>> sortedList = Sorter.sortByValueAndReturnAsList(mapSummedTFIDF, true);

		// create NLP tags
		List<NlpTag> list = new ArrayList<>();
		for (Entry<String, Double> entry : sortedList) {
			String name = entry.getKey();
			Double tfidfValue = entry.getValue();
			String link = null;
			if(mapSpotlightNamesToSpotlightURIs.containsKey(name)){
				link = mapSpotlightNamesToSpotlightURIs.get(name);
			}
			Set<String> sources = new HashSet<>();
			sources.add(NLPResultUtil.propertyNameTFIDF);
			NlpTag nlpTag = new NlpTag(name, link, tfidfValue);

			
			list.add(nlpTag);
			
		}
		return list;

	}

}
