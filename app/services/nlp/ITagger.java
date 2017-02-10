package services.nlp;

import java.util.List;

public interface ITagger {
    
	String getTagsAsString(String input);
    List<NlpTag> getTags(String input);
}
