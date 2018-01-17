package services.nlp.recommendation;

public class TagRecommendationFilterSettings {
	
	private int minCharLengthForTag;
	private int minFrequencyOfTermOrEntityToBeConsidered;
	private int maxNumberOfWordsForNEsWhenNoLinkAvailable;
	private int maxEntriesToReturnTagRecommendation;
	
	public TagRecommendationFilterSettings(int minFrequencyOfTermOrEntityToBeConsidered, int minCharLengthForTag, int maxNumberOfWordsForNEsWhenNoLinkAvailable,
			int maxEntriesToReturnTagRecommendation) {
		super();
		this.minFrequencyOfTermOrEntityToBeConsidered = minFrequencyOfTermOrEntityToBeConsidered;
		this.minCharLengthForTag = minCharLengthForTag;
		this.maxNumberOfWordsForNEsWhenNoLinkAvailable = maxNumberOfWordsForNEsWhenNoLinkAvailable;
		this.maxEntriesToReturnTagRecommendation = maxEntriesToReturnTagRecommendation;
	}

	public int getMinFrequencyOfTermOrEntityToBeConsidered() {
		return minFrequencyOfTermOrEntityToBeConsidered;
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
