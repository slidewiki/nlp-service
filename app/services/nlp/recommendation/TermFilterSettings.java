package services.nlp.recommendation;

public class TermFilterSettings {

	private int minCharLength;
	private int minFrequencyOfTermOrEntityToBeConsidered;
	private int maxNumberOfWords;
	
	public TermFilterSettings(int minCharLength, int minFrequencyOfTermOrEntityToBeConsidered,
			int maxNumberOfWords) {
		super();
		this.minCharLength = minCharLength;
		this.minFrequencyOfTermOrEntityToBeConsidered = minFrequencyOfTermOrEntityToBeConsidered;
		this.maxNumberOfWords = maxNumberOfWords;
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
