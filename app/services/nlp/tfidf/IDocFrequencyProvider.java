package services.nlp.tfidf;

public interface IDocFrequencyProvider {
		
	public Integer getDocFrequency(String term, String language);
	
	public Integer getNumberOfAllDocs(String language);
	

}
