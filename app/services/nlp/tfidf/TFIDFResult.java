package services.nlp.tfidf;

import java.util.Map;

public class TFIDFResult {

	private Map<String,Map<String,Double>> tfidfMap;
	private String language;
	private int numberOfDecksInPlatformWithGivenLanguage;
	private int numberOfDecksInPlatformOverall;
	private boolean tfidfValuesWereCalculatedLanguageDependent;
	
	public TFIDFResult(Map<String, Map<String, Double>> tfidfMap, String language,
			int numberOfDecksInPlatformWithGivenLanguage, int numberOfDecksInPlatformOverall,
			boolean tfidfValuesWereCalculatedLanguageDependent) {
		
		this.tfidfMap = tfidfMap;
		this.language = language;
		this.numberOfDecksInPlatformWithGivenLanguage = numberOfDecksInPlatformWithGivenLanguage;
		this.numberOfDecksInPlatformOverall = numberOfDecksInPlatformOverall;
		this.tfidfValuesWereCalculatedLanguageDependent = tfidfValuesWereCalculatedLanguageDependent;
	}


	public Map<String, Map<String, Double>> getTfidfMap() {
		return tfidfMap;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public int getNumberOfDecksInPlatformWithGivenLanguage() {
		return numberOfDecksInPlatformWithGivenLanguage;
	}
	
	public int getNumberOfDecksInPlatformOverall() {
		return numberOfDecksInPlatformOverall;
	}


	public boolean isTfidfValuesWereCalculatedLanguageDependent() {
		return tfidfValuesWereCalculatedLanguageDependent;
	}

	
	
}
