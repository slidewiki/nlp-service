package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TagRecommendationFilter {

	public static List<NlpTag> filter(List<NlpTag> inputList, TagRecommendationFilterSettings tagRecommendationSettings){
		
		List<NlpTag> result = new ArrayList<>();
		int counterEntries = 0;
		for (NlpTag nlpTag : inputList) {
			String name = nlpTag.getName();
			
			if(name.length()<tagRecommendationSettings.getMinCharLengthForTag()){
				continue;
			}
			if(StringUtils.isNumeric(name)){
				continue;
			}
			String link = nlpTag.getLink();
			if(link==null){
				// no link available: check length of name (NER tends to be greedy and creates long strange names)
				// exclude these long names (but only if there is no dbpedia link
				if(name.split(" ").length>tagRecommendationSettings.getMaxNumberOfWordsForNEsWhenNoLinkAvailable()){
					continue;
				}
			}
			result.add(nlpTag);
			counterEntries++;
			if(tagRecommendationSettings.getMaxEntriesToReturnTagRecommendation()>-1 && counterEntries>=tagRecommendationSettings.getMaxEntriesToReturnTagRecommendation()){
				break;
			}
		}
		
		return result;
	}
}
