import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import controllers.NLPController;
import play.Configuration;
import play.Logger;
import services.nlp.ITagger;
import services.nlp.NLPComponent;
import services.nlp.TaggerComponent;
import services.nlp.dbpediaspotlight.DBPediaSpotlightUtil;
import services.nlp.html.HTMLJsoup;
import services.nlp.html.IHtmlToText;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.languagedetection.LanguageDetector_optimaize;
import services.nlp.ner.INER;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NERLanguageDependentViaMap;
import services.nlp.ner.NER_OpenNLP;
import services.nlp.stopwords.IStopwordRemover;
import services.nlp.stopwords.StopwordRemover_None;
import services.nlp.tfidf.DocFrequencyProviderViaMap;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tokenization.ITokenizer;
import services.nlp.tokenization.ITokenizerLanguageDependent;
import services.nlp.tokenization.TokenizerLanguageDependentViaMap;
import services.nlp.tokenization.Tokenizer_OpenNLP;

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
public class Module extends AbstractModule {

    @Override
    public void configure() {
        
        bind(ILanguageDetector.class).to(LanguageDetector_optimaize.class);
        bind(ITokenizerLanguageDependent.class).to(TokenizerLanguageDependentViaMap.class);
//      bind(ITokenizer.class).to(Tokenizer_OpenNLP.class);
        bind(INERLanguageDependent.class).to(NERLanguageDependentViaMap.class);

        bind(ITagger.class).to(TaggerComponent.class);
        bind(IHtmlToText.class).to(HTMLJsoup.class);
        bind(IStopwordRemover.class).to(StopwordRemover_None.class);

    }

    @Provides
    public TokenizerLanguageDependentViaMap provideTokenizerLanguageDependentViaMap(Configuration configuration) {
        
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

    
    @Provides
    public NERLanguageDependentViaMap provideNERLanguageDependentViaMap(Configuration configuration){
    	    	
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
    
    @Provides
    public DBPediaSpotlightUtil provideDBPediaSpotlightUtil(Configuration configuration){
    	String spotlightURL = configuration.getString("dbpediaSpotlight.webservice.url");
    	List<String> spotlightFallBackURLsList = configuration.getStringList("dbpediaSpotlight.webservice.fallbackURLs");
    	String[] spotlightFallBackURLs = new String[spotlightFallBackURLsList.size()];
    	spotlightFallBackURLsList.toArray(spotlightFallBackURLs);
    	return new DBPediaSpotlightUtil(spotlightURL, spotlightFallBackURLs);
    }
    
//    @Provides
//    public IDocFrequencyProvider provideDocFreqencyProvider(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
//    	
//    	String filepathDocFreqProvider = configuration.getString("tfidf.filepathDocumentFrequency");
//		IDocFrequencyProvider docFrequencyProvider = DocFrequencyProviderViaMap.deserializeFromFile(filepathDocFreqProvider );
//		return docFrequencyProvider;
//    }
    
    
    @Provides
    public Map<String,IDocFrequencyProvider> provideDocFrequencyProviderMap(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException {
    	Map<String,IDocFrequencyProvider> map = new HashMap<>();
    	//
    	String filepath;
    	
//    	//=================
//    	// old platform (slidewiki1)
//    	// ================
//    	// tokens language dependent
//    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent);
//    	Logger.info("loading " + filepath);
//    	if(filepath !=null && filepath.trim().length()>0){
//        	map.put(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
//    	}
//    	// tokens not language dependent
//    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent);
//    	Logger.info("loading " + filepath);
//    	if(filepath !=null && filepath.trim().length()>0){
//        	map.put(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki1_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
//    	}
//    	// dbpedia spotlight language dependent
//    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent);
//    	Logger.info("loading " + filepath);
//    	if(filepath !=null && filepath.trim().length()>0){
//        	map.put(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
//    	}
//    	// dbpedia spotlight not language dependent
//    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent);
//    	Logger.info("loading " + filepath);
//    	if(filepath !=null && filepath.trim().length()>0){
//        	map.put(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki1_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
//    	}

  
      	//=================
    	// new platform (slidewiki2)
    	// ================
    	// tokens language dependent
    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// tokens not language dependent
    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPComponent.propertyNameDocFreqProvider_Tokens_SlideWiki2_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// dbpedia spotlight language dependent
    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_languageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}
    	// dbpedia spotlight not language dependent
    	filepath = configuration.getString(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent);
    	Logger.info("loading " + filepath);
    	if(filepath !=null && filepath.trim().length()>0){
        	map.put(NLPComponent.propertyNameDocFreqProvider_Spotlight_SlideWiki2_perDeck_notlanguageDependent, DocFrequencyProviderViaMap.deserializeFromFile(filepath));
    	}    	
    	
    	return map;
    }
 
    @Provides
    public NLPController provideNLPController(IHtmlToText htmlToText, ILanguageDetector languageDetector, ITokenizerLanguageDependent tokenizer, IStopwordRemover stopwordRemover,
			INERLanguageDependent ner, DBPediaSpotlightUtil dbPediaSpotlightUtil, Map<String,IDocFrequencyProvider> mapDocFrequencyProvider) {
    	return new NLPController(htmlToText, languageDetector, tokenizer, stopwordRemover, ner, dbPediaSpotlightUtil, mapDocFrequencyProvider);
    }


}
