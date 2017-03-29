package services.nlp.stopwords;

import java.util.Collection;
import java.util.Map;

public interface IStopwordRemover {

	public void removeStopwords(Collection<String> words, String language);
	
	public <E> void removeStopwords(Map<String,E> map, String language);
}
