package services.nlp.tfidf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import services.util.MapCounting;
import services.util.Sorter;

public class TFIDF {
	

	public static double calcTFIDF(Integer frequencyOfTermInDoc, double frequencyOfMostFrequentTermInDoc, Integer numberOfDocsContainingTerm, Integer numberOfAllDocuments){
		double tf = calcTermFrequency_Augmented(frequencyOfTermInDoc, frequencyOfMostFrequentTermInDoc);
		double idf = calcInverseDocumentFrequency(numberOfDocsContainingTerm, numberOfAllDocuments);
		return tf * idf;
	}
		
	public static List<Entry<String,Double>> getTFIDFValuesTopX(Map<String,Integer> typeCounts, int frequencyOfMostFrequentTokenInDoc, String language, IDocFrequencyProviderTypeDependent docFrequencyProvider, String entityTypeForDocFrequencyProvider, int maxTypesToReturn){
		
		if(typeCounts.size()== 0 || maxTypesToReturn==0 || !docFrequencyProvider.supportsType(entityTypeForDocFrequencyProvider)){
			return new ArrayList<Entry<String,Double>>();
		}
		
		Map<String,Double> tfidf = getTFIDFValues(typeCounts, frequencyOfMostFrequentTokenInDoc, language, docFrequencyProvider, entityTypeForDocFrequencyProvider);
		List<Entry<String, Double>> tfidfSorted = Sorter.sortByValueAndReturnAsList(tfidf, true);
		if(maxTypesToReturn<0 || maxTypesToReturn>=tfidfSorted.size()){
			return tfidfSorted;
		}
		return tfidfSorted.subList(0, maxTypesToReturn);		
		
	}
	
	/**
	 * Convenience method getting top x tfidf values for an array of tokens and dependent on language.
	 * @param tokens
	 * @param toLowerCase
	 * @param language
	 * @param docFrequencyProvider
	 * @param maxTypesToReturn
	 * @return
	 */
	public static List<Entry<String,Double>> getTFIDFValuesTopX(Collection<String> tokens, boolean toLowerCase, String language, IDocFrequencyProviderTypeDependent docFrequencyProvider, String entityTypeForDocFrequencyProvider, int maxTypesToReturn){
		
		if(tokens.size()== 0 || maxTypesToReturn==0 || !docFrequencyProvider.supportsType(entityTypeForDocFrequencyProvider)){
			return new ArrayList<Entry<String,Double>>();
		}
		Map<String,Double> tfidfValues = getTFIDFValues(tokens, toLowerCase, language, docFrequencyProvider, entityTypeForDocFrequencyProvider);
		List<Entry<String, Double>> tfidfSorted = Sorter.sortByValueAndReturnAsList(tfidfValues, true);
		
		if(maxTypesToReturn<0 || maxTypesToReturn>=tfidfSorted.size()){
			return tfidfSorted;
		}
		return tfidfSorted.subList(0, maxTypesToReturn);		
		
	}

	
	
	
	
	/**
	 * Convenience method getting tfidf for a collection of tokens and dependent on language.
	 * @param tokens
	 * @param toLowerCase
	 * @param language
	 * @param docFrequencyProvider
	 * @param sortByValueReverse if set to true, the map is sorted by their value in reverse order, if set to false, just tfidf values are returned;
	 * @return
	 */
	public static Map<String,Double> getTFIDFValues(Collection<String> tokens, boolean toLowerCase, String language, IDocFrequencyProviderTypeDependent docFrequencyProvider, String entityTypeForDocFrequencyProvider){
		
		
		if(tokens.size()== 0 || !docFrequencyProvider.supportsType(entityTypeForDocFrequencyProvider)){
			return new HashMap<String,Double>();
		}
		
		Map<String,Integer> rawTermFrequencies = getWordTypeFrequencies(tokens, toLowerCase);
		int frequencyOfMostFrequentTermInDoc = Sorter.sortByValueAndReturnAsList(rawTermFrequencies, true).get(0).getValue();
		
		Map<String,Double> tfidfResult = getTFIDFValues(rawTermFrequencies, frequencyOfMostFrequentTermInDoc, language, docFrequencyProvider, entityTypeForDocFrequencyProvider);
		
		return tfidfResult;
	}
	
	
	/**
	 * Convenience method getting tfidf for word types with frequency information and dependent on language and dependent on entityTypeForDocFrequencyProvider.
	 * @param wordCountings
	 * @param frequencyOfMostFrequentWordType
	 * @param toLowerCase
	 * @param language
	 * @param docFrequencyProvider
	 * @return
	 */
	public static Map<String,Double> getTFIDFValues(Map<String,Integer> wordCountings, int frequencyOfMostFrequentWordType, String language, IDocFrequencyProviderTypeDependent docFrequencyProvider, String entityTypeForDocFrequencyProvider){
		
		Map<String,Double> tfidfResult = new HashMap<>();
		if(wordCountings.size()== 0 || !docFrequencyProvider.supportsType(entityTypeForDocFrequencyProvider)){
			return tfidfResult;
		}
		int numberOfAllDocuments = docFrequencyProvider.getNumberOfAllDocs(language);

		Set<String> words = wordCountings.keySet();
		for (String word : words) {
			Integer freqWordTypeInDoc = wordCountings.get(word);
			Integer numberOfDocsContainingTerm = docFrequencyProvider.getDocFrequency(entityTypeForDocFrequencyProvider, word, language);
			double tfIdfCurrentTerm = calcTFIDF(freqWordTypeInDoc, frequencyOfMostFrequentWordType, numberOfDocsContainingTerm, numberOfAllDocuments);
			tfidfResult.put(word, new Double(tfIdfCurrentTerm));
		}		
		return tfidfResult;	
	}
		
	
	private static Map<String,Integer> getWordTypeFrequencies(Collection<String> tokens, boolean toLowerCase){
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
