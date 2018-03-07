package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TagRecommendationFilter {

	public static List<NlpTag> filter(List<NlpTag> inputList, TermFilterSettings termFilterSettings, boolean applyMaxWordsOnlyWhenNoLinkAvailable, int maxEntriesToReturn){
		
		List<NlpTag> result = new ArrayList<>();
		int counterEntries = 0;
		for (NlpTag nlpTag : inputList) {
			String name = nlpTag.getName();
			
			if(name.length()<termFilterSettings.getMinCharLength()){
				continue;
			}
			if(StringUtils.isNumeric(name)){
				continue;
			}
			boolean applyMaxWords = true;
			if(applyMaxWordsOnlyWhenNoLinkAvailable){
				String link = nlpTag.getLink();
				if(link!=null){
					applyMaxWords = false;
				}
			}
			if(applyMaxWords){
				// check length of name (NER tends to be greedy and creates long strange names)
				if(name.split(" ").length>termFilterSettings.getMaxNumberOfWords()){
					continue;
				}
			}
			result.add(nlpTag);
			counterEntries++;
			if(maxEntriesToReturn>-1 && counterEntries>=maxEntriesToReturn){
				break;
			}
		}
		
		return result;
	}
}
