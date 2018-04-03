package services.nlp.recommendation;

public class TermFilterSettings {

	private int minCharLength;
	private int minFrequencyOfTermOrEntityToBeConsidered;
	private int maxNumberOfWords;
	private boolean applyMinFrequencyOfTermOnlyAfterTitleBoost;
	
	public TermFilterSettings(int minCharLength, int minFrequencyOfTermOrEntityToBeConsidered,
			int maxNumberOfWords, boolean applyMinFrequencyOfTermOnlyAfterTitleBoost) {
		super();
		this.minCharLength = minCharLength;
		this.minFrequencyOfTermOrEntityToBeConsidered = minFrequencyOfTermOrEntityToBeConsidered;
		this.maxNumberOfWords = maxNumberOfWords;
		this.applyMinFrequencyOfTermOnlyAfterTitleBoost = applyMinFrequencyOfTermOnlyAfterTitleBoost;
	}

	public boolean isApplyMinFrequencyOfTermOnlyAfterTitleBoost() {
		return applyMinFrequencyOfTermOnlyAfterTitleBoost;
	}

	public int getMinCharLength() {
		return minCharLength;
	}

	public int getMinFrequencyOfTermOrEntityToBeConsidered() {
		return minFrequencyOfTermOrEntityToBeConsidered;
	}

	public int getMaxNumberOfWords() {
		return maxNumberOfWords;
	}

	
	
}
