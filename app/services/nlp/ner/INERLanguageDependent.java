package services.nlp.ner;

import java.util.List;

import services.nlp.NlpAnnotation;

public interface INERLanguageDependent {
	List<NlpAnnotation> getNEs(String[] tokens, String language);

}
