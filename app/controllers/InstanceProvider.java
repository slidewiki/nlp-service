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
import services.nlp.dbpediaspotlight.DBPediaSpotlightUtil;
import services.nlp.html.HTMLJsoup;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.languagedetection.LanguageDetector_optimaize;
import services.nlp.ner.INER;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NERLanguageDependentViaMap;
import services.nlp.ner.NER_OpenNLP;
import services.nlp.nlpresultstorage.NLPResultUtil;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.stopwords.StopwordRemoverFactory;
import services.nlp.tfidf.DocFrequencyProviderTypeDependentViaMap;
import services.nlp.tfidf.DocFrequencyProviderViaMap;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tokenization.ITokenizer;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.tokenization.TokenizerLanguageDependentViaMap;
import services.nlp.tokenization.Tokenizer_OpenNLP;

public class InstanceProvider {

	
	public static NLPController provideNLPController(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
		
		IHtmlToText htmlToText = new HTMLJsoup();
		ILanguageDetector languageDetector = new LanguageDetector_optimaize();
		ITokenizerLanguageDependent tokenizer = provideTokenizerLanguageDependentViaMap(configuration);
		IStopwordRemover stopwordRemover = provideStopwordRemoverViaMap(configuration);
		INERLanguageDependent ner = provideNERLanguageDependentViaMap(configuration);
		DBPediaSpotlightUtil dbPediaSpotlightUtil = provideDBPediaSpotlightUtil(configuration);
		IDocFrequencyProviderTypeDependent docFrequencyProvider = provideDocFrequencyProviderSerializedFiles(configuration);
		return new NLPController(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, docFrequencyProvider);
	}
	
	public static NLPComponent provideNLPCompomnent(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
		
		IHtmlToText htmlToText = new HTMLJsoup();
		ILanguageDetector languageDetector = new LanguageDetector_optimaize();
		ITokenizerLanguageDependent tokenizer = provideTokenizerLanguageDependentViaMap(configuration);
		IStopwordRemover stopwordRemover = provideStopwordRemoverViaMap(configuration);
		INERLanguageDependent ner = provideNERLanguageDependentViaMap(configuration);
		DBPediaSpotlightUtil dbPediaSpotlightUtil = provideDBPediaSpotlightUtil(configuration);
		IDocFrequencyProviderTypeDependent docFrequencyProvider = provideDocFrequencyProviderSerializedFiles(configuration);
		return new NLPComponent(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, docFrequencyProvider);
	}
	
	public static NLPController provideNLPController(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil,IDocFrequencyProviderTypeDependent docFrequencyProvider) {
    	return new NLPController(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, docFrequencyProvider);
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
    
    public static DBPediaSpotlightUtil provideDBPediaSpotlightUtil(Configuration configuration){
    	String spotlightURL = configuration.getString("dbpediaSpotlight.webservice.url");
    	List<String> spotlightFallBackURLsList = configuration.getStringList("dbpediaSpotlight.webservice.fallbackURLs");
    	String[] spotlightFallBackURLs = new String[spotlightFallBackURLsList.size()];
    	spotlightFallBackURLsList.toArray(spotlightFallBackURLs);
    	return new DBPediaSpotlightUtil(spotlightURL, spotlightFallBackURLs);
    }
    
    
    public static DocFrequencyProviderTypeDependentViaMap provideDocFrequencyProviderSerializedFiles(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException {
    	Map<String,IDocFrequencyProvider> map = new HashMap<>();
    	//
    	String filepath;
  
      	//=================
    	// new platform (slidewiki2)
    	// ================
    	// tokens language dependent
    	filepath = configuration.getString(NLPResultUtil.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// tokens not language dependent
    	filepath = configuration.getString(NLPResultUtil.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// dbpedia spotlight language dependent
    	filepath = configuration.getString(NLPResultUtil.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPResultUtil.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// dbpedia spotlight not language dependent
    	filepath = configuration.getString(NLPResultUtil.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPResultUtil.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}    	
    	
    	return new DocFrequencyProviderTypeDependentViaMap(map);
    }
	
}
