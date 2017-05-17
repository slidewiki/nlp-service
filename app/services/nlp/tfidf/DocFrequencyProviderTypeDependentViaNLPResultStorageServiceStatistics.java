package services.nlp.tfidf;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;

public class DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics implements IDocFrequencyProviderTypeDependent{

	private Map<String,String>  mapSupportedTypesToPaths;
	private NLPStorageUtil nlpStorageUtil;

	public DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics(Map<String, String> mapSupportedTypesToPaths,
			NLPStorageUtil nlpStorageUtil) {
		super();
		this.mapSupportedTypesToPaths = mapSupportedTypesToPaths;
		this.nlpStorageUtil = nlpStorageUtil;
	}

	
	public DocFrequencyProviderTypeDependentViaNLPResultStorageServiceStatistics(NLPStorageUtil nlpStorageUtil){
		this.nlpStorageUtil = nlpStorageUtil;
		initializeDefaultPaths();
		
	}
	
	private void initializeDefaultPaths(){
		this.mapSupportedTypesToPaths = new HashMap<>();
		this.mapSupportedTypesToPaths.put(NLPResultUtil.propertyNameDocFreqProvider_Tokens, NLPResultUtil.propertyNameWordFrequenciesExclStopwords + "." + NLPResultUtil.propertyNameInFrequencyEntriesForWord); // might be also "children.tokens but in the word frequenices they are toLowercase and stopwords removed"
		this.mapSupportedTypesToPaths.put(NLPResultUtil.propertyNameDocFreqProvider_NamedEntities, NLPResultUtil.propertyNameNERFrequencies + "." + NLPResultUtil.propertyNameInFrequencyEntriesForWord); 
		this.mapSupportedTypesToPaths.put(NLPResultUtil.propertyNameDocFreqProvider_SpotlightURI, NLPResultUtil.propertyNameDBPediaSpotlightURIFrequencies + "." + NLPResultUtil.propertyNameInFrequencyEntriesForWord );
		this.mapSupportedTypesToPaths.put(NLPResultUtil.propertyNameDocFreqProvider_SpotlightSurfaceForm, NLPResultUtil.propertyNameDBPediaSpotlight + ".Resources.@surfaceForm");

	}
	
	@Override
	public boolean supportsType(String type) {
		return this.mapSupportedTypesToPaths.containsKey(type);
	}

	@Override
	public Integer getDocFrequency(String type, String term, String language) {
		
		String languageToUse = language;
		if(language!=null && language.length()==0){
			languageToUse = null;
		}
		if(!supportsType(type)){
			throw new IllegalArgumentException("Given type \"" + type + "\" is not supported");
		}
		
		String path = mapSupportedTypesToPaths.get(type);
		
		Response response = this.nlpStorageUtil.getStatisticsDeckCount(path, term, languageToUse);
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException(response); // TODO: handle this exception in NLPController, better output (meessage here)
		}
		
		Integer result =NLPStorageUtil.getIntegerFromMessageBody(response);
		return result;
	}

	@Override
	public Integer getNumberOfAllDocs(String language) {
		String languageToUse = language;
		if(language!=null && language.length()==0){
			languageToUse = null;
		}
		Response response = this.nlpStorageUtil.getStatisticsDeckCount(null, null, languageToUse);
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException(response); // TODO: handle this exception in NLPController, better output (meessage here)
		}
		
		Integer result =NLPStorageUtil.getIntegerFromMessageBody(response);
		return result;
	}


	public Map<String, String> getMapSupportedTypesToPaths() {
		return mapSupportedTypesToPaths;
	}


}
