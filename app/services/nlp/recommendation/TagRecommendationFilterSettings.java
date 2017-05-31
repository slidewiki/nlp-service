package services.nlp.recommendation;

public class TagRecommendationFilterSettings {
	
	private int minCharLengthForTag;
	private int maxNumberOfWordsForNEsWhenNoLinkAvailable;
	private int maxEntriesToReturnTagRecommendation;
	
	public TagRecommendationFilterSettings(int minCharLengthForTag, int maxNumberOfWordsForNEsWhenNoLinkAvailable,
			int maxEntriesToReturnTagRecommendation) {
		super();
		this.minCharLengthForTag = minCharLengthForTag;
		this.maxNumberOfWordsForNEsWhenNoLinkAvailable = maxNumberOfWordsForNEsWhenNoLinkAvailable;
		this.maxEntriesToReturnTagRecommendation = maxEntriesToReturnTagRecommendation;
	}

	public int getMinCharLengthForTag() {
		return minCharLengthForTag;
	}

	public int getMaxNumberOfWordsForNEsWhenNoLinkAvailable() {
		return maxNumberOfWordsForNEsWhenNoLinkAvailable;
	}

	public int getMaxEntriesToReturnTagRecommendation() {
		return maxEntriesToReturnTagRecommendation;
	}

	
}
