package services.nlp.tfidf;

import java.util.Map;
import java.util.Set;

public class DocFrequencyProviderTypeDependentViaMap implements IDocFrequencyProviderTypeDependent {

	Map<String,DocFrequencyProviderViaMap> mapTypeToDocFrequencyProvider;

	public DocFrequencyProviderTypeDependentViaMap(Map<String, DocFrequencyProviderViaMap> mapTypeToDocFrequencyProvider) {
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
	public Integer getNumberOfAllDocs(String language) {
		
		IDocFrequencyProvider docFreqProvider = this.mapTypeToDocFrequencyProvider.values().iterator().next();
		return docFreqProvider.getNumberOfAllDocs(language);
		
	}

	public void add(String type, String language, Set<String> terms){
		if(!supportsType(type)){
			return;
		}
		DocFrequencyProviderViaMap docFreqProviderForGivenType = this.mapTypeToDocFrequencyProvider.get(type);
		docFreqProviderForGivenType.addDocument(terms, language);
		
	}
	
}
