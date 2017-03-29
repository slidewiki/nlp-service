package services.nlp.tfidf;

import java.util.Map;

public class DocFrequencyProviderTypeDependentViaMap implements IDocFrequencyProviderTypeDependent {

	Map<String,IDocFrequencyProvider> mapTypeToDocFrequencyProvider;

	public DocFrequencyProviderTypeDependentViaMap(Map<String, IDocFrequencyProvider> mapTypeToDocFrequencyProvider) {
		super();
		this.mapTypeToDocFrequencyProvider = mapTypeToDocFrequencyProvider;
	}

	@Override
	public boolean supportsType(String type) {
		return this.mapTypeToDocFrequencyProvider.containsKey(type);
	}

	
	@Override
	public Integer getDocFrequency(String type, String term, String language) {
		if(this.mapTypeToDocFrequencyProvider.containsKey(type)){
			IDocFrequencyProvider docFreqProvider = this.mapTypeToDocFrequencyProvider.get(type);
			return docFreqProvider.getDocFrequency(term, language);
		}
		return 0;
	}

	@Override
	public Integer getNumberOfAllDocs(String type, String language) {
		if(this.mapTypeToDocFrequencyProvider.containsKey(type)){
			IDocFrequencyProvider docFreqProvider = this.mapTypeToDocFrequencyProvider.get(type);
			return docFreqProvider.getNumberOfAllDocs(language);
		}
		return 0;
	}

	
}
