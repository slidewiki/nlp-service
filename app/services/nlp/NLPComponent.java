package services.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

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
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.TFIDF;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.util.DeckServiceUtil;

//TODO: clean up
public class NLPComponent implements INLPComponent{
	
	public static String propertyNameOriginalInput = "input";
	public static String propertyNameHtmlToPlainText = "htmlToPlainText";
	public static String propertyNameSlideTitleAndText = "slideTitleAndText";
	public static String propertyNameLanguage = "detectedLanguage";
	public static String propertyNameTokens = "tokens";
	public static String propertyNameTypes = "types";
	public static String propertyNameNER = "NER";
	public static String propertyNameTFIDF = "TFIDF";
	public static String propertyNameDBPediaSpotlight = "DBPediaSpotlight";
	
	// slidewiki 1 (old platform)
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent = "docFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent = "docFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki1_perSlide_languageDependent = "docFreqProvider_Spotlight_SlideWiki1_perSlide_languageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki1_perSlide_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki1_perSlide_notlanguageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent = "docFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent";

	// slidewiki 2 (new platform)
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_languageDependent";
//	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perSlide_notlanguageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent";
	public static String propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent = "docFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent";

	public int maxEntriesForTFIDFResult = 10;

	private IHtmlToText htmlToPlainText;
	private ILanguageDetector languageDetector;
	private ITokenizerLanguageDependent tokenizer;
    private INERLanguageDependent ner;
    private IStopwordRemover stopwordRemover;
    private Map<String,IDocFrequencyProvider> mapDocFrequencyProvider; 
    private DeckServiceUtil deckServiceUtil;  
    private DBPediaSpotlightUtil dbPediaSpotlightUtil;  
    private boolean tfidfCalculationWithToLowerCase = true;
    private boolean stopwordRemovalWithToLowerCase = true;
    private double dbpediaspotlightdefaultConfidence = 0.6; // TODO: make this configurable

    
	@Inject
	public NLPComponent(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, Map<String,IDocFrequencyProvider> mapDocFrequencyProvider) {
		super();
		this.htmlToPlainText = htmlToText;
		this.languageDetector = languageDetector;
		this.tokenizer = tokenizer;
		this.stopwordRemover = stopwordRemover;
		this.ner = ner;
		this.mapDocFrequencyProvider = mapDocFrequencyProvider;
		this.deckServiceUtil = new DeckServiceUtil();
		this.dbPediaSpotlightUtil = new DBPediaSpotlightUtil();
	}
	
	public ObjectNode getPlainTextFromHTML(String input, ObjectNode node){
    	String plainText = this.htmlToPlainText.getText(input);
    	return node.put(propertyNameHtmlToPlainText, plainText);
	}
	
	public ObjectNode detectLanguage(String input, ObjectNode node){
    	String language = this.languageDetector.getLanguage(input);	
    	return node.put(propertyNameLanguage, language);
	}
	
	public String detectLanguage(String input){
    	return this.languageDetector.getLanguage(input);	
	}
	
	public ObjectNode tokenize(String input, String language, ObjectNode node){
    	String[] tokens = this.tokenizer.tokenize(input, language);	
    	JsonNode tokenNode = Json.toJson(tokens);
    	node.set(propertyNameTokens, tokenNode);
    	return node;
	}
	
	public ObjectNode ner(String[] tokens, String language, ObjectNode node){
    	List<NlpTag> ners = this.ner.getNEs(tokens, language);	
    	JsonNode nerNode = Json.toJson(ners);
    	node.set(propertyNameNER, nerNode);
    	return node;
	}
	
	
	public JsonNode performDBpediaSpotlight(String input, double confidence){
		
		return dbPediaSpotlightUtil.callDBPediaSpotlight(input, confidence);
			
	}
	
	public ObjectNode performDBpediaSpotlight(String input, double confidence, ObjectNode node){
		
		JsonNode resultNode = performDBpediaSpotlight(input, confidence);
		node.set(propertyNameDBPediaSpotlight, resultNode);
		return node;
		
	}
	
	public ObjectNode performNLP(String input, ObjectNode node){
		
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
    	
    	// TFIDF all token tyes (regardless NER)
		IDocFrequencyProvider docFrequencyProvider_Tokens_SlideWiki1 = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent);
    	List<Entry<String,Double>> tfidfEntries = TFIDF.getTFIDFValuesTopX(tokens, this.tfidfCalculationWithToLowerCase, detectedLanguage, docFrequencyProvider_Tokens_SlideWiki1, maxEntriesForTFIDFResult);
    	ArrayNode tfidfNode = tfidfEntryListAsArrayNode(tfidfEntries, "term", "tfidf");
		node.set(propertyNameTFIDF, tfidfNode);
		
		// dbpediaspotlight
		node = performDBpediaSpotlight(plainText, dbpediaspotlightdefaultConfidence, node);
		
    	return node;
	}

	
	public ObjectNode processDeck(int deckId, boolean performDBPediaSpotlightPerSlide){
		Logger.info("deckId: " + deckId);

		JsonNode deckNode = this.deckServiceUtil.getSlidesForDeckIdFromDeckservice(deckId);
		Iterator<JsonNode> slidesIterator = DeckServiceUtil.getSlidesIteratorForDeckserviceResultDeckSlides(deckNode);
		ObjectNode result = processSlidesOfDeck(slidesIterator, performDBPediaSpotlightPerSlide);
		return result;
	}
	
	/**
	 * without tfidf
	 * @param slidesIterator
	 * @param minConfidenceDBPediaSpotlightPerSlide
	 * @param minConfidenceDBPediaSpotlightPerDeck
	 * @return
	 */
	public ObjectNode processNLPForSlidesOfDeck(Iterator<JsonNode> slidesIterator, double minConfidenceDBPediaSpotlightPerSlide, double minConfidenceDBPediaSpotlightPerDeck){
		
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

		List<String> tokensOfWholeDeck = new ArrayList<>(); // needed for language detection of whole deck
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
			String languageOfSlide = languageDetector.getLanguage(slideTitleAndText);
			resultsForSlide.put(NLPComponent.propertyNameLanguage, languageOfSlide);

			// tokens
			String[] tokenArray = tokenizer.tokenize(slideTitleAndText, languageOfSlide);
			List<String> tokens = Arrays.asList(tokenArray);
			tokensOfWholeDeck.addAll(tokens); // needed for language detection of whole deck and tfidf
			resultsForSlide.set(NLPComponent.propertyNameTokens, Json.toJson(tokenArray));
						
			// NER
			// TODO: add NER
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				JsonNode spotlightresult = dbPediaSpotlightUtil.callDBPediaSpotlight(slideTitleAndText, minConfidenceDBPediaSpotlightPerSlide);
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
			JsonNode spotlightresult = dbPediaSpotlightUtil.callDBPediaSpotlight(deckText, minConfidenceDBPediaSpotlightPerDeck);
			resultNodeWholeDeck.set(propertyNameDBPediaSpotlight, spotlightresult);	
		}

		result.set("nlpProcessResults", resultNodeWholeDeck);
		
		return result;

		
	}
	
	/**
	 * incl. tfidf
	 * @param slidesIterator
	 * @param performDBPediaSpotlightPerSlide
	 * @param performDBPediaSpotlightPerDeck
	 * @return
	 */
	public ObjectNode processSlidesOfDeck(Iterator<JsonNode> slidesIterator, boolean performDBPediaSpotlightPerSlide){
		
		ObjectNode result = Json.newObject();
		ArrayNode slideArrayNode = Json.newArray();

		List<String> tokensOfWholeDeck = new ArrayList<>(); // needed for language detection of whole deck
		Set<String> typesOfWholeDeck = new HashSet<>();
		StringBuilder sbWholeDeckText = new StringBuilder();

		List<String> resourceURIsOfDeckRetrievedPerSlide = new ArrayList<>();

		while (slidesIterator.hasNext()){
			
			ObjectNode slide = (ObjectNode) slidesIterator.next();
			ObjectNode resultsForSlide = Json.newObject();

			String slideTitleAndText = retrieveSlideTitleAndTextWithoutHTML(slide, "\n");
			resultsForSlide.put(propertyNameSlideTitleAndText, slideTitleAndText);
			sbWholeDeckText.append("\n" + slideTitleAndText);

			if(slideTitleAndText.length()==0){
				// no text content for slide
//				counterSlidesWithoutText++;
				continue;
			}

			// language
			String languageOfSlide = languageDetector.getLanguage(slideTitleAndText);
			resultsForSlide.put(NLPComponent.propertyNameLanguage, languageOfSlide);

			// tokens
			String[] tokenArray = tokenizer.tokenize(slideTitleAndText, languageOfSlide);
			List<String> tokens = Arrays.asList(tokenArray);
			tokensOfWholeDeck.addAll(tokens); // needed for language detection of whole deck and tfidf
			resultsForSlide.set(NLPComponent.propertyNameTokens, Json.toJson(tokenArray));
			
//			// types (needed for tfidf)
//			Set<String> typesOfSlide = new HashSet<>(Arrays.asList(tokenArray));			
//			if(tfidfCalculationWithToLowerCase){
//				Set<String> typesLowerCase = new HashSet<>();
//				for (String type : typesOfSlide) {
//					typesLowerCase.add(type.toLowerCase());
//				}
//				typesOfSlide = typesLowerCase;
//			}
//			stopwordRemover.removeStopwords(typesOfSlide, languageOfSlide);	
//			typesOfWholeDeck.addAll(typesOfSlide);
//			resultsForSlide.set(this.propertyNameTypes, Json.toJson(typesOfSlide));
			
			// NER
			// TODO: add NER
			
			// dbpedia spotlight per slide
			if(performDBPediaSpotlightPerSlide){
	
				JsonNode spotlightresult = dbPediaSpotlightUtil.callDBPediaSpotlight(slideTitleAndText, 0.35);
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

			slide.set("nlpProcessResults", resultsForSlide);
	
			slideArrayNode.add(slide);
		}
		
		// add single slide results
		result.set("children", slideArrayNode);

		
		ObjectNode resultNodeWholeDeck = Json.newObject();
		
		// language detection for whole deck
		String deckText = sbWholeDeckText.toString();
		String languageWholeDeck = languageDetector.getLanguage(deckText);
		resultNodeWholeDeck.put("languageDetectedWholeDeck", languageWholeDeck);

		
		// remove stopwords
		List<String> tokensOfWholeDeckStopwordsRemoved = new ArrayList<>(tokensOfWholeDeck);
		if(stopwordRemover != null){
			stopwordRemover.removeStopwords(tokensOfWholeDeckStopwordsRemoved, languageWholeDeck);
		}
		String[] tokensOfWholeDeckStopwordsRemovedArray = new String[tokensOfWholeDeckStopwordsRemoved.size()];
		tokensOfWholeDeckStopwordsRemovedArray = tokensOfWholeDeckStopwordsRemoved.toArray(tokensOfWholeDeckStopwordsRemovedArray);
		// TODO: add to lower case
		
		
//		// tfidf for tokens compared to SlideWiki1
//		// language dependent
//		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent)){
//			IDocFrequencyProvider docFrequencyProvider_Tokens_SlideWiki1_languagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent);
//			List<Entry<String,Double>> tfidfresultTokensSlideWiki1_languagedependent = TFIDF.getTFIDFValuesTopX(tokensOfWholeDeckStopwordsRemovedArray, this.tfidfCalculationWithToLowerCase, languageWholeDeck, docFrequencyProvider_Tokens_SlideWiki1_languagedependent, maxEntriesForTFIDFResult);
//			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki1_languagedependent, "term", "tfidf"));
//		}
//		// not language dependent
//		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent)){
//			IDocFrequencyProvider docFrequencyProvider_Tokens_SlideWiki1_notlanguagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent);
//			List<Entry<String,Double>> tfidfresultTokensSlideWiki1_notlanguagedependent = TFIDF.getTFIDFValuesTopX(tokensOfWholeDeckStopwordsRemovedArray, this.tfidfCalculationWithToLowerCase, "ALL", docFrequencyProvider_Tokens_SlideWiki1_notlanguagedependent, maxEntriesForTFIDFResult);
//			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki1_notlanguagedependent, "term", "tfidf"));
//		}
		// tfidf for tokens compared to SlideWiki2
		// language dependent
		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent)){
			IDocFrequencyProvider docFrequencyProvider_Tokens_SlideWiki2 = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent);
			List<Entry<String,Double>> tfidfresultTokensSlideWiki2 = TFIDF.getTFIDFValuesTopX(tokensOfWholeDeckStopwordsRemovedArray, this.tfidfCalculationWithToLowerCase, languageWholeDeck, docFrequencyProvider_Tokens_SlideWiki2, maxEntriesForTFIDFResult);
			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki2, "term", "tfidf"));
		}
		// not language dependent
		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent)){
			IDocFrequencyProvider docFrequencyProvider_Tokens_SlideWiki2_notlanguagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent);
			List<Entry<String,Double>> tfidfresultTokensSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(tokensOfWholeDeckStopwordsRemovedArray, this.tfidfCalculationWithToLowerCase, "ALL", docFrequencyProvider_Tokens_SlideWiki2_notlanguagedependent, maxEntriesForTFIDFResult);
			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultTokensSlideWiki2_notlanguagedependent, "term", "tfidf"));
		}
		
		// tfidf for spotlight entities whole deck
		String[] spotlightEntitesOfDeckAsArray = new String[resourceURIsOfDeckRetrievedPerSlide.size()];
		spotlightEntitesOfDeckAsArray = resourceURIsOfDeckRetrievedPerSlide.toArray(spotlightEntitesOfDeckAsArray);
//		// compared to SlideWiki1
//		// language dependent
//		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent)){
//			IDocFrequencyProvider docFrequencyProvider_Spotlight_SlideWiki1_perDeck_languagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent);
//			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki1_languagedependent = TFIDF.getTFIDFValuesTopX(spotlightEntitesOfDeckAsArray, false, languageWholeDeck, docFrequencyProvider_Spotlight_SlideWiki1_perDeck_languagedependent, maxEntriesForTFIDFResult);
//			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki1_languagedependent, "spotlightEntity", "tfidf"));	
//		}
//		// not language dependent
//		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent)){
//			IDocFrequencyProvider docFrequencyProvider_Spotlight_SlideWiki1_perDeck_notlanguagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent);
//			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki1_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightEntitesOfDeckAsArray, false, "ALL", docFrequencyProvider_Spotlight_SlideWiki1_perDeck_notlanguagedependent, maxEntriesForTFIDFResult);
//			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki1_notlanguagedependent, "spotlightEntity", "tfidf"));	
//		}
		// compared to SlideWiki2
//		// language dependent
//		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent)){
//			IDocFrequencyProvider docFrequencyProvider_Spotlight_SlideWiki2_perDeck = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent);
//			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2 = TFIDF.getTFIDFValuesTopX(spotlightEntitesOfDeckAsArray, false, languageWholeDeck, docFrequencyProvider_Spotlight_SlideWiki2_perDeck, maxEntriesForTFIDFResult);
//			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki2, "spotlightEntity", "tfidf"));
//		}
		// not language dependent
		if(mapDocFrequencyProvider.containsKey(propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent)){
			IDocFrequencyProvider docFrequencyProvider_Spotlight_SlideWiki2_perDeck_notlanguagedependent = mapDocFrequencyProvider.get(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent);
			List<Entry<String,Double>> tfidfresultSpotlightSlideWiki2_notlanguagedependent = TFIDF.getTFIDFValuesTopX(spotlightEntitesOfDeckAsArray, false, "ALL", docFrequencyProvider_Spotlight_SlideWiki2_perDeck_notlanguagedependent, maxEntriesForTFIDFResult);
			resultNodeWholeDeck.set(propertyNameTFIDF + "_" + propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, tfidfEntryListAsArrayNode(tfidfresultSpotlightSlideWiki2_notlanguagedependent, "spotlightEntity", "tfidf"));	
		}

		
//		// dbpedia spotlight per deck 
//		if(performDBPediaSpotlightPerDeck){
//			JsonNode spotlightresult = dbPediaSpotlightUtil.callDBPediaSpotlight(deckText, 0.35);
//			resultNodeWholeDeck.set(propertyNameDBPediaSpotlight, Json.toJson(resourceURIsOfDeckRetrievedPerDeck));
//		}


		result.set("nlpProcessResults", resultNodeWholeDeck);
		
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
		String slidetitle = normalizeSlideTitle(DeckServiceUtil.getSlideTitle(slide));
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
		String[] thingsToRemove = new String[]{"no title", "No title", "new slide", "New slide"};
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

	
}
