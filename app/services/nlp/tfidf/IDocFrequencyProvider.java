package services.nlp.tfidf;

public interface IDocFrequencyProvider {
	
	public Integer getDocFrequency(String term);
	
	public Integer getNumberOfAllDocs();

}
