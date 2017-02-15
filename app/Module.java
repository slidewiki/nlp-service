import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import play.Configuration;
import play.Logger;
import services.nlp.ITagger;
import services.nlp.TaggerComponent;
import services.nlp.languagedetection.ILanguageDetector;
import services.nlp.languagedetection.LanguageDetector_optimaize;
import services.nlp.ner.INER;
import services.nlp.ner.INERLanguageDependent;
import services.nlp.ner.NERLanguageDependentViaMap;
import services.nlp.ner.NER_OpenNLP;
import services.nlp.tfidf.DocumentFrequencyProviderViaMap;
import services.nlp.tfidf.IDocFrequencyProvider;
import services.nlp.tfidf.TFIDF;
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
//        bind(ITokenizer.class).to(Tokenizer_OpenNLP.class);
        bind(INERLanguageDependent.class).to(NERLanguageDependentViaMap.class);

       bind(ITagger.class).to(TaggerComponent.class);

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
    public TFIDF provideTFIDF(Configuration configuration) throws FileNotFoundException, ClassNotFoundException, IOException{
    	
    	String filepathDocFreqProvider = configuration.getString("tfidf.filepathDocumentFrequency");
		IDocFrequencyProvider docFrequencyProvider = DocumentFrequencyProviderViaMap.deserializeFromFile(filepathDocFreqProvider );
		return new TFIDF(docFrequencyProvider);
    }

}
