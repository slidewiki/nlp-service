package services.nlp.tfidf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import services.nlp.microserviceutil.FrequencyResultUtil;
import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.recommendation.TermFilterSettings;
import services.util.MapCounting;
import services.util.NodeUtil;
import services.util.Sorter;

public class TFIDF {
	
	/**
	 * 
	 * @param nlpStorageUtil
	 * @param deckId
	 * @param minDocsToPerformLanguageDependent
	 * @param titleBoostSettings for no title boosting set to null, for details of title boosting see {@link TitleBoostSettings}
	 * @return
	 */
	public static Map<String,Map<String,Double>> getTFIDFViaNLPStoreFrequencies(NLPStorageUtil nlpStorageUtil, String deckId, int minDocsToPerformLanguageDependent, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings){
		

		// get frequencies data from nlp store
		Response responseFrequencies = nlpStorageUtil.getStatisticsDeckFrequencies(deckId);
		JsonNode nlpStoreFrequencyNode = MicroserviceUtil.getJsonFromMessageBody(responseFrequencies);
		
		int frequencyOfMostFrequentWord = FrequencyResultUtil.getfrequencyOfMostFrequentWord(nlpStoreFrequencyNode);
		int numberOfDocsForGivenLanguage = FrequencyResultUtil.getNumberOfDocsForLanguage(nlpStoreFrequencyNode);
		int numberOfDocsOverall = FrequencyResultUtil.getNumberOfDocsTotal(nlpStoreFrequencyNode);		
				
		//====================================
		boolean performLanguageDependent = true;
		// TODO: decide: also perform language independent if language = "UNKNOWN"?
		if(numberOfDocsForGivenLanguage < minDocsToPerformLanguageDependent){
			performLanguageDependent = false;
		}
				
		String propertyNameForFrequencyEntries;
		String tfidfproviderName;
		
		Map<String,Map<String,Double>> tfidfResult = new HashMap<>();
		
		// tokens
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameWordFrequenciesExclStopwords;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFToken;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}	
		Map<String,Double> tfidfTokens = calcTFIDFViaNLPStoreFrequencyNodeInclTitleBoost(nlpStoreFrequencyNode, propertyNameForFrequencyEntries, frequencyOfMostFrequentWord, numberOfDocsOverall, numberOfDocsForGivenLanguage, performLanguageDependent, titleBoostSettings, termFilterSettings);
		tfidfResult.put(tfidfproviderName, tfidfTokens);
		
		// NER
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameNERFrequencies;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFNER;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}	
		Map<String,Double> tfidfNER = calcTFIDFViaNLPStoreFrequencyNodeInclTitleBoost(nlpStoreFrequencyNode, propertyNameForFrequencyEntries, frequencyOfMostFrequentWord, numberOfDocsOverall, numberOfDocsForGivenLanguage, performLanguageDependent, titleBoostSettings, termFilterSettings);
		tfidfResult.put(tfidfproviderName, tfidfNER);

		// Spotlight
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFDBPediaSpotlightURIs;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}	
		Map<String,Double> tfidfSpotlight = calcTFIDFViaNLPStoreFrequencyNodeInclTitleBoost(nlpStoreFrequencyNode, propertyNameForFrequencyEntries, frequencyOfMostFrequentWord, numberOfDocsOverall, numberOfDocsForGivenLanguage, performLanguageDependent, titleBoostSettings, termFilterSettings);
		tfidfResult.put(tfidfproviderName, tfidfSpotlight);

		
		return tfidfResult;
	}
	
	public static Map<String,Double> calcTFIDFViaNLPStoreFrequencyNode(JsonNode nlpStoreFrequencyNode, String keynameFrequencies, int frequencyOfMostFrequentWord, int numberOfAllDocsNotLanguageDependent, int numberOfAllDocsLanguageDependent, boolean performLanguageDependent){
		
		List<TermFrequency> termFrequencyData = FrequencyResultUtil.getTermFrequenciesFromFrequencyResultNode(nlpStoreFrequencyNode, keynameFrequencies);
		Map<String,Double> tfidfResult = new HashMap<>();
		
		for (TermFrequency termFrequency : termFrequencyData) {
			String entry = termFrequency.getEntry();
			int frequencyTerm = termFrequency.getFrequency();
			int numberOfDocsContainingTerm = termFrequency.getFrequencyOtherDecksWithLanguageRestriction();
			int numberOfDocs = numberOfAllDocsLanguageDependent;
			if(!performLanguageDependent){
				numberOfDocsContainingTerm = termFrequency.getFrequencyOtherDecks();
				numberOfDocs = numberOfAllDocsNotLanguageDependent;
			}
			
			double tfidf = calcTFIDF(frequencyTerm, frequencyOfMostFrequentWord, numberOfDocsContainingTerm, numberOfDocs);
			tfidfResult.put(entry, new Double(tfidf));

		}
		
		return tfidfResult;
	}
	
	public static Map<String,Double> calcTFIDFViaNLPStoreFrequencyNodeInclTitleBoost(JsonNode nlpStoreFrequencyNode, String keynameFrequencies, int frequencyOfMostFrequentWord, int numberOfAllDocsNotLanguageDependent, int numberOfAllDocsLanguageDependent, boolean performLanguageDependent, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings){
		
		// get term frequency data from node
		List<TermFrequency> termFrequencyData = FrequencyResultUtil.getTermFrequenciesFromFrequencyResultNode(nlpStoreFrequencyNode, keynameFrequencies);
		int numberOfSlidesWithText = FrequencyResultUtil.getNumberOfSlidesWithText(nlpStoreFrequencyNode);
		
		Map<String,Double> tfidfResult = new HashMap<>();
		boolean performTitleBoost = false;
		if(titleBoostSettings !=null && titleBoostSettings.isPerformTitleBoost()){
			performTitleBoost = true;
		}
		
		// for each term
		for (TermFrequency termFrequency : termFrequencyData) {
			
			String entry = termFrequency.getEntry();
			
			// filter min char length
			if(entry.length() < termFilterSettings.getMinCharLength()){
				continue;
			}
			// filter max number of words
			if(termFilterSettings.getMaxNumberOfWords()>=0){
				if(entry.split(" ").length > termFilterSettings.getMaxNumberOfWords()){
					continue;
				}
			}
			int frequencyTerm = termFrequency.getFrequency();
			int frequencyToUseForTerm = frequencyTerm;
			
			// title boost
			if(performTitleBoost){
				int frequencyTermInTitle = termFrequency.getFrequencyInTitle();
				if(frequencyTermInTitle > 0){
					
					int factorToUse = 1;
					if(numberOfSlidesWithText>0){
						factorToUse = numberOfSlidesWithText;
					}
					if(titleBoostSettings.getTitleBoostWithFixedFactor()>0){
						factorToUse = titleBoostSettings.getTitleBoostWithFixedFactor();
					}
					
					frequencyToUseForTerm = (frequencyTerm-frequencyTermInTitle) + (frequencyTermInTitle * factorToUse);
					
					if(titleBoostSettings.isLimitTitleBoostToFrequencyOfMostFrequentWord() && frequencyToUseForTerm > frequencyOfMostFrequentWord){
						frequencyToUseForTerm = frequencyOfMostFrequentWord;
					}
				}
			}
			
			// filter min frequency
			if(frequencyToUseForTerm< termFilterSettings.getMinFrequencyOfTermOrEntityToBeConsidered()){
				continue;
			}
			
			// params based language dependent or not
			int numberOfDocsContainingTerm = termFrequency.getFrequencyOtherDecksWithLanguageRestriction();
			int numberOfDocs = numberOfAllDocsLanguageDependent;
			if(!performLanguageDependent){
				numberOfDocsContainingTerm = termFrequency.getFrequencyOtherDecks();
				numberOfDocs = numberOfAllDocsNotLanguageDependent;
			}
			
			// tfidf
			double tfidf = calcTFIDF(frequencyToUseForTerm, frequencyOfMostFrequentWord, numberOfDocsContainingTerm, numberOfDocs);
			
			// add entry with tfidf value to result object
			tfidfResult.put(entry, new Double(tfidf));

		}
		
		return tfidfResult;
	}
	
	
	public static ObjectNode getTFIDFViaNLPResultAndDocFreqProvider(ObjectNode nlpResult,  IDocFrequencyProviderTypeDependent docFrequencyProvider, int minDocsToPerformLanguageDependent, int maxEntriesToReturn){
		
		ArrayNode tfidfResultArrayNode = Json.newArray();

		// get needed general data from nlp result
		String languageInNLPResult = NLPResultUtil.getLanguage(nlpResult);
		int frequencyOfMostFrequentWord = NLPResultUtil.getFrequencyOfMostFrequentWordInDoc(nlpResult);

		int numberOfDocsForGivenLanguage = docFrequencyProvider.getNumberOfAllDocs(languageInNLPResult);
		String languageToUse = languageInNLPResult;
		boolean performLanguageDependent = true;
		if(numberOfDocsForGivenLanguage < minDocsToPerformLanguageDependent){
			languageToUse = DocFrequencyProviderViaMap.specialNameForLanguageIndependent;
			performLanguageDependent = false;
		}
		
		// fixed params
		String propertyNameInFrequencyEntriesForWord = NLPResultUtil.propertyNameInFrequencyEntriesForWord;
		String propertyNameInFrequencyEntriesForFrequency = NLPResultUtil.propertyNameInFrequencyEntriesForFrequency;
	
		// params dependent on type (token/NER/Spotlight entities)
		String docFrequencyProviderType;
		String propertyNameForFrequencyEntries;
		String tfidfproviderName;
		
		// tokens
		docFrequencyProviderType = NLPResultUtil.propertyNameDocFreqProvider_Tokens;
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameWordFrequenciesExclStopwords;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFToken;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}	
		JsonNode tfidfTokens = getTFIDFAsNodeCalculatedBasedOnFrequenciesStoredInNLPResult(nlpResult, docFrequencyProvider, docFrequencyProviderType, propertyNameForFrequencyEntries, tfidfproviderName, languageToUse, frequencyOfMostFrequentWord, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency, maxEntriesToReturn);
		tfidfResultArrayNode.add(tfidfTokens);
		
	
		// NER
		docFrequencyProviderType = NLPResultUtil.propertyNameDocFreqProvider_NamedEntities;
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameNERFrequencies;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFNER;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}		
		JsonNode tfidfNER = getTFIDFAsNodeCalculatedBasedOnFrequenciesStoredInNLPResult(nlpResult, docFrequencyProvider, docFrequencyProviderType, propertyNameForFrequencyEntries, tfidfproviderName, languageToUse, frequencyOfMostFrequentWord, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency, maxEntriesToReturn);
		tfidfResultArrayNode.add(tfidfNER);

		// spotlight
		docFrequencyProviderType = NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI;
		propertyNameForFrequencyEntries = NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies;
		tfidfproviderName = NLPResultUtil.propertyNameTFIDFDBPediaSpotlightURIs;
		if(performLanguageDependent){
			tfidfproviderName = tfidfproviderName + "_languagedependent";
		}else{
			tfidfproviderName = tfidfproviderName + "_notlanguagedependent";
		}		
		JsonNode tfidfSpotlight = getTFIDFAsNodeCalculatedBasedOnFrequenciesStoredInNLPResult(nlpResult, docFrequencyProvider, docFrequencyProviderType, propertyNameForFrequencyEntries, tfidfproviderName, languageToUse, frequencyOfMostFrequentWord, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency, maxEntriesToReturn);
		tfidfResultArrayNode.add(tfidfSpotlight);
		
		ObjectNode result = Json.newObject();
		result.set(NLPResultUtil.propertyNameTFIDF, tfidfResultArrayNode);

		return result;
	}
	
	

	public static JsonNode getTFIDFAsNodeCalculatedBasedOnFrequenciesStoredInNLPResult(ObjectNode nlpResult, IDocFrequencyProviderTypeDependent docFrequencyProvider, String docFrequencyProviderType, String propertyNameForFrequencyEntries, String targetProvidername, String language, Integer frequencyOfMostFrequentWord,  String propertyNameInFrequencyEntriesForWord, String propertyNameInFrequencyEntriesForFrequency,  int maxEntriesToReturn){
		
		List<Entry<String,Double>> tfidfresult = getTFIDFAsListCalculatedBasedOnFrequenciesStoredInNLPResult(nlpResult, docFrequencyProvider, docFrequencyProviderType, propertyNameForFrequencyEntries, language, frequencyOfMostFrequentWord, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency, maxEntriesToReturn);		
		JsonNode resultNode = tfidfEntryListToTFIDFSubResultNode(targetProvidername, tfidfresult);
		return resultNode;
	}
	
	public static List<Entry<String,Double>> getTFIDFAsListCalculatedBasedOnFrequenciesStoredInNLPResult(ObjectNode nlpResult, IDocFrequencyProviderTypeDependent docFrequencyProvider, String docFrequencyProviderType, String propertyNameForFrequencyEntries, String language, Integer frequencyOfMostFrequentWord, String propertyNameInFrequencyEntriesForWord, String propertyNameInFrequencyEntriesForFrequency, int maxEntriesToReturn){
		
		Map<String,Integer> frequencies = NLPResultUtil.getFrequenciesStoredInNLPResult(nlpResult, propertyNameForFrequencyEntries, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency);
		if(frequencies.size()==0 || !docFrequencyProvider.supportsType(docFrequencyProviderType)){
			return new ArrayList<Entry<String,Double>>();
		}
		List<Entry<String,Double>> tfidfresult = TFIDF.getTFIDFValuesTopX(frequencies, frequencyOfMostFrequentWord, language, docFrequencyProvider, docFrequencyProviderType, maxEntriesToReturn);
		return tfidfresult;
		
	}
	
	public static JsonNode tfidfEntryListToTFIDFSubResultNode(String providerName, List<Entry<String, Double>> listContainingTFIDFResults ){
		
		ObjectNode tfidfSubresultNode = Json.newObject();
		
		// create array node from list
		ArrayNode tfidfResults = NodeUtil.createArrayNodeFromStringDoubleEntryList(listContainingTFIDFResults, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
		
		// set provider name and tfidfresult
		tfidfSubresultNode.put(NLPResultUtil.propertyNameTFIDFProviderName, providerName);
		tfidfSubresultNode.set(NLPResultUtil.propertyNameTFIDFResultArrayName, tfidfResults);
		
		return tfidfSubresultNode;
	}
	// ===================
	// convenience methods for frequencies of entities and DocFrequencyProvider
	// ===================

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
	 * Convenience method getting tfidf for word types with frequency information and dependent on language and dependent on entityTypeForDocFrequencyProvider.
	 * @param wordCountings
	 * @param frequencyOfMostFrequentWordType
	 * @param toLowerCase
	 * @param language
	 * @param docFrequencyProvider
	 * @return
	 */
	public static Map<String,Double> getTFIDFValues(Map<String,Integer> wordCountings, int frequencyOfMostFrequentWordType, String language, IDocFrequencyProviderTypeDependent docFrequencyProvider, String entityTypeForDocFrequencyProvider){
		
		int numberOfAllDocuments = docFrequencyProvider.getNumberOfAllDocs(language);

		Map<String,Double> tfidfResult = new HashMap<>();
		if(wordCountings.size()== 0 || !docFrequencyProvider.supportsType(entityTypeForDocFrequencyProvider)){
			return tfidfResult;
		}

		Set<String> words = wordCountings.keySet();
		for (String word : words) {
			Integer freqWordTypeInDoc = wordCountings.get(word);
			Integer numberOfDocsContainingTerm = docFrequencyProvider.getDocFrequency(entityTypeForDocFrequencyProvider, word, language);
			double tfIdfCurrentTerm = calcTFIDF(freqWordTypeInDoc, frequencyOfMostFrequentWordType, numberOfDocsContainingTerm, numberOfAllDocuments);
//			Logger.info(numberOfAllDocuments + "\t" + word + "\t" + freqWordTypeInDoc + "\t" + numberOfDocsContainingTerm + "\t" + tfIdfCurrentTerm);
			tfidfResult.put(word, new Double(tfIdfCurrentTerm));
		}		
		return tfidfResult;	
	}
	
	// ===================
	// convenience methods for list of entities and DocFrequencyProvider
	// ===================

	
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
	 * Convenience method getting tfidf for a collection of tokens and dependent on language. If you already have frequencies, use getTFIDFValues(Map<String,Integer> wordCountings,..) instead
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

	
	
	// ===================
	// general tfidf methods
	// ===================
	
	public static double calcTFIDF(Integer frequencyOfTermInDoc, double frequencyOfMostFrequentTermInDoc, Integer numberOfDocsContainingTerm, Integer numberOfAllDocuments){
		double tf = calcTermFrequency_Augmented(frequencyOfTermInDoc, frequencyOfMostFrequentTermInDoc);
		double idf = calcInverseDocumentFrequency(numberOfDocsContainingTerm, numberOfAllDocuments);
		double tfidf = tf * idf;
		return tfidf;
	}

	
	private static double calcTermFrequency_Augmented(Integer frequencyOfTermInDoc, double frequencyOfMostFrequentTermInDoc){
		
		double result = 0.5 + 0.5 * frequencyOfTermInDoc / frequencyOfMostFrequentTermInDoc;
		return result;
	}
	
	private static double calcTermFrequency_logarithm(Integer frequencyOfTermInDoc){
		if(frequencyOfTermInDoc==0){
			return 0;
		}
		double result = 1 + Math.log(frequencyOfTermInDoc);
		return result;
	}
	
	private static double calcInverseDocumentFrequency(Integer numberOfDocsContainingTerm, double numberOfAllDocuments){
		return Math.log10((numberOfAllDocuments/new Double(numberOfDocsContainingTerm).doubleValue()));
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


}
