package services.nlp.tfidf;

import java.util.List;
import java.util.Map;

import services.nlp.recommendation.NlpTag;

public interface ITFIDFMerger {

	public List<NlpTag> mergeTFIDFValuesOfDifferentProviders(Map<String,Map<String,Double>> mapProviderToTFIDFValues);

}
