package services.nlp.tfidf;

public class TitleBoostSettings {

	private boolean performTitleBoost;
	private int titleBoostWithFixedFactor;
	private boolean limitTitleBoostToFrequencyOfMostFrequentWord;
	public TitleBoostSettings(boolean performTitleBoost, int titleBoostWithFixedFactor, boolean limitTitleBoostToFrequencyOfMostFrequentWord) {
		super();
		this.performTitleBoost = performTitleBoost;
		this.titleBoostWithFixedFactor = titleBoostWithFixedFactor;
		this.limitTitleBoostToFrequencyOfMostFrequentWord = limitTitleBoostToFrequencyOfMostFrequentWord;
	}
	public boolean isPerformTitleBoost() {
		return performTitleBoost;
	}
	
	public int getTitleBoostWithFixedFactor() {
		return titleBoostWithFixedFactor;
	}
	public boolean isLimitTitleBoostToFrequencyOfMostFrequentWord() {
		return limitTitleBoostToFrequencyOfMostFrequentWord;
	}
	
	
}
