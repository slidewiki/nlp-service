package services.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.util.Span;

public class NERLanguageDependentViaMap implements INERLanguageDependent {

    private Map<String,Set<INER>> mapLanguageToNERs;
	private String defaultLanguageToUseIfGivenLanguageNotAvailable;
	boolean useAllNERMethodsInMapRegardlessGivenLanguage;
	
	public NERLanguageDependentViaMap(Map<String,Set<INER>> mapLanguageToNERs,
			String defaultLanguageToUseIfGivenLanguageNotAvailable, 
			boolean useAllNERMethodsInMapRegardlessGivenLanguage) {
		
		if(!useAllNERMethodsInMapRegardlessGivenLanguage && !mapLanguageToNERs.containsKey(defaultLanguageToUseIfGivenLanguageNotAvailable)){
			throw new IllegalArgumentException("The value set for the parameter defaultLanguageToUseIfGivenLanguageNotAvailable must be availble in the map! Please add a an entry to the map for the set default language " + defaultLanguageToUseIfGivenLanguageNotAvailable + " or adjust default value to an existing entry of the map");
		}
		this.mapLanguageToNERs = mapLanguageToNERs;
		this.defaultLanguageToUseIfGivenLanguageNotAvailable = defaultLanguageToUseIfGivenLanguageNotAvailable;
		this.useAllNERMethodsInMapRegardlessGivenLanguage = useAllNERMethodsInMapRegardlessGivenLanguage;
	}



	@Override
	public List<NlpTag> getNEs(String[] tokens, String language) {
	  	
		Set<INER> nersToUse;
		if(useAllNERMethodsInMapRegardlessGivenLanguage){
			nersToUse = new HashSet<>();
			Set<String> languages = this.mapLanguageToNERs.keySet();
			for (String lang : languages) {
				nersToUse.addAll(mapLanguageToNERs.get(lang));
			}
		} else if(this.mapLanguageToNERs.containsKey(language)){
    		nersToUse = mapLanguageToNERs.get(language);
    	}else{
    		nersToUse = mapLanguageToNERs.get(defaultLanguageToUseIfGivenLanguageNotAvailable);
    	}
    	
		List<NlpTag> nerResults = new ArrayList<>();
    	for (INER ner : nersToUse) {
			Span[] neSpans = ner.getNEs(tokens);
			List<NlpTag> listCurrentNER = NlpTag.fromSpans(neSpans, tokens, ner.getName());
			nerResults.addAll(listCurrentNER);
		}
    	return nerResults;

	}

}
