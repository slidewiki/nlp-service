package services.nlp;

import java.util.List;

public interface INERLanguageDependent {
	List<NlpTag> getNEs(String[] tokens, String language);

}
