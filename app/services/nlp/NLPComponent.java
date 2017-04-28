package services.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.types.TypeCounter;
import services.util.Sorter;

//TODO: clean up

/**
 * Convenience class for performing complex nlp processes of several sub components with 1 class.
 * @author aschlaf
 *
 */
public class NLPComponent {
	

	public int maxEntriesForTFIDFResult = 10; //TODO: make this configurable?

	private IHtmlToText htmlToPlainText;
	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
    private IStopwordRemover stopwordRemover;
    private IDocFrequencyProviderTypeDependent docFrequencyProvider; 
    private DeckServiceUtil deckServiceUtil;  
    private DBPediaSpotlightUtil dbPediaSpotlightUtil;  
    private NLPStorageUtil nlpStorageUtil;
    
    private boolean typesToLowerCase = true;

    
	@Inject
	public NLPComponent(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, IDocFrequencyProviderTypeDependent docFrequencyProvider, NLPStorageUtil nlpStorageUtil) {
		super();
		this.htmlToPlainText = htmlToText;
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.stopwordRemover = stopwordRemover;
		this.ner = ner;
		this.docFrequencyProvider = docFrequencyProvider;
		this.deckServiceUtil = new DeckServiceUtil();
		this.dbPediaSpotlightUtil = dbPediaSpotlightUtil;
		this.nlpStorageUtil = nlpStorageUtil;
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
	
	public List<NlpAnnotation> performNER(String[] tokens, String language){
    	return this.ner.getNEs(tokens, language);	
	}
	
	public ObjectNode performNER(String[] tokens, String language, ObjectNode node){
    	List<NlpAnnotation> ners = this.ner.getNEs(tokens, language);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(NLPResultUtil.propertyNameNER, nerNode);
    	return node;
	}
	
	
	public Response performDBpediaSpotlight(String input, double confidence){
		
		return dbPediaSpotlightUtil.performDBPediaSpotlight(input, confidence);
			
	}
	
//	public ObjectNode performDBpediaSpotlight(String input, double dbpediaSpotlightConfidence, ObjectNode node){
//		
//		JsonNode resultNode = performDBpediaSpotlight(input, dbpediaSpotlightConfidence);
//		node.set(propertyNameDBPediaSpotlight, resultNode);
//		return node;
//		
//	}
	

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

    	List<NlpAnnotation> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(NLPResultUtil.propertyNameNER, nerNode);

    	
		// type frequencies
		Map<String,Integer> typeCountings = TypeCounter.getTypeCountings(tokens, typesToLowerCase);
		Map<String,Integer> typeCountingsSorted = Sorter.sortByValue(typeCountings, true);
		int frequencyOfMostFrequentType = typeCountingsSorted.entrySet().iterator().next().getValue(); // needed for tfidf
		
		// types stop words removed
		Map<String,Integer> typeCountingsSortedStopWordsRemoved = new LinkedHashMap<>(typeCountingsSorted);
		stopwordRemover.removeStopwords(typeCountingsSortedStopWordsRemoved, detectedLanguage);

		ObjectNode TFIDFResultNode= Json.newObject();

    	// TFIDF all token types (regardless NER)
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
	    	List<Entry<String,Double>> tfidfEntries = TFIDF.getTFIDFValuesTopX(typeCountingsSortedStopWordsRemoved, frequencyOfMostFrequentType, detectedLanguage, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, maxEntriesForTFIDFResult);
	    	ArrayNode tfidfNode = createArrayNodeFromStringDoubleEntryList(tfidfEntries, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
	    	TFIDFResultNode.set(NLPResultUtil.propertyNameTFIDF + "_tokens", tfidfNode);
		}
		// dbpediaspotlight
		Response response = performDBpediaSpotlight(plainText, dbpediaSpotlightConfidence);
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
			TFIDFResultNode.set(NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntities", createArrayNodeFromStringDoubleEntryList(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
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
	public ObjectNode processDeck(String deckId, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck) throws WebApplicationException{

		
		Logger.debug("process deckId: " + deckId);
		Response response = this.deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId);
		int status = response.getStatus();
		if(status == 200){
			JsonNode deckNode = DeckServiceUtil.getJsonFromMessageBody(response);
			ObjectNode result = processNLPForDeck(deckId, deckNode, minConfidenceDBPediaSpotlightPerSlide, minConfidenceDBPediaSpotlightPerDeck);
			return result;

		}else{
			throw new WebApplicationException("Problem while getting slides via deck service for deck id " + deckId + ". The deck service responded with status " + status + " (" + response.getStatusInfo() + ")", response);
		}
		

	}
	
	/**
	/**
	 * without tfidf
	 * @param slidesIterator
	 * @param minConfidenceDBPediaSpotlightPerSlide
	 * @param minConfidenceDBPediaSpotlightPerDeck
	 * @return
	 * @throws WebApplicationException if dbpedia spotlight service doesn't return status code 200
	 */
	@Deprecated
	public ObjectNode processNLPForDeckWithoutTFIDF(String deckId, Iterator<JsonNode> slidesIterator, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck) throws WebApplicationException{
		
		boolean performDBPediaSpotlightPerSlide = true;
		if(minConfidenceDBPediaSpotlightPerSlide>1){
			performDBPediaSpotlightPerSlide = false;
		}
		boolean performDBPediaSpotlightPerDeck = true;
		if(minConfidenceDBPediaSpotlightPerDeck>1){
			performDBPediaSpotlightPerDeck = false;
		}
		
		ObjectNode result = Json.newObject();
		result.put("deckId", deckId);
		ArrayNode slideArrayNode = Json.newArray();

		List<String> tokensOfWholeDeck = new ArrayList<>(); // used for tfidf of tokens
		StringBuilder sbWholeDeckText = new StringBuilder();

		while (slidesIterator.hasNext()){
			
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();
			resultsForSlide.put("slideId", slide.get("id").textValue());

			String slideTitleAndText = retrieveSlideTitleAndTextWithoutHTML(slide, "\n");
			resultsForSlide.put(NLPResultUtil.propertyNameSlideTitleAndText, slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
				slideArrayNode.add(resultsForSlide);
				continue;
			}

			sbWholeDeckText.append("\n" + slideTitleAndText);

			// language
			String languageOfSlide = detectLanguage(slideTitleAndText);
			resultsForSlide.put(NLPResultUtil.propertyNameLanguage, languageOfSlide);

			// tokens
			String[] tokenArrayOfSlide = tokenizer.tokenize(slideTitleAndText, languageOfSlide);
			tokensOfWholeDeck.addAll(Arrays.asList(tokenArrayOfSlide)); 
			resultsForSlide.set(NLPResultUtil.propertyNameTokens, Json.toJson(tokenArrayOfSlide));
			
			// NER
			performNER(tokenArrayOfSlide, languageOfSlide, resultsForSlide);
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide);
				if(response.getStatus()!=200){
					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", response);
				}
				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);

				resultsForSlide.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresult);
			}

			slideArrayNode.add(resultsForSlide);
		}
		
		// add single slide results
		result.set("children", slideArrayNode);

				
		String deckText = sbWholeDeckText.toString();
		if(deckText.length()==0){
			result.put("info", "Deck contains no text. No NLP processing prossible.");
			return result;
		}
				
		// language detection for whole deck
		String languageWholeDeck = detectLanguage(deckText);
		result.put("languageDetectedWholeDeck", languageWholeDeck);

		// further processing of tokens (frequencies, stop word removal)
			
		// type frequencies
		Map<String,Integer> typeCountings = TypeCounter.getTypeCountings(tokensOfWholeDeck, typesToLowerCase);
		List<Entry<String,Integer>> typeCountingsSortedAsList = Sorter.sortByValueAndReturnAsList(typeCountings, true);
		int frequencyOfMostFrequentType = typeCountingsSortedAsList.get(0).getValue();
		result.put(NLPResultUtil.propertyNameFrequencyOfMostFrequentWord, frequencyOfMostFrequentType);

		// types stop words removed
		Map<String,Integer> typeCountingsStopWordsRemoved = new HashMap<>(typeCountings);
		stopwordRemover.removeStopwords(typeCountingsStopWordsRemoved, languageWholeDeck);
		
		if(typeCountingsStopWordsRemoved.size()>0){
			// output types without stopwords sorted by frequency
			List<Entry<String,Integer>> typeCountingsSortedStopWordsRemoved=  Sorter.sortByValueAndReturnAsList(typeCountingsStopWordsRemoved, true);
			JsonNode wordTypeCountingsStopWordsRemoved = createArrayNodeFromStringIntegerEntryList(typeCountingsSortedStopWordsRemoved, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
			result.set(NLPResultUtil.propertyNameWordFrequenciesExclStopwords, wordTypeCountingsStopWordsRemoved);

		}

		
		// dbpedia spotlight per deck 
		if(performDBPediaSpotlightPerDeck && deckText.length()>0){
			Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck);
			if(response.getStatus()!=200){
				throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + deckText + "\"", response);
			}
			JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);			
			result.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresult);
			
		}

		
		return result;

		
	}
	
	/**
	 * performs nlp for slides of deck incl. tfidf
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
		result.put("deckId", deckId);

		StringBuilder sbWholeDeckText = new StringBuilder();

		// get deck title and description
		String deckTitle = DeckServiceUtil.getDeckTitle(deckNode);
		sbWholeDeckText.append(deckTitle);
		String languageOfDecktitle = languageDetector.getLanguage(deckTitle);
		String[] tokensOfDecktitle = tokenizer.tokenize(deckTitle, languageOfDecktitle);
		
//		List<String> tokensOfWholeDeckRetrievedPerSlide = new ArrayList<>(); // used for word frequencies and tfidf of tokens if thez are retrieved per slide
//		tokensOfWholeDeckRetrievedPerSlide.addAll(Arrays.asList(tokensOfDecktitle)); 

		// TODO: also add deck description to deck text (=sbWholeDeckText)(deck description needs to be retrieved via deck service method GET /deck/{id} -> "description"

		ArrayNode slideArrayNode = Json.newArray();
		Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceJsonResult(deckNode);
		List<String> spotlightResourceURIsOfDeckRetrievedPerSlide = new ArrayList<>();

		while (slidesIterator.hasNext()){
			
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();
			resultsForSlide.put("slideId", slide.get("id").textValue());

			String slideTitleAndText = retrieveSlideTitleAndTextWithoutHTML(slide, "\n");
			resultsForSlide.put(NLPResultUtil.propertyNameSlideTitleAndText, slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
				slideArrayNode.add(resultsForSlide);
				continue;
			}

			sbWholeDeckText.append("\n" + slideTitleAndText);

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
	
				Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide);
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
		performNER(tokenArrayOfDeck, languageWholeDeck, result);
		
		if(performDBPediaSpotlightPerDeck){
			Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck);
			if(response.getStatus()!=200){
				throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + deckText + "\"", response);
			}
			JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);			
			result.set(NLPResultUtil.propertyNameDBPediaSpotlight, spotlightresult);
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
		stopwordRemover.removeStopwords(wordCountingsStopWordsRemoved, languageWholeDeck);
		
		// add to nlp result: words without stopwords sorted by frequency
		if(wordCountingsStopWordsRemoved.size()>0){
			// output types without stopwords sorted by frequency
			List<Entry<String,Integer>> wordCountingsSortedStopWordsRemoved=  Sorter.sortByValueAndReturnAsList(wordCountingsStopWordsRemoved, true);
			JsonNode wordCountingsStopWordsRemovedNode = createArrayNodeFromStringIntegerEntryList(wordCountingsSortedStopWordsRemoved, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
			result.set(NLPResultUtil.propertyNameWordFrequenciesExclStopwords, wordCountingsStopWordsRemovedNode);
		}
		
		// tfidf words without stopwords
		if(wordCountingsStopWordsRemoved.size()>0){
			
			// tfidf for tokens compared to SlideWiki2
			// language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_languageDependent = TFIDF.getTFIDFValuesTopX(wordCountingsStopWordsRemoved, frequencyOfMostFrequentType, languageWholeDeck, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_languagedependent";
				tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_languageDependent));
				}
			// not language dependent
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(wordCountingsStopWordsRemoved, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens,  maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_notlanguagedependent";
				tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_notlanguagedependent));
			}

		}		
			
		//###############################
		// further processing of NER (frequencies, tfidf)
		//###############################	
		
		// frequencies
		Map<String,Integer> neFrequencies = NLPResultUtil.getNERFrequenciesByAnalyzingNEs(result);
		List<Entry<String,Integer>> neFrequenciesSortedList=  Sorter.sortByValueAndReturnAsList(neFrequencies, true);
		JsonNode neFrequenciesNode = createArrayNodeFromStringIntegerEntryList(neFrequenciesSortedList, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
		result.set(NLPResultUtil.propertyNameNERFrequencies, neFrequenciesNode);

		
		// tfidf
		// language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities)){
			List<Entry<String,Double>> tfidfresultTokensSlideWiki_languagedependent = TFIDF.getTFIDFValuesTopX(neFrequencies, frequencyOfMostFrequentType, languageWholeDeck, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, maxEntriesForTFIDFResult);
			String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_NamedEntities  + "_languagedependent";
			tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_languagedependent));

		}
		// not language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities)){
			List<Entry<String,Double>> tfidfresultTokensSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(neFrequencies, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities,  maxEntriesForTFIDFResult);
			String providerName = NLPResultUtil.propertyNameTFIDF + "_" + NLPResultUtil.propertyNameDocFreqProvider_NamedEntities + "_notlanguagedependent";
			tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultTokensSlideWiki_notlanguagedependent));

		}

		
		
		
		//###############################
		// further processing of Spotlight (frequencies, stop word removal, tfidf)
		//###############################	

		// dbpedia spotlight per deck (frequencies, tfidf)
		if(performDBPediaSpotlightPerDeck){ //only makes sense if spotlight was performed
			
			// frequencies
			Map<String,Integer> spotlightURIFrequencies = NLPResultUtil.getSpotlightFrequenciesForURIsByAnalyzingSpotlightResults(result);
			List<Entry<String,Integer>> spotlightURIFrequenciesSortedList=  Sorter.sortByValueAndReturnAsList(spotlightURIFrequencies, true);
			JsonNode spotlightURIFrequenciesNode = createArrayNodeFromStringIntegerEntryList(spotlightURIFrequenciesSortedList, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
			result.set(NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies, spotlightURIFrequenciesNode);

			
			//tfidf for these spotlight entities (not language dependent)
			if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
				List<Entry<String,Double>> tfidfresultSpotlightSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightURIFrequencies, frequencyOfMostFrequentType, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerDeck_" + NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI;
				tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultSpotlightSlideWiki_notlanguagedependent));

			}
		}

		// TODO: add spotlight tfidf for surface form
		
		//###############################
		// further processing of Spotlight entities retrieved per slide (frequencies, tfidf)
		//###############################
		// tfidf for spotlight entity URIs retrieved per slide
		if(performDBPediaSpotlightPerSlide){// tfidf of spotlight entities per slide can only be performed if spotlight entities were retrieved before

			// not language dependent
			if(spotlightResourceURIsOfDeckRetrievedPerSlide.size()>0 && docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
				List<Entry<String,Double>> tfidfresultSpotlightSlideWiki_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightResourceURIsOfDeckRetrievedPerSlide, false, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, maxEntriesForTFIDFResult);
				String providerName = NLPResultUtil.propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerSlide_"+ NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI;
				tfidfResultArrayNode.add(tfidfEntryListToTFIDFSubResultNode(providerName, tfidfresultSpotlightSlideWiki_notlanguagedependent));

			}
		}
		

		result.set(NLPResultUtil.propertyNameTFIDF, tfidfResultArrayNode);
		
		return result;
	}
	
	

	public ObjectNode getTFIDF(String deckId){
		Response response = this.nlpStorageUtil.getNLPResultForDeckId(deckId);
		ObjectNode nlpResult = (ObjectNode) NLPStorageUtil.getJsonFromMessageBody(response);
		ObjectNode result = Json.newObject();
		
		result = getTFIDFForWordTypesUsingNlpResultAsInput(nlpResult, result);
		result = getTFIDFForSpotlightUsingNlpResultAsInput(nlpResult, result);
		result = getTFIDFForNamedEntitiesUsingNlpResultAsInput(nlpResult, result);
		
		return result;
	}
	
	public ArrayNode getTFIDFAsNodeCalculatedBasedOnFrequenciesStoredInNLPResult(ObjectNode nlpResult, String language, Integer frequencyOfMostFrequentWord, String propertyNameForFrequencyEntries, String propertyNameInFrequencyEntriesForWord, String propertyNameInFrequencyEntriesForFrequency, String docFrequencyProviderType, int maxEntriesToReturn){
		
		List<Entry<String,Double>> tfidfresult = getTFIDFAsListCalculatedBasedOnFrequenciesStoredInNLPResult(nlpResult, language, frequencyOfMostFrequentWord, propertyNameForFrequencyEntries, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency, docFrequencyProviderType, maxEntriesToReturn);
		ArrayNode tfidfResultsAsArrayNode = createArrayNodeFromStringDoubleEntryList(tfidfresult, NLPResultUtil.propertyNameTFIDFEntityName , NLPResultUtil.propertyNameTFIDFValueName);
		return tfidfResultsAsArrayNode;
	}
	
	public List<Entry<String,Double>> getTFIDFAsListCalculatedBasedOnFrequenciesStoredInNLPResult(ObjectNode nlpResult, String language, Integer frequencyOfMostFrequentWord, String propertyNameForFrequencyEntries, String propertyNameInFrequencyEntriesForWord, String propertyNameInFrequencyEntriesForFrequency, String docFrequencyProviderType, int maxEntriesToReturn){
		
		Map<String,Integer> frequencies = NLPResultUtil.getFrequenciesStoredInNLPResult(nlpResult, propertyNameForFrequencyEntries, propertyNameInFrequencyEntriesForWord, propertyNameInFrequencyEntriesForFrequency);
		if(frequencies.size()==0 || !docFrequencyProvider.supportsType(docFrequencyProviderType)){
			return null;
		}
		List<Entry<String,Double>> tfidfresult = TFIDF.getTFIDFValuesTopX(frequencies, frequencyOfMostFrequentWord, language, docFrequencyProvider, docFrequencyProviderType, maxEntriesToReturn);
		return tfidfresult;
		
	}
	
	public ObjectNode getTFIDFForWordTypesUsingNlpResultAsInput(ObjectNode nlpResult, ObjectNode targetNode){
				
		// get needed data from nlp result
		String language = NLPResultUtil.getLanguage(nlpResult);
		if(language== null){return targetNode;}
		Integer frequencyOfMostFrequentWord = NLPResultUtil.getFrequencyOfMostFrequentWordInDoc(nlpResult);		
		if(frequencyOfMostFrequentWord== null){return targetNode;}
		Map<String,Integer> mapWordFrequencies = NLPResultUtil.getFrequenciesStoredInNLPResult(nlpResult, NLPResultUtil.propertyNameWordFrequenciesExclStopwords, NLPResultUtil.propertyNameInFrequencyEntriesForWord, NLPResultUtil.propertyNameInFrequencyEntriesForFrequency);
		if(mapWordFrequencies.size()==0){return targetNode;}
				
		// language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
			List<Entry<String,Double>> tfidfListLanguageDependent = TFIDF.getTFIDFValuesTopX(mapWordFrequencies, frequencyOfMostFrequentWord, language, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_languageDependent", tfidfResultLanguageDependent);

		}
		// not language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_Tokens)){
			List<Entry<String,Double>> tfidfListNotLanguageDependent = TFIDF.getTFIDFValuesTopX(mapWordFrequencies, frequencyOfMostFrequentWord, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_Tokens, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultNotLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListNotLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_Tokens + "_notlanguageDependent", tfidfResultNotLanguageDependent);
		}
		return targetNode;
	}
	
	public ObjectNode getTFIDFForSpotlightUsingNlpResultAsInput(ObjectNode nlpResult, ObjectNode targetNode){
		
		// get needed data from nlp result
		String language = NLPResultUtil.getLanguage(nlpResult);
		if(language== null){return targetNode;}
		Integer frequencyOfMostFrequentWord = NLPResultUtil.getFrequencyOfMostFrequentWordInDoc(nlpResult);		
		if(frequencyOfMostFrequentWord== null){return targetNode;}
		Map<String,Integer> spotlightFrequencies = NLPResultUtil.getSpotlightFrequenciesForURIsByAnalyzingSpotlightResults(nlpResult);
		if(spotlightFrequencies.size()==0){return targetNode;}

		// language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
			List<Entry<String,Double>> tfidfListLanguageDependent = TFIDF.getTFIDFValuesTopX(spotlightFrequencies, frequencyOfMostFrequentWord, language, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI + "_languageDependent", tfidfResultLanguageDependent);
		}
		// not language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
			List<Entry<String,Double>> tfidfListNotLanguageDependent = TFIDF.getTFIDFValuesTopX(spotlightFrequencies, frequencyOfMostFrequentWord, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultNotLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListNotLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI + "_notlanguageDependent", tfidfResultNotLanguageDependent);
		}
		return targetNode;
	}
	
	public ObjectNode getTFIDFForNamedEntitiesUsingNlpResultAsInput(ObjectNode nlpResult, ObjectNode targetNode){
		
		// get needed data from nlp result
		String language = NLPResultUtil.getLanguage(nlpResult);
		if(language== null){return targetNode;}
		Integer frequencyOfMostFrequentWord = NLPResultUtil.getFrequencyOfMostFrequentWordInDoc(nlpResult);		
		if(frequencyOfMostFrequentWord== null){return targetNode;}
		Map<String,Integer> frequencies = NLPResultUtil.getNERFrequenciesByAnalyzingNEs(nlpResult);
		if(frequencies.size()==0){return targetNode;}

		// language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities)){
			List<Entry<String,Double>> tfidfListLanguageDependent = TFIDF.getTFIDFValuesTopX(frequencies, frequencyOfMostFrequentWord, language, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities + "_languageDependent", tfidfResultLanguageDependent);
		}
		// not language dependent
		if(docFrequencyProvider.supportsType(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI)){
			List<Entry<String,Double>> tfidfListNotLanguageDependent = TFIDF.getTFIDFValuesTopX(frequencies, frequencyOfMostFrequentWord, null, docFrequencyProvider, NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, this.maxEntriesForTFIDFResult);
			ArrayNode tfidfResultNotLanguageDependent = createArrayNodeFromStringDoubleEntryList(tfidfListNotLanguageDependent, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
			targetNode.set(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities + "_notlanguageDependent", tfidfResultNotLanguageDependent);
		}
		return targetNode;
	}
	
	/**
	 * create an array node for an entry list List<Entry<String, Integer>> which would be typically a list of frequency entries
	 * @param entryList
	 * @param nameForKey
	 * @param nameForValue
	 * @return
	 */
	public static ArrayNode createArrayNodeFromStringIntegerEntryList(List<Entry<String,Integer>> entryList, String nameForKey, String nameForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<String,Integer> entry : entryList) {
			ObjectNode node = Json.newObject();
			node.put(nameForKey, entry.getKey());
			node.put(nameForValue, entry.getValue());
			arrayNode.add(node);
		}
		return arrayNode;
	}
	
	/**
	 * create an array node for an entry list List<Entry<String, Double>> which would be typically a list of tfidf entries
	 * @param list
	 * @param nameToUseForKey
	 * @param nameToUseForValue
	 * @return
	 */
	private static ArrayNode createArrayNodeFromStringDoubleEntryList(List<Entry<String, Double>> list, String nameToUseForKey, String nameToUseForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<String, Double> entry : list) {		
			ObjectNode singleNode = Json.newObject();
			singleNode.put(nameToUseForKey, entry.getKey());
			singleNode.put(nameToUseForValue, entry.getValue());
			arrayNode.add(singleNode);
		}
		return arrayNode;
	}
	
	private static JsonNode tfidfEntryListToTFIDFSubResultNode(String providerName, List<Entry<String, Double>> listContainingTFIDFResults ){
		
		ObjectNode tfidfSubresultNode = Json.newObject();
		
		// create array node from list
		ArrayNode tfidfResults = createArrayNodeFromStringDoubleEntryList(listContainingTFIDFResults, NLPResultUtil.propertyNameTFIDFEntityName, NLPResultUtil.propertyNameTFIDFValueName);
		
		// set provider name and tfidfresult
		tfidfSubresultNode.put(NLPResultUtil.propertyNameTFIDFProviderName, providerName);
		tfidfSubresultNode.set(NLPResultUtil.propertyNameTFIDFResultArrayName, tfidfResults);
		
		return tfidfSubresultNode;
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


	public String retrieveSlideTitleAndTextWithoutHTML(JsonNode slide, String separatorToUseBetweenSlideTitleAndSlideContent){
		// slide title
		String slidetitle = this.htmlToPlainText.getText(DeckServiceUtil.getSlideTitle(slide));
		slidetitle =	normalizeSlideTitle(slidetitle);
		String slideTitleAndText = "";
		if(slidetitle.length()>0 ){
			slideTitleAndText = slidetitle;
		}
		// slide content without html
		String contentWithoutHTML = this.htmlToPlainText.getText(DeckServiceUtil.getSlideContent(slide));
		contentWithoutHTML = normalizeSlideContent(contentWithoutHTML);

		// whole slide text (title & content without html)
		if(contentWithoutHTML.length()>0){
			if(slideTitleAndText.length()==0){
				slideTitleAndText = contentWithoutHTML;
			}else{
				slideTitleAndText = slideTitleAndText + separatorToUseBetweenSlideTitleAndSlideContent + contentWithoutHTML;
			}
		}
		return slideTitleAndText;

	}
	
	/**
	 * Removes such text like "New slide" or "No title"
	 * @return
	 */
	public static String normalizeSlideTitle(String input){
		String result = input;
		String[] thingsToRemove = new String[]{"no title", "No title", "new slide", "New slide", "\n"};
		for (String stringToRemove : thingsToRemove) {
			result = result.replace(stringToRemove, " ").trim();
		}
		return result;
	}

	/**
	 * TODO: make this more general - recheck with default values used when empty / default slides are created
	 * @param input
	 * @return
	 */
	public static String normalizeSlideContent(String input){
		String result = input;
		String[] thingsToRemove = new String[]{"Text bullet 1", "Text bullet 2", "Text bullet 3", "Text bullet 4", "Bullet 1", "Bullet 2", "Bullet 3", "Bullet 4"};
		for (String stringToRemove : thingsToRemove) {
			result = result.replace(stringToRemove, " ").trim();
		}
		String [] thingstoReplaceByNewLine = new String[]{"•"};
		for (String stringToReplace : thingstoReplaceByNewLine) {
			if(result.contains(stringToReplace)){
				result = result.replace(stringToReplace, " \n").trim();

			}
		}
		
		return result.trim();
	}

	
	
	
}
