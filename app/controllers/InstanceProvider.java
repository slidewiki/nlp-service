package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Configuration;
import play.Logger;
import services.nlp.NLPComponent;
import services.nlp.html.HTMLJsoup;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.languagedetection.LanguageDetector_optimaize;
import services.nlp.microserviceutil.ActivitiesServiceUtil;
import services.nlp.microserviceutil.DBPediaSpotlightUtil;
import services.nlp.microserviceutil.DeckServiceUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.ner.INER;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NERLanguageDependentViaMap;
import services.nlp.ner.NER_OpenNLP;
import services.nlp.recommendation.ITagRecommender;
import services.nlp.recommendation.TagRecommenderTFIDFViaNLStoreFrequencies;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.stopwords.StopwordRemoverFactory;
import services.nlp.tfidf.DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics;
import services.nlp.tfidf.ITFIDFMerger;
import services.nlp.tfidf.TFIDFMerger;
import services.nlp.tfidf.TitleBoostSettings;
import services.nlp.tokenization.ITokenizer;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.tokenization.TokenizerLanguageDependentViaMap;
import services.nlp.tokenization.Tokenizer_OpenNLP;

public class InstanceProvider {

	
	public static NLPController provideNLPController(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
		
		NLPComponent nlpComponent = provideNLPCompomnent(configuration);
		return new NLPController(nlpComponent);
	}
	
	public static NLPComponent provideNLPCompomnent(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
		
		DeckServiceUtil deckServiceUtil = provideDeckServiceUtil(configuration);
		IHtmlToText htmlToText = provideHtmlToTextVisJSoup(configuration);
		ILanguageDetector languageDetector = provideLanguageDetectorViaOptimaize(configuration);
		ITokenizerLanguageDependent tokenizer = provideTokenizerLanguageDependentViaMap(configuration);
		IStopwordRemover stopwordRemover = provideStopwordRemoverViaMap(configuration);
		INERLanguageDependent ner = provideNERLanguageDependentViaMap(configuration);
		DBPediaSpotlightUtil dbPediaSpotlightUtil = provideDBPediaSpotlightUtil(configuration);
		NLPStorageUtil nlpStorageUtil = provideNLPStorageUtil(configuration);
		
		ITagRecommender tagRecommender = provideTagRecommenderTFIDFViaNLPStoreFrequencies(configuration, nlpStorageUtil);
		return new NLPComponent(deckServiceUtil, htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, nlpStorageUtil, tagRecommender);
	}
	
	public static DeckServiceUtil provideDeckServiceUtil(Configuration configuration){
	    	String deckserviceUrl = configuration.getString("deckservice.baseurl");
	    	return new DeckServiceUtil(deckserviceUrl);
	}
	
	public static ActivitiesServiceUtil provideActivitiesServiceUtil(Configuration configuration){
    	String deckserviceUrl = configuration.getString("activitiesservice.baseurl");
    	return new ActivitiesServiceUtil(deckserviceUrl);
}
	
	public static IHtmlToText provideHtmlToTextVisJSoup(Configuration configuration){
		return new HTMLJsoup();
	}
	
	public static ILanguageDetector provideLanguageDetectorViaOptimaize(Configuration configuration) throws IOException{
		return new LanguageDetector_optimaize();
	}
	
	public static ITokenizerLanguageDependent provideTokenizerLanguageDependentViaMap(Configuration configuration) {
	        
	    	String defaultLanguageToUseIfGivenLanguageNotAvailable = configuration.getString("tokenizer.defaultLanguageToUseIfGivenLanguageNotAvailable");
	    	
	    	Map<String,ITokenizer> tokenizerMap = new HashMap<>();
	    	// openNLP
	    	List<String> tokenizerModelFilepathsOpenNLP = configuration.getStringList("tokenizer.opennlp.model.filepaths");
	    	for (String tokenizerModelFilepath : tokenizerModelFilepathsOpenNLP) {
				String modelFullName = tokenizerModelFilepath.substring(tokenizerModelFilepath.lastIndexOf("/")+1);
	    		String language = modelFullName.substring(0,2);
	    		Logger.info("loading tokenizer model for language \"" + language +"\"");
				tokenizerMap.put(language, new Tokenizer_OpenNLP(tokenizerModelFilepath));
			}
	    	return new TokenizerLanguageDependentViaMap(tokenizerMap, defaultLanguageToUseIfGivenLanguageNotAvailable);     
	}

	
	public static IStopwordRemover provideStopwordRemoverViaMap(Configuration configuration) throws FileNotFoundException, IOException {
    	
    	String folderpath = configuration.getString("stopwords.wortschatz.folderpathContainingWortschatzFiles");
		int topX = configuration.getInt("stopwords.wortschatz.topXEntriesToUseAsStopwords");
		boolean includeSpecialCharsAtBeginningAdditionalToTopX = configuration.getBoolean("stopwords.wortschatz.includeSpecialCharsAtBeginningAdditionalToTopX");
		boolean toLowerCase = configuration.getBoolean("stopwords.wortschatz.transformStopwordsFromFileToLowerCase");
		String filepathOfLanguageCodes = configuration.getString("stopwords.wortschatz.filepathOfLanguageCodes");
		
		return StopwordRemoverFactory.getStopwordRemoverFromWortschatzData(folderpath, topX, includeSpecialCharsAtBeginningAdditionalToTopX, toLowerCase, filepathOfLanguageCodes);
    }
	
    public static INERLanguageDependent provideNERLanguageDependentViaMap(Configuration configuration){
    	
    	String defaultLanguageToUseIfGivenLanguageNotAvailable = configuration.getString("NER.defaultLanguageToUseIfGivenLanguageNotAvailable");
    	boolean useAllNERMethodsInMapRegardlessGivenLanguage = configuration.getBoolean("NER.useAllGivenNERModelsRegardlessLanguage");

    	
    	Configuration configOpenNLP = configuration.getConfig("NER.opennlp.model.filepaths");
    	Map<String,Set<INER>> mapLanguageToNERs = new HashMap<>();
    	Set<String> languageKeyNames = configOpenNLP.keys();
    	for (String languageKeyName : languageKeyNames) {
    		Logger.info("loading NER models for language \"" + languageKeyName +"\"");
    		Set<String> modelPaths = new HashSet<String>(configOpenNLP.getStringList(languageKeyName));
    		Set<INER> nerSet = new HashSet<>();
    		for (String modelPath : modelPaths) {
				String modelName = modelPath.substring(modelPath.lastIndexOf("/")+1, modelPath.lastIndexOf("."));
	    		String sourceNameToUse = "NER_OPENNLP_" + modelName;
				Logger.info("loading model \"" + modelName +"\"");
				INER ner = new NER_OpenNLP(modelPath, sourceNameToUse);
				nerSet.add(ner);
    		}
    		mapLanguageToNERs.put(languageKeyName, nerSet);
		}
    	
    	return new NERLanguageDependentViaMap(mapLanguageToNERs, defaultLanguageToUseIfGivenLanguageNotAvailable, useAllNERMethodsInMapRegardlessGivenLanguage);
    }
    
    
    public static NLPStorageUtil provideNLPStorageUtil(Configuration configuration){
    	String nlpStoreServiceURL = configuration.getString("nlpstoreservice.baseurl");
    	return new NLPStorageUtil(nlpStoreServiceURL);
    }
    
    public static DBPediaSpotlightUtil provideDBPediaSpotlightUtil(Configuration configuration){
    	String spotlightURL = configuration.getString("dbpediaSpotlight.webservice.url");
    	List<String> spotlightFallBackURLsList = configuration.getStringList("dbpediaSpotlight.webservice.fallbackURLs");
    	String[] spotlightFallBackURLs = new String[spotlightFallBackURLsList.size()];
    	spotlightFallBackURLsList.toArray(spotlightFallBackURLs);
    	return new DBPediaSpotlightUtil(spotlightURL, spotlightFallBackURLs);
    }

    
    public static DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics provideDocFrequencyProviderTypeDependentViaNLPResultStorageService(NLPStorageUtil nlpStorageUtil) throws FileNotFoundException, ClassNotFoundException, IOException {
    	return new DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics(nlpStorageUtil);
    }

   
    public static TagRecommenderTFIDFViaNLStoreFrequencies provideTagRecommenderTFIDFViaNLPStoreFrequencies(Configuration configuration, NLPStorageUtil nlpStorageUtil){
    	
  
    	int minDocsToPerformLanguageDependent = configuration.getInt("tagrecommendation.TFIDF.minDocsToPerformLanguageDependent");
		ITFIDFMerger tfidfMerger = provideTFIDFMerger();
		return new TagRecommenderTFIDFViaNLStoreFrequencies(nlpStorageUtil, minDocsToPerformLanguageDependent, tfidfMerger);
    }
    
    public static TitleBoostSettings provideTitleBoostSettingDefault(){
    	return new TitleBoostSettings(false, -1, true); // no title boost
    }
    
    public static ITFIDFMerger provideTFIDFMerger(){
    	return new TFIDFMerger(true);
    }
        
    

	
}
