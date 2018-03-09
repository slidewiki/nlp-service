package services.nlp.tfidf;

import java.util.Map;
import java.util.Set;

import services.util.Sorter;

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

	public void reduceTfIdfMapToTopX(int x){
		Set<String> providerNames = tfidfMap.keySet();
		for (String provider : providerNames) {
			
			Map<String,Double> map = tfidfMap.get(provider);
			Map<String,Double> reducedMap = Sorter.keepOnlyTopXValues(map, x);
			this.tfidfMap.put(provider, reducedMap);
		}
	}
	
}
