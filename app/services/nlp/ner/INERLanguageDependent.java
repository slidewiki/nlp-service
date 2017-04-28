package services.nlp.ner;

import java.util.List;

public interface INERLanguageDependent {
	List<NerAnnotation> getNEs(String[] tokens, String language);

}
