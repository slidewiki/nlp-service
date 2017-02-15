package services.nlp.tokenization;

import java.util.Map;

public class TokenizerLanguageDependentViaMap implements ITokenizerLanguageDependent {

	
	private Map<String,ITokenizer> tokenizerMap;
	private String defaultLanguageToUseIfGivenLanguageNotAvailable;
	

	public TokenizerLanguageDependentViaMap(Map<String, ITokenizer> tokenizerMap,
			String defaultLanguageToUseIfGivenLanguageNotAvailable) {
		
		if(!tokenizerMap.containsKey(defaultLanguageToUseIfGivenLanguageNotAvailable)){
			throw new IllegalArgumentException("The value set for the parameter defaultLanguageToUseIfGivenLanguageNotAvailable must be availble in the map! Please add a an entry to the map for the set default language " + defaultLanguageToUseIfGivenLanguageNotAvailable + " or adjust default value to an existing entry of the map");
		}
		this.tokenizerMap = tokenizerMap;
		this.defaultLanguageToUseIfGivenLanguageNotAvailable = defaultLanguageToUseIfGivenLanguageNotAvailable;
		
	}


	@Override
	public String[] tokenize(String input, String language) {
	  	ITokenizer tokenizerToUse;
    	if(this.tokenizerMap.containsKey(language)){
    		tokenizerToUse = tokenizerMap.get(language);
    	}else{
    		tokenizerToUse = tokenizerMap.get(defaultLanguageToUseIfGivenLanguageNotAvailable);
    	}
    	
    	String[] tokens = tokenizerToUse.tokenize(input);
    	return tokens;
	}

}
