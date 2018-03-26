package services.nlp.tfidf;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import services.nlp.microserviceutil.MicroserviceUtil;
import services.nlp.microserviceutil.NLPStorageUtil;

public class TFIDFResultProviderRetrievedViaStoredPrecalculatedTfidfResult implements ITFIDFResultProvider{

	NLPStorageUtil nlpStorageUtil;
	
	
	
	public TFIDFResultProviderRetrievedViaStoredPrecalculatedTfidfResult(NLPStorageUtil nlpStorageUtil) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
	}



	@Override
	public TFIDFResult provideTFIDFResult(String deckId) {
		
		// get precalculated tfidf result from nlp store
		Response responsePrecalculatedTFIDFResult  = nlpStorageUtil.getPrecalculatedTFIDFResult(deckId);
		if(responsePrecalculatedTFIDFResult.getStatus()!=200){
			throw new WebApplicationException("Problem retrieving precalculated tfidf result from nlpStore. Returned status was: " + responsePrecalculatedTFIDFResult.getStatus() + "\"", responsePrecalculatedTFIDFResult);
		}
		JsonNode precalculatedTFIDFResultNode = MicroserviceUtil.getJsonFromMessageBody(responsePrecalculatedTFIDFResult);
		TFIDFResult tfidfResult = Json.fromJson(precalculatedTFIDFResultNode, TFIDFResult.class);

		return tfidfResult;
	}

}
