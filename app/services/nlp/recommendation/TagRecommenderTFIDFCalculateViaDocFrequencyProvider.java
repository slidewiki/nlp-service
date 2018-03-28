package services.nlp.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;

import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPResultUtil;
import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.tfidf.IDocFrequencyProviderTypeDependent;
import services.nlp.tfidf.ITFIDFMerger;
import services.nlp.tfidf.TFIDF;
import services.nlp.tfidf.TitleBoostSettings;

/**
 * Deprecated! Use {@link TagRecommenderTFIDFViaNLStoreFrequencies} instead !
 * Tag Recommendations based on tfidf and docfrequency provider which needs to be updated from time to time to be up-to-date.
 * Title boost won't have any effect!
 * @author aschlaf
 *
 */
public class TagRecommenderTFIDFCalculateViaDocFrequencyProvider implements ITagRecommender{

	private NLPStorageUtil nlpStorageUtil;
	private IDocFrequencyProviderTypeDependent docFrequencyProvider;
	private int maxEntriesToReturnForTFIDF;
	private ITFIDFMerger tfidfMerger;

	
	
	
	public TagRecommenderTFIDFCalculateViaDocFrequencyProvider(NLPStorageUtil nlpStorageUtil,
			IDocFrequencyProviderTypeDependent docFrequencyProvider, int maxEntriesToReturnForTFIDF, ITFIDFMerger tfidfMerger) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.docFrequencyProvider = docFrequencyProvider;
		this.maxEntriesToReturnForTFIDF = maxEntriesToReturnForTFIDF;
		this.tfidfMerger = tfidfMerger;
	}

	@Override
	public List<NlpTag> getTagRecommendations(String deckId, TitleBoostSettings titleBoostSettings, TermFilterSettings termFilterSettings, int tfidfMinDocsToPerformLanguageDependent, int maxEntriesToReturn) {		
		
		// get nlp result for deck id
		Response response = nlpStorageUtil.getNLPResultForDeckId(deckId);
		
		int status = response.getStatus();
		if(status != 200){
			throw new WebApplicationException("Problem while getting nlp result via nlp store service for deck id " + deckId + ". The nlp store service responded with status " + status + " (" + response.getStatusInfo() + ")", response);

		}
		ObjectNode nlpResult = (ObjectNode) MicroserviceUtil.getJsonFromMessageBody(response);
		
		
		// calc tfidf
		ObjectNode tfidf = TFIDF.getTFIDFViaNLPResultAndDocFreqProvider(nlpResult, docFrequencyProvider, tfidfMinDocsToPerformLanguageDependent, maxEntriesToReturnForTFIDF);
		Map<String, Map<String, Double>> mapProviderNameToTFIDFEnntries = NLPResultUtil.getTFIDFEntries(tfidf);
		
		
		if(mapProviderNameToTFIDFEnntries == null){
			throw new ProcessingException("No TFIDF results could be retrieved for deck id " + deckId + " from nlp result. Either tfidf wasn't performed or deck has no content. You can recheck via nlp store service.");

		}
		
		if(mapProviderNameToTFIDFEnntries.size()==0){
			return new ArrayList<>();
		}
		
		// merge tfidf values
		List<NlpTag> tfidfmerged = tfidfMerger.mergeTFIDFValuesOfDifferentProviders(mapProviderNameToTFIDFEnntries);

		// filter list
		List<NlpTag> result = TagRecommendationFilter.filter(tfidfmerged, termFilterSettings, true, maxEntriesToReturn);
		
		
		return result;
	}



	
}
