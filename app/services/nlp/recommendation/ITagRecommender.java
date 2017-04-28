package services.nlp.recommendation;

import java.util.List;

public interface ITagRecommender {
	
	public List<NlpTag> getTagRecommendations(String deckId);

}
