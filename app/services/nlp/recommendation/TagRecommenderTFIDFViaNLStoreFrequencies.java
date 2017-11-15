package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.tfidf.ITFIDFMerger;
import services.nlp.tfidf.TFIDF;
import services.nlp.tfidf.TitleBoostSettings;

public class TagRecommenderTFIDFViaNLStoreFrequencies implements ITagRecommender{

	private NLPStorageUtil nlpStorageUtil;
	private ITFIDFMerger tfidfMerger;
	private int tfidfMinDocsToPerformLanguageDependent;



	public TagRecommenderTFIDFViaNLStoreFrequencies(NLPStorageUtil nlpStorageUtil,
			int minDocsToPerformLanguageDependent, ITFIDFMerger tfidfMerger) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.tfidfMinDocsToPerformLanguageDependent = minDocsToPerformLanguageDependent;
		this.tfidfMerger = tfidfMerger;
	}



	@Override
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TagRecommendationFilterSettings tagRecommendationFilterSettings) {
		
		// calculate tfidf for tokens, NER and Spotlight
		Map<String,Map<String,Double>> tfidfMap = TFIDF.getTFIDFViaNLPStoreFrequencies(nlpStorageUtil, deckId, tfidfMinDocsToPerformLanguageDependent, tagRecommendationFilterSettings.getMinFrequencyOfTermOrEntityToBeConsidered(), titleBoostSettings);
	
		if(tfidfMap.size()==0){
			return new ArrayList<NlpTag>();
		}
		
		// merge tfidf values
		List<NlpTag> tfidfmerged = tfidfMerger.mergeTFIDFValuesOfDifferentProviders(tfidfMap);

		// filter list
		List<NlpTag> result = TagRecommendationFilter.filter(tfidfmerged, tagRecommendationFilterSettings);
		
		return result;
		
	}

	
	
}
