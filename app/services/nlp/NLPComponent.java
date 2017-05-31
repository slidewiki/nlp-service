package services.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NerAnnotation;
import services.nlp.recommendation.ITagRecommender;
import services.nlp.recommendation.NlpTag;
import services.nlp.recommendation.TagRecommendationFilterSettings;
import services.nlp.slidecontentutil.SlideContentUtil;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.tfidf.DocFrequencyCreatorForDecks;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tfidf.TFIDF;
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
    private IDocFrequencyProviderTypeDependent docFrequencyProvider; 
    private NLPStorageUtil nlpStorageUtil;
    private ITagRecommender tagRecommenderOlderVersion;
    private ITagRecommender tagRecommender;
    private boolean typesToLowerCase = true;

    
	@Inject
	public NLPComponent(DeckServiceUtil deckserviceUtil, IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, IDocFrequencyProviderTypeDependent docFrequencyProvider, NLPStorageUtil nlpStorageUtil, ITagRecommender tagRecommender, ITagRecommender tagRecommenderOlderVersion) {
		super();
		
		this.deckServiceUtil = deckserviceUtil;
		this.htmlToPlainText = htmlToText;
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.stopwordRemover = stopwordRemover;
		this.ner = ner;
		this.docFrequencyProvider = docFrequencyProvider;
		this.dbPediaSpotlightUtil = dbPediaSpotlightUtil;
		this.nlpStorageUtil = nlpStorageUtil;
		this.tagRecommender = tagRecommender;
		this.tagRecommenderOlderVersion = tagRecommenderOlderVersion;

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

	
	public List<NlpTag> getTagRecommendationsOlderVersion(String deckId, TitleBoostSettings titleBoostSettings, TagRecommendationFilterSettings tagRecommendationFilterSettings){
		return this.tagRecommenderOlderVersion.getTagRecommendations(deckId, titleBoostSettings, tagRecommendationFilterSettings);
	}
	
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TagRecommendationFilterSettings tagRecommendationFilterSettings){
		return this.tagRecommender.getTagRecommendations(deckId, titleBoostSettings, tagRecommendationFilterSettings);
	}
	
	

	@Deprecated
	public ObjectNode performNLP(String input, ObjectNode node, double dbpediaSpotlightConfidence){
		
		String plainText = htmlToPlainText.getText(input).trim();
		node.put(NLPResultUtil.propertyNameOriginalInput, input);
		node.put(NLPResultUtil.propertyNameHtmlToPlainText, plainText);
		
		String detectedLanguage = this.languageDetector.getLanguage(plainText);	
		node.put(NLPResultUtil.propertyNameLanguage, detectedLanguage);
		
    	String[] tokens = this.tokenizer.tokenize(plainText, detectedLanguage);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(NLPResultUtil.propertyNameTokens, tokenNode);

    	List<NerAnnotation> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(NLPResultUtil.propertyNameNER, nerNode);

    	
		// type frequencies
		Map<String,Integer> typeCountings = TypeCounter.getTypeCountings(tokens, typesToLowerCase);
		Map<String,Integer> typeCountingsSorted = Sorter.sortByValue(typeCountings, true);
		int frequencyOfMostFrequentType = typeCountingsSorted.entrySet().iterator().next().getValue(); // needed for tfidf later, stored in nlp result
		
		// types stop words removed
		Map<String,Integer> typeCountingsSortedStopWordsRemoved = new LinkedHashMap<>(typeCountingsSorted);
		stopwordRemover.removeStopwords(typeCountingsSortedStopWordsRemoved, detectedLanguage);

		ObjectNode TFIDFResultNode= Json.newObject();

    	// TFIDF all token types (regardless NER)
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
	    	List<Entry<String,Double>> tfidfEntries = TFIDF.getTFIDFValuesTopX(typeCountingsSortedStopWordsRemoved, frequencyOfMostFrequentType, detectedLanguage, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, maxEntriesForTFIDFResult);
	    	ArrayNode tfidfNode = NodeUtil.createArrayNodeFromStringDoubleEntryList(tfidfEntries, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
	    	TFIDFResultNode.set(NLPResultUtil.propertyNameTFIDF + "_tokens", tfidfNode);
		}
		// dbpediaspotlight
		Response response = performDBpediaSpotlight(plainText, dbpediaSpotlightConfidence, null);
		if(response.getStatus()!=200){
			throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + plainText + "\"", response);
		}
		JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);
		
		
		// tfidf
		List<String> spoltlightURIs = new ArrayList<>();
		JsonNode resourcesNode = spotlightresult.get("Resources");
		if(resourcesNode!=null && !resourcesNode.isNull()){
			ArrayNode resources = (ArrayNode) resourcesNode;
			for (int i = 0; i < resources.size(); i++) {
				JsonNode resourceNode = resources.get(i);
				String URI = resourceNode.get("@URI").textValue();
				spoltlightURIs.add(URI);
			}
		}
		
		
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spoltlightURIs, false, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI,  maxEntriesForTFIDFResult);
			TFIDFResultNode.set(NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntities", NodeUtil.createArrayNodeFromStringDoubleEntryList(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
		}
		node.set(NLPResultUtil.propertyNameTFIDF, TFIDFResultNode);

		
    	return node;
	}

	/**
	 * 
	 * @param deckId
	 * @param minConfidenceDBPediaSpotlightPerSlide
	 * @param minConfidenceDBPediaSpotlightPerDeck
	 * @return
	 * @throws WebApplicationException if deckservice or dbpedia spotlight service doesn't return status code 200
	 */
	public ObjectNode processDeck(String deckId, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck, boolean performTfidf) throws WebApplicationException{

		
		Logger.debug("process deckId: " + deckId);
		Response response = this.deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId);
		int status = response.getStatus();
		if(status == 200){
			JsonNode deckNode = DeckServiceUtil.getJsonFromMessageBody(response);
			ObjectNode result = processNLPForDeck(deckId, deckNode, minConfidenceDBPediaSpotlightPerSlide, minConfidenceDBPediaSpotlightPerDeck, performTfidf);
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
	public ObjectNode processNLPForDeck(String deckId, JsonNode deckNode, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck, boolean performTfidf){
		

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
//		String languageOfDecktitle = languageDetector.getLanguage(deckTitle);
//		String[] tokensOfDecktitle = tokenizer.tokenize(deckTitle, languageOfDecktitle);
		// TODO: deckTitle: add deckTitle (tokens & NER, spotlight results) to nlpResult and to frequencies results (how? extra)
		// maybe tokenize when whole deck language known
		
		// TODO: also add deck description to deck text (=sbWholeDeckText)(deck description needs to be retrieved via deck service method GET /deck/{id} -> "description"

		ArrayNode slideArrayNode = Json.newArray();
		int numberOfSlides = 0; // used for title boost factor
		int numberOfSlidesWithText = 0; // used for title boost factor
		Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceJsonResult(deckNode);
		List<String> spotlightResourceURIsOfDeckRetrievedPerSlide = new ArrayList<>();
		
		while (slidesIterator.hasNext()){
			numberOfSlides++;
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();
			resultsForSlide.put("slideId", slide.get("id").textValue());

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
			String[] tokenArrayOfSlide = tokenizer.tokenize(slideTitleAndText, languageOfSlide);
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
				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);
				
				resultsForSlide.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresult);

				// track all resources of deck to analyze them later for tfidf
				JsonNode resourcesNode = spotlightresult.get("Resources");
				if(resourcesNode!=null && !resourcesNode.isNull()){
					ArrayNode resources = (ArrayNode) resourcesNode;
					for (int i = 0; i < resources.size(); i++) {
						JsonNode resourceNode = resources.get(i);
						String URI = resourceNode.get("@URI").textValue();
						spotlightResourceURIsOfDeckRetrievedPerSlide.add(URI);
					}
				}

				

			}

	
			slideArrayNode.add(resultsForSlide);
		}
		
		// add single slide results
		result.set("children", slideArrayNode);

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
			spotlightresultWholeDeck = DBPediaSpotlightUtil.getJsonFromMessageBody(response);			
			result.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresultWholeDeck);
		}
		
		
		//###############################
		// further processing of tokens (frequencies, stop word removal, tfidf)
		//###############################	
		//TFIDF intial node (contains all tfidf sub results (tfidf for tokens, NER, Spotlight entities)
		ArrayNode tfidfResultArrayNode = Json.newArray();
		
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
		
		// tfidf words without stopwords
		if(performTfidf && wordCountingsStopWordsRemoved.size()>0){
			
			// tfidf for tokens compared to SlideWiki2
			// language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_languageDependent = TFIDF.getTFIDFValuesTopX(wordCountingsStopWordsRemoved, frequencyOfMostFrequentType, languageWholeDeck, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_languagedependent";
				tfidfResultArrayNode.add(TFIDF.tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_languageDependent));
				}
			// not language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(wordCountingsStopWordsRemoved, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens,  maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_notlanguagedependent";
				tfidfResultArrayNode.add(TFIDF.tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_notlanguagedependent));
			}

		}		
			
		//###############################
		// further processing of NER (frequencies, tfidf)
		//###############################	
		
		// frequencies
		Map<String,Integer> neFrequencies = NerAnnotation.getNERFrequenciesByAnalyzingNEs(nesOfWholeDeck, true);
		List<Entry<String,Integer>> neFrequenciesSortedList=  Sorter.sortByValueAndReturnAsList(neFrequencies, true);
		JsonNode neFrequenciesNode = NodeUtil.createArrayNodeFromStringIntegerEntryList(neFrequenciesSortedList, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
		result.set(NLPResultUtil.propertyNameNERFrequencies, neFrequenciesNode);

		
		// tfidf
		if(performTfidf){
			// language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_languagedependent = TFIDF.getTFIDFValuesTopX(neFrequencies, frequencyOfMostFrequentType, languageWholeDeck, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_NamedEntities  + "_languagedependent";
				tfidfResultArrayNode.add(TFIDF.tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_languagedependent));
	
			}
			// not language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(neFrequencies, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities,  maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_NamedEntities + "_notlanguagedependent";
				tfidfResultArrayNode.add(TFIDF.tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_notlanguagedependent));
	
			}
		}
		
		
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

			
			//tfidf for these spotlight entities (not language dependent)
			if(performTfidf){
				if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
					List<Entry<String,Double>> tfidfresultSpotlightSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightURIFrequencies, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, maxEntriesForTFIDFResult);
					String providerName = NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerDeck_" + NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI;
					tfidfResultArrayNode.add(TFIDF.tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultSpotlightSlideWiki_notlanguagedependent));
	
				}
			}
		}

		// TODO: add spotlight tfidf for surface form
		
//		//###############################
//		// further processing of Spotlight entities retrieved per slide (frequencies, tfidf)
//		//###############################
//		// tfidf for spotlight entity URIs retrieved per slide
//		if(performDBPediaSpotlightPerSlide){// tfidf of spotlight entities per slide can only be performed if spotlight entities were retrieved before
//
//			// not language dependent
//			if(spotlightResourceURIsOfDeckRetrievedPerSlide.size()>0 && docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
//				List<Entry<String,Double>> tfidfresultSpotlightSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightResourceURIsOfDeckRetrievedPerSlide, false, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, maxEntriesForTFIDFResult);
//				String providerName = NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerSlide_"+ NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI;
//				tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultSpotlightSlideWiki_notlanguagedependent));
//
//			}
//		}
		
		if(performTfidf){
			result.set(NLPResultUtil.propertyNameTFIDF, tfidfResultArrayNode);
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
	
	
	public boolean reloadDocFrequencyProviderFromNlpStore(){
		IDocFrequencyProviderTypeDependent docFrequencyProviderReloaded = DocFrequencyCreatorForDecks.createDocFrequencyProviderViaMapByRetrievingAllDataFromNLPStoreFirst(this.deckServiceUtil, this.nlpStorageUtil);
		this.docFrequencyProvider = docFrequencyProviderReloaded;
		return true;
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
