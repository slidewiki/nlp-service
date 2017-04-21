package services.nlp.recommendation;

import java.util.List;

import services.nlp.NlpTag;

public interface ITagRecommender {
	
	public List<NlpTag> getTagRecommendations(String deckId);

}
