package services.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.DeckServiceUtil;
import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.microserviceutil.NLPStoreIndexResultUtil;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NerAnnotation;
import services.nlp.recommendation.CosineSimilarity;
import services.nlp.recommendation.DeckRecommendation;
import services.nlp.recommendation.ITagRecommender;
import services.nlp.recommendation.NlpTag;
import services.nlp.recommendation.TermFilterSettings;
import services.nlp.slidecontentutil.SlideContentUtil;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.tfidf.ITFIDFResultProvider;
import services.nlp.tfidf.TFIDF;
import services.nlp.tfidf.TFIDFResult;
import services.nlp.tfidf.TFIDFResultProviderCalculateViaNLPStoreFrequencies;
import services.nlp.tfidf.TFIDFResultProviderRetrievedViaStoredPrecalculatedTfidfResult;
import services.nlp.tfidf.TitleBoostSettings;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.types.TypeCounter;
import services.util.NodeUtil;
import services.util.Sorter;

//TODO: clean up

/**
 * Convenience class for performing complex nlp processes of several sub components with 1 class.
 * @author aschlaf
 *
 */
public class NLPComponent {
	

	public int maxEntriesForTFIDFResult = 10; //TODO: make this configurable?

    private DeckServiceUtil deckServiceUtil;  
	private IHtmlToText htmlToPlainText;
	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private IStopwordRemover stopwordRemover;
    private INERLanguageDependent ner;
    private DBPediaSpotlightUtil dbPediaSpotlightUtil;  
    private NLPStorageUtil nlpStorageUtil;
    private ITagRecommender tagRecommender;
    private boolean typesToLowerCase = true;

    
	@Inject
	public NLPComponent(DeckServiceUtil deckserviceUtil, IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, NLPStorageUtil nlpStorageUtil, ITagRecommender tagRecommender) {
		super();
		
		this.deckServiceUtil = deckserviceUtil;
		this.htmlToPlainText = htmlToText;
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.stopwordRemover = stopwordRemover;
		this.ner = ner;
		this.dbPediaSpotlightUtil = dbPediaSpotlightUtil;
		this.nlpStorageUtil = nlpStorageUtil;
		this.tagRecommender = tagRecommender;

	}
	
	/**
	 * Extracts text from html.
	 * @param input hmtl string
	 * @return extracted text
	 */
	public String getPlainTextFromHTML(String input){
    	return this.htmlToPlainText.getText(input);
	}
	
	/**
	 * Extracts text from html and puts result to objectsNode with key name of value of propertyNameHtmlToPlainText
	 * @param input
	 * @param node
	 * @return
	 */
	public ObjectNode getPlainTextFromHTML(String input, ObjectNode node){
    	String plainText = this.htmlToPlainText.getText(input);
    	return node.put(NLPResultUtil.propertyNameHtmlToPlainText, plainText);
	}
	
	public String detectLanguage(String input){
    	return this.languageDetector.getLanguage(input);	
	}
	
	public ObjectNode detectLanguage(String input, ObjectNode node){
    	String language = this.languageDetector.getLanguage(input);	
    	return node.put(NLPResultUtil.propertyNameLanguage, language);
	}
	
	public String[] tokenize(String input, String language){
    	return this.tokenizer.tokenize(input, language);	
	}
	
	public ObjectNode tokenize(String input, String language, ObjectNode node){
    	String[] tokens = this.tokenizer.tokenize(input, language);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(NLPResultUtil.propertyNameTokens, tokenNode);
    	return node;
	}
	
	public List<NerAnnotation> performNER(String[] tokens, String language){
    	return this.ner.getNEs(tokens, language);	
	}
	
	public ObjectNode performNER(String[] tokens, String language, ObjectNode node){
    	List<NerAnnotation> ners = this.ner.getNEs(tokens, language);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(NLPResultUtil.propertyNameNER, nerNode);
    	return node;
	}
	
	
	public Response performDBpediaSpotlight(String input, double confidence, String types){
		
		return dbPediaSpotlightUtil.performDBPediaSpotlight(input, confidence, types);
			
	}

	
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int tfidfMinDocsToPerformLanguageDependent, int maxEntriesToReturn){
		return this.tagRecommender.getTagRecommendations(deckId, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent, maxEntriesToReturn);
	}
	

	
	

	/**
	 * 
	 * @param deckId
	 * @param minConfidenceDBPediaSpotlightPerSlide
	 * @param minConfidenceDBPediaSpotlightPerDeck
	 * @return
	 * @throws WebApplicationException if deckservice or dbpedia spotlight service doesn't return status code 200
	 */
	public ObjectNode processDeck(String deckId, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck) throws WebApplicationException{

		
		Logger.debug("process deckId: " + deckId);
		Response response = this.deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId);
		int status = response.getStatus();
		if(status == 200){
			JsonNode deckNode = MicroserviceUtil.getJsonFromMessageBody(response);
			ObjectNode result = processNLPForDeck(deckId, deckNode, minConfidenceDBPediaSpotlightPerSlide, minConfidenceDBPediaSpotlightPerDeck);
			return result;

		}else{
			throw new WebApplicationException("Problem while getting slides via deck service for deck id " + deckId + ". The deck service responded with status " + status + " (" + response.getStatusInfo() + ")", response);
		}
		

	}
	

	
	/**
	 * performs nlp for slides of deck incl. frequencies
	 * @param deckId
	 * @param slidesIterator
	 * @param minConfidenceDBPediaSpotlightPerSlide confidence to be used for DBPediaSpotlight performed on slide text. If >1, spotlight retrieval will be skipped for slides
	 * @param minConfidenceDBPediaSpotlightPerDeck confidence to be used for DBPediaSpotlight performed on deck text. If >1, spotlight retrieval will be skipped for deck text (concatenated slide texts)
	 * @return
	 */
	public ObjectNode processNLPForDeck(String deckId, JsonNode deckNode, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck){
		

		boolean performDBPediaSpotlightPerSlide = true;
		if(minConfidenceDBPediaSpotlightPerSlide>1){
			performDBPediaSpotlightPerSlide = false;
		}
		boolean performDBPediaSpotlightPerDeck = true;
		if(minConfidenceDBPediaSpotlightPerDeck>1){
			performDBPediaSpotlightPerDeck = false;
		}
		
		ObjectNode result = Json.newObject();
		result.put(NLPResultUtil.propertyNameDeckId, deckId);

		StringBuilder sbWholeDeckText = new StringBuilder();

		// get deck title and description
		String deckTitle = DeckServiceUtil.getDeckTitle(deckNode);
		result.put(NLPResultUtil.propertyNameDeckTitle, deckTitle);

		sbWholeDeckText.append(deckTitle);
		
		// TODO: also add deck description to deck text (=sbWholeDeckText)(deck description needs to be retrieved via deck service method GET /deck/{id} -> "description"

		ArrayNode slideArrayNode = Json.newArray();
		int numberOfSlides = 0; // used for title boost factor
		int numberOfSlidesWithText = 0; // used for title boost factor
		Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceJsonResult(deckNode);
		
		while (slidesIterator.hasNext()){
			numberOfSlides++;
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();
			resultsForSlide.put(NLPResultUtil.propertyNameSlideId, slide.get("id").textValue());

			String slideTitleAndText = SlideContentUtil.retrieveSlideTitleAndTextWithoutHTML(htmlToPlainText, slide, " \n ");
			resultsForSlide.put(NLPResultUtil.propertyNameSlideTitleAndText, slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
				slideArrayNode.add(resultsForSlide);
				continue;
			}

			numberOfSlidesWithText++;

			sbWholeDeckText.append(" \n " + slideTitleAndText);

			//+++++++++++++++++++++++++++++++++
			// processing for single slide (slide title and content)
			//+++++++++++++++++++++++++++++++++
			
			
			// language
			String languageOfSlide = detectLanguage(slideTitleAndText);
			resultsForSlide.put(NLPResultUtil.propertyNameLanguage, languageOfSlide);

			// tokens
			//remove punctuation before tokenization (but keep it in general, so the original text is kept. Give this original text to spotlight, so returned spans will match
			String slideTitleAndTextWithPunctuationRemoved = slideTitleAndText.replaceAll("\\W", " ");
			slideTitleAndTextWithPunctuationRemoved = slideTitleAndTextWithPunctuationRemoved.replaceAll("  ", " ");

			String[] tokenArrayOfSlide = tokenizer.tokenize(slideTitleAndTextWithPunctuationRemoved, languageOfSlide);
//			tokensOfWholeDeckRetrievedPerSlide.addAll(Arrays.asList(tokenArrayOfSlide)); 
			resultsForSlide.set(NLPResultUtil.propertyNameTokens, Json.toJson(tokenArrayOfSlide));
			
			// NER
			performNER(tokenArrayOfSlide, languageOfSlide, resultsForSlide);
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide, null);
				if(response.getStatus()!=200){
					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", response);
				}
				JsonNode spotlightresult = MicroserviceUtil.getJsonFromMessageBody(response);
				
				resultsForSlide.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresult);

			}

	
			slideArrayNode.add(resultsForSlide);
		}
		
		// add single slide results
		result.set(NLPResultUtil.propertyNameSlidesNode, slideArrayNode);

		result.put(NLPResultUtil.propertyNameNumberOfSlides, numberOfSlides);
		result.put(NLPResultUtil.propertyNameNumberOfSlidesWithText, numberOfSlidesWithText);
		
		//+++++++++++++++++++++++++++++++++
		// processing for whole deck (deck title (maybe also deck description in future) and all slide contents)
		//+++++++++++++++++++++++++++++++++

		
		String deckText = sbWholeDeckText.toString(); // 
		if(deckText.length()==0){
			result.put("info", "Deck contains no text. No NLP processing possible.");
			return result;
		}
		
		
		// language detection for whole deck
		String languageWholeDeck = detectLanguage(deckText);
		result.put(NLPResultUtil.propertyNameLanguage, languageWholeDeck);

		// tokens
		String[] tokenArrayOfDeck = tokenizer.tokenize(deckText, languageWholeDeck);
//		result.set(NLPResultUtil.propertyNameTokens, Json.toJson(tokenArrayOfDeck)); // TODO: un-comment this if all tokens of text should be included in nlp result.
		
		// NER
		List<NerAnnotation> nesOfWholeDeck = this.ner.getNEs(tokenArrayOfDeck, languageWholeDeck);	
    	JsonNode nerNode = Json.toJson(nesOfWholeDeck);
    	result.set(NLPResultUtil.propertyNameNER, nerNode);
		
		// DBPedia Spotlight
    	JsonNode spotlightresultWholeDeck = null;
		if(performDBPediaSpotlightPerDeck){		
			Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck, null);
			if(response.getStatus()!=200){
				throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + deckText + "\"", response);
			}
			spotlightresultWholeDeck = MicroserviceUtil.getJsonFromMessageBody(response);			
			result.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresultWholeDeck);
		}
		
		
		//###############################
		// further processing of tokens (frequencies, stop word & number removal)
		//###############################	
		
		// word frequencies
		Map<String,Integer> wordCountings = TypeCounter.getTypeCountings(tokenArrayOfDeck, typesToLowerCase);
		List<Entry<String,Integer>> wordCountingsSortedAsList = Sorter.sortByValueAndReturnAsList(wordCountings, true);
		int frequencyOfMostFrequentType = wordCountingsSortedAsList.get(0).getValue();
		result.put(NLPResultUtil.propertyNameFrequencyOfMostFrequentWord, frequencyOfMostFrequentType);

		// types stop words removed
		Map<String,Integer> wordCountingsStopWordsRemoved = new HashMap<>(wordCountings);
		stopwordRemover.removeStopwords(wordCountingsStopWordsRemoved, languageWholeDeck); // might also include removal of special chars when stopword remover wortschatz is used 
		
		// remove numbers
		Iterator<Map.Entry<String,Integer>> iter = wordCountingsStopWordsRemoved.entrySet().iterator();
		while (iter.hasNext()) {
		    Map.Entry<String,Integer> entry = iter.next();
		    if(StringUtils.isNumeric(entry.getKey())){
		        iter.remove();
		    }
		}
		
		// add to nlp result: words without stopwords sorted by frequency
		JsonNode wordCountingsStopWordsRemovedNode = null;
		if(wordCountingsStopWordsRemoved.size()>0){
			// output types without stopwords sorted by frequency
			List<Entry<String,Integer>> wordCountingsSortedStopWordsRemoved=  Sorter.sortByValueAndReturnAsList(wordCountingsStopWordsRemoved, true);
			wordCountingsStopWordsRemovedNode = NodeUtil.createArrayNodeFromStringIntegerEntryList(wordCountingsSortedStopWordsRemoved, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
			result.set(NLPResultUtil.propertyNameWordFrequenciesExclStopwords, wordCountingsStopWordsRemovedNode);
		}
				
			
		//###############################
		// further processing of NER (frequencies, tfidf)
		//###############################	
		
		// frequencies
		Map<String,Integer> neFrequencies = NerAnnotation.getNERFrequenciesByAnalyzingNEs(nesOfWholeDeck, true);
		List<Entry<String,Integer>> neFrequenciesSortedList=  Sorter.sortByValueAndReturnAsList(neFrequencies, true);
		JsonNode neFrequenciesNode = NodeUtil.createArrayNodeFromStringIntegerEntryList(neFrequenciesSortedList, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
		result.set(NLPResultUtil.propertyNameNERFrequencies, neFrequenciesNode);

				
		//###############################
		// further processing of Spotlight (frequencies, stop word removal, tfidf)
		//###############################	

		// dbpedia spotlight per deck (frequencies, tfidf)
		JsonNode spotlightURIFrequenciesNode= null;
		if(performDBPediaSpotlightPerDeck){ //only makes sense if spotlight was performed
			
			// frequencies
			Map<String,Integer> spotlightURIFrequencies = DBPediaSpotlightUtil.getSpotlightFrequenciesForURIsByAnalyzingSpotlightResults(spotlightresultWholeDeck);
			List<Entry<String,Integer>> spotlightURIFrequenciesSortedList=  Sorter.sortByValueAndReturnAsList(spotlightURIFrequencies, true);
			spotlightURIFrequenciesNode = NodeUtil.createArrayNodeFromStringIntegerEntryList(spotlightURIFrequenciesSortedList, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
			result.set(NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies, spotlightURIFrequenciesNode);

			
		}

		
		//============================================
		// special processing of deck title: include frequencies of deck title as extra field in frequencies (can be used for title boost for tfidf)
		//============================================
		// tokens
		String[] tokensDeckTitle = tokenizer.tokenize(deckTitle, languageWholeDeck);
		Map<String,Integer> wordCountingsDeckTitle = TypeCounter.getTypeCountings(tokensDeckTitle, typesToLowerCase);
		stopwordRemover.removeStopwords(wordCountingsDeckTitle, languageWholeDeck); // might also include removal of special chars when stopword remover wortschatz is used 
		Iterator<Map.Entry<String,Integer>> iterDeckTitle = wordCountingsDeckTitle.entrySet().iterator();
		while (iterDeckTitle.hasNext()) {
		    Map.Entry<String,Integer> entry = iterDeckTitle.next();
		    if(StringUtils.isNumeric(entry.getKey())){
		        iter.remove();
		    }
		}
		JsonNode tokenFreqNodeInclTitleFreqs = NLPResultUtil.putTitleFrequenciesToFrequencyNode(wordCountingsStopWordsRemovedNode, wordCountingsDeckTitle);
		result.set(NLPResultUtil.propertyNameWordFrequenciesExclStopwords, tokenFreqNodeInclTitleFreqs);

				
		// NER
		List<NerAnnotation> nesOfTitle = NerAnnotation.filterForNERsWithGivenTokenSpans(nesOfWholeDeck, 0, tokensDeckTitle.length-1);
		Map<String,Integer> neFrequenciesOfTitle = NerAnnotation.getNERFrequenciesByAnalyzingNEs(nesOfTitle, true);
		JsonNode neFreqNodeInclTitleFreqs = NLPResultUtil.putTitleFrequenciesToFrequencyNode(neFrequenciesNode, neFrequenciesOfTitle);
		result.set(NLPResultUtil.propertyNameNERFrequencies, neFreqNodeInclTitleFreqs);

		// DBPedia Spotlight
		if(performDBPediaSpotlightPerDeck){
			// get spotlight entities for title from spotlight entities of whole deck
			ArrayNode spotlightResourcesWholeDeck = DBPediaSpotlightUtil.getSpotlightResources(spotlightresultWholeDeck);
			ArrayNode spotlightResourcesTitle = DBPediaSpotlightUtil.filterResourcesForTextSpan(spotlightResourcesWholeDeck, 0, deckTitle.length()-1);
			Map<String,Integer> spotlightFrequenciesTitle = DBPediaSpotlightUtil.getSpotlightURIFrequenciesByAnalyzingSpotlightResources(spotlightResourcesTitle);
			JsonNode spotligthFreqNodeInclTitleFreqs = NLPResultUtil.putTitleFrequenciesToFrequencyNode(spotlightURIFrequenciesNode, spotlightFrequenciesTitle);
			result.set(NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies, spotligthFreqNodeInclTitleFreqs);

		}

		
		//============================================
		// return nlp result
		//============================================
		
		return result;
	}
	
	private TFIDFResult calculateTfidfResultViaNLPStoreFrequenciesAndReturnAsTFIDFResult(String deckId, int tfidfMinDocsToPerformLanguageDependent, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings){
		return TFIDF.getTFIDFViaNLPStoreFrequencies(nlpStorageUtil, deckId, tfidfMinDocsToPerformLanguageDependent, titleBoostSettings, termFilterSettings);
	}
	
	public ObjectNode calculateTfidfResultViaNLPStoreFrequenciesAndReturnAsJsonNode(String deckId, int tfidfMinDocsToPerformLanguageDependent, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings){
		
		ObjectNode result = Json.newObject();
		
		TFIDFResult tfidfResult = calculateTfidfResultViaNLPStoreFrequenciesAndReturnAsTFIDFResult(deckId, tfidfMinDocsToPerformLanguageDependent, titleBoostSettings, termFilterSettings);
		JsonNode tfidfResultAsJsonNode = Json.toJson(tfidfResult);
		
		result.set("tfidfResult", tfidfResultAsJsonNode);
		return result;
	}
	
	public ObjectNode calculateCosineSimilarity(String deckId1, String deckId2, boolean performLiveTFIDFCalculation, int maxValuesToConsider, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int minDocsToPerformLanguageDependent, boolean calculateAndIncludeSimilarityPerProvider, boolean includeDetailsAboutSharedEntriesForProviders){
		
		ITFIDFResultProvider tfidfResultProvider = getTFIDFProvider(performLiveTFIDFCalculation, titleBoostSettings, termFilterSettings, minDocsToPerformLanguageDependent);
	
		ObjectNode result = calculateCosineSimilarityViaTFIDFResultProvider(deckId1, deckId2, tfidfResultProvider, maxValuesToConsider, calculateAndIncludeSimilarityPerProvider, includeDetailsAboutSharedEntriesForProviders);
		return result;
	}
	
	private ObjectNode calculateCosineSimilarityViaTFIDFResultProvider(String deckId1, String deckId2, ITFIDFResultProvider tfidfResultProvider, int maxValuesToConsider, boolean calculateAndIncludeSimilarityPerProvider, boolean includeDetailsAboutSharedEntriesForProviders){
		
		// get tfidf result via provider
		TFIDFResult tfidfResult1 = tfidfResultProvider.provideTFIDFResult(deckId1);
		TFIDFResult tfidfResult2 = tfidfResultProvider.provideTFIDFResult(deckId1);

		ObjectNode result = calculateCosineSimilarityUsingGivenTFDIDFResults(tfidfResult1, tfidfResult2, maxValuesToConsider, calculateAndIncludeSimilarityPerProvider, includeDetailsAboutSharedEntriesForProviders);
		return result;
	}

	private ObjectNode calculateCosineSimilarityUsingGivenTFDIDFResults(TFIDFResult tfidfResult1, TFIDFResult tfidfResult2, int maxValuesToConsider, boolean calculateAndIncludeSimilarityPerProvider, boolean includeDetailsAboutSharedEntriesForProviders){
		
		ObjectNode result = Json.newObject();

		// reduce to top x
		tfidfResult1.reduceTfIdfMapToTopX(maxValuesToConsider);
		tfidfResult2.reduceTfIdfMapToTopX(maxValuesToConsider);

		// get providers
		Set<String> providers= tfidfResult1.getTfidfMap().keySet();

		Map<String,Double> tfidfMapForAllProvidersTogetherForDeck1 = new HashMap<>();
		Map<String,Double> tfidfMapForAllProvidersTogetherForDeck2 = new HashMap<>();
		
		ArrayNode detailsArrayNode = Json.newArray();
		for (String provider : providers) {
		
			Map<String,Double> tfidfMapDeck1 = tfidfResult1.getTfidfMap().get(provider);
			Map<String,Double> tfidfMapDeck2 = tfidfResult2.getTfidfMap().get(provider);

			tfidfMapForAllProvidersTogetherForDeck1.putAll(tfidfMapDeck1);
			tfidfMapForAllProvidersTogetherForDeck2.putAll(tfidfMapDeck2);

			if(calculateAndIncludeSimilarityPerProvider){
				
				ObjectNode detailsNode = Json.newObject();
				detailsNode.put("shortname", NLPResultUtil.getShortName(provider));
				detailsNode.put("longname", provider);

				if(includeDetailsAboutSharedEntriesForProviders){
					
					// add cosine similarity and details about shared entries to given node
					detailsNode = CosineSimilarity.getCosineSimilarityWithExtendedInfo(tfidfMapDeck1, tfidfMapDeck2, detailsNode);
				}else{
					// just calculate similarity and add it to details node
					double d = CosineSimilarity.getCosineSimilarity(tfidfMapDeck1, tfidfMapDeck2);
					detailsNode.put("cosinesimilarity", d);
				}
				
				detailsArrayNode.add(detailsNode);

			}

		}

		// calculate cosine similarity as well for tokens, NEs, Spotlight entities as one
		double d = CosineSimilarity.getCosineSimilarity(tfidfMapForAllProvidersTogetherForDeck1, tfidfMapForAllProvidersTogetherForDeck2);
		result.put("cosineSimilarity", d);
		result.set("details", detailsArrayNode);

		
		return result;

	}

	public ObjectNode getDeckRecommendation(String deckId, boolean performLiveTFIDFCalculationOfGivenDeck, boolean performLiveTFIDFCalculationOfDeckCandidates, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int tfidfMinDocsToPerformLanguageDependent, int maxTermsToConsider, int maxCandidatesToUseForSimilarityCalculation, int maxRecommendationsToReturn){
		
    	ITFIDFResultProvider tfidfResultProviderForGivenDeck = getTFIDFProvider(performLiveTFIDFCalculationOfGivenDeck, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent);
    	ITFIDFResultProvider tfidfResultProviderForDeckCandidates = getTFIDFProvider(performLiveTFIDFCalculationOfDeckCandidates, titleBoostSettings, termFilterSettings, tfidfMinDocsToPerformLanguageDependent);

    	return getDeckRecommendation(deckId, tfidfResultProviderForGivenDeck, tfidfResultProviderForDeckCandidates, maxTermsToConsider, maxCandidatesToUseForSimilarityCalculation, maxRecommendationsToReturn);
    	
	}
	
	private ITFIDFResultProvider getTFIDFProvider(boolean performLiveTFIDFCalculation, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int minDocsToPerformLanguageDependent){
		
		ITFIDFResultProvider tfidfResultProvider;
		if(performLiveTFIDFCalculation){
			tfidfResultProvider = new TFIDFResultProviderCalculateViaNLPStoreFrequencies(nlpStorageUtil, minDocsToPerformLanguageDependent, titleBoostSettings, termFilterSettings);
		}else{
			tfidfResultProvider = new TFIDFResultProviderRetrievedViaStoredPrecalculatedTfidfResult(nlpStorageUtil);
		}
		return tfidfResultProvider;
	}

	private ObjectNode getDeckRecommendation(String deckId, ITFIDFResultProvider tfidfResultProviderForGivenDeck, ITFIDFResultProvider tfidfResultProviderForDeckCandidates, int maxTermsToConsider, int maxCandidatesToUseForSimilarityCalculation, int maxRecommendationsToReturn){

		
		ObjectNode result = Json.newObject();
		
		// identify most important tokens and entities in given deck
		TFIDFResult tfidfResultGivenDeck = tfidfResultProviderForGivenDeck.provideTFIDFResult(deckId);

		// create index query from deck recommendation background info, to query similar decks based on most important terms and entities contained in deck
		ObjectNode deckRecommendationBackgroundInfoNode = DeckRecommendation.createDeckRecommendationBackgroundInfoNodeIncludingLuceneQueryFromTFIDFResult(tfidfResultGivenDeck, maxTermsToConsider);

		// query lucene/solr
		String luceneQuery = deckRecommendationBackgroundInfoNode.get("luceneQuery").textValue();
		String language = deckRecommendationBackgroundInfoNode.get("language").textValue();
		
		// set decks to exclude from Candidates
		List<String> decksToExcludeFromCandidates = new ArrayList<>();
		decksToExcludeFromCandidates.add(deckId);

		// retrieve forks from given deck and add them to decksToExcludeFromCandidates: use deck service `/deck/:deckid/forkGroup`
		Response responseForForks = deckServiceUtil.getForksForGivenDeck(deckId);
		if(responseForForks.getStatus()!=200){
			throw new WebApplicationException("Problem calling deckServiceUtil to retrieve forks for given deck. Returned status " + responseForForks.getStatus());
		}
		ArrayNode forksArrayNode = (ArrayNode) MicroserviceUtil.getJsonFromMessageBody(responseForForks);
		Iterator<JsonNode> iteratorForForks = forksArrayNode.iterator();
		while(iteratorForForks.hasNext()){
			JsonNode fork = iteratorForForks.next();
			String fordId = fork.asText();
			decksToExcludeFromCandidates.add(fordId);
		}
		
//		System.out.println(Timer.getDateAndTime()+ "\tquery candidates from nlp store");
		// query candidates from nlp store
		Response responseFromIndex = nlpStorageUtil.queryIndex(luceneQuery, language, decksToExcludeFromCandidates, maxCandidatesToUseForSimilarityCalculation);
		if(responseFromIndex.getStatus()!=200){
			throw new WebApplicationException("Problem calling nlpStore index to retrieve candidates. Returned status " + responseFromIndex.getStatus() + ". Query to index was:\n\"" + NLPStorageUtil.getJsonObjectToQueryNLPStoreIndex(luceneQuery, language, decksToExcludeFromCandidates, maxCandidatesToUseForSimilarityCalculation) + "\"", responseFromIndex);
		}
		JsonNode indexResultNode = MicroserviceUtil.getJsonFromMessageBody(responseFromIndex);
		ArrayNode itemsArrayNode = NLPStoreIndexResultUtil.getArrayNodeWithItems(indexResultNode);

		// TODO: recommendation possible improvement: handle empty result of candidates: repeat with more tfidf values?
		
//		System.out.println(Timer.getDateAndTime()+ "\tfor each candidate");
		// for each returned deck
		Iterator<JsonNode> iterator = itemsArrayNode.iterator();		
		Map<String,Double> mapDeckIdToSimilarityValue = new HashMap<>();
		while(iterator.hasNext()){
			
//			System.out.println("\t" + Timer.getDateAndTime()+ "\t\tnext candidate");
			JsonNode itemNode = iterator.next();
			String itemDeckId = NLPStoreIndexResultUtil.getDeckIdFromSingleItemEntry(itemNode);
			//double itemScore = NLPStoreIndexResultUtil.getValueFromSingleItemEntry(itemNode);
			
//			System.out.println("\t" + "\t" + Timer.getDateAndTime()+ "\t\t\tcalc tfidf");
			// get tfidf result for candidatee via tfidf result provider (e.g. by calculating tfidf values life or retrieve from precalcualted stored result, see implementations of interface ITFIDFResultProvider)
			TFIDFResult tfidfResultItem = tfidfResultProviderForDeckCandidates.provideTFIDFResult(itemDeckId);				
			
//			System.out.println("\t" + "\t" + Timer.getDateAndTime()+ "\t\t\tcalc cosine similarity to given deck");
			
			// TODO: recommendation: if needed/wished: include detailed info about shared entities and words for similar decks. Detailed infos can be retrieved by setting parameter "includeDetailsAboutSharedEntriesForProviders" to true. These then have to be included in the returned json object
			ObjectNode cosineSimilarityNode = calculateCosineSimilarityUsingGivenTFDIDFResults(tfidfResultGivenDeck, tfidfResultItem, maxTermsToConsider, false, false);
			
			double cosinesimilarity = cosineSimilarityNode.get("cosineSimilarity").asDouble();
			
			mapDeckIdToSimilarityValue.put(itemDeckId, cosinesimilarity);
			
			
		}
		

		// sort by similarity value reverse and include in result until maxRecommendationsToReturn
		List<Entry<String, Double>> sortedDeckIdsWithSimilarityValue = Sorter.sortByValueAndReturnAsList(mapDeckIdToSimilarityValue, true);
		ArrayNode resultArrayNode = Json.newArray();
		int counter = 0;
		for (Entry<String, Double> entry : sortedDeckIdsWithSimilarityValue) {
			if(counter >= maxRecommendationsToReturn){
				break;
			}
			counter++;
			ObjectNode entryNode = Json.newObject();
			entryNode.put("deckId", entry.getKey());
			entryNode.put("value", entry.getValue());
			
			resultArrayNode.add(entryNode);
			
			}
		
		
		result.put("name", "deckRecommendationsBasedOnDeckContentSimilarity");
		result.set("items", resultArrayNode);
		
		
//		System.out.println(Timer.getDateAndTime()+ "\treturn top result");

		return result;
	}
	
	public ILanguageDetector getLanguageDetector() {
		return languageDetector;
	}



	public void setLanguageDetector(ILanguageDetector languageDetector) {
		this.languageDetector = languageDetector;
	}



	public ITokenizerLanguageDependent getTokenizer() {
		return tokenizer;
	}



	public void setTokenizer(ITokenizerLanguageDependent tokenizer) {
		this.tokenizer = tokenizer;
	}



	public INERLanguageDependent getNer() {
		return ner;
	}



	public void setNer(INERLanguageDependent ner) {
		this.ner = ner;
	}

	

	public int getMaxEntriesForTFIDFResult() {
		return maxEntriesForTFIDFResult;
	}

	public void setMaxEntriesForTFIDFResult(int maxEntriesForTFIDFResult) {
		this.maxEntriesForTFIDFResult = maxEntriesForTFIDFResult;
	}

	public void close(){
		this.dbPediaSpotlightUtil.close();
	}


	
	

	
	
	
}
