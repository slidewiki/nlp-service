package services.nlp.tfidf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import services.util.MapCounting;
import services.util.Sorter;

public class TFIDF {
	

	public static double calcTFIDF(Integer frequencyOfTermInDoc, double frequencyOfMostFrequentTermInDoc, Integer numberOfDocsContainingTerm, Integer numberOfAllDocuments){
		double tf = calcTermFrequency_Augmented(frequencyOfTermInDoc, frequencyOfMostFrequentTermInDoc);
		double idf = calcInverseDocumentFrequency(numberOfDocsContainingTerm, numberOfAllDocuments);
		return tf * idf;
	}
	
	/**
	 * Convenience method getting tfidf for an array of tokens and dependent on language
	 * @param tokens
	 * @param toLowerCase
	 * @param language
	 * @param docFrequencyProvider
	 * @return
	 */
	public static Map<String,Double> getTFIDFValues(String[] tokens, boolean toLowerCase, String language, IDocFrequencyProvider docFrequencyProvider){
		
		Map<String,Double> tfidfResult = new HashMap<>();
		Map<String,Integer> rawTermFrequencies = getRawTermFrequencies(tokens, toLowerCase);

		int numberOfAllDocuments = docFrequencyProvider.getNumberOfAllDocs(language);
		int frequencyOfMostFrequentTermInDoc = Sorter.sortByValueAndReturnAsList(rawTermFrequencies, true).get(0).getValue();
		Set<String> terms = rawTermFrequencies.keySet();
		for (String term : terms) {
			
			Integer frequencyOfTermInDoc = rawTermFrequencies.get(term);
			Integer numberOfDocsContainingTerm = docFrequencyProvider.getDocFrequency(term, language);
			double tfIdfCurrentTerm = calcTFIDF(frequencyOfTermInDoc, frequencyOfMostFrequentTermInDoc, numberOfDocsContainingTerm, numberOfAllDocuments);
			tfidfResult.put(term, new Double(tfIdfCurrentTerm));
		}
		
		return tfidfResult;		
		
	}
	
	
	private static Map<String,Integer> getRawTermFrequencies(String[] tokens, boolean toLowerCase){
		Map<String,Integer> countingMap = new HashMap<>();
		for (String token : tokens) {
			String tokenToUse = token;
			if(toLowerCase){
				tokenToUse = token.toLowerCase();
			}
			MapCounting.addToCountingMap(countingMap, tokenToUse);
		}
		return countingMap;
	}
	
	private static double calcTermFrequency_Augmented(Integer frequencyOfTermInDoc, double frequencyOfMostFrequentTermInDoc){
		
		double result = 0.5 + 0.5 * frequencyOfTermInDoc / frequencyOfMostFrequentTermInDoc;
		return result;
	}
	
	private static double calcTermFrequency_logarithm(Integer frequencyOfTermInDoc){
		
		double result = 1 + Math.log(frequencyOfTermInDoc);
		return result;
	}
	
	private static double calcInverseDocumentFrequency(Integer numberOfDocsContainingTerm, double numberOfAllDocuments){
		return Math.log((numberOfAllDocuments/numberOfDocsContainingTerm));
	}
	

}
