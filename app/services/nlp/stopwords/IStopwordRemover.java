package services.nlp.stopwords;

import java.util.Collection;

public interface IStopwordRemover {

	public void removeStopwords(Collection<String> words, String language);
}
