package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.tfidf.ITFIDFMerger;
import services.nlp.tfidf.TitleBoostSettings;

/**
 * Deprecated!
 * Tag Recommender uses tfidf results stored in NLP result. This is deprecated since tfidf values are not stored in nlp result anymore.
 * Title boost settings do not have any effect!
 * @author aschlaf
 *
 */
@Deprecated
public class TagRecommenderTFIDFStoredInNLPResult implements ITagRecommender {

	private NLPStorageUtil nlpStorageUtil;
	private ITFIDFMerger tfidfMerger;

	
	
	
	public TagRecommenderTFIDFStoredInNLPResult(NLPStorageUtil nlpStorageUtil) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
	}

	

	@Override
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int maxEmtriesToReturn) {
		
		Response response = nlpStorageUtil.getNLPResultForDeckId(deckId);
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException("Problem while getting nlp result via nlp store service for deck id " + deckId + ". The nlp store service responded with status " + status + " (" + response.getStatusInfo() + ")", response);

		}
		JsonNode nlpResult = MicroserviceUtil.getJsonFromMessageBody(response);
		
		
		
		Map<String,Map<String,Double>> mapProviderNameToTFIDFEnntries = NLPResultUtil.getTFIDFEntries(nlpResult);
		
		if(mapProviderNameToTFIDFEnntries == null){
			throw new ProcessingException("No TFIDF results could be retrieved for deck id " + deckId + " from nlp result. Either tfidf wasn't performed or deck has no content. You can recheck via nlp store service.");

		}
		
		if(mapProviderNameToTFIDFEnntries.size()==0){
			return new ArrayList<>();
		}
					
		
		// merge tfidf values
		List<NlpTag> tfidfmerged = tfidfMerger.mergeTFIDFValuesOfDifferentProviders(mapProviderNameToTFIDFEnntries);

		
		List<NlpTag> result = TagRecommendationFilter.filter(tfidfmerged, termFilterSettings, true, maxEmtriesToReturn);
		
		return result;
	}


	
}
