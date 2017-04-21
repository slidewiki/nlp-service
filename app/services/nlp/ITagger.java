package services.nlp;

import java.util.List;

public interface ITagger {
    
    List<NlpAnnotation> getTags(String input);
}
