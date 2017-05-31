package services.nlp.recommendation;

import java.util.List;

import services.nlp.tfidf.TitleBoostSettings;

public interface ITagRecommender {
	
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TagRecommendationFilterSettings tagRecommendationFilterSettings);
	


}
