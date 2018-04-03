package services.nlp.tfidf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TermFrequency {

	private String entry;
	private int frequency;
	private int frequencyInTitle;
	private int frequencyOtherDecks;
	
	
	@JsonCreator
	public TermFrequency(@JsonProperty("entry") String entry, @JsonProperty("frequency") int frequency, @JsonProperty("frequencyInDeckTitle") int frequencyInDeckTitle ,@JsonProperty("frequencyOtherDecks") int frequencyOtherDecks) {
		super();
		this.entry = entry;
		this.frequency = frequency;
		this.frequencyInTitle = frequencyInDeckTitle;
		this.frequencyOtherDecks = frequencyOtherDecks;
	}



	public String getEntry() {
		return entry;
	}

	public int getFrequency() {
		return frequency;
	}
	public int getFrequencyInTitle() {
		return frequencyInTitle;
	}
	public int getFrequencyOtherDecks() {
		return frequencyOtherDecks;
	}

}
