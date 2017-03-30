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
		if(words.size()==0){
			return;
		}
		if(this.mapLanguageToStopwords.containsKey(language)){
			Set<String> stopwordsForGivenLanguage = this.mapLanguageToStopwords.get(language);
			words.removeAll(stopwordsForGivenLanguage);
		}
		return;
	}


	@Override
	public <E> void removeStopwords(Map<String, E> map, String language) {
		if(map.size()==0){
			return;
		}
		if(this.mapLanguageToStopwords.containsKey(language)){
			Set<String> stopwordsForGivenLanguage = this.mapLanguageToStopwords.get(language);
			for (String stopword : stopwordsForGivenLanguage) {
				map.remove(stopword);
			}
		}
		
		return;
	}
	


}
