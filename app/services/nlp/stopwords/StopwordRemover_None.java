package services.nlp.stopwords;

import java.util.Collection;
import java.util.Map;

public class StopwordRemover_None implements IStopwordRemover {

	@Override
	public void removeStopwords(Collection<String> words, String language) {
		return;

	}

	@Override
	public <E> void removeStopwords(Map<String, E> map, String language) {
		return;
	}

}
