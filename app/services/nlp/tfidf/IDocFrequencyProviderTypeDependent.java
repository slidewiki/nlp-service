package services.nlp.tfidf;

/**
 * Interface for getting doc frequencies dependent on type and language
 * Types may be Tokens, DBPediaSpotlight entities, etc.
 * @author aschlaf
 *
 */
public interface IDocFrequencyProviderTypeDependent {


	public boolean supportsType(String type);

	public Integer getDocFrequency(String type, String term, String language);
	
	public Integer getNumberOfAllDocs(String type, String language);

}
