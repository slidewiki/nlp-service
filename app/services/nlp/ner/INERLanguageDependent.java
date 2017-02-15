package services.nlp.ner;

import java.util.List;

import services.nlp.NlpTag;

public interface INERLanguageDependent {
	List<NlpTag> getNEs(String[] tokens, String language);

}
