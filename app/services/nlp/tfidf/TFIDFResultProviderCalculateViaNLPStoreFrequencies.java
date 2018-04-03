package services.nlp.tfidf;

import services.nlp.microserviceutil.NLPStorageUtil;
import services.nlp.recommendation.TermFilterSettings;

public class TFIDFResultProviderCalculateViaNLPStoreFrequencies implements ITFIDFResultProvider {

	private NLPStorageUtil nlpStorageUtil;
	private int minDocsToPerformLanguageDependent;
	private TitleBoostSettings titleBoostSettings;
	private TermFilterSettings termFilterSettings;

	
	
	public TFIDFResultProviderCalculateViaNLPStoreFrequencies(NLPStorageUtil nlpStorageUtil,
			int minDocsToPerformLanguageDependent, TitleBoostSettings titleBoostSettings,
			TermFilterSettings termFilterSettings) {
		super();
		this.nlpStorageUtil = nlpStorageUtil;
		this.minDocsToPerformLanguageDependent = minDocsToPerformLanguageDependent;
		this.titleBoostSettings = titleBoostSettings;
		this.termFilterSettings = termFilterSettings;
		
	}



	@Override
	public TFIDFResult provideTFIDFResult(String deckId) {
	
		return TFIDF.getTFIDFViaNLPStoreFrequencies(nlpStorageUtil, deckId, minDocsToPerformLanguageDependent, titleBoostSettings, termFilterSettings);
	}

}
