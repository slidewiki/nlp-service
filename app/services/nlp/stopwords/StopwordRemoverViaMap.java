package services.nlp.stopwords;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class StopwordRemoverViaMap implements IStopwordRemover{

	private Map<String,Set<String>> mapLanguageToStopwords;
	
	
	
	public StopwordRemoverViaMap(Map<String, Set<String>> mapLanguageToStopwords) {
		super();
		this.mapLanguageToStopwords = mapLanguageToStopwords;
	}


	@Override
	public void removeStopwords(Collection<String> words, String language) {
		Set<String> stopwordsForGivenLanguage = this.mapLanguageToStopwords.get(language);
		words.removeAll(stopwordsForGivenLanguage);
	}
	


}
