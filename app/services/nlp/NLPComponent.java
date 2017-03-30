package services.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import services.nlp.dbpediaspotlight.DBPediaSpotlightUtil;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.types.TypeCounter;
import services.util.DeckServiceUtil;
import services.util.Sorter;

//TODO: clean up

/**
 * Convenience class for performing complex nlp processes of several sub components with 1 class.
 * @author aschlaf
 *
 */
public class NLPComponent {
	
	public static String propertyNameOriginalInput = "input";
	public static String propertyNameHtmlToPlainText = "htmlToPlainText";
	public static String propertyNameSlideTitleAndText = "slideTitleAndText";
	public static String propertyNameLanguage = "detectedLanguage";
	public static String propertyNameTokens = "tokens";
	public static String propertyNameTypes = "types";
	public static String propertyNameNER = "NER";
	public static String propertyNameTFIDF = "TFIDF";
	public static String propertyNameDBPediaSpotlight = "DBPediaSpotlight";
	
	// key names for specific document frequency providers
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent";

	public int maxEntriesForTFIDFResult = 10; //TODO: make this configurable?

	private IHtmlToText htmlToPlainText;
	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
    private IStopwordRemover stopwordRemover;
    private IDocFrequencyProviderTypeDependent docFrequencyProvider; 
    private DeckServiceUtil deckServiceUtil;  
    private DBPediaSpotlightUtil dbPediaSpotlightUtil;  
    
    private boolean tfidfCalculationWithToLowerCase = true;
    private boolean stopwordRemovalWithToLowerCase = true;
    private boolean typesToLowerCase = true;

    
	@Inject
	public NLPComponent(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, IDocFrequencyProviderTypeDependent docFrequencyProvider) {
		super();
		this.htmlToPlainText = htmlToText;
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.stopwordRemover = stopwordRemover;
		this.ner = ner;
		this.docFrequencyProvider = docFrequencyProvider;
		this.deckServiceUtil = new DeckServiceUtil();
		this.dbPediaSpotlightUtil = dbPediaSpotlightUtil;
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
    	return node.put(propertyNameHtmlToPlainText, plainText);
	}
	
	public String detectLanguage(String input){
    	return this.languageDetector.getLanguage(input);	
	}
	
	public ObjectNode detectLanguage(String input, ObjectNode node){
    	String language = this.languageDetector.getLanguage(input);	
    	return node.put(propertyNameLanguage, language);
	}
	
	public String[] tokenize(String input, String language){
    	return this.tokenizer.tokenize(input, language);	
	}
	
	public ObjectNode tokenize(String input, String language, ObjectNode node){
    	String[] tokens = this.tokenizer.tokenize(input, language);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);
    	return node;
	}
	
	public List<NlpTag> performNER(String[] tokens, String language){
    	return this.ner.getNEs(tokens, language);	
	}
	
	public ObjectNode performNER(String[] tokens, String language, ObjectNode node){
    	List<NlpTag> ners = this.ner.getNEs(tokens, language);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);
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
		node.put(propertyNameOriginalInput, input);
		node.put(propertyNameHtmlToPlainText, plainText);
		
		String detectedLanguage = this.languageDetector.getLanguage(plainText);	
		node.put(propertyNameLanguage, detectedLanguage);
		
    	String[] tokens = this.tokenizer.tokenize(plainText, detectedLanguage);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);

    	List<NlpTag> ners = this.ner.getNEs(tokens, detectedLanguage);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);

    	
		// type frequencies
		Map<String,Integer> typeCountings = TypeCounter.getTypeCountings(tokens, typesToLowerCase);
		Map<String,Integer> typeCountingsSorted = Sorter.sortByValue(typeCountings, true);
		int frequencyOfMostFrequentType = typeCountingsSorted.entrySet().iterator().next().getValue(); // needed for tfidf
		
		// types stop words removed
		Map<String,Integer> typeCountingsSortedStopWordsRemoved = new LinkedHashMap<>(typeCountingsSorted);
		stopwordRemover.removeStopwords(typeCountingsSortedStopWordsRemoved, detectedLanguage);

		ObjectNode TFIDFResultNode= Json.newObject();

    	// TFIDF all token types (regardless NER)
		if(docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent)){
	    	List<Entry<String,Double>> tfidfEntries = TFIDF.getTFIDFValuesTopX(typeCountingsSortedStopWordsRemoved, frequencyOfMostFrequentType, this.tfidfCalculationWithToLowerCase, detectedLanguage, docFrequencyProvider, propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, maxEntriesForTFIDFResult);
	    	ArrayNode tfidfNode = tfidfEntryListAsArrayNode(tfidfEntries, "term", "tfidf");
	    	TFIDFResultNode.set(propertyNameTFIDF + "_tokens", tfidfNode);
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
		
		
		if(docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent)){
			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spoltlightURIs, false, "ALL", docFrequencyProvider, propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent,  maxEntriesForTFIDFResult);
			TFIDFResultNode.set(propertyNameTFIDF + "_forSpotlightEntities", tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
		}
		node.set(propertyNameTFIDF, TFIDFResultNode);

		
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
			Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorFromDeckserviceResponse(response);
			ObjectNode result = processSlidesOfDeck(deckId, slidesIterator, minConfidenceDBPediaSpotlightPerSlide, minConfidenceDBPediaSpotlightPerDeck);
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
	public ObjectNode processNLPForSlidesOfDeck(Iterator<JsonNode> slidesIterator, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck) throws WebApplicationException{
		
		boolean performDBPediaSpotlightPerSlide = true;
		if(minConfidenceDBPediaSpotlightPerSlide>1){
			performDBPediaSpotlightPerSlide = false;
		}
		boolean performDBPediaSpotlightPerDeck = true;
		if(minConfidenceDBPediaSpotlightPerDeck>1){
			performDBPediaSpotlightPerDeck = false;
		}
		ObjectNode result = Json.newObject();
		ArrayNode slideArrayNode = Json.newArray();

		StringBuilder sbWholeDeckText = new StringBuilder();

		List<String> resourceURIsOfDeckRetrievedPerSlide = new ArrayList<>();

		while (slidesIterator.hasNext()){
			
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			
			ObjectNode slideResult = Json.newObject();
			slideResult.put("id", slide.get("id").textValue());
			
			ObjectNode resultsForSlide = Json.newObject();

			String slideTitleAndText = retrieveSlideTitleAndTextWithoutHTML(slide, "\n");
			resultsForSlide.put(propertyNameSlideTitleAndText, slideTitleAndText);
			sbWholeDeckText.append("\n" + slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
				continue;
			}

			// language
			String languageOfSlide = detectLanguage(slideTitleAndText);
			resultsForSlide.put(NLPComponent.propertyNameLanguage, languageOfSlide);

			// tokens
			tokenize(slideTitleAndText, languageOfSlide, resultsForSlide);
						
			// NER
			// TODO: add NER
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide);
				if(response.getStatus()!=200){
					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", response);
				}
				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);
				resultsForSlide.set(NLPComponent.propertyNameDBPediaSpotlight, spotlightresult);

				// track all resources of deck to analyze them later
				ArrayNode resources = (ArrayNode) spotlightresult.get("Resources");
				if(resources!=null){
					for (int i = 0; i < resources.size(); i++) {
						JsonNode resourceNode = resources.get(i);
						String URI = resourceNode.get("@URI").textValue();
						resourceURIsOfDeckRetrievedPerSlide.add(URI);
					}
				}

				

			}

			slideResult.set("nlpProcessResults", resultsForSlide);
	
			slideArrayNode.add(slideResult);
		}
		
		// add results of all single slides to result node
		result.set("children", slideArrayNode);

		
		ObjectNode resultNodeWholeDeck = Json.newObject();
		
		// language detection for whole deck
		String deckText = sbWholeDeckText.toString();
		String languageWholeDeck = languageDetector.getLanguage(deckText);
		resultNodeWholeDeck.put("languageDetectedWholeDeck", languageWholeDeck);
		
		// dbpedia spotlight per deck 
		if(performDBPediaSpotlightPerDeck){
			Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck);
			if(response.getStatus()!=200){
				throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + deckText + "\"", response);
			}
			JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);
			
			resultNodeWholeDeck.set(propertyNameDBPediaSpotlight, spotlightresult);	
		}

		result.set("nlpProcessResults", resultNodeWholeDeck);
		
		return result;

		
	}
	
	/**
	 * prforms nlp for slides of deck incl. tfidf
	 * @param deckId
	 * @param slidesIterator
	 * @param minConfidenceDBPediaSpotlightPerSlide confidence to be used for DBPediaSpotlight performed on slide text. If >1, spotlight retrieval will be skipped for slides
	 * @param minConfidenceDBPediaSpotlightPerDeck confidence to be used for DBPediaSpotlight performed on deck text. If >1, spotlight retrieval will be skipped for deck text (concatenated slide texts)
	 * @return
	 */
	public ObjectNode processSlidesOfDeck(String deckId, Iterator<JsonNode> slidesIterator, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck){
		

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

		List<String> spotlightResourceURIsOfDeckRetrievedPerSlide = new ArrayList<>();

		while (slidesIterator.hasNext()){
			
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();
			resultsForSlide.put("slideId", slide.get("id").textValue());

			String slideTitleAndText = retrieveSlideTitleAndTextWithoutHTML(slide, "\n");
			resultsForSlide.put(propertyNameSlideTitleAndText, slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
				slideArrayNode.add(resultsForSlide);
				continue;
			}

			sbWholeDeckText.append("\n" + slideTitleAndText);

			// language
			String languageOfSlide = detectLanguage(slideTitleAndText);
			resultsForSlide.put(NLPComponent.propertyNameLanguage, languageOfSlide);

			// tokens
			String[] tokenArrayOfSlide = tokenizer.tokenize(slideTitleAndText, languageOfSlide);
			tokensOfWholeDeck.addAll(Arrays.asList(tokenArrayOfSlide)); 
			resultsForSlide.set(NLPComponent.propertyNameTokens, Json.toJson(tokenArrayOfSlide));
			
			// NER
			performNER(tokenArrayOfSlide, languageOfSlide, resultsForSlide);
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide);
				if(response.getStatus()!=200){
					throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + slideTitleAndText + "\"", response);
				}
				JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);

				resultsForSlide.set(NLPComponent.propertyNameDBPediaSpotlight, spotlightresult);

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

				
		String deckText = sbWholeDeckText.toString();
		if(deckText.length()==0){
			result.put("info", "Deck contains no text. No NLP processing prossible.");
			return result;
		}
		
		
		// language detection for whole deck
		String languageWholeDeck = detectLanguage(deckText);
		result.put("languageDetectedWholeDeck", languageWholeDeck);

		
		//TFIDF intial node
		ObjectNode TFIDFResultNode= Json.newObject();

		// further processing of tokens (frequencies, stop word removal, tfidf)
			
		// type frequencies
		Map<String,Integer> typeCountings = TypeCounter.getTypeCountings(tokensOfWholeDeck, typesToLowerCase);
		List<Entry<String,Integer>> typeCountingsSortedAsList = Sorter.sortByValueAndReturnAsList(typeCountings, true);
		int frequencyOfMostFrequentType = typeCountingsSortedAsList.get(0).getValue();
		
		// types stop words removed
		Map<String,Integer> typeCountingsStopWordsRemoved = new HashMap<>(typeCountings);
		stopwordRemover.removeStopwords(typeCountingsStopWordsRemoved, languageWholeDeck);
		
		if(typeCountingsStopWordsRemoved.size()>0){
			// output types without stopwords sorted by frequency
			List<Entry<String,Integer>> typeCountingsSortedStopWordsRemoved=  Sorter.sortByValueAndReturnAsList(typeCountingsStopWordsRemoved, true);
			JsonNode wordTypeCountingsStopWordsRemoved = createArrayNodeForEntryList(typeCountingsSortedStopWordsRemoved, "word", "frequency");
			result.set("wordsAndFrequenciesStopwordsRemoved", wordTypeCountingsStopWordsRemoved);
			
			// tfidf for tokens compared to SlideWiki2
			// language dependent
			if(docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki2 = TFIDF.getTFIDFValuesTopX(typeCountingsStopWordsRemoved, frequencyOfMostFrequentType, this.tfidfCalculationWithToLowerCase, languageWholeDeck, docFrequencyProvider, propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, maxEntriesForTFIDFResult);
				TFIDFResultNode.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki2, "term", "tfidf"));
			}
			// not language dependent
			if(docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent)){
				List<Entry<String,Double>> tfidfresultTokensSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(typeCountingsStopWordsRemoved, frequencyOfMostFrequentType, this.tfidfCalculationWithToLowerCase, "ALL", docFrequencyProvider, propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent,  maxEntriesForTFIDFResult);
				TFIDFResultNode.set(propertyNameTFIDF + "_" + NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki2_notlanguagedependent, "term", "tfidf"));
			}

		}

		
			
		// tfidf for spotlight entity URIs
		if(performDBPediaSpotlightPerSlide){// tfidf of spotlight entities can only be performed if spotlight entities were retrieved before

			// not language dependent
			if(spotlightResourceURIsOfDeckRetrievedPerSlide.size()>0 && docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent)){
				List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightResourceURIsOfDeckRetrievedPerSlide, false, "ALL", docFrequencyProvider, propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, maxEntriesForTFIDFResult);
				TFIDFResultNode.set(propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerSlide_"+ propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
			}
		}

		
		// dbpedia spotlight per deck 
		if(performDBPediaSpotlightPerDeck && deckText.length()>0){
			Response response = dbPediaSpotlightUtil.performDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck);
			if(response.getStatus()!=200){
				throw new WebApplicationException("Problem calling DBPedia Spotlight for given text. Returned status " + response.getStatus() + ". Text was:\n\"" + deckText + "\"", response);
			}
			JsonNode spotlightresult = DBPediaSpotlightUtil.getJsonFromMessageBody(response);			
			result.set(propertyNameDBPediaSpotlight + "_perDeck", spotlightresult);
			
			JsonNode resourcesNode = spotlightresult.get("Resources");
			Set<String> spotlightResourceURIsOfDeckRetrievedPerDeck = new HashSet<>();
			if(resourcesNode!=null && !resourcesNode.isNull()){
				ArrayNode resources = (ArrayNode) resourcesNode;
				for (int i = 0; i < resources.size(); i++) {
					JsonNode resourceNode = resources.get(i);
					String URI = resourceNode.get("@URI").textValue();
					spotlightResourceURIsOfDeckRetrievedPerDeck.add(URI);
				}
			}
			
			// spotlight counting
			
			//tfidf for these spotlight entities
			if(docFrequencyProvider.supportsType(propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent)){
				List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightResourceURIsOfDeckRetrievedPerDeck, false, "ALL", docFrequencyProvider, propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, maxEntriesForTFIDFResult);
				TFIDFResultNode.set(propertyNameTFIDF + "_forSpotlightEntitiesRetrievedPerDeck_" + propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
			}
		}

		result.set(propertyNameTFIDF, TFIDFResultNode);
		
		return result;
	}
	

	private static ArrayNode tfidfEntryListAsArrayNode(List<Entry<String, Double>> list, String nameToUseForKey, String nameToUseForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<String, Double> entry : list) {		
			ObjectNode singleNode = Json.newObject();
			singleNode.put(nameToUseForKey, entry.getKey());
			singleNode.put(nameToUseForValue, entry.getValue());
			arrayNode.add(singleNode);
		}
		return arrayNode;
	}
	
	public String getPropertyNameLanguage() {
		return propertyNameLanguage;
	}



	public String getPropertyNameTokens() {
		return propertyNameTokens;
	}



	public String getPropertyNameNER() {
		return propertyNameNER;
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

	public String getPropertyNameOriginalInput() {
		return propertyNameOriginalInput;
	}


	public String getPropertyNameHtmlToPlainText() {
		return propertyNameHtmlToPlainText;
	}


	public String getPropertyNameTFIDF() {
		return propertyNameTFIDF;
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
			result.replace(stringToRemove, " ").trim();
		}
				
		return result.trim();
	}

	public static <E,F> ArrayNode createArrayNodeForEntryList(List<Entry<E,F>> entryList, String nameForKey, String nameForValue){
		ArrayNode arrayNode = Json.newArray();
		for (Entry<E,F> entry : entryList) {
			ObjectNode node = Json.newObject();
			node.putPOJO(nameForKey, entry.getKey());
			node.putPOJO(nameForValue, entry.getValue());
			arrayNode.add(node);
		}
		return arrayNode;
	}
	
	
}
